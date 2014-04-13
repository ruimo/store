select
  item_name,
  site_name,
  unit_price,
  (
    select 
      case m.metadata
      when 0 then '在庫なし'
      when 1 then 'お問い合わせください'
      when 2 then '在庫あり'
      else ''
      end
    from site_item_numeric_metadata m
    where m.item_id = item.item_id and m.site_id = site.site_id and m.metadata_type = 1
  ) promotion_index,
  (
    select tm.metadata
    from item_text_metadata tm
    where tm.item_id = item.item_id and tm.metadata_type = 0
  ) about_height
  from item
  inner join item_name on item.item_id = item_name.item_id
  inner join item_description on item.item_id = item_description.item_id 
  inner join item_price on item.item_id = item_price.item_id 
  inner join item_price_history on item_price.item_price_id = item_price_history.item_price_id 
  inner join site_item on item.item_id = site_item.item_id and item_price.site_id = site_item.site_id 
  inner join site on site_item.site_id = site.site_id 
  where item_name.locale_id = 1
  and not exists (
    select coalesce(metadata, 0)
    from site_item_numeric_metadata 
    where item.item_id = site_item_numeric_metadata.item_id
    and site.site_id = site_item_numeric_metadata.site_id 
    and site_item_numeric_metadata.metadata_type = 3
    and site_item_numeric_metadata.metadata = 1
  ) and item_description.locale_id = 1
  and item_price_history.item_price_history_id = (
    select iph.item_price_history_id
    from item_price_history iph 
    where iph.item_price_id = item_price.item_price_id and iph.valid_until > timestamp'2014-04-08 21:22:10'
    order by iph.valid_until limit 1 
  )
  order by item_name.item_name ASC;

