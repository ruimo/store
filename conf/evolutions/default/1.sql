# --- First database schema

# --- !Ups
create table locale (
  locale_id bigint not null,
  lang varchar(8) not null,
  country varchar(3) not null default '',
  precedence integer not null,
  constraint pk_locale primary key (locale_id),
  unique (lang, country)
);

insert into locale (locale_id, lang, precedence) values (1, 'ja', 2);
insert into locale (locale_id, lang, precedence) values (2, 'en', 1);

create table site (
  site_id bigint not null,
  locale_id bigint not null references locale,
  site_name varchar(32) not null unique,
  constraint pk_site primary key (site_id)
);

create sequence site_seq start with 1000;

create table category (
  category_id bigint not null,
  constraint pk_category primary key (category_id)
);

create sequence category_seq start with 1000;

create table item (
  item_id bigint not null,
  category_id bigint not null references category,
  constraint pk_item primary key (item_id)
);

create sequence item_seq start with 1000;

create table item_numeric_metadata (
  item_numeric_metadata_id bigint not null,
  item_id bigint not null references item,
  metadata_type integer not null,
  metadata bigint,
  constraint pk_item_numeric_metadata primary key (item_numeric_metadata_id),
  unique (item_id, metadata_type)
);

create sequence item_numeric_metadata_seq start with 1000;

create table site_item_numeric_metadata (
  site_item_numeric_metadata_id bigint not null,
  site_id bigint not null references site,
  item_id bigint not null references item,
  metadata_type integer not null,
  metadata bigint,
  constraint pk_site_item_numeric_metadata primary key (site_item_numeric_metadata_id),
  unique (site_id, item_id, metadata_type)
);

create sequence site_item_numeric_metadata_seq start with 1000;

create table item_name (
  item_name_id bigint not null,
  locale_id bigint not null references locale,
  item_id bigint not null references item on delete cascade,
  item_name text not null,
  constraint pk_item_name primary key (item_name_id),
  unique (locale_id, item_id)
);

create sequence item_name_seq start with 1000;

create index ix_item_name1 on item_name (item_id);

create table item_description (
  item_description_id bigint not null,
  locale_id bigint not null references locale,
  description text not null,
  item_id bigint not null references item on delete cascade,
  site_id bigint not null references site on delete cascade,
  constraint pk_item_description primary key (item_description_id),
  unique (locale_id, item_id, site_id)
);

create sequence item_description_seq start with 1000;

create index ix_item_description1 on item_description (item_id);

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
  path_length integer not null,
  primary key (ancestor, descendant)
);

create index ix_category_path1 on category_path(ancestor);
create index ix_category_path2 on category_path(descendant);
create index ix_category_path3 on category_path(path_length);

create table site_category (
  category_id bigint not null references category on delete cascade,
  site_id bigint not null references site on delete cascade,
  constraint pk_site_category primary key (category_id, site_id)
);

create table tax (
  tax_id bigint not null,
  constraint pk_tax primary key(tax_id)
);

create sequence tax_seq start with 1000;

create table tax_name (
  tax_id bigint not null references tax on delete cascade,
  locale_id bigint not null references locale,
  tax_name varchar(32) not null,
  unique (tax_id, locale_id)
);

create table tax_history (
  tax_history_id bigint not null,
  tax_id bigint not null references tax on delete cascade,
  tax_type integer not null,
  rate decimal(5,3) not null,
  -- Exclusive
  valid_until timestamp not null,
  constraint tax_history_check1 check (tax_type in (0,1, 2)),
  constraint pk_tax_history primary key(tax_history_id),
  unique (tax_id, valid_until)
);

create sequence tax_history_seq start with 1000;

create index ix_tax_history1 on tax_history (valid_until);

create table item_price (
  item_price_id bigint not null,
  site_id bigint not null references site on delete cascade,
  item_id bigint not null references item on delete cascade,
  constraint pk_item_price primary key (item_price_id),
  unique (site_id, item_id)
);

create sequence item_price_seq start with 1000;

create table currency (
  currency_id bigint not null,
  -- ISO 4217
  currency_code varchar(3) not null,
  constraint pk_currency primary key (currency_id)
);

insert into currency (currency_id, currency_code) values (1, 'JPY');
insert into currency (currency_id, currency_code) values (2, 'USD');

create table item_price_history (
  item_price_history_id bigint not null,
  item_price_id bigint not null references item_price on delete cascade,
  tax_id bigint not null references tax on delete cascade,
  currency_id bigint not null references currency on delete cascade,
  unit_price decimal(15,2) not null,
  -- Exclusive
  valid_until timestamp not null,
  constraint pk_item_price_history primary key (item_price_history_id),
  unique (item_price_id, valid_until)
);

