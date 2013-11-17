# --- 

# --- !Ups
create table item_text_metadata (
  item_text_metadata_id bigint not null,
  item_id bigint not null references item,
  metadata_type integer not null,
  metadata text,
  constraint pk_item_text_metadata primary key (item_text_metadata_id),
  unique (item_id, metadata_type)
);

create sequence item_text_metadata_seq start with 1000;

# --- !Downs

drop table item_text_metadata;
drop sequence item_text_metadata_seq;
