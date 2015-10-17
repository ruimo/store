# --- 

# --- !Ups

alter table category add column category_code varchar(20) default null;

update category set category_code = to_char(category_id, 'FM9999999999999999999');

alter table category alter column category_code set not null;
alter table category add constraint category_code_unique unique (category_code);

# --- !Downs

alter table category drop column category_code;
