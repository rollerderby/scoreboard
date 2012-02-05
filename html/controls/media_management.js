

$sb(function() {
  $("#tabBar>button")
    .filter(".Remove").click(function() {
      createRemoveTypeDialog();
    }).end()
    .filter(".Add").click(function() {
      createAddSpecificDialog();
    }).end()
    .button();

  $("body>table.TypeTemplate")
    .find("tr.Type>th.Type>button")
    .filter(".Show,.Hide").click(function() {
      $(this).closest("table").toggleClass("Hide");
    }).end()
    .filter(".ShowAvailable,.HideAvailable").click(function() {
      $(this).closest("table").toggleClass("ShowAvailable");
    }).end()
    .end()
    .find("tr.Controls>th.Remove>button.Remove").click(function() {
      var table = $(this).closest("table");
      table.data("sbType").children(table.data("childName")).each(function() {
        $sb(this).$sbRemove();
      });
    }).end()
    .find("tr.Controls>th.Preview>button.ShowPreview").click(function() {
      $(this).closest("table.Type").find("tr.Item>td.Preview").addClass("Show");
    }).end()
    .find("tr.Controls>th.Preview>button.HidePreview").click(function() {
      $(this).closest("table.Type").find("tr.Item>td.Preview").removeClass("Show");
    }).end()
    .find("tr>td.Preview>button.Preview").click(function() {
      $(this).parent().addClass("Show");
    }).end();

  $.each( [ "Images", "Videos", "CustomHtml" ], function() {
    $("body>div.TabTemplate").clone()
      .removeClass("TabTemplate").attr("id", String(this))
      .appendTo("#tabsDiv");
  });

  $("#tabsDiv").tabs();

  setupTab("Images", "Image", "<img>");
  setupTab("Videos", "Video", "<video>");
  setupTab("CustomHtml", "Html", "<iframe>");
});

function setupTab(parentName, childName, previewElement) {
  $sb(parentName).$sbBindAddRemoveEach("Type", function(event,node) {
    var newTable = $("body>table.TypeTemplate").clone(true)
      .removeClass("TypeTemplate").addClass("Type")
      .data({
        type: node.$sbId,
        sbType: node,
        parentName: parentName,
        childName: childName,
        previewElement: previewElement
      })
      .find("th.Type>button.Upload").click(function() {
        createUploadMediaDialog($(this).closest("table"));
      }).end()
      .find("th.Type>button.RefreshAvailable,th.Type>button.ShowAvailable").click(function() {
        refreshAvailable($(this).closest("table"));
      }).end()
      .find("th.Type>button.AddAllAvailable").click(function() {
        $(this).closest("table").find("tr.Available>td.Add>button.Add").click();
      }).end()
      .find("thead button").button().end()
      .find("tr.Type>th.Type>a.Type>span.Type").html(node.$sbId).end();
    _windowFunctions.appendAlphaSortedByData($("#"+parentName+">div.Type"), newTable, "type");
  }, function(event,node) {
    $("#"+parentName+">div.Type>table.Type")
      .filter(function() { return $(this).data("type") == node.$sbId; })
      .remove();
  });

  $sb(parentName).$sbBindAddRemoveEach({
    childname: childName,
    subChildren: true,
    add: function(event,node) {
      var sbType = $sb(node.parent());
      var type = sbType.$sbId;
      var srcprefix = "/"+parentName.toLowerCase()+"/"+type+"/";
      var table = $("#"+parentName+">div.Type>table.Type")
        .filter(function() { return $(this).data("type") == type; });
      var newRow = table.find("tr.ItemTemplate").clone(true)
        .removeClass("ItemTemplate").addClass("Item").data("sbId", node.$sbId);
      newRow.find("button").button()
        .filter(".Remove").click(function() { node.$sbRemove(); });
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
      _windowFunctions.appendAlphaSortedByData(table.children("tbody"), newRow, "sbId", 2);
      table.find("tr.Available").filter(function() { return $(this).data("sbId") == node.$sbId; }).remove();
    },
    remove: function(event,node) {
      $("#"+parentName+">div.Type>table.Type")
        .filter(function() { return $(this).data("type") == $sb(node.parent()).$sbId; })
        .find("tr.Item:not(.Available)")
        .filter(function() { return $(this).data("sbId") == node.$sbId; })
        .remove();
    }
  });
}

