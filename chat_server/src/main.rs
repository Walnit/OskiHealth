use std::collections::HashMap;
use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};
use std::path::Path;
use std::sync::Mutex;
use std::time::{SystemTime, UNIX_EPOCH};

use database::two_string_hash;
use reqwest::Client;
use rocket::{State, Shutdown};
use rocket::form::Form;
use rocket::fs::NamedFile;
use rocket::http::Status;
use rocket::request::{FromRequest, Outcome};
use rocket::response::stream::{EventStream, Event};
use rocket::serde::json::Json;
use rocket_db_pools::sqlx::{self, SqlitePool, Row};
use rocket_db_pools::{Connection, Database};
use rocket::serde::{Deserialize, Serialize};
use rocket::tokio::sync::broadcast::{channel, Sender, error::RecvError};
use rocket::tokio::select;

#[macro_use] extern crate rocket;
#[macro_use] mod database;

#[derive(Database)]
#[database("sqlite")]
pub struct ChatDB(SqlitePool);

#[derive(Deserialize, Serialize)] struct User { username: String, psych: bool }

#[derive(Serialize, Clone)]
struct Message {
    content: String,
    sender: String,
    recipient: String,
    timestamp: i64,
}

#[rocket::async_trait]
impl<'r> FromRequest<'r> for User {
    type Error = ();

    async fn from_request(request: &'r rocket::Request<'_>) -> Outcome<Self, Self::Error> {
        let mut db = request.guard::<Connection<ChatDB>>()
            .await.succeeded().unwrap();
        let username = match request.headers().get_one("X-Username") {
            Some(n) => n,
            None => return Outcome::Failure((Status::BadRequest, ()))
        };
        let password = match request.headers().get_one("X-Password") {
            Some(n) => n,
            None => return Outcome::Failure((Status::BadRequest, ()))
        };

        let details: Result<(String, u8), _> = query_gen!(
            sqlx::query("SELECT password, psych FROM users WHERE username = ?")
            .bind(username), &mut *db).and_then(|row|
                Ok((row.try_get(0)?, row.try_get(1)?)));
        
        if let Ok((hash, psych)) = details {
            let mut hasher = DefaultHasher::new();
            password.hash(&mut hasher);
            if hash == hasher.finish().to_string() {
                return Outcome::Success(User {username: username.to_string(), psych: psych == 1})
            }
        }
        Outcome::Failure((Status::Forbidden, ()))
    }
}

struct MessageChannels { map: Mutex<HashMap<u64, Sender<Message>>> }
#[derive(FromForm)] struct SubscribeForm { username: String }
#[post("/subscribe", data = "<form>")]
async fn subscribe(form: Form<SubscribeForm>, user: User,
        state: &State<MessageChannels>, mut end: Shutdown) -> Option<EventStream![]> {

    let convo_hash = two_string_hash(&form.username, &user.username);
    let mut lock = state.map.lock().expect("lock issue");
    let channel = match lock.get(&convo_hash) {
        Some(val) => val,
        _ => {
            let channel = channel::<Message>(1024).0;
            lock.insert(convo_hash, channel);
            lock.get(&convo_hash).expect("what the fuck?!?!?")
        }
    };
    let mut rx = channel.subscribe();
    Some(EventStream! {
        loop {
            let msg = select! {
                msg = rx.recv() => match msg {
                    Ok(msg) => msg,
                    Err(RecvError::Closed) => break,
                    Err(RecvError::Lagged(_)) => continue,
                },
                _ = &mut end => break,
            };

            yield Event::json(&msg);
        }
    })
}

#[derive(FromForm)] struct MessageRequest { recipient: String, content: String }
#[post("/send", data = "<form>")]
async fn send_message(form: Form<MessageRequest>, user: User,
        mut db: Connection<ChatDB>, state: &State<MessageChannels>) -> Option<String> {

    if !user_exists!(&form.recipient, &mut *db) { return None }
    if form.content.trim().len() == 0 { return None }
    if form.recipient == user.username { return None }
    let http_string = serde_urlencoded::to_string(
        &[("message", &form.content)]
    ).unwrap();
    let rating = reqwest::get("http://localhost:5000/nlp?".to_owned() + &http_string)
        .await.ok()?.text().await.ok();

    let timestamp: i64 = SystemTime::now().duration_since(UNIX_EPOCH).unwrap()
        .as_millis().try_into().unwrap();
    
    // add to database
    let _ = query_gen!(sqlx::query(
        "INSERT INTO messages VALUES(?, ?, ?, ?)"
    ).bind(&user.username).bind(&form.recipient).bind(&form.content).bind(&timestamp), &mut *db);

    // send it down the eventstream
    let convo_hash = two_string_hash(&form.recipient, &user.username);
    let mut lock = state.map.lock().expect("lock issue");
    let sender = match lock.get(&convo_hash) {
        Some(val) => val,
        _ => {
            let channel = channel::<Message>(1024).0;
            lock.insert(convo_hash, channel);
            lock.get(&convo_hash).expect("what the fuck?!?!?")
        }
    };

    // A send 'fails' if there are no active subscribers. That's okay.
    let _ = sender.send(Message {
        content: form.content.clone(),
        sender: user.username,
        recipient: form.recipient.clone(),
        timestamp
    });
    
    rating
}


