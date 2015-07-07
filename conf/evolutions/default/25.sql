# --- 

# --- !Ups

alter table site_item add column created timestamp not null default current_timestamp;

# --- !Downs

alter table site_item drop column created;
