# --- 

# --- !Ups

alter table item_price_history add column cost_price decimal(15,2) not null default 0;
alter table transaction_item add column cost_price decimal(15,2) not null default 0;

# --- !Downs

alter table item_price_history drop column cost_price;
alter table transaction_item drop column cost_price;
