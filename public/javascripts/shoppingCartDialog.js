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
      width: '80%',
      height: 450,
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
  putIntoCart: function(siteId, itemId) {
    var self = this;

    $.ajax({
      type: 'post',
      url: self.arg.addToShoppingCardJsonUrl,
      data: JSON.stringify({siteId: siteId, itemId: itemId}),
      contentType: 'application/json',
      dataType: 'json',
      cache: false,
      success: function(data, status, jqXhr) {
        var dlg = $("#cartDialog");
        var content = $("#cartDialogContent");
        content.empty();
        content.append(
          "<tr>" +
          "  <th class='itemName header'>" + self.arg.cartDialog.itemNameHeader + "</th>" +
          "  <th class='siteName header'>" + self.arg.cartDialog.siteNameHeader + "</th>" +
          "  <th class='unitPrice header'>" + self.arg.cartDialog.unitPriceHeader + "</th>" +
          "  <th class='quantity header'>" + self.arg.cartDialog.quantityHeader + "</th>" +
          "  <th class='subtotal header'>" + self.arg.cartDialog.subtotalHeader + "</th>" +
          "</tr>"
        );
        $.each(data, function(idx, e) {
          content.append(
            "<tr>" +
            "  <td class='itemName body'>" + e.itemName + "</td>" +
            "  <td class='siteName body'>" + e.siteName + "</td>" +
            "  <td class='unitPrice body'>" + e.unitPrice + "</td>" +
            "  <td class='quantity body'>" + e.quantity + "</td>" +
            "  <td class='price body'>" + e.price + "</td>" +
            "</tr>"
          );
        });
        dlg.dialog("open");
      },
      error: function(jqXhr, status, error) {
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
      }
    });
  }
};
