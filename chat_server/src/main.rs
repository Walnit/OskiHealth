use std::collections::HashMap;
use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};
use std::path::Path;
use std::sync::Mutex;
use std::time::{SystemTime, UNIX_EPOCH};

use database::two_string_hash;
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

#[derive(Deserialize, Serialize)]
struct User {
    username: String,
    psych: bool
}

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

        let details: Result<(String, i32), _> = query_gen!(
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

// the fucking eventstream thing
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


// MESSAGING
#[derive(FromForm)] struct MessageRequest { recipient: String, content: String, }
#[post("/send", data = "<form>")]
async fn send_message(form: Form<MessageRequest>, user: User,
        mut db: Connection<ChatDB>, state: &State<MessageChannels>) -> &'static str {

    let timestamp: i64 = SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis()
        .try_into().unwrap();
    
    // INSERT INTO messages VALUES(uuid, content, sender, signature, timestamp, hash)
    let _ = query_gen!(sqlx::query(
        "INSERT INTO messages VALUES(?, ?, ?, ?)"
    ).bind(&user.username).bind(&form.recipient).bind(&form.content)
    .bind(&timestamp), &mut *db);

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
    "ok"
}


/* im using a form bcos i cant find a way to get parameters from a GET request
which also has to be validated by the User FromRequest thing
works without parameters tho */
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
            "SELECT timestamp, content, sender FROM messages WHERE (sender = ? AND recipient = ?) OR (sender = ? AND recipient = ?)"
        ).bind(&form.username).bind(&user.username)
        .bind(&user.username).bind(&form.username), &mut *db) {
            Ok(val) => val,
            _ => return None
        };
    Some(Json(query.iter().map(|row| message_from_row!(row)).collect()))
}

#[derive(FromForm)] struct UserForm { username: String, password: String }
#[post("/create-user", data = "<form>")]
async fn create_user(form: Form<UserForm>, mut db: Connection<ChatDB>) -> &'static str {
    let mut hasher = DefaultHasher::new();
    form.password.hash(&mut hasher);
    let query = sqlx::query("INSERT INTO users VALUES (?, ?, ?)"
        ).bind(&form.username).bind(hasher.finish().to_string()).bind(0);
    if query.execute(&mut *db).await.is_ok() {
        return "good"
    }
    "fail"
}

#[get("/my-chats")]
async fn conversations(user: User, mut db: Connection<ChatDB>) -> Option<Json<Vec<String>>> {
    let query = match query_all!(sqlx::query(
        "SELECT DISTINCT CASE WHEN person1 = ? THEN person2 ELSE person1 END AS correspondent FROM conversations WHERE person1 = ? OR person2 = ?"
    ).bind(&user.username).bind(&user.username).bind(&user.username), &mut *db) {
            Ok(val) => val,
            _ => return None
        };
    Some(Json(query.iter().map(|row| row.get(0)).collect()))
}

#[get("/")]
async fn index() -> Option<NamedFile> {
    NamedFile::open(Path::new("index.html")).await.ok()
}

#[launch]
fn rocket() -> _ {
    rocket::build()
        .attach(ChatDB::init())
        .manage(MessageChannels{map: Mutex::new(HashMap::new())})
        .mount("/", routes![index, conversations, all_messages,
            send_message, subscribe, create_user])
}