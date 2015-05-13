# --- 

# --- !Ups
create table employee (
  employee_id bigint not null,
  site_id bigint not null references site,
  store_user_id bigint not null unique references store_user,
  constraint pk_employee primary key (employee_id)
);

create sequence employee_seq start with 1000;

# --- !Downs

drop table employee;
drop sequence employee_seq;
