# --- 

# --- !Ups

alter table transaction_item add column tax_id bigint default 2;
alter table transaction_item alter column tax_id set not null;

# --- !Downs

alter table transaction_item drop column tax_id;
