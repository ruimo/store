CREATE EXTENSION pg_trgm;
CREATE INDEX item_name_gin_idx ON item_name USING gin (item_name gin_trgm_ops);
CREATE INDEX item_description_gin_idx ON item_description USING gin (description gin_trgm_ops);
SET enable_seqscan = off;
