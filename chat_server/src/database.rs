use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};

#[macro_export]
macro_rules! query_one {
    ($query:expr, $conn:expr) => {{
        $query.fetch_one($conn).await
            .and_then(|r| Ok(r.try_get(0)?)).ok()
    }};
}

#[macro_export]
macro_rules! query_gen {
    ($query:expr, $conn:expr) => {{
        $query.fetch_one($conn).await
    }};
}

#[macro_export]
macro_rules! query_all {
    ($query:expr, $conn:expr) => {{
        $query.fetch_all($conn).await
    }};
}

#[macro_export]
macro_rules! user_exists {
    ($username:expr, $conn:expr) => {{
        sqlx::query("SELECT psych FROM users WHERE username = ?")
            .bind($username).fetch_one($conn).await.is_ok()
    }};
}

pub fn two_string_hash(str1: &str, str2: &str) -> u64 {
    let mut hasher = DefaultHasher::new();
    if str1 < str2 {
        str1.hash(&mut hasher);
        str2.hash(&mut hasher);
    } else {
        str2.hash(&mut hasher);
        str1.hash(&mut hasher);
    }
    hasher.finish()
}