CREATE TABLE IF NOT EXISTS storages (
    id           SERIAL PRIMARY KEY,
    storage_type VARCHAR(20)  NOT NULL,
    bucket       VARCHAR(255) NOT NULL,
    path         VARCHAR(255) NOT NULL
);
