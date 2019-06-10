
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */


$(function() {

	$("body>table.TypeTemplate")
		.find("tr.Type>th.Type>button")
		.filter(".Show,.Hide").click(function() {
			$(this).closest("table").toggleClass("Hide");
		}).end();

	$.each( [ "images", "videos", "customhtml" ], function() {
		$("body>div.TabTemplate").clone()
			.removeClass("TabTemplate").attr("id", String(this))
			.appendTo("#tabsDiv");
	});

	$("#tabsDiv").tabs();

	WS.Register("ScoreBoard.Media.Format(*).Type(*)" , function(k, v) {
		if (k.field != "Type") {
			return;
		}
		if (v == null) {
			$("#"+k.Format+">div.Type>table.Type")
				.filter(function() { return $(this).data("type") == k.Format; })
				.remove();
			return;
		}
		var newTable = $("body>table.TypeTemplate").clone(true)
			.removeClass("TypeTemplate").addClass("Type")
			.attr("type", k.Type)
			.find("th.Type>button.Upload").click(function() {
				createUploadMediaDialog(k.Format, k.Type);
			}).end()
		.find("thead button").button().end()
			.find("tr.Type>th.Type>a.Type>span.Type").text(k.Type).end();
		_windowFunctions.appendAlphaSortedByData($("#"+k.Format+">div.Type"), newTable, "Type");

	});

	WS.Register("ScoreBoard.Media.Format(*).Type(*).File(*).Name", function(k, v) {
		var table = $("#"+k.Format+">div.Type>table.Type[type="+k.Type+"]");
		if (v == null) {
			table.find("tr.Item[file='"+k.File+"']").remove();
			return;
		}
		var newRow = table.find("tr.ItemTemplate").clone(true)
			.removeClass("ItemTemplate").addClass("Item").attr("file", k.File);
		newRow.find("button.Remove").button().click(function() { createRemoveMediaDialog(k.Format, k.Type, k.File); });
		newRow.find("td.Name>input").val(v);
		newRow.find("td.Src>span").text(k.File);
		var previewElement = "<iframe>";
		if (k.Format == "images") {
			previewElement = "<img>";
		} else if (k.Format == "videos") {
			previewElement = "<video>";
		}
		$(previewElement).attr("src", "/"+k.Format+"/"+k.Type+"/" + k.File).appendTo(newRow.find("td.Preview"));
		_windowFunctions.appendAlphaSortedByAttr(table.children("tbody"), newRow, "file", 1);
	});

	WS.AutoRegister();
	WS.Connect();
});

function createRemoveMediaDialog(format, type, file) {
	var div = $("body>div.RemoveMediaDialog.DialogTemplate").clone(true)
		.removeClass("DialogTemplate");
	div.find("a.File").text(format+"/"+type+"/"+file);
	div.dialog({
		title: "Remove media",
		modal: true,
		width: 700,
		close: function() { $(this).dialog("destroy").remove(); },
		buttons: {
			"Yes, Remove": function() {
				div.find("p.Warning,p.Confirm").text("");
				div.find("p.Status").text("Removing file...");
				$.post("/Media/remove", {
					media: format,
					type: type,
					filename: file
				}).fail(function(jqxhr, textStatus, errorThrown) {
					div.find("p.Status").text("Error removing media file: "+jqxhr.responseText);
					div.dialog("option", "buttons", { Close: function() { div.dialog("close"); } });
				}).done(function(data, textStatus, jqXHR) {
					div.dialog("close");
				});
			},
			"No": function() {
				div.dialog("close");
			}
		}
	});
}

function createUploadMediaDialog(format, type) {
	var div = $("body>div.UploadMediaDialog.DialogTemplate").clone(true)
		.removeClass("DialogTemplate");
	var uploader = div.find("div.Upload").fileupload({
		url: "/Media/upload",
		dropZone: null,
		singleFileUploads: false
	});
	var inputFile = div.find("input:file.File");
	var uploadFunction = function() {
		var data = { files: $(this).find("input:file.File")[0].files };
		var length = data.files.length;
		var statustxt = "file"+(length>1?"s":"");
		uploader.fileupload("option", "formData", [
				{ name: "media", value: format },
				{ name: "type", value: type }
		]);
		uploader.fileupload("send", data)
			.done(function(data, textStatus, jqxhr) {
				div.find("a.Status").text(data);
			})
		.fail(function(jqxhr, textStatus, errorThrown) {
			div.find("a.Status").text("Error while uploading : "+jqxhr.responseText);
		})
		.always(function() {
			var newInputFile = inputFile.clone(true).insertAfter(inputFile);
			inputFile.remove();
			inputFile = newInputFile.change();
		});
		uploader.fileupload("option", "formData", []);
	};
	var closeFunction = function() { $(this).dialog("close"); };
	var buttonsCloseOnly = { Close: closeFunction };
	var buttonsUploadClose = { Upload: uploadFunction, Close: closeFunction };

	div.dialog({
		title: "Upload media "+format+" : "+type,
		modal: true,
		width: 700,
		close: function() { $(this).dialog("destroy").remove(); },
		buttons: buttonsCloseOnly
	});
	inputFile.change(function() {
		var files = this.files;
		if (!files || !files.length) {
			inputName.val("").prop("disabled", true);
			div.dialog("option", "buttons", buttonsCloseOnly);
			return;
		}
		div.dialog("option", "buttons", buttonsUploadClose);
	}).change();
}

//# sourceURL=controls\media_management.js
