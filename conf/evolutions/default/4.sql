# --- 

# --- !Ups
alter table transaction_shipping add column shipping_date timestamp default timestamp '1970-01-01 00:00:00' not null ;

# --- !Downs
alter table transaction_shipping_user drop column shipping_date;
