# --- 

# --- !Ups
create table item_inquiry (
  item_inquiry_id bigint not null,
  site_id bigint not null references site,
  item_id bigint not null references item,
  store_user_id bigint not null references store_user,
  inquiry_type integer not null,
  submit_user_name varchar(255) not null,
  email varchar(255) not null,
  status integer not null,
  created timestamp not null,
  constraint pk_item_inquiry primary key (item_inquiry_id)
);
  
create sequence item_inquiry_seq start with 1000;

create table item_inquiry_field (
  item_inquiry_field_id bigint not null,
  item_inquiry_id bigint not null references item_inquiry,
  field_name varchar(80) not null,
  field_value text not null,
  constraint pk_item_inquiry_field primary key (item_inquiry_field_id),
  unique (item_inquiry_id, field_name)
);

create index ix_item_inquiry_field1 on item_inquiry_field (item_inquiry_id);

create sequence item_inquiry_field_seq start with 1000;

# --- !Downs

drop index ix_item_inquiry_field1;

drop table item_inquiry_field;

drop sequence item_inquiry_field_seq;

drop table item_inquiry;

drop sequence item_inquiry_seq;
