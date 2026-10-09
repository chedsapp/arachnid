CREATE TABLE IF NOT EXISTS frontier (
    url         TEXT PRIMARY KEY,
    depth       INTEGER NOT NULL,
    source      TEXT NOT NULL,
    eligible_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS visited (
    url           TEXT PRIMARY KEY,
    last_visited  TEXT NOT NULL,
    visit_count   INTEGER NOT NULL DEFAULT 1,
    etag          TEXT,
    last_modified TEXT
);

CREATE TABLE IF NOT EXISTS robots_cache (
    host       TEXT PRIMARY KEY,
    rules_blob BLOB NOT NULL,
    fetched_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY,
    applied_at TEXT NOT NULL
);

