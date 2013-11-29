select
  tbl.site_name, tbl.box_name, tbl.box_size,
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id1
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id2
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id3
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id4
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id5
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id6
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id7
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id8
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id9
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id10
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id11
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id12
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id13
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id14
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id15
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id16
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id17
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id18
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id19
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id20
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id21
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id22
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id23
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id24
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id25
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id26
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id27
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id28
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id29
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id30
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id31
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id32
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id33
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id34
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id35
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id36
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id37
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id38
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id39
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id40
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id41
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id42
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id43
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id44
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id45
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id46
   and current_timestamp < valid_until
   order by valid_until
   limit 1),
  (select fee from shipping_fee_history
   where shipping_fee_history.shipping_fee_id = tbl.id47
   and current_timestamp < valid_until
   order by valid_until
   limit 1)
from (
  select
    site_name, box_name, box_size,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=1) id1,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=2) id2,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=3) id3,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=4) id4,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=5) id5,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=6) id6,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=7) id7,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=8) id8,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=9) id9,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=10) id10,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=11) id11,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=12) id12,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=13) id13,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=14) id14,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=15) id15,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=16) id16,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=17) id17,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=18) id18,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=19) id19,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=20) id20,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=21) id21,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=22) id22,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=23) id23,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=24) id24,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=25) id25,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=26) id26,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=27) id27,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=28) id28,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=29) id29,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=30) id30,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=31) id31,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=32) id32,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=33) id33,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=34) id34,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=35) id35,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=36) id36,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=37) id37,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=38) id38,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=39) id39,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=40) id40,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=41) id41,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=42) id42,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=43) id43,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=44) id44,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=45) id45,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=46) id46,
    (select shipping_fee_id from shipping_fee where shipping_box_id = shipping_box.shipping_box_id and location_code=47) id47
  from shipping_box
    inner join site on site.site_id = shipping_box.site_id
    order by site_name, shipping_box.item_class
) tbl
;