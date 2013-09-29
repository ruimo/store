delete from item_numeric_metadata where item_numeric_metadata_id < 1000;

delete from item_name where item_id < 1000;

delete from item_description where item_id < 1000;

delete from site_item where item_id < 1000;

delete from category_name where category_id <1000;

delete from category_path where ancestor = descendant and ancestor < 1000;

delete from site_category where category_id < 1000;

delete from tax where tax_id < 1000;

delete from tax_history where tax_history_id < 1000;

delete from item_price where item_price_id < 1000;

delete from item_price_history where item_price_history_id < 1000;

delete from shipping_fee_history where shipping_fee_history_id < 1000;

delete from shipping_fee where shipping_fee_id < 1000;

delete from shipping_box where shipping_box_id < 1000;

delete from site_item_numeric_metadata where site_item_numeric_metadata_id < 1000;

delete from item where item_id < 1000;

delete from category where category_id < 1000;

delete from site where site_id < 1000;

insert into site (site_id, locale_id, site_name) values (1, 1, 'ハードマップ');
insert into site (site_id, locale_id, site_name) values (2, 1, '老松通商');

insert into category (category_id) values (1);
insert into category (category_id) values (2);
insert into category (category_id) values (3);

-- Stock
insert into item (item_id, category_id) values (1, 1);
insert into item (item_id, category_id) values (2, 2);
insert into item (item_id, category_id) values (3, 3);
insert into item (item_id, category_id) values (4, 1);
insert into item (item_id, category_id) values (5, 2);
insert into item (item_id, category_id) values (6, 1);
insert into item (item_id, category_id) values (7, 2);
insert into item (item_id, category_id) values (8, 1);
insert into item (item_id, category_id) values (9, 2);
insert into item (item_id, category_id) values (10, 1);
insert into item (item_id, category_id) values (11, 2);
insert into item (item_id, category_id) values (12, 1);
insert into item (item_id, category_id) values (13, 2);
insert into item (item_id, category_id) values (14, 1);
insert into item (item_id, category_id) values (15, 2);

