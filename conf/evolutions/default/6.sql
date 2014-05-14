# --- !Ups

CREATE SEQUENCE relatedurl_id_seq;
CREATE TABLE relatedurl (
	id integer DEFAULT nextval('relatedurl_id_seq'),
  stmt_id integer,
  FOREIGN KEY (stmt_id) REFERENCES statement(id) ON DELETE CASCADE,
  title varchar(8096) NOT NULL,
  url varchar(8096) NOT NULL,
  confidence DOUBLE PRECISION NOT NULL,
  lastseen timestamp NOT NULL,
  urltype integer NOT NULL,
  PRIMARY KEY(id)
);
CREATE INDEX relatedurl_asc_index ON relatedurl (stmt_id ASC, url ASC);

# --- !Downs

DROP INDEX relatedurl_asc_index;
DROP TABLE relatedurl;
DROP SEQUENCE relatedurl_id_seq;
