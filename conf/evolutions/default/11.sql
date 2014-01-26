# --- 

# --- !Ups

alter table shipping_fee add constraint shipping_fee_constraint1 unique(shipping_box_id, country_code, location_code);

# --- !Downs

alter table shipping_fee drop constraint shipping_fee_constraint1;
