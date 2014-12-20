# --- 

# --- !Ups
create table recommend_by_admin (
  recommend_by_admin_id bigint not null,
  site_id bigint not null references site on delete cascade,
  item_id bigint not null references item on delete cascade,
  score bigint not null,
  enabled boolean not null,
  constraint pk_recommend_by_admin primary key(recommend_by_admin_id),
  unique(site_id, item_id)
);

create index ix_recommend_by_admin1 on recommend_by_admin(score);
create sequence recommend_by_admin_seq start with 1000;

# --- !Downs

drop table recommend_by_admin;

drop sequence recommend_by_admin_seq;
