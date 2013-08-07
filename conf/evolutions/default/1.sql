# --- First database schema

# --- !Ups
create table Site (
  siteId bigint not null,
  locale smallint not null,
  siteName varchar(32) not null unique,
  constraint pkSite primary key (id)
);

create sequence SiteSeq start with 1;

create table Item (
  itemId bigint not null,
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

create table Tax (
  taxId bigint not null,
  constraint pkTax primary key(taxId)
);

create sequence TaxSeq start with 1;

create table TaxHistory (
  taxHistoryId bigint not null,
  taxId bigint not null references Tax on delete cascade,
  taxType smallint not null,
  rate decimal(5,3) not null,
  # Exclusive
  validUntil not null timestamp,
  constraint pkTaxHistory primary key(taxHistoryId)
);

create sequence TaxHistorySeq start with 1;

create index ixTaxHistory1 on TaxHistory (expirationDate);

create table ItemPrice (
  itemPriceId bigint not null,
  siteId bigint not null references Site on delete cascade,
  itemId bigint not null references Item on delete cascade,
  currency varchar(3) not null,
  constraint pkItemPrice primary key (itemPriceId),
  unique (siteId, itemId)
);

create sequence ItemPriceSeq start with 1;

create table ItemPriceHistory (
  itemPriceHistoryId bigint not null,
  itemPriceId bigint not null references ItemPrice on delete cascade,
  taxId bigint not null reference Tax on delete cascade,
  unitPrice decimal(15,2) not null,
  # Exclusive
  validUntil not null timestamp,
  constraint pkItemPriceHistory primary key (itemPriceHistoryId)
);

create sequence ItemPriceHistorySeq start with 1;

create table Transaction (
  transactionId bigint not null,
  siteId bigint not null,
  transactionTime timestamp not null,
  currency varchar(3) not null,
  totalAmount decimal(15,2) not null,
  taxAmount decimal(15,2) not null,
  transactionType smallint not null,
  constraint pkTransaction primary key (transactionId)
);

create sequence TransactionSeq start with 1;

create table TransactionItem (
  transactionItemId bigint not null,
  transactionId bigint not null reference Transaction on delete cascade,
  itemPriceHistoryId bigint not null,
  transactionShippingId bigint not null reference TransactionShipping on delete cascade,
  quantity decimal(15,2) not null,
  amount decimal(15,2) not null,
  constraint pkTransactionItem primary key (transactionItemId)
);

create sequence TransactionItemSeq start with 1;

create table TransactionTax (
  transactionTaxId bigint not null,
  transactionId bigint not null reference Transaction on delete cascade,
  taxHistoryId bigint not null,
  targetAmount decimal(15,2) not null,
  amount decimal(15,2) not null,
  constraint pkTransactionTax primary key (transactionTaxId)
);

create sequence TransactionTaxSeq start with 1;

create table TransactionShipping (
  transactionShippingId bigint not null,
  transactionId bigint not null reference Transaction on delete cascade,
  amount decimal(15,2) not null,
  constraint pkTransactionShipping primary key (transactionShippingId)
);

create sequence TransactionShippingSeq start with 1;

create table TransactionCreditTender (
  transactionCreditTenderId bigint not null,
  transactionId bigint not null reference Transaction on delete cascade,
  amount decimal(15,2) not null,
  constraint pkTransactionCreditTender primary key (transactionCreditTenderId)
);

create sequence TransactionCreditTenderSeq start with 1;

# --- !Downs

set referential_integrity false;

drop table if exists Item;

drop table if exists ItemName;

drop table if exists Site;

drop table if exists Category;

drop table if exists CategoryName;

drop table if exists CategoryPath;

drop table if exists SiteCategory;

drop table if exists ItemPrice;

drop table if exists ItemPriceHistory;

drop table if exists Tax;

drop table if exists TaxHistory;

set referential_integrity true;

drop sequence if exists SiteSeq;

drop sequence if exists ItemSeq;

drop sequence if exists CategorySeq;

drop sequence if exists ItemPriceSeq;

drop sequence if exists ItemPriceHistorySeq;

drop sequence if exists TaxSeq;

drop sequence if exists TaxHistorySeq;
