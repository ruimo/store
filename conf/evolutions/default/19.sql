# --- 

# --- !Ups

alter table item_price_history add column list_price decimal(15,2);

# --- !Downs

alter table item_price_history drop column list_price;
