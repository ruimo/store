# --- 

# --- !Ups
create table transporter (
  transporter_id bigint not null,
  constraint pk_transporter primary key(transporter_id)
);

create sequence transporter_seq start with 1000;

create table transporter_name (
  transporter_name_id bigint not null,
  locale_id bigint not null references locale,
  transporter_id bigint not null references transporter on delete cascade,
  transporter_name varchar(64),
  constraint pk_transporter_name primary key(transporter_name_id),
  unique (transporter_name),
  unique (locale_id, transporter_id)
);

create sequence transporter_name_seq start with 1000;

alter table transaction_status add column transporter_id bigint default null;
alter table transaction_status add constraint transaction_status_fk1 foreign key (transporter_id) references transporter;
alter table transaction_status add column slip_code varchar(128) default null;
alter table transaction_status add column shipped_time timestamp default null;

# --- !Downs

alter table transaction_status drop column transporter_id;
alter table transaction_status drop column slip_code;
alter table transaction_status drop column shipped_time;

drop table transporter;

drop table transporter_name;

drop sequence transporter_seq;
