# --- First database schema

# --- !Ups
create table locale (
  locale_id bigint not null,
  lang varchar(8) not null,
  country varchar(3) not null default '',
  constraint pk_locale primary key (locale_id),
  unique (lang, country)
);

insert into locale (locale_id, lang) values (1, 'ja');
insert into locale (locale_id, lang) values (2, 'en');

create table site (
  site_id bigint not null,
  locale_id bigint not null references locale,
  site_name varchar(32) not null unique,
  constraint pk_site primary key (site_id)
);

create sequence site_seq start with 1;

create table category (
  category_id bigint not null,
  constraint pk_category primary key (category_id)
);

create sequence category_seq start with 1;

create table item (
  item_id bigint not null,
  category_id bigint not null references category,
  constraint pk_item primary key (item_id)
);

create sequence item_seq start with 1;

create table item_name (
  locale_id bigint not null references locale,
  item_name text not null,
  item_id bigint not null references item on delete cascade,
  constraint pk_item_name primary key (locale_id, item_id)
);

create index ix_item_name1 on item_name (item_id);

create table site_item (
  item_id bigint not null references item on delete cascade,
  site_id bigint not null references site on delete cascade,
  constraint pk_site_item primary key (item_id, site_id)
);

create table category_name (
  locale_id bigint not null references locale,
  category_name varchar(32) not null,
  category_id bigint not null references category on delete cascade,
  constraint pk_category_name primary key (locale_id, category_id)
);

create index ix_category_name1 on category_name(category_id);

create table category_path (
  ancestor bigint not null references category on delete cascade,
  descendant bigint not null references category on delete cascade,
  path_length smallint not null,
  primary key (ancestor, descendant)
);

create table site_category (
  category_id bigint not null references category on delete cascade,
  site_id bigint not null references site on delete cascade,
  constraint pk_site_category primary key (category_id, site_id)
);

create table tax (
  tax_id bigint not null,
  constraint pk_tax primary key(tax_id)
);

create sequence tax_seq start with 1;

create table tax_history (
  tax_history_id bigint not null,
  tax_id bigint not null references tax on delete cascade,
  tax_type smallint not null,
  rate decimal(5,3) not null,
  -- Exclusive
  valid_until timestamp not null,
  constraint pk_tax_history primary key(tax_history_id)
);

create sequence tax_history_seq start with 1;

create index ix_tax_history1 on tax_history (valid_until);

create table item_price (
  item_price_id bigint not null,
  site_id bigint not null references site on delete cascade,
  item_id bigint not null references item on delete cascade,
  currency varchar(3) not null,
  constraint pk_item_price primary key (item_price_id),
  unique (site_id, item_id)
);

create sequence item_price_seq start with 1;

create table item_price_history (
  item_price_history_id bigint not null,
  item_price_id bigint not null references item_price on delete cascade,
  tax_id bigint not null references tax on delete cascade,
  unit_price decimal(15,2) not null,
  -- Exclusive
  valid_until timestamp not null,
  constraint pk_item_price_history primary key (item_price_history_id)
);

create sequence item_price_history_seq start with 1;

create table transaction_header (
  transaction_id bigint not null,
  site_id bigint not null,
  transaction_time timestamp not null,
  currency varchar(3) not null,
  total_amount decimal(15,2) not null,
  tax_amount decimal(15,2) not null,
  transaction_type smallint not null,
  constraint pk_transaction primary key (transaction_id)
);

create sequence transaction_header_seq start with 1;

create table transaction_shipping (
  transaction_shipping_id bigint not null,
  transaction_id bigint not null references transaction_header on delete cascade,
  amount decimal(15,2) not null,
  constraint pk_transaction_shipping primary key (transaction_shipping_id)
);

create sequence transaction_shipping_seq start with 1;

create table transaction_item (
  transaction_item_id bigint not null,
  transaction_id bigint not null references transaction_header on delete cascade,
  item_price_history_id bigint not null,
  transaction_shipping_id bigint not null references transaction_shipping on delete cascade,
  quantity decimal(15,2) not null,
  amount decimal(15,2) not null,
  constraint pk_transaction_item primary key (transaction_item_id)
);

create sequence transaction_item_seq start with 1;

create table transaction_tax (
  transaction_tax_id bigint not null,
  transaction_id bigint not null references transaction_header on delete cascade,
  tax_history_id bigint not null,
  target_amount decimal(15,2) not null,
  amount decimal(15,2) not null,
  constraint pk_transaction_tax primary key (transaction_tax_id)
);

create sequence transaction_tax_seq start with 1;

create table transaction_credit_tender (
  transaction_credit_tender_id bigint not null,
  transaction_id bigint not null references transaction_header on delete cascade,
  amount decimal(15,2) not null,
  constraint pk_transaction_credit_tender primary key (transaction_credit_tender_id)
);

create sequence transaction_credit_tender_seq start with 1;

# --- !Downs

-- No down script. Recreate database before reloading 1.sql.
