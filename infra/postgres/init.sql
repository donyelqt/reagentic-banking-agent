-- Create the per-service databases (single Postgres instance, 6 DBs).
CREATE DATABASE auth;
CREATE DATABASE account;
CREATE DATABASE payment;
CREATE DATABASE ledger;
CREATE DATABASE notification;
CREATE DATABASE transaction_db;
