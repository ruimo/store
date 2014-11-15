# --- 

# --- !Ups
create table reset_password (
  reset_password_id bigint not null,
  store_user_id bigint not null references store_user on delete cascade,
  token bigint not null,
  reset_time timestamp not null,
  unique(store_user_id)
);

create sequence reset_password_seq start with 1000;

# --- !Downs

drop table reset_password;

drop sequence reset_password_seq;
