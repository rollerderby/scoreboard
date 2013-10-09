
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
			var media = parentName.toLowerCase();
			var type = sbType.$sbId;
			var srcprefix = "/"+media+"/"+type+"/";
			var table = $("#"+parentName+">div.Type>table.Type")
				.filter(function() { return $(this).data("type") == type; });
			var newRow = table.find("tr.ItemTemplate").clone(true)
				.removeClass("ItemTemplate").addClass("Item").data("sbId", node.$sbId);
			newRow.find("button").button()
				.filter(".Remove").click(function() { createRemoveMediaDialog(media, type, node); });
			node.$sb("Name").$sbControl(newRow.find("td.Name>input:text"));
			node.$sb("Src").$sbControl(newRow.find("td.Src>input:text"), {
				sbelement: {
					convert: function(val) { return String(val).replace(new RegExp("^"+srcprefix), ""); }
				}, sbcontrol: {
					convert: function(val) { return srcprefix + String(val); }
				}
			});
			node.$sb("Src").$sbElement(previewElement).appendTo(newRow.find("td.Preview"));
			_windowFunctions.appendAlphaSortedByData(table.children("tbody"), newRow, "sbId", 1);
		},
		remove: function(event,node) {
			$("#"+parentName+">div.Type>table.Type")
				.filter(function() { return $(this).data("type") == $sb(node.parent()).$sbId; })
				.find("tr.Item")
				.filter(function() { return $(this).data("sbId") == node.$sbId; })
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
	var uploadFunction = function() {
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

