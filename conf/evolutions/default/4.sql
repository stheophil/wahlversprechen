# --- !Ups

CREATE TABLE statement_rating AS SELECT id as stmt_id, rating, rated FROM statement WHERE rating IS NOT NULL;
ALTER TABLE statement_rating ALTER stmt_id SET NOT NULL;
ALTER TABLE statement_rating ALTER rating SET NOT NULL;
ALTER TABLE statement_rating ALTER rated SET NOT NULL;
ALTER TABLE statement_rating ADD CONSTRAINT fk_stmt_id FOREIGN KEY (stmt_id) REFERENCES statement (id);
ALTER TABLE statement_rating ADD CONSTRAINT pk_stmt_id PRIMARY KEY (stmt_id, rated);

CREATE INDEX stmt_rating_asc_index ON statement_rating (stmt_id, rated ASC);

# --- !Downs

DROP INDEX stmt_rating_asc_index;
DROP TABLE statement_rating;
