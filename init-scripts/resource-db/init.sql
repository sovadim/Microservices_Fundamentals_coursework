CREATE TABLE IF NOT EXISTS resources (
    id             SERIAL PRIMARY KEY,
    s3_key         VARCHAR     NOT NULL,
    storage_type   VARCHAR(20),
    storage_id     INTEGER,
    storage_bucket VARCHAR(255)
);
