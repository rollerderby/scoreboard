function createDataManagementTab(tab) {

	tab.attr('id', 'DataManagementTab').append($('<ul>').attr('id', 'dmTabBar'));

	createTab('Image', 'images');
	createTab('Video', 'videos');
	createTab('Custom Screens', 'custom');
	createTab('Game Data', 'game-data');
	createSaveLoadTab(createTab('ScoreBoard Data', 'saveLoad'));
	tab.tabs();

	function createTab(name, id) {
		var newTab = $('<div>').attr('id', id).append($('<div>').addClass('Type'))
		.appendTo(tab);
		$('<li>').append($('<a>').attr('href', '#'+id).text(name))
		.appendTo($('#dmTabBar'));
		return newTab;
	}
	
	var typeTemplate = $('<table>').addClass('Type')
		.append($('<col>')).append($('<col>')).append($('<col>')).append($('<col>'))
		.append($('<thead>').append($('<tr>').addClass('Type')
			.append($('<th>').attr('colspan', '4').addClass('Type')
				.append($('<button>').addClass('Show').text('Show').button().on('click', function() {
					$(this).closest("table").removeClass("Hide");
				}))
				.append($('<button>').addClass('Hide').text('Hide').button().on('click', function() {
					$(this).closest("table").addClass("Hide");
				}))
				.append($('<a>').addClass('Type')
					.append($('<span>').addClass('Label').text('Type: '))
					.append($('<span>').addClass('Type')))
				.append($('<button>').addClass('Upload Right').text('Upload').button())))
			.append($('<tr>').addClass('Controls')
				.append($('<th>').addClass('Remove'))
				.append($('<th>').addClass('Name').text('Name'))
				.append($('<th>').addClass('Preview'))
				.append($('<th>').addClass('Download'))))
		.append($('<tbody>'));
	
	WS.Register("ScoreBoard.Media.Format(*).Type(*)" , function(k, v) {
		if (v == null && k.field == "Type") {
			tab.find(">#"+k.Format+">div.Type>table.Type")
			.filter(function() { return $(this).data("type") == k.Format; })
			.remove();
			return;
		}
		if (tab.find(">#"+k.Format+">div.Type>table.Type[type="+k.Type+"]").length == 0) {
			var newTable = typeTemplate.clone(true).attr("type", k.Type)
			.find("th.Type>button.Upload").on('click', function() {
				createUploadMediaDialog(k.Format, k.Type);
			}).end()
			.find("tr.Type>th.Type>a.Type>span.Type").text(k.Type).end();
			_windowFunctions.appendAlphaSortedByAttr($("#"+k.Format+">div.Type"), newTable, "Type");
		}

	});

	var itemTemplate = $('<tr>').addClass('Item')
		.append($('<td>').addClass('Remove').append($('<button>').addClass('Remove')))
		.append($('<td>').addClass('Name').append($('<input type="text">').addClass('Name')))
		.append($('<td>').addClass('Preview'))
		.append($('<td>').addClass('Download').append($('<a download>').addClass('Download')));
	
	var itemTemplateNoPreview = $('<tr>').addClass('Item')
		.append($('<td>').addClass('Remove').append($('<button>').addClass('Remove')))
		.append($('<td>').attr('colspan', '2').addClass('Src'))
		.append($('<td>').addClass('Download').append($('<a download>').addClass('Download')));
	
	WS.Register('ScoreBoard.Media.Format(*).Type(*).File(*).Name', function(k, v) {
		var table = tab.find('>#'+k.Format+'>div.Type>table.Type[type='+k.Type+']');
		table.find('tr.Item[file="'+k.File+'"]').remove();
		if (v == null) {
			return;
		}
		var newRow;
		if (k.Format == 'game-data') {
			newRow = itemTemplateNoPreview.clone(true);
		} else {
			newRow = itemTemplate.clone(true);
		}
		newRow.attr('name', v).attr('file', k.File);
		newRow.find('button.Remove').text('Remove').button().on('click', function() { createRemoveMediaDialog(k.Format, k.Type, k.File); });
		newRow.find('td.Name>input').val(v).on('change', function(e) {
			WS.Set('ScoreBoard.Media.Format('+k.Format+').Type('+k.Type+').File('+k.File+').Name', e.target.value)
		});
		newRow.find('td.Src').text(k.File);
		newRow.find('td.Download>a').attr('href', '/'+k.Format+'/'+k.Type+'/' + k.File).text('Download').button();
		var previewElement = '<iframe>';
		if (k.Format == 'images') {
			previewElement = '<img>';
		} else if (k.Format == 'videos') {
			previewElement = '<video>';
		}
		$(previewElement).attr('src', '/'+k.Format+'/'+k.Type+'/' + k.File).appendTo(newRow.find('td.Preview'));
		_windowFunctions.appendAlphaSortedByAttr(table.children('tbody'), newRow, 'name', 1);
	});
	
	var removeDialogTemplate = $('<div>').addClass('RemoveMediaDialog')
		.append($('<p>').html('Media file : <a class="File"></a>'))
		.append($('<p>').addClass('Warning').text(
				'This will delete the media file from your system.'
				+ 'You cannot undo this operation.'))
		.append($('<p>').addClass('Confirm').text('Are you sure?'))
		.append($('<p>').addClass('Status'));

	function createRemoveMediaDialog(format, type, file) {
		var div = removeDialogTemplate.clone(true);
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
	
	var uploadDialogTemplate = $('<div>').addClass('UploadMediaDialog')
		.append($('<p>').append($('<div>').addClass('Upload')))
		.append($('<p>').append($('<a>').addClass('File').text('File: '))
				.append($('<input type="file" multiple>').addClass('File')))
		.append($('<p>').append($('<a>').addClass('Status')));

	function createUploadMediaDialog(format, type) {
		var div = uploadDialogTemplate.clone(true);
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
				inputFile = newInputFile.trigger('change');
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
		inputFile.on('change', function() {
			var files = this.files;
			if (!files || !files.length) {
				div.dialog("option", "buttons", buttonsCloseOnly);
				return;
			}
			div.dialog("option", "buttons", buttonsUploadClose);
		}).trigger('change');
	}
	
	function createSaveLoadTab(tab) {
		var table = $("<table>").attr("id", "SaveLoad").appendTo(tab);
		
		// Download table
		var sbDownloadTable = $("<table>").addClass("Download")
			.appendTo($("<td>").appendTo($("<tr>").appendTo(table)));
		$("<tr>").addClass("Name").appendTo(sbDownloadTable)
			.append("<td colspan='4'>Download ScoreBoard JSON</td>");
		var contentRow = $("<tr>").addClass("Content").appendTo(sbDownloadTable);
	
		var links = [
		{ name: "All data", url: "" },
		{ name: "Teams", url: "teams.json?path=ScoreBoard.PreparedTeam" }
		];
		$.each( links, function() {
			$("<td><a download/></td>").appendTo(contentRow)
				.children("a").text(this.name).button()
				.attr('href', '/SaveJSON/'+this.url);
		});
		var allDataA = contentRow.find(">td:eq(0)>a");
		var updateAllUrl = function() {
			var d = new Date();
			var name = $.datepicker.formatDate("yy-mm-dd_", d);
			name += _timeConversions.twoDigit(d.getHours());
			name += _timeConversions.twoDigit(d.getMinutes());
			name += _timeConversions.twoDigit(d.getSeconds());
			allDataA.attr("href", "/SaveJSON/scoreboard-"+name+".json");
		};
		setInterval(updateAllUrl, 1000);
	
	
		// Upload table
		var sbUploadTable = $("<table>").addClass("Upload")
			.appendTo($("<td>").appendTo($("<tr>").appendTo(table)));
		$("<tr>").addClass("Name").appendTo(sbUploadTable)
			.append("<td>Upload ScoreBoard JSON</td>");
		var contentTd = $("<td>")
			.appendTo($("<tr>").addClass("Content").appendTo(sbUploadTable));
	
		var iframeId = "SaveLoadUploadHiddenIframe";
		var uploadForm = $("<form method='post' enctype='multipart/form-data' target='"+iframeId+"'/>")
			.append("<iframe id='"+iframeId+"' name='"+iframeId+"' style='display: none'/>")
			.append("<input type='file' name='jsonFile'/>")
			.appendTo(contentTd);
		$("<button>").html("Add/Merge").attr("data-method", "merge").appendTo(uploadForm).button();
		$("<button>").html("Replace running scoreboard").attr("data-method", "load").appendTo(uploadForm).button();
		uploadForm.children("button").on('click', function() {
			uploadForm.attr("action", "/LoadJSON/"+$(this).attr("data-method")).submit();
		});
		_crgUtils.bindAndRun(uploadForm.children("input:file").button(), "change", function() {
			uploadForm.children("button").button(this.value ? "enable" : "disable");
		});
	}
}
