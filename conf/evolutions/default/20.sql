# --- 

# --- !Ups
create table site_item_text_metadata (
  site_item_text_metadata_id bigint not null,
  site_id bigint not null references site,
  item_id bigint not null references item,
  metadata_type integer not null,
  metadata text,
  constraint pk_site_item_text_metadata primary key (site_item_text_metadata_id),
  unique(site_id, item_id, metadata_type)
);

create sequence site_item_text_metadata_seq start with 1000;

# --- !Downs

drop table site_item_text_metadata;

drop sequence site_item_text_metadata_seq;
