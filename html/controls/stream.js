
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

XML_ELEMENT_SELECTOR = "SaveLoad";

$sb(function() {
	var saveStream = $sb("SaveLoad.SaveStream");
	saveStream.$sb("Filename").$sbControl("#Save input.Filename");
	saveStream.$sb("Filename").$sbElement("#Save a.Filename");
	$("#Save button.Start").click(function() { saveStream.$sb("Start").$sbSet("true"); }).button();
	$("#Save button.Stop").click(function() { saveStream.$sb("Stop").$sbSet("true"); }).button();
	saveStream.$sb("Running").$sbBindAndRun("sbchange", function(event, value) {
		$("#Save").toggleClass("Running", isTrue(value));
		$("#Save button.Start").button("option", "disabled", isTrue(value));
		$("#Save button.Stop").button("option", "disabled", !isTrue(value));
	});
	saveStream.$sb("Error").$sbBindAndRun("sbchange", function(event, value) {
		$("#Save").toggleClass("Error", isTrue(value));
	});
	saveStream.$sb("Message").$sbElement("#Save a.Status");

	var loadStream = $sb("SaveLoad.LoadStream");
	var loadSelect = loadStream.$sb("Filename").$sbControl($("#Load select.Filename"));
	loadStream.$sb("Filename").$sbElement("#Load a.Filename");
	$("#Load button.Start").click(function() { loadStream.$sb("Start").$sbSet("true"); }).button();
	$("#Load button.Stop").click(function() { loadStream.$sb("Stop").$sbSet("true"); }).button();
	loadStream.$sb("Pause").$sbControl("#Load .Pause", { sbcontrol: { button: true }});
	loadStream.$sb("Speed").$sbElement("#Load a.Speed");
	$("#Load div.Speed").slider({
		min: -2,
		max: 4,
		value: 0
	}).bind("slide", function(event, ui) {
//FIXME - set up so scoreboard controls slider value
		loadStream.$sb("Speed").$sbSet(Math.pow(2,ui.value));
	});
	loadStream.$sb("Running").$sbBindAndRun("sbchange", function(event, value) {
		$("#Load").toggleClass("Running", isTrue(value));
		$("#Load button.Start").button("option", "disabled", isTrue(value));
		$("#Load button.Stop").button("option", "disabled", !isTrue(value));
	});
	loadStream.$sb("Error").$sbBindAndRun("sbchange", function(event, value) {
		$("#Load").toggleClass("Error", isTrue(value));
	});
	loadStream.$sb("Message").$sbElement("#Load a.Status");
	$("#Load div.Progress").progressbar();
	var updateProgress = function() {
		var pct = Math.floor((loadStream.$sb("CurrentTime").$sbGet()*100) / loadStream.$sb("EndTime").$sbGet());
		$("#Load div.Progress").progressbar("value", pct);
	};
	loadStream.$sb("CurrentTime").$sbElement("#Load a.CurrentTime", { sbelement: { convert: _timeConversions.msToMinSec }});
	loadStream.$sb("CurrentTime").bind("sbchange", updateProgress);
	loadStream.$sb("EndTime").$sbElement("#Load a.EndTime", { sbelement: { convert: _timeConversions.msToMinSec }});
	loadStream.$sb("EndTime").bind("sbchange", updateProgress);
	updateProgress();

	$("#Load button.Filename").click(function() {
		var updateButton = $(this).button("option", "label", "Updating...").button("disable");
		var currentSelection = loadSelect.find("option:selected").val();
		loadSelect.empty().append($("<option>").text("No Selection").val("")).prop("disabled", true);
		$.get("/Stream/list")
			.fail(function(jqxhr, textStatus, errorThrown) {
				alert("Error getting list of saved stream files : "+jqxhr.responseText);
			})
			.always(function() {
				updateButton.button("option", "label", "Update file list").button("enable");
				loadSelect.prop("disabled", false);
			})
			.done(function(data, status, jqxhr) {
				var response = jqxhr.responseText.trim();
				if (!response)
					return;
				$.each(response.split("\n"), function(i,e) {
					var o = $("<option>").text(e).val(e);
					_windowFunctions.appendAlphaSortedByProp(loadSelect, o, "text", 1);
				});
				loadSelect.find("option").each(function(i,e) {
					if (currentSelection === $(e).val()) {
						$(e).prop("selected", true);
						return false;
					}
				});
			});
	}).button().click();

	$("#tabsDiv").tabs();
});
