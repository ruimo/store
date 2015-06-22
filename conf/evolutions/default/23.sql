# --- 

# --- !Ups

alter table shipping_fee_history add column cost_fee decimal(15,2) default null;

# --- !Downs

alter table shipping_fee_history drop column cost_fee;
