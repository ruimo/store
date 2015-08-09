# --- 

# --- !Ups

create table supplemental_category (
  item_id bigint not null references item on delete cascade,
  category_id bigint not null references category on delete cascade,
  constraint pk_supplemental_category primary key (supplemental_category_id),
  unique (item_id, category_id)
);

# --- !Downs

drop table supplemental_category;
