# --- 

# --- !Ups

alter table address add column email varchar(255) not null default '';

# --- !Downs

alter table address drop column email;
