var shoppingCartDialog = {
  init: function(arg) {
    var self = this;
    self.arg = arg;

    var cartDialogButtonFactory = function(closeFunc, shoppingCartFunc) {
      var buttons = {};
      buttons[arg.cartDialog.closeButtonTitle] = closeFunc;
      buttons[arg.cartDialog.shoppingCartButtonTitle] = shoppingCartFunc;
      return buttons;
    };

    var errorDialogButtonFactory = function(closeFunc) {
      var buttons = {};
      buttons[arg.errorDialog.closeButtonTitle] = closeFunc;
    }

    $("#cartDialog").dialog({
      autoOpen: false,
      width: '90%',
      height: 550,
      title: arg.cartDialog.title,
      modal: true,
      buttons: cartDialogButtonFactory(
        function() {$(this).dialog("close")},
        function() {
          $(this).dialog("close");
          location.href = arg.cartDialog.showShoppingCartUrl;
        }
      )
    });
  
    $("#errorDialog").dialog({
      autoOpen: false,
      title: 'Server error',
      modal: true,
      buttons: errorDialogButtonFactory(function() {
        $(this).dialog("close");
      })
    });
  },

  _showDialog: function(self, data, status, jqXhr) {
    var dlg = $("#cartDialog");

    var addedContent = $("#cartDialogAddedContent");
    addedContent.empty();
    addedContent.append(
      "<tr>" +
      "  <th class='itemName header'>" + self.arg.cartDialog.itemNameHeader + "</th>" +
      "  <th class='siteName header'>" + self.arg.cartDialog.siteNameHeader + "</th>" +
      "  <th class='unitPrice header'>" + self.arg.cartDialog.unitPriceHeader + "</th>" +
      "  <th class='quantity header'>" + self.arg.cartDialog.quantityHeader + "</th>" +
      "  <th class='subtotal header'>" + self.arg.cartDialog.subtotalHeader + "</th>" +
      "</tr>"
    );
    
    $.each(data.added, function(idx, e) {
      addedContent.append(
        "<tr>" +
        "  <td class='itemName body'>" + e.itemName + "</td>" +
        "  <td class='siteName body'>" + e.siteName + "</td>" +
        "  <td class='unitPrice body'>" + e.unitPrice + "</td>" +
        "  <td class='quantity body'>" + e.quantity + "</td>" +
        "  <td class='price body'>" + e.price + "</td>" +
        "</tr>"
      );
    });

    var currentContent = $("#cartDialogCurrentContent");
    currentContent.empty();
    currentContent.append(
      "<tr>" +
      "  <th class='itemName header'>" + self.arg.cartDialog.itemNameHeader + "</th>" +
      "  <th class='siteName header'>" + self.arg.cartDialog.siteNameHeader + "</th>" +
      "  <th class='unitPrice header'>" + self.arg.cartDialog.unitPriceHeader + "</th>" +
      "  <th class='quantity header'>" + self.arg.cartDialog.quantityHeader + "</th>" +
      "  <th class='subtotal header'>" + self.arg.cartDialog.subtotalHeader + "</th>" +
      "</tr>"
    );

    $.each(data.current, function(idx, e) {
      currentContent.append(
        "<tr>" +
        "  <td class='itemName body'>" + e.itemName + "</td>" +
        "  <td class='siteName body'>" + e.siteName + "</td>" +
        "  <td class='unitPrice body'>" + e.unitPrice + "</td>" +
        "  <td class='quantity body'>" + e.quantity + "</td>" +
        "  <td class='price body'>" + e.price + "</td>" +
        "</tr>"
      );
    });

    var recommendedContent = $("#recommendedContent");
    recommendedContent.empty();

    $.ajax({
      type: 'get',
      url: self.arg.recommendByShoppingCartJsonUrl,
      dataType: 'json',
      cache: false,
      success: function(data, status, jqXhr) {
        if (data.recommended.length != 0) {
          recommendedContent.append(
            '<div class="recommendedItemTitle">' + self.arg.recommendedItemTitle + '</div>'
          )
        }

        $.each(data.recommended, function(idx, e) {
          recommendedContent.append(
            '<div class="recommendedItem">' +
            '  <a href="' + self.arg.itemDetailUrl.replace('111', e.itemId).replace('222', e.siteId) + '">' +
            '    <img src="' + self.arg.itemImageUrl.replace('111', e.itemId) + '" class="recommendedItemImage">' +
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

    dlg.dialog("open");
  },

  _showErrorDialog: function(self, jqXhr, status, error) {
    if (jqXhr.status === 401) {
      var responseJson = $.parseJSON(jqXhr.responseText);
      if (responseJson.status === "Redirect") {
        location.href = responseJson.url;
      }
    }
    else if (jqXhr.status === 400) {
      // CSRF error?
      location.href = "/";
    }
    else {
      var dlg = $("#errorDialog");
      dlg.dialog("open");
    }
  },

  putIntoCart: function(siteId, itemId, quantity) {
    var self = this;

    $.ajax({
      type: 'post',
      url: self.arg.addToShoppingCardJsonUrl + "&urlAfterLogin=" + 
        self.arg.addToShoppingCartUrl.replace('111', siteId).replace('222', itemId).replace('333', quantity),
      data: JSON.stringify({siteId: siteId, itemId: itemId, quantity: quantity}),
      contentType: 'application/json',
      dataType: 'json',
      cache: false,
      success: function(data, status, jqXhr) {
        self._showDialog(self, data, status, jqXhr);
      },
      error: function(jqXhr, status, error) {
        self._showErrorDialog(self, jqXhr, status, error);
      }
    });
  },

  putOrderHistory: function(siteId, tranSiteId) {
    var self = this;

    $.ajax({
      type: 'post',
      url: self.arg.addOrderHistoryJsonUrl,
      data: JSON.stringify({siteId: siteId, tranSiteId: tranSiteId}),
      contentType: 'application/json',
      dataType: 'json',
      cache: false,
      success: function(data, status, jqXhr) {
        self._showDialog(self, data, status, jqXhr);
      },
      error: function(jqXhr, status, error) {
        self._showErrorDialog(self, jqXhr, status, error);
      }
    });
  }
};
