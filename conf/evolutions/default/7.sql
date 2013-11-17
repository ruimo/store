# --- 

# --- !Ups
alter table address add column comment text default '' not null;

# --- !Downs

alter table address drop column comment;
