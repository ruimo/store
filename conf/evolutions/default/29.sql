# --- 

# --- !Ups

create table web_snippet (
  web_snippet_id bigint not null,
  site_id bigint not null references site on delete cascade,
  content_code varchar(16) not null,
  content text not null,
  updated_time timestamp not null,
  constraint pk_web_snippet primary key (web_snippet_id)
);

create sequence web_snippet_seq;

# --- !Downs

drop table web_snippet;

drop sequence web_snippet_seq;
