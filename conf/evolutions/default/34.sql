# --- 

# --- !Ups

alter table store_user add column stretch_count integer not null default 1;
alter table store_user drop constraint user_user_role_check1;
alter table store_user add constraint user_user_role_check1 check (user_role in (0, 1, 2, 3));

# --- !Downs

alter table store_user drop column stretch_count;
alter table store_user drop constraint user_user_role_check1;
alter table store_user add constraint user_user_role_check1 check (user_role in (0, 1, 2));
