# Tasks schema
 
# --- !Ups
ALTER TABLE statement DROP COLUMN latestEntry;

ALTER TABLE statement ADD COLUMN textsearchable tsvector;

UPDATE statement SET textsearchable =
     to_tsvector('german', coalesce(title,'') || ' ' || coalesce(quote,''));

CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE
	ON statement FOR EACH ROW EXECUTE PROCEDURE
	tsvector_update_trigger(textsearchable, 'pg_catalog.german', title, quote);

CREATE INDEX stmt_textsearch_idx ON statement USING gin(textsearchable);

# --- !Downs

ALTER TABLE tag ADD COLUMN latestEntry timestamp;

DROP INDEX stmt_textsearch_idx;

DROP TRIGGER tsvectorupdate;

ALTER TABLE statement DROP COLUMN textsearchable;