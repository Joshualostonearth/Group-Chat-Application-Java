--SQL Databases Queries I used to make my tables

--Users Table Query:
CREATE TABLE users (
    id INT,
    username VARCHAR(255),
    password VARCHAR(255)
);

--Chat History Table Query:
CREATE TABLE messages (
    time DATETIME,
    name VARCHAR(255),
    content TEXT
);