#[derive(FromForm)] struct MessagesQuery { username: String }
#[derive(Serialize, Clone)]
struct NetworkMessage {
    sender: String,
    content: String,
    timestamp: i64
}
#[post("/messages", data = "<form>")]
async fn all_messages(form: Form<MessagesQuery>, user: User,
        mut db: Connection<ChatDB>) -> Option<Json<Vec<NetworkMessage>>> {
            
    let query = match query_all!(sqlx::query(
            "SELECT sender, content, timestamp FROM messages WHERE (sender = ? AND recipient = ?) OR (sender = ? AND recipient = ?)"
        ).bind(&form.username).bind(&user.username)
        .bind(&user.username).bind(&form.username), &mut *db) {
            Ok(val) => val,
            _ => return None
        };
    Some(Json(query.iter().map(|row|
        NetworkMessage {
            sender: row.try_get(0).unwrap(),
            content: row.try_get(1).unwrap(),
            timestamp: row.try_get(2).unwrap()
        }
    ).collect()))
}

#[derive(FromForm)] struct UserForm { username: String, password: String }
#[post("/create-user", data = "<form>")]
async fn create_user(form: Form<UserForm>, mut db: Connection<ChatDB>) -> &'static str {
    let mut hasher = DefaultHasher::new();
    form.password.hash(&mut hasher);
    let query = sqlx::query("INSERT INTO users VALUES (?, ?, ?)"
        ).bind(&form.username).bind(hasher.finish().to_string()).bind(0);
    if query.execute(&mut *db).await.is_ok() { return "good" }
    "fail"
}

#[post("/get-help")]
async fn get_help(user: User, mut db: Connection<ChatDB>) -> Option<String> {
    let psych: String = query_one!(sqlx::query(
        "SELECT username FROM users WHERE psych == 1 ORDER BY RANDOM() LIMIT 1"
    ), &mut *db)?;
    
    let timestamp: i64 = SystemTime::now().duration_since(UNIX_EPOCH).unwrap()
        .as_millis().try_into().unwrap();
    let _ = query_gen!(sqlx::query(
        "INSERT INTO messages VALUES(?, ?, 'I''m a verified psychiatrist here to help improve your mental health and well-being. Let''s work together to develop strategies to cope with difficult emotions, manage stress, and achieve your goals.', ?)"
    ).bind(&psych).bind(&user.username).bind(&timestamp), &mut *db);
    Some(psych)
}

const MAGIC_QUERY: &str = "SELECT DISTINCT CASE WHEN c.person1 = u.username THEN c.person1 ELSE c.person2 END AS correspondent, u.psych FROM conversations c INNER JOIN users u ON u.username IN (c.person1, c.person2) AND u.username <> ? WHERE ? IN (c.person1, c.person2)";
#[get("/my-chats")]
async fn conversations(user: User, mut db: Connection<ChatDB>) -> Option<Json<Vec<User>>> {
    let query = match query_all!(sqlx::query(MAGIC_QUERY)
        .bind(&user.username).bind(&user.username), &mut *db) {
            Ok(val) => val,
            _ => return None
        };
    Some(Json(query.iter().map(|row|
        User {
            username: row.get(0),
            psych: row.get::<u8, usize>(1) == 1
        }
    ).collect()))
}

#[derive(Serialize, Deserialize)] struct ChatData { role: String, content: String }
#[post("/chatgpt", data = "<json>")]
async fn chatgpt(json: Json<Vec<ChatData>>) -> Option<String> {
    let client = Client::new();
    let response = client.post("http://localhost:5000/chatgpt")
        .header("Content-Type", "application/json")
        .body(serde_json::to_string(&json.0).unwrap())
        .send().await.ok()?;
    response.text().await.ok()
}

#[get("/login")] fn login(_user: User) -> &'static str { "ok" }
#[get("/")] async fn index() -> Option<NamedFile> { NamedFile::open(Path::new("index.html")).await.ok() }

#[launch]
fn rocket() -> _ {
    rocket::build()
        .attach(ChatDB::init())
        .manage(MessageChannels{map: Mutex::new(HashMap::new())})
        .mount("/", routes![index, conversations, all_messages, get_help,
            send_message, subscribe, create_user, login, chatgpt])
}