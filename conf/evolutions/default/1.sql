# Tasks schema
 
# --- !Ups
CREATE SEQUENCE cat_id_seq;
CREATE TABLE category (
	id integer DEFAULT nextval('cat_id_seq'),
    name varchar(255) UNIQUE NOT NULL,
    ordering integer NOT NULL,
    PRIMARY KEY(id)
);
CREATE INDEX cat_asc_index ON category (ordering ASC);

CREATE SEQUENCE author_id_seq;
CREATE TABLE author (
	id integer DEFAULT nextval('author_id_seq'),
    name varchar(255) UNIQUE NOT NULL,
    ordering integer NOT NULL,
    rated boolean NOT NULL,
    color varchar(30) NOT NULL,
    background varchar(30) NOT NULL,
    PRIMARY KEY(id)
);
CREATE INDEX author_asc_index ON author (ordering ASC);

CREATE SEQUENCE tag_id_seq;
CREATE TABLE tag (
	id integer DEFAULT nextval('tag_id_seq'),
    name varchar(255) UNIQUE NOT NULL,
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
    author_id integer NOT NULL,
    FOREIGN KEY (author_id) REFERENCES author(id),
    cat_id integer NOT NULL,
    FOREIGN KEY (cat_id) REFERENCES category(id),
    quote varchar(8096),
    quote_src varchar(1024),
    rating integer,
    merged_id integer,
    FOREIGN KEY (merged_id) REFERENCES statement(id) ON UPDATE SET NULL,
    PRIMARY KEY (id)
);

CREATE INDEX stmt_merged_index ON statement (merged_id );
CREATE INDEX stmt_cat_author_index ON statement (cat_id, author_id);

CREATE SEQUENCE entry_id_seq;
CREATE TABLE entry (
	id integer DEFAULT nextval('entry_id_seq'),
	stmt_id integer NOT NULL,
	FOREIGN KEY (stmt_id) REFERENCES statement(id),
	content varchar(8192) NOT NULL,
	date timestamp NOT NULL,
	user_id integer NOT NULL,
	FOREIGN KEY (user_id) REFERENCES users(id)
);	
CREATE INDEX entry_stmt_date_index ON entry (stmt_id, date DESC);

CREATE TABLE statement_tags (
	tag_id integer NOT NULL,
    FOREIGN KEY (tag_id) REFERENCES tag(id),
    stmt_id integer NOT NULL,
    FOREIGN KEY (stmt_id) REFERENCES statement(id),
    PRIMARY KEY(tag_id, stmt_id)
);


# --- !Downs
 
DROP INDEX entry_stmt_date_index;
DROP TABLE entry;
DROP SEQUENCE entry_id_seq;

DROP TABLE statement_tags;

DROP TABLE users;
DROP SEQUENCE user_id_seq;

DROP TABLE tag;
DROP SEQUENCE tag_id_seq;

DROP INDEX stmt_cat_author_index;
DROP INDEX stmt_merged_index;

DROP TABLE statement;
DROP SEQUENCE stmt_id_seq;

DROP INDEX cat_asc_index;
DROP TABLE category;
DROP SEQUENCE cat_id_seq;

DROP INDEX author_asc_index;
DROP TABLE author;
DROP SEQUENCE author_id_seq;