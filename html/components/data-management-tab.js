function createSaveLoadTab(tab) {
	var table = $("<table>").attr("id", "DataManagement").appendTo(tab);
	
	// Download table
	var sbDownloadTable = $("<table>").addClass("Download")
		.appendTo($("<td>").appendTo($("<tr>").appendTo(table)));
	$("<tr>").addClass("Name").appendTo(sbDownloadTable)
		.append("<td colspan='4'>Download ScoreBoard JSON</td>");
	$("<tr>").addClass("Instruction").appendTo(sbDownloadTable)
		.append("<td colspan='4'>To download, right-click and Save - to view JSON, left-click</td>");
	var contentRow = $("<tr>").addClass("Content").appendTo(sbDownloadTable);

	var links = [
	{ name: "All data", url: "" },
	{ name: "Teams", url: "teams.json?path=ScoreBoard.PreparedTeam" }
	];
	$.each( links, function() {
		$("<td><a/></td>").appendTo(contentRow)
			.children("a").html(this.name)
			.attr({ href: "/SaveJSON/"+this.url, target: "_blank" });
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
	uploadForm.children("button").click(function() {
		uploadForm.attr("action", "/LoadJSON/"+$(this).attr("data-method")).submit();
	});
	_crgUtils.bindAndRun(uploadForm.children("input:file").button(), "change", function() {
		uploadForm.children("button").button(this.value ? "enable" : "disable");
	});

}