create sequence item_price_history_seq start with 1000;

create table store_user (
  store_user_id bigint not null,
  user_name varchar(64) not null unique,
  first_name varchar(64) not null,
  middle_name varchar(64),
  last_name varchar(64) not null,
  email varchar(255) not null,
  password_hash bigint not null,
  salt bigint not null,
  deleted boolean not null,
  user_role integer not null,
  constraint user_user_role_check1 check (user_role in (0,1)),
  constraint pk_user primary key (store_user_id)
);

create sequence store_user_seq start with 1000;

create table transaction_header (
  transaction_id bigint not null,
  site_id bigint not null references site,
  store_user_id bigint not null references store_user,
  transaction_time timestamp not null,
  currency_id bigint not null references currency,
  total_amount decimal(15,2) not null,
  tax_amount decimal(15,2) not null,
  transaction_type integer not null,
  constraint pk_transaction primary key (transaction_id)
);

create sequence transaction_header_seq start with 1000;

create table address (
  address_id bigint not null,
  country_code integer not null,
  first_name varchar(64) not null,
  middle_name varchar(64) not null,
  last_name varchar(64) not null,
  first_name_kana varchar(64) not null,
  last_name_kana varchar(64) not null,
  zip1 varchar(32) not null,
  zip2 varchar(32) not null,
  zip3 varchar(32) not null,
  prefecture integer not null,
  address1 varchar(256) not null,
  address2 varchar(256) not null,
  address3 varchar(256) not null,
  address4 varchar(256) not null,
  address5 varchar(256) not null,
  tel1 varchar(32) not null,
  tel2 varchar(32) not null,
  tel3 varchar(32) not null,

  constraint pk_address primary key (address_id)
);

create sequence address_seq start with 1000;

create table transaction_shipping (
  transaction_shipping_id bigint not null,
  transaction_id bigint not null references transaction_header on delete cascade,
  amount decimal(15,2) not null,
  address_id bigint not null references address,
  constraint pk_transaction_shipping primary key (transaction_shipping_id)
);

create sequence transaction_shipping_seq start with 1000;

create table transaction_item (
  transaction_item_id bigint not null,
  transaction_id bigint not null references transaction_header on delete cascade,
  item_price_history_id bigint not null,
  transaction_shipping_id bigint not null references transaction_shipping on delete cascade,
  quantity decimal(15,2) not null,
  amount decimal(15,2) not null,
  constraint pk_transaction_item primary key (transaction_item_id)
);

create sequence transaction_item_seq start with 1000;

create table transaction_tax (
  transaction_tax_id bigint not null,
  transaction_id bigint not null references transaction_header on delete cascade,
  tax_history_id bigint not null,
  target_amount decimal(15,2) not null,
  amount decimal(15,2) not null,
  constraint pk_transaction_tax primary key (transaction_tax_id)
);

create sequence transaction_tax_seq start with 1000;

create table transaction_credit_tender (
  transaction_credit_tender_id bigint not null,
  transaction_id bigint not null references transaction_header on delete cascade,
  amount decimal(15,2) not null,
  constraint pk_transaction_credit_tender primary key (transaction_credit_tender_id)
);

create sequence transaction_credit_tender_seq start with 1000;

create table site_user (
  site_user_id bigint not null,
  site_id bigint not null references site on delete cascade,
  store_user_id bigint not null references store_user on delete cascade,
  user_role integer not null,
  constraint pk_site_user primary key (site_user_id),
  constraint site_user_user_role_check1 check (user_role in (0,1)),
  unique (site_id, store_user_id)
);

create sequence site_user_seq start with 1000;

create table shopping_cart (
  shopping_cart_id bigint not null,
  store_user_id bigint not null references store_user on delete cascade,
  seq integer not null,
  site_id bigint not null references site on delete cascade,
  item_id bigint not null references item on delete cascade,
  quantity integer not null,
  constraint pk_shopping_cart primary key (shopping_cart_id),
  unique(store_user_id, seq)
);

create index ix_shopping_cart1 on shopping_cart (item_id);

create sequence shopping_cart_seq start with 1000;

create table shipping_address_history (
  shipping_address_history_id bigint not null,
  store_user_id bigint not null references store_user on delete cascade,
  address_id bigint not null references address on delete cascade,
  updated_time timestamp not null,
  constraint pk_shipping_address_history primary key (shipping_address_history_id),
  unique(store_user_id, address_id, updated_time)
);

create sequence shipping_address_history_seq start with 1000;

# --- !Downs

-- No down script. Recreate database before reloading 1.sql.
