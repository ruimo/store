# --- 

# --- !Ups

create table transaction_site_item_numeric_metadata (
  transaction_site_item_numeric_metadata_id bigint not null,
  transaction_item_id bigint not null references transaction_item on delete cascade,
  metadata_type integer not null,
  metadata bigint,
  constraint pk_transaction_site_item_numeric_metadata primary key (transaction_site_item_numeric_metadata_id),
  unique (transaction_item_id, metadata_type)
);

create table transaction_site_item_text_metadata (
  transaction_site_item_text_metadata_id bigint not null,
  transaction_item_id bigint not null references transaction_item on delete cascade,
  metadata_type integer not null,
  metadata text,
  constraint pk_transaction_site_item_text_metadata primary key (transaction_site_item_text_metadata_id),
  unique (transaction_item_id, metadata_type)
);

create table transaction_item_numeric_metadata (
  transaction_item_numeric_metadata_id bigint not null,
  transaction_item_id bigint not null references transaction_item on delete cascade,
  metadata_type integer not null,
  metadata bigint,
  constraint pk_transaction_item_numeric_metadata primary key (transaction_item_numeric_metadata_id),
  unique (transaction_item_id, metadata_type)
);

create table transaction_item_text_metadata (
  transaction_item_text_metadata_id bigint not null,
  transaction_item_id bigint not null references transaction_item on delete cascade,
  metadata_type integer not null,
  metadata text,
  constraint pk_transaction_item_text_metadata primary key (transaction_item_text_metadata_id),
  unique (transaction_item_id, metadata_type)
);

create sequence transaction_item_numeric_metadata_seq;
create sequence transaction_item_text_metadata_seq;
create sequence transaction_site_item_numeric_metadata_seq;
create sequence transaction_site_item_text_metadata_seq;

alter table transaction_shipping add column cost_amount decimal(15,2) default null;

# --- !Downs

drop table transaction_item_text_metadata;
drop table transaction_item_numeric_metadata;
drop table transaction_site_item_text_metadata;
drop table transaction_site_item_numeric_metadata;

drop sequence transaction_item_numeric_metadata_seq;
drop sequence transaction_item_text_metadata_seq;
drop sequence transaction_site_item_numeric_metadata_seq;
drop sequence transaction_site_item_text_metadata_seq;

alter table transaction_shipping drop column cost_amount;
