# --- 

# --- !Ups

alter table category add column category_code varchar(20) default null;

update category set category_code = to_char(category_id, '9999999999999999999');

alter table category alter column category_code set not null;

# --- !Downs

alter table category drop column category_code;
