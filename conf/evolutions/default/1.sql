# Tasks schema
 
# --- !Ups
CREATE SEQUENCE cat_id_seq;
CREATE TABLE category (
	id integer NOT NULL DEFAULT nextval('cat_id_seq'),
    name varchar(255) UNIQUE NOT NULL,
    ordering integer NOT NULL,
    PRIMARY KEY(id)
);

CREATE SEQUENCE user_id_seq;
CREATE TABLE user (
	id integer NOT NULL DEFAULT nextval('user_id_seq'),
	email VARCHAR(255) UNIQUE NOT NULL, 
	name VARCHAR(255) NOT NULL,
	password VARCHAR(255) NOT NULL,
	salt VARCHAR(255) NOT NULL,
	role integer NOT NULL,
	PRIMARY KEY (id)
);

CREATE SEQUENCE stmt_id_seq;
CREATE TABLE statement (
	id integer NOT NULL DEFAULT nextval('stmt_id_seq'),
    title varchar(255),
    cat_id integer NOT NULL,
    FOREIGN KEY (cat_id) REFERENCES category(id),
    rating integer NOT NULL,
    PRIMARY KEY (id)
);

CREATE SEQUENCE entry_id_seq;
CREATE TABLE entry (
	id integer NOT NULL DEFAULT nextval('entry_id_seq'),
	stmt_id integer NOT NULL,
	FOREIGN KEY (stmt_id) REFERENCES statement(id),
	content varchar(8192) NOT NULL,
	date date NOT NULL,
	user_id integer NOT NULL,
	FOREIGN KEY (user_id) REFERENCES user(id),
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