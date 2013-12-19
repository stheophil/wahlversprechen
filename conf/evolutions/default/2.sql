# Tasks schema
 
# --- !Ups
ALTER TABLE tag ADD COLUMN important boolean DEFAULT FALSE;

CREATE INDEX tag_name_important ON tag (important);

# --- !Downs
DROP INDEX tag_name_important;

ALTER TABLE tag DROP COLUMN important;