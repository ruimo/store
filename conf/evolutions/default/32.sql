# --- 

# --- !Ups

alter table store_user add column created_time timestamp default current_timestamp;
create index ix_store_user1 on store_user (created_time);
create index ix_store_user2 on store_user (user_role);

# --- !Downs

drop index ix_store_user1;
drop index ix_store_user2;
alter table store_user drop column created_time;
