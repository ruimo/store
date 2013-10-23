# --- 

# --- !Ups
create table shopping_cart_shipping (
  shopping_cart_shipping_id bigint not null,
  store_user_id bigint not null references store_user on delete cascade,
  site_id bigint not null references site on delete cascade,
  shipping_date timestamp not null,
  constraint pk_shopping_cart_shipping primary key (shopping_cart_shipping_id),
  unique(store_user_id, site_id)
);

create sequence shopping_cart_shipping_seq start with 1000;

# --- !Downs
drop table shopping_cart_shipping;

drop sequence shopping_cart_shipping_seq;
