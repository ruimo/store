# --- 

# --- !Ups

alter table transaction_status drop column shipped_time;
alter table transaction_status add column last_update timestamp default current_timestamp;
alter table transaction_status alter column last_update set not null;

# --- !Downs

alter table transaction_status add column shipped_time timestamp default null;
alter table transaction_status drop column last_update;
