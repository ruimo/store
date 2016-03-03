# --- 

# --- !Ups

alter table store_user add column created_time timestamp default current_timestamp;
alter table store_user drop constraint user_user_role_check1;
alter table store_user add constraint user_user_role_check1 check (user_role in (0, 1, 2));
create index ix_store_user1 on store_user (created_time);
create index ix_store_user2 on store_user (user_role);

# --- !Downs

drop index ix_store_user1;
drop index ix_store_user2;
alter table store_user drop column created_time;
alter table store_user drop constraint user_user_role_check1;
alter table store_user add constraint user_user_role_check1 check (user_role in (0, 1));
