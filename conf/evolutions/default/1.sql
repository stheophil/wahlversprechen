# Tasks schema
 
# --- !Ups
CREATE TABLE category (
    name varchar(255) NOT NULL,
    ordering integer NOT NULL,
    PRIMARY KEY(name)
);

CREATE TABLE user (
	email VARCHAR(255) NOT NULL, 
	name VARCHAR(255) NOT NULL,
	password VARCHAR(255) NOT NULL,
	role integer NOT NULL,
	PRIMARY KEY (email)
);

CREATE SEQUENCE stmt_id_seq;
CREATE TABLE statement (
	id integer NOT NULL DEFAULT nextval('stmt_id_seq'),
    title varchar(255),
    category varchar(255) NOT NULL,
    FOREIGN KEY (category) REFERENCES category(name),
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
	email VARCHAR(255) NOT NULL,
	FOREIGN KEY (email) REFERENCES user(email),
);	

# --- !Downs
 
DROP TABLE entry;
DROP SEQUENCE entry_id_seq;

DROP TABLE statement;
DROP SEQUENCE stmt_id_seq;

DROP TABLE category;
DROP TABLE user;