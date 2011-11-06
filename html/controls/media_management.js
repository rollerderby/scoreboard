

$sb(function() {
  $("body>table.TypeTemplate")
    .find("tr.Type>th.Type>button").click(function() {
      $(this).closest("table").toggleClass("Hide");
    }).button().end()
    .find("tr.Controls>th.Remove>input:checkbox.Remove").click(function() {
      $(this).closest("table.Type").find("tr.Item>td.Remove>input:checkbox")
        .prop("checked", this.checked);
    }).end()
    .find("tr.Controls>th.Preview>button.ShowPreview").click(function() {
      $(this).closest("table.Type").find("tr.Item>td.Preview").addClass("Show");
    }).button().end()
    .find("tr.Controls>th.Preview>button.HidePreview").click(function() {
      $(this).closest("table.Type").find("tr.Item>td.Preview").removeClass("Show");
    }).button().end()
    .find("tr.ItemTemplate>td.Preview>button.Preview").click(function() {
      $(this).parent().addClass("Show");
    }).button().end();

  $.each( [ "Images", "Videos", "CustomHtml" ], function() {
    $("body>div.TabTemplate").clone()
      .removeClass("TabTemplate").attr("id", String(this))
      .appendTo("#tabsDiv");
  });

  $("#tabsDiv").tabs();

  setupTab("Images", "Image", "<img>");
  setupTab("Videos", "Video", "<video>");
  setupTab("CustomHtml", "Html", "<iframe>");

  var imagesDialog = createDialog("Images", "Image");
  var videosDialog = createDialog("Videos", "Video");
  var customHtmlDialog = createDialog("CustomHtml", "Html");

  $("#Images>div.Buttons>button.Add").button()
    .click(function() { imagesDialog.dialog("open"); });
  $("#Videos>div.Buttons>button.Add").button()
    .click(function() { videosDialog.dialog("open"); });
  $("#CustomHtml>div.Buttons>button.Add").button()
    .click(function() { customHtmlDialog.dialog("open"); });
  $("#Images,#Videos,#CustomHtml").each(function() {
    var div = $(this);
    div.find(">div.Buttons>button.Remove").button().click(function() {
      div.find(">div.Type>table.Type").each(function() {
        var removed = 0;
        $(this).find("tr.Item").each(function() {
          if ($(this).find("td.Remove>input:checkbox").is(":checked")) {
            $(this).data("sb").$sbRemove();
            removed++;
          }
        });
        if ($(this).find("tr.Item").length == removed)
          $(this).data("sb").$sbRemove();
      });
    });
  });
});

function setupTab(parentName, childName, previewElement) {
  var compareType = function(o, n) {
    return ($(o).data("Type") > $(n).data("Type"));
  };

  $sb(parentName).$sbBindAddRemoveEach("Type", function(event,node) {
    var newTable = $("body>table.TypeTemplate").clone(true)
      .removeClass("TypeTemplate").addClass("Type").data("Type", node.$sbId).data("sb", node)
      .find("tr.Type>th.Type>a.Type>span.Type").html(node.$sbId).end();
    _windowFunctions.appendSorted($("#"+parentName+">div.Type"), newTable, compareType);
  }, function(event,node) {
    $("#"+parentName+">div.Type>table.Type")
      .filter(function() { return $(this).data("Type") == node.$sbId; })
      .remove();
  });

  var compareChildren = function(o, n) {
    return $(o).data(childName) > $(n).data(childName);
  };

  $sb(parentName).$sbBindAddRemoveEach({
    childname: childName,
    subChildren: true,
    add: function(event,node) {
      var table = $("#"+parentName+">div.Type>table.Type").filter(function() {
        return $(this).data("Type") == $sb(node.parent()).$sbId;
      });
      var newRow = table.find("tr.ItemTemplate").clone(true)
        .removeClass("ItemTemplate").addClass("Item").data(childName, node.$sbId).data("sb", node);
      node.$sb("Name").$sbControl(newRow.find("td.Name>input:text"));
      node.$sb("Src").$sbControl(newRow.find("td.Src>input:text"));
      node.$sb("Src").$sbElement(previewElement).click(function() { 
        $(this).parent().removeClass("Show");
      }).appendTo(newRow.find("td.Preview"));
      _windowFunctions.appendSorted(table.children("tbody"), newRow, compareChildren, 1);
    },
    remove: function(event,node) {
      $("#"+parentName+">div.Type>table.Type")
        .filter(function() { return $(this).data("Type") == $sb(node.parent()).$sbId; })
        .find("tr.Item")
        .filter(function() { return $(this).data(childName) == node.$sbId; })
        .remove();
    }
  });
}

function createDialog(parentName, childName) {
  var close = function() {
    $(this).find("input:text").val("");
    $(this).dialog("close");
  };
  return $("body>div.AddDialogTemplate").clone(true)
    .removeClass("AddDialogTemplate").addClass("AddDialog "+parentName+"Dialog")
    .dialog({
      modal: true,
      autoOpen: false,
      width: "700px",
      buttons: { Add: function() {
          var type = $(this).find("input:text.Type").val();
          var name = $(this).find("input:text.Name").val();
          var src = $(this).find("input:text.Src").val();
          var id = _crgUtils.checkSbId(name);
          var newChild = $sb(parentName+".Type("+type+")."+childName+"("+id+")");
          newChild.$sb("Name").$sbSet(name);
          newChild.$sb("Src").$sbSet(src);
          close.call(this);
        }, Close: close
      }
    });
}

