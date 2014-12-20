var showRecommendation = function(
  recommendedContent, cartApiUrl, title, itemUrlTemplate, imageUrlTemplate
) {
  recommendedContent.empty();

  $.ajax({
    type: 'get',
    url: cartApiUrl,
    dataType: 'json',
    cache: false,
    success: function(data, status, jqXhr) {
      if (data.recommended.length != 0) {
        recommendedContent.append(
          '<div class="recommendedItemTitle">' + title + '</div>'
        )
      }

      $.each(data.recommended, function(idx, e) {
        recommendedContent.append(
          '<div class="recommendedItem">' +
          '  <a href="' + itemUrlTemplate.replace('111', e.itemId).replace('222', e.siteId) + '">' +
          '    <img src="' + imageUrlTemplate.replace('111', e.itemId) + '" class="recommendedItemImage">' +
          '    <div class="itemName">' + e.name + '</div>' +
          '    <div class="price">' + e.price + '</div>' +
          '  </a>' +
          '</div>'
        );
      });
    },
    error: function(jqXhr, status, error) {
      // Just ignore.
    }
  });
};
