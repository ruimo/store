# --- 

# --- !Ups

alter table site add column deleted boolean default FALSE;
alter table site alter column deleted set not null;

# --- !Downs

alter table site drop column deleted;
