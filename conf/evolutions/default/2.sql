# --- 

# --- !Ups
alter table store_user add company_name varchar(64);

# --- !Downs
alter table store_user drop company_name;
