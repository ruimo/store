var links = {
  'newer': '@query(0, list.pageSize, 0, "site_item.created desc")',
  'older': '@query(0, list.pageSize, 0, "site_item.created")',
  'name': '@query(0, list.pageSize, 0, "item_name.item_name")',
  'nameReverse': '@query(0, list.pageSize, 0, "item_name.item_name desc")',
  'price': '@query(0, list.pageSize, 0, "item_price_history.unit_price")',
  'priceReverse': '@query(0, list.pageSize, 0, "item_price_history.unit_price desc")',
}
var onSortOrderChanged = function(o) {
  location.href = links[o.value];
}