-- Promotion
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (16, 1, 1, 1, 0);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (17, 2, 2, 1, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (18, 1, 3, 1, 1);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (19, 2, 4, 1, 1);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (20, 1, 5, 1, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (21, 1, 6, 1, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (22, 1, 7, 1, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (23, 1, 8, 1, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (24, 1, 9, 1, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (25, 1, 10, 1, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (26, 1, 11, 1, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (27, 1, 12, 1, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (28, 1, 13, 1, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (29, 1, 14, 1, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (30, 1, 15, 1, 2);

-- Shipping size
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (31, 1, 1, 2, 1);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (32, 2, 2, 2, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (33, 1, 3, 2, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (34, 2, 4, 2, 1);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (35, 1, 5, 2, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (36, 1, 6, 2, 1);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (37, 1, 7, 2, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (38, 1, 8, 2, 1);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (39, 1, 9, 2, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (40, 1, 10, 2, 1);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (41, 1, 11, 2, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (42, 1, 12, 2, 1);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (43, 1, 13, 2, 2);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (44, 1, 14, 2, 1);
insert into site_item_numeric_metadata
  (site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata) values (45, 1, 15, 2, 2);

-- Height
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (1, 1, 0, 150);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (2, 2, 0, 300);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (3, 3, 0, 1500);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (4, 4, 0, 100);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (5, 5, 0, 200);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (6, 6, 0, 200);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (7, 7, 0, 200);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (8, 8, 0, 200);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (9, 9, 0, 200);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (10, 10, 0, 200);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (11, 11, 0, 200);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (12, 12, 0, 200);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (13, 13, 0, 200);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (14, 14, 0, 200);
insert into item_numeric_metadata (item_numeric_metadata_id, item_id, metadata_type, metadata) values (15, 15, 0, 200);

insert into category_name (locale_id, category_id, category_name) values (1, 1, 'CPU');
insert into category_name (locale_id, category_id, category_name) values (1, 2, 'ディスプレイ');
insert into category_name (locale_id, category_id, category_name) values (1, 3, 'キーボード');

insert into item_name (item_name_id, locale_id, item_id, item_name) values (1, 1, 1, 'Intel i7-3770K Box');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (2, 1, 2, 'DELL 23インチ');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (3, 1, 3, 'Cherry 青軸');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (4, 1, 4, 'Motorola MC6800 1MHz');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (5, 1, 5, 'NANAO CRT 20インチ');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (6, 1, 6, 'Hitachi HD6309 2MHz');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (7, 1, 7, 'DELL 30インチ U3014');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (8, 1, 8, 'Zilog Z80 4MHz');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (9, 1, 9, 'IYAMA 15インチ');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (10, 1, 10, 'Intel 8080');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (11, 1, 11, 'HITACHI 20インチ CRT');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (12, 1, 12, 'Motorola MC68000 8MHz');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (13, 1, 13, 'HITACHI 17インチ CRT');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (14, 1, 14, 'Intel 8087 8MHz');
insert into item_name (item_name_id, locale_id, item_id, item_name) values (15, 1, 15, 'IYAMA 23インチ');

insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (1, 1, 1, 'Intel CPU', 1);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (2, 1, 2, 'DELL LCD', 2);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (3, 1, 3, 'Cherry', 1);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (4, 1, 4, 'Motorola', 2);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (5, 1, 5, 'NANAO', 1);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (6, 1, 6, 'Hitachi', 1);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (7, 1, 7, 'DELL 30', 1);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (8, 1, 8, 'Zilog Z80', 1);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (9, 1, 9, 'IYAMA', 1);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (10, 1, 10, 'Intel', 1);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (11, 1, 11, 'HITACHI 20', 1);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (12, 1, 12, 'Motorola', 1);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (13, 1, 13, 'HITACHI', 1);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (14, 1, 14, 'Intel co-processor', 1);
insert into item_description(item_description_id, locale_id, item_id, description, site_id)
  values (15, 1, 15, 'IYAMA', 1);

insert into site_item(item_id, site_id) values (1, 1);
insert into site_item(item_id, site_id) values (2, 2);
insert into site_item(item_id, site_id) values (3, 1);
insert into site_item(item_id, site_id) values (4, 2);
insert into site_item(item_id, site_id) values (5, 1);
insert into site_item(item_id, site_id) values (6, 1);
insert into site_item(item_id, site_id) values (7, 1);
insert into site_item(item_id, site_id) values (8, 1);
insert into site_item(item_id, site_id) values (9, 1);
insert into site_item(item_id, site_id) values (10, 1);
insert into site_item(item_id, site_id) values (11, 1);
insert into site_item(item_id, site_id) values (12, 1);
insert into site_item(item_id, site_id) values (13, 1);
insert into site_item(item_id, site_id) values (14, 1);
insert into site_item(item_id, site_id) values (15, 1);

insert into category_path (ancestor, descendant, path_length) values (1, 1, 0);
insert into category_path (ancestor, descendant, path_length) values (2, 2, 0);
insert into category_path (ancestor, descendant, path_length) values (3, 3, 0);

insert into site_category (category_id, site_id) values (1, 1);
insert into site_category (category_id, site_id) values (1, 2);
insert into site_category (category_id, site_id) values (2, 1);
insert into site_category (category_id, site_id) values (2, 2);
insert into site_category (category_id, site_id) values (3, 1);
insert into site_category (category_id, site_id) values (3, 2);

insert into tax (tax_id) values (1);

insert into tax_history (tax_history_id, tax_id, tax_type, rate, valid_until)
  values (1, 1, 1, 5, timestamp '9999-12-31 00:00:00');

insert into item_price (item_price_id, site_id, item_id) values (1, 1, 1);
insert into item_price (item_price_id, site_id, item_id) values (2, 1, 3);
insert into item_price (item_price_id, site_id, item_id) values (3, 1, 5);
insert into item_price (item_price_id, site_id, item_id) values (4, 2, 2);
insert into item_price (item_price_id, site_id, item_id) values (5, 2, 4);
insert into item_price (item_price_id, site_id, item_id) values (6, 1, 6);
insert into item_price (item_price_id, site_id, item_id) values (7, 1, 7);
insert into item_price (item_price_id, site_id, item_id) values (8, 1, 8);
insert into item_price (item_price_id, site_id, item_id) values (9, 1, 9);
insert into item_price (item_price_id, site_id, item_id) values (10, 1, 10);
insert into item_price (item_price_id, site_id, item_id) values (11, 1, 11);
insert into item_price (item_price_id, site_id, item_id) values (12, 1, 12);
insert into item_price (item_price_id, site_id, item_id) values (13, 1, 13);
insert into item_price (item_price_id, site_id, item_id) values (14, 1, 14);
insert into item_price (item_price_id, site_id, item_id) values (15, 1, 15);

insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (1, 1, 1, 1, 800, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (2, 2, 1, 1, 1200, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (3, 3, 1, 1, 400, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (4, 4, 1, 1, 3000, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (5, 5, 1, 1, 700, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (6, 6, 1, 1, 700, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (7, 7, 1, 1, 700, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (8, 8, 1, 1, 700, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (9, 9, 1, 1, 700, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (10, 10, 1, 1, 700, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (11, 11, 1, 1, 700, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (12, 12, 1, 1, 700, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (13, 13, 1, 1, 700, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (14, 14, 1, 1, 700, timestamp '9999-12-31 00:00:00');
insert into item_price_history (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until)
  values (15, 15, 1, 1, 700, timestamp '9999-12-31 00:00:00');

insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(1, 1, 1, 10, '小箱10個口');
insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(2, 1, 2, 3, '中箱3個口');
insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(3, 1, 3, 1, '大箱1個口');

insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(4, 2, 1, 8, '小箱8個口');
insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(5, 2, 2, 4, '中箱4個口');
insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(6, 2, 3, 1, '大箱1個口');

-- Japan/Tokyo
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(1, 1, 152, 13);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(2, 2, 152, 13);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(3, 3, 152, 13);

insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(4, 4, 152, 13);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(5, 5, 152, 13);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(6, 6, 152, 13);

insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, fee, valid_until)
  values(1, 1, 800, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, fee, valid_until)
  values(2, 2, 1000, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, fee, valid_until)
  values(3, 3, 1300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, fee, valid_until)
  values(4, 4, 900, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, fee, valid_until)
  values(5, 5, 1100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, fee, valid_until)
  values(6, 6, 1500, timestamp '9999-12-31 00:00:00');

