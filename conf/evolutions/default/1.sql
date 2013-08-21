# Tasks schema
 
# --- !Ups
CREATE SEQUENCE cat_id_seq;
CREATE TABLE category (
	id integer DEFAULT nextval('cat_id_seq'),
    name varchar(255) UNIQUE NOT NULL,
    ordering integer NOT NULL,
    PRIMARY KEY(id)
);

CREATE SEQUENCE user_id_seq;
CREATE TABLE users (
	id integer DEFAULT nextval('user_id_seq'),
	email VARCHAR(255) UNIQUE NOT NULL, 
	name VARCHAR(255) NOT NULL,
	password VARCHAR(255) NOT NULL,
	salt VARCHAR(255) NOT NULL,
	role integer NOT NULL,
	PRIMARY KEY (id)
);

CREATE SEQUENCE stmt_id_seq;
CREATE TABLE statement (
	id integer DEFAULT nextval('stmt_id_seq'),
    title varchar(255) NOT NULL,
    cat_id integer NOT NULL,
    FOREIGN KEY (cat_id) REFERENCES category(id),
    rating integer NOT NULL,
    PRIMARY KEY (id)
);

CREATE SEQUENCE entry_id_seq;
CREATE TABLE entry (
	id integer DEFAULT nextval('entry_id_seq'),
	stmt_id integer NOT NULL,
	FOREIGN KEY (stmt_id) REFERENCES statement(id),
	content varchar(8192) NOT NULL,
	date date NOT NULL,
	user_id integer NOT NULL,
	FOREIGN KEY (user_id) REFERENCES users(id),
);	

# --- !Downs
 
DROP TABLE entry;
DROP SEQUENCE entry_id_seq;

DROP TABLE statement;
DROP SEQUENCE stmt_id_seq;

DROP TABLE category;
DROP SEQUENCE cat_id_seq;

DROP TABLE user;
DROP SEQUENCE user_id_seq;