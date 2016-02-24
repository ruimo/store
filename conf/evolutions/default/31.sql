# --- 

# --- !Ups

alter table transaction_status add column planned_shipping_date timestamp default null;
alter table transaction_status add column planned_delivery_date timestamp default null;

# --- !Downs

alter table transaction_status drop column planned_shipping_date;
alter table transaction_status drop column planned_delivery_date;
