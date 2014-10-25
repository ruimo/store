# --- 

# --- !Ups
create table coupon (
  coupon_id bigint not null,
  constraint pk_coupon primary key (coupon_id)
);

create sequence coupon_seq start with 1000;

create table coupon_item (
  coupon_item_id bigint not null,
  item_id bigint not null references address on delete cascade,
  coupon_id bigint not null references address on delete cascade,
  constraint pk_coupon_item primary key (coupon_item_id),
  unique(item_id, coupon_id)
);

create sequence coupon_item_seq start with 1000;

create table transaction_coupon (
  transaction_coupon_id bigint not null,
  transaction_item_id bigint not null references transaction_item on delete cascade,
  coupon_id bigint not null,
  constraint pk_transaction_coupon primary key (transaction_coupon_id)
);

create sequence transaction_coupon_seq start with 1000;

# --- !Downs

drop table transaction_coupon;

drop sequence transaction_coupon_seq;

drop table coupon_item;

drop sequence coupon_item_seq;

drop table coupon;

drop sequence coupon_seq;
