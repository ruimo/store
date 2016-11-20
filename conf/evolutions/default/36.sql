# --- 

# --- !Ups

drop index site_item_numeric_metadata_site_id_item_id_metadata_type_key;
alter table site_item_numeric_metadata drop constraint if exists CONSTRAINT_BB2;
alter table site_item_numeric_metadata add column valid_until timestamp not null default '9999-12-31';
create unique index site_item_numeric_metadata_u on site_item_numeric_metadata (site_id, item_id, metadata_type, valid_until);

# --- !Downs

alter table site_item_numeric_metadata drop constraint if exists site_item_numeric_metadata_u;
alter table site_item_numeric_metadata drop column if exists valid_until;
create unique index site_item_numeric_metadata_site_id_item_id_metadata_type_key on site_item_numeric_metadata (site_id, item_id, metadata_type) ;

