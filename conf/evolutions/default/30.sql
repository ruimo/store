# --- 

# --- !Ups

create table supplemental_user_email (
  supplemental_user_email_id bigint not null,
  email varchar(255) not null,
  store_user_id bigint not null references store_user on delete cascade,
  constraint pk_supplemental_user_email primary key (supplemental_user_email_id)
);

create index ix_supplemental_user_email on supplemental_user_email (store_user_id);

create sequence supplemental_user_email_seq start with 1000;

# --- !Downs

drop sequence supplemental_user_email_seq;

drop index ix_supplemental_user_email;

drop table supplemental_user_email;
