# --- !Ups
ALTER TABLE statement RENAME merged_id TO linked_id;
ALTER TABLE author RENAME rated TO top_level;

CREATE VIEW full_statement AS SELECT statement.id as id, statement.title, statement.quote, statement.quote_src,    
	MAX(entry.date) AS latestEntry, statement.rating, statement.rated, statement.linked_id, statement.textsearchable,  
	statement2.rating AS linked_rating, statement2.rated AS linked_rated,   
	category.id as cat_id, category.name as cat_name, category.ordering as cat_ordering,   
	author.id as author_id, author.name as author_name, author.ordering as author_ordering, 
	author.top_level as author_top_level, author.color as author_color, author.background as author_background,   
	ARRAY_AGG(tag.id) AS tag_id, ARRAY_AGG(tag.name) AS tag_name, ARRAY_AGG(tag.important) AS tag_important   
	FROM statement   
	JOIN category ON category.id=statement.cat_id   
	JOIN author ON author.id=statement.author_id   
	LEFT JOIN statement statement2 ON statement2.id = statement.linked_id   
	LEFT JOIN statement_tags ON statement.id = statement_tags.stmt_id    
	LEFT JOIN tag on statement_tags.tag_id = tag.id   
	LEFT JOIN entry on statement.id = entry.stmt_id  
	GROUP BY statement.id, category.id, author.id, statement2.id   
	ORDER BY statement.id ASC;

ALTER TABLE entry DROP CONSTRAINT entry_stmt_id_fkey;
ALTER TABLE entry ADD CONSTRAINT entry_stmt_id_fkey 
	FOREIGN KEY (stmt_id) REFERENCES statement(id) ON DELETE CASCADE;

ALTER TABLE statement_tags DROP CONSTRAINT statement_tags_stmt_id_fkey;
ALTER TABLE statement_tags ADD CONSTRAINT statement_tags_stmt_id_fkey 
	FOREIGN KEY (stmt_id) REFERENCES statement(id) ON DELETE CASCADE;

ALTER TABLE statement_tags DROP CONSTRAINT statement_tags_tag_id_fkey;
ALTER TABLE statement_tags ADD CONSTRAINT statement_tags_tag_id_fkey 
	FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE;

ALTER TABLE statement DROP CONSTRAINT statement_merged_id_fkey;
ALTER TABLE statement ADD CONSTRAINT statement_merged_id_fkey FOREIGN KEY (linked_id) REFERENCES statement(id) ON DELETE SET NULL ON UPDATE SET NULL ;

ALTER TABLE statement_rating DROP CONSTRAINT fk_stmt_id;
ALTER TABLE statement_rating ADD CONSTRAINT fk_stmt_id 
	FOREIGN KEY (stmt_id) REFERENCES statement (id) ON DELETE CASCADE;

# --- !Downs

DROP VIEW full_statement;


ALTER TABLE statement RENAME linked_id TO merged_id;
ALTER TABLE author RENAME top_level TO rated;