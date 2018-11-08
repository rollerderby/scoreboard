
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */


$sb(function() {

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

	setupTab("ScoreBoard.Media.images", "File", "<img>");
	setupTab("ScoreBoard.Media.videos", "File", "<video>");
	setupTab("ScoreBoard.Media.customhtml", "File", "<iframe>");
});

function setupTab(parentName, childName, previewElement) {
	$sb(parentName).$sbBindAddRemoveEach("", function(event,node) {
		var newTable = $("body>table.TypeTemplate").clone(true)
			.removeClass("TypeTemplate").addClass("Type")
			.data({
				type: node.$sbName,
				sbType: node,
				parentName: parentName,
				childName: childName,
				previewElement: previewElement
			})
			.find("th.Type>button.Upload").click(function() {
				createUploadMediaDialog($(this).closest("table"));
			}).end()
			.find("thead button").button().end()
			.find("tr.Type>th.Type>a.Type>span.Type").html(node.$sbName).end();
		_windowFunctions.appendAlphaSortedByData($("#"+parentName.split(".")[2]+">div.Type"), newTable, childName);
	}, function(event,node) {
		$("#"+parentName.split(".")[2]+">div.Type>table.Type")
			.filter(function() { return $(this).data("type") == node.$sbName; })
			.remove();
	});

	$sb(parentName).$sbBindAddRemoveEach({
		childname: childName,
		subChildren: true,
		add: function(event,node) {
			var sbType = $sb(node.parent());
			var media = parentName.split(".")[2];
			var type = sbType.$sbName;
			var srcprefix = "/"+media+"/"+type+"/";
			var table = $("#"+media+">div.Type>table.Type")
				.filter(function() { return $(this).data("type") == type; });
			var newRow = table.find("tr.ItemTemplate").clone(true)
				.removeClass("ItemTemplate").addClass("Item").data("sbName", node.$sbName);
			newRow.find("button").button()
				.filter(".Remove").click(function() { createRemoveMediaDialog(media, type, node); });
			node.$sb("Name").$sbControl(newRow.find("td.Name>input:text"));
			node.$sb("Src").$sbElement(newRow.find("td.Src>span"), {
				sbelement: {
					convert: function(val) { return String(val).replace(new RegExp("^"+srcprefix), ""); }
				}});
			node.$sb("Src").$sbElement(previewElement).appendTo(newRow.find("td.Preview"));
			_windowFunctions.appendAlphaSortedByData(table.children("tbody"), newRow, "sbName", 1);
		},
		remove: function(event,node) {
			$("#"+parentName.split(".")[2]+">div.Type>table.Type")
				.filter(function() { return $(this).data("type") == $sb(node.parent()).$sbName; })
				.find("tr.Item")
				.filter(function() { return $(this).data("sbName") == node.$sbName; })
				.remove();
		}
	});
}

function createRemoveMediaDialog(media, type, node) {
	var filename = node.$sbId;
	var div = $("body>div.RemoveMediaDialog.DialogTemplate").clone(true)
		.removeClass("DialogTemplate");
	div.find("a.File").text(media+"/"+type+"/"+filename);
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
					media: media,
					type: type,
					filename: filename
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

function createUploadMediaDialog(table) {
	var media = table.data("parentName").split(".")[2];
	var type = table.data("type");
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
			{ name: "media", value: media },
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
			title: "Upload media "+media+" : "+type,
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

//# sourceURL=controls\media_management.js
