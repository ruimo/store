# --- First database schema

# --- !Ups
create table Site (
  id bigint not null,
  locale smallint not null,
  siteName varchar(32) not null unique,
  constraint pkSite primary key (id)
);

create sequence SiteSeq start with 1;

create table Item (
  id bigint not null,
  constraint pkItem primary key (id)
);

create sequence ItemSeq start with 1;

create table ItemName (
  locale smallint not null,
  itemName text not null,
  itemId bigint not null references Item on delete cascade,
  constraint pkItemName primary key (locale, itemId)
);

create index ixItemName1 on ItemName (itemId);

create table SiteItem (
  itemId bigint not null references Item on delete cascade,
  siteId bigint not null references Site on delete cascade,
  constraint pkSiteItem primary key (itemId, siteId)
);

create table Category (
  id bigint not null,
  constraint pkCategory primary key (id)
);

create sequence CategorySeq start with 1;

create table CategoryName (
  locale smallint not null,
  categoryName varchar(32) not null,
  categoryId bigint not null references Category on delete cascade,
  constraint pkCategoryName primary key (locale, categoryId)
);

create index ixCategoryName1 on CategoryName(categoryId);

create table CategoryPath (
  ancestor bigint not null references Category on delete cascade,
  descendant bigint not null references Category on delete cascade,
  pathLength smallint not null,
  primary key (ancestor, descendant)
);

create table SiteCategory (
  categoryId bigint not null references Category on delete cascade,
  siteId bigint not null references Site on delete cascade,
  constraint pkSiteCategory primary key (categoryId, siteId)
);

create table ItemPrice (

);

# --- !Downs

set referential_integrity false;

drop table if exists Item;

drop table if exists ItemName;

drop table if exists Site;

drop table if exists Category;

drop table if exists CategoryName;

drop table if exists CategoryPath;

drop table if exists SiteCategory;

set referential_integrity true;

drop sequence if exists SiteSeq;

drop sequence if exists ItemSeq;

drop sequence if exists CategorySeq;
