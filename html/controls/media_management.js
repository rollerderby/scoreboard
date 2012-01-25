

$sb(function() {
  $("body>table.TypeTemplate")
    .find("tr.Type>th.Type>button").filter(".Show,.Hide").click(function() {
      $(this).closest("table").toggleClass("Hide");
    }).end().button().end()
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

  var imagesDialog = createAddSpecificDialog("Images", "Image");
  var videosDialog = createAddSpecificDialog("Videos", "Video");
  var customHtmlDialog = createAddSpecificDialog("CustomHtml", "Html");

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
      .find("tr.Type>th.Type>button.Add").click(function() {
        createAddTypeDialog(parentName, node.$sbId, childName);
      }).end()
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
      var type = $sb(node.parent()).$sbId;
      var srcprefix = "/"+parentName.toLowerCase()+"/"+type+"/";
      var table = $("#"+parentName+">div.Type>table.Type").filter(function() {
        return $(this).data("Type") == type;
      });
      var newRow = table.find("tr.ItemTemplate").clone(true)
        .removeClass("ItemTemplate").addClass("Item").data(childName, node.$sbId).data("sb", node);
      node.$sb("Name").$sbControl(newRow.find("td.Name>input:text"));
      node.$sb("Src").$sbControl(newRow.find("td.Src>input:text"), {
        sbelement: {
          convert: function(val) { return String(val).replace(new RegExp("^"+srcprefix), ""); }
        }, sbcontrol: {
          convert: function(val) { return srcprefix + String(val); }
        }
      });
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

function createAddTypeDialog(parentName, type, childName) {
  var media = parentName.toLowerCase();
  var sbType = $sb(parentName+".Type("+type+")");
  var div = $("body>div.AddTypeDialogTemplate").clone(true)
    .removeClass("AddTypeDialogTemplate").addClass("AddTypeDialog");
  var listTable = div.children("table.List");
  var listTbody = listTable.children("tbody");
  var statusTd = listTbody.find("tr.Status>td");
  var addFiles = function() {
    alert("implement add files");
    div.dialog("close");
  };
  var refreshList = function() {
    $.get("/listmedia", { media: media, type: type })
      .fail(function() { statusTd.text("Error loading media list").show(); })
      .done(function(data, status, jqxhr) {
        $.each(String(jqxhr.responseText.trim()).split("\n"), function(i,e) {
          var name = e.replace(/\.[^.]*$/, "");
          var source = "/"+media+"/"+type+"/"+e;
          var exists = sbType.children(childName).children("Src:contains("+source+")").length;
          var row = $("<tr>").addClass("Media")
            .append($("<td>").addClass("Add"))
            .append($("<td>").addClass("Source"))
            .data("source", e)
            .addClass(exists?"Exists":"New");
          $("<a>").text("Added").hide().appendTo(row.children("td.Add"));
          $("<button>").text("Add").button().click(function() {
            var newElem = $sb(parentName+".Type("+type+")."+childName+"("+name+")");
            newElem.$sb("Name").$sbSet(name);
            newElem.$sb("Src").$sbSet(source);
            $(this).hide().prev("a").show();
          }).appendTo(row.children("td.Add"));
          row.children("td.Source").text(e);
          _windowFunctions.appendAlphaSortedByData(listTbody, row, "source", 1);
        });
        statusTd.hide();
      })
      .always(function() { div.children("button.Refresh").button("enable"); });
  };
  div.children("button.Refresh").button().click(function() {
    $(this).button("disable");
    listTbody.children("tr.Media").remove();
    statusTd.text("Loading...").show();
    refreshList();
  }).click();
  div.dialog({
    title: "Add '"+type+"' media",
    modal: true,
    width: "700px",
    close: function() { div.dialog("destroy").remove(); },
    buttons: { Close: function() { div.dialog("close"); } }
  });
}

function createAddSpecificDialog(parentName, childName) {
  var close = function() {
    $(this).find("input:text").val("");
    $(this).dialog("close");
  };
  return $("body>div.AddSpecificDialogTemplate").clone(true)
    .removeClass("AddSpecificDialogTemplate").addClass("AddSpecificDialog "+parentName+"Dialog")
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

