# --- 

# --- !Ups
create table order_notification (
  order_notification_id bigint not null,
  store_user_id bigint not null references store_user,
  constraint pk_order_notification primary key (order_notification_id),
  unique(store_user_id)
);

create sequence order_notification_seq start with 1000;

alter table transaction_shipping add column box_count integer default 1 not null ;
alter table transaction_shipping add column box_name varchar(32) default '-' not null ;

# --- !Downs
drop table order_notification;

drop sequence order_notification_seq;

alter table transaction_shipping drop column box_count;
alter table transaction_shipping drop column box_name;
