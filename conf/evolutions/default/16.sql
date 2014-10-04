# --- 

# --- !Ups
create table user_address (
  user_address_id bigint not null,
  store_user_id bigint not null references store_user on delete cascade,
  address_id bigint not null references address on delete cascade,
  seq integer not null,
  constraint pk_user_address primary key (user_address_id),
  unique(store_user_id, address_id),
  unique(store_user_id, address_id, seq)
);

create index user_address1 on user_address(store_user_id);
create sequence user_address_seq start with 1000;

create table password_dict (
  password varchar(24) not null,
  constraint pk_password_dict primary key(password)
);

# --- !Downs

drop table user_address;

drop sequence user_address_seq;

drop table password_dict;
