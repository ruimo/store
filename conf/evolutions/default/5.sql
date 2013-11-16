# --- 

# --- !Ups
create table order_notification (
  order_notification_id bigint not null,
  store_user_id bigint not null references store_user,
  constraint pk_order_notification primary key (order_notification_id),
  unique(store_user_id)
);

create sequence order_notification_seq start with 1000;

# --- !Downs
drop table order_notification;

drop sequence order_notification_seq;