function refreshAvailable(table) {
  var parentName = table.data("parentName");
  var childName = table.data("childName");
  var previewElement = table.data("previewElement");
  var media = parentName.toLowerCase();
  var type = table.data("type");
  var sbType = table.data("sbType");

  // Disable the buttons while refreshing
  table.find("th.Type>button.AvailableControl").button("disable");

  $.get("/Media/list", { media: media, type: type })
    .fail(function(jqxhr, textStatus, errorThrown) {
      alert("Error getting available media");
    })
    .always(function() {
      table.find("th.Type>button.AvailableControl").button("enable");
    })
    .done(function(data, status, jqxhr) {
      table.find("tr.Available").remove();
      $.each(String(jqxhr.responseText.trim()).split("\n"), function(i,e) {
        var name = e.replace(/\.[^.]*$/, "");
        var source = "/"+media+"/"+type+"/"+e;
        if (sbType.children(childName).filter(function() { return $sb(this).$sbId == e; }).length)
          return;
        var newRow = table.find("tr.AvailableTemplate").clone(true)
          .removeClass("AvailableTemplate").addClass("Available Item").data("sbId", e);
        newRow.find("button").button()
          .filter(".Add").click(function() {
            var newElem = sbType.$sb(childName+"("+e+")");
            newElem.$sb("Name").$sbSet(name);
            newElem.$sb("Src").$sbSet(source);
            $(this).button("option", "label", "Adding...").button("disable");
          });
        newRow.find("td.Name>a").text(name);
        newRow.find("td.Src>a").text(e);
        $(previewElement).attr("src", source).click(function() { 
          $(this).parent().removeClass("Show");
        }).appendTo(newRow.find("td.Preview"));
        _windowFunctions.appendAlphaSortedByData(table.children("tbody"), newRow, "sbId", 2);
      });
    });
}

function createUploadMediaDialog(table) {
  var media = table.data("parentName").toLowerCase();
  var type = table.data("type");
  var div = $("body>div.UploadMediaDialog.DialogTemplate").clone(true)
    .removeClass("DialogTemplate");
  var uploader = div.find("div.Upload").fileupload({
    url: "/Media/upload",
    dropZone: null,
    singleFileUploads: false
  });
  var inputFile = div.find("input:file.File");
  var inputName = div.find("input:text.Name");
  div.dialog({
      title: "Upload media",
      modal: true,
      width: 700,
      close: function() { $(this).dialog("destroy").remove(); },
      buttons: {
        Upload: function() {
          var data = { files: $(this).find("input:file.File")[0].files };
          var length = data.files.length;
          var name = (inputName.prop("disabled") ? undefined : inputName.val());
          var statustxt = "file"+(length>1?"s":"")+(name?" '"+name+"'":"");
          uploader.fileupload("option", "formData", [
            { name: "media", value: media },
            { name: "type", value: type }
          ]);
          if (name)
            uploader.fileupload("option", "formData").push({ name: "name", value: name });
          uploader.fileupload("send", data)
            .done(function(data, textStatus, jqxhr) {
              div.find("a.Status").text(data);
            })
            .fail(function(jqxhr, textStatus, errorThrown) {
              div.find("a.Status").text("Error while uploading "+statustxt+" : "+textStatus);
            })
            .always(function() {
              var newInputFile = inputFile.clone(true).insertAfter(inputFile);
              inputFile.remove();
              inputFile = newInputFile.change();
            });
          uploader.fileupload("option", "formData", []);
        },
        Close: function() {
          $(this).dialog("close");
        }
      }
    });
  var uploadButton = div.dialog("widget").find("div.ui-dialog-buttonset>button:contains('Upload')");
  inputFile.change(function() {
    var files = this.files;
    if (!files || !files.length) {
      inputName.val("").prop("disabled", true);
      uploadButton.button("option", "disabled", true);
      return;
    }
    uploadButton.button("option", "disabled", false);
    if (files.length == 1) {
      var file = files[0];
      var name = file.name.replace(/\.[^.]*$/, "");
      if (file.name.match(/\.[zZ][iI][pP]$/))
        inputName.val("Cannot set name when uploading zip file").prop("disabled", true);
      else
        inputName.val(name).prop("disabled", false);
    } else {
      inputName.val("Cannot set name when uploading multiple files").prop("disabled", true);
    }
  }).change();
}

function createAddSpecificDialog() {
  return $("body>div.AddSpecificDialog.DialogTemplate").clone(true)
    .removeClass("DialogTemplate")
    .dialog({
      title: "Add unavailable media",
      modal: true,
      width: 700,
      close: function() { $(this).dialog("destroy").remove(); },
      buttons: {
        Add: function() {
          var option = $(this).find("select.Media>option:selected");
          var parentName = option.text();
          var childName = option.val();
          var type = $(this).find("input:text.Type").val();
          var name = $(this).find("input:text.Name").val();
          var filename = $(this).find("input:text.Filename").val();
          var id = _crgUtils.checkSbId(filename);
          var src = "/"+parentName.toLowerCase()+"/"+type+"/"+filename;
          var newChild = $sb(parentName+".Type("+type+")."+childName+"("+id+")");
          newChild.$sb("Name").$sbSet(name);
          newChild.$sb("Src").$sbSet(src);
          $(this).dialog("close");
        },
        Cancel: function() {
          $(this).dialog("close");
        }
      }
    });
}

function createRemoveTypeDialog() {
  return $("body>div.RemoveTypeDialog.DialogTemplate").clone(true)
    .removeClass("DialogTemplate")
    .dialog({
      title: "Remove empty Type",
      modal: true,
      width: 700,
      close: function() { $(this).dialog("destroy").remove(); },
      buttons: {
        Remove: function() {
          var option = $(this).find("select.Media>option:selected");
          var parentName = option.text();
          var type = $(this).find("input:text.Type").val();
          var sbType = $sb(parentName).children("Type[Id='"+type+"']");
          if (sbType.length)
            $sb(sbType).$sbRemove();
          $(this).dialog("close");
        },
        Cancel: function() {
          $(this).dialog("close");
        }
      }
    });
}

