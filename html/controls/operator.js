
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */


$.fx.interval = 33;
_include("/json", [ "WS.js" ]);
_include("sk", [ "sk-sheet.js" ]);
$('head').append('<link rel="stylesheet" href="sk/sk-sheet.css" type="text/css" />');


$sb(function() {
	createScoreTimeTab();
	createScoreBoardViewTab();
	createTeamsTab();
	createSaveLoadTab();
	// Only connect after any registrations from the above are in place.
	// This avoids repeating work on the initial load.
	WS.AutoRegister();
	WS.Connect();

	$("#tabsDiv").tabs();
	_crgUtils.bindAndRun($("#tabsDiv"), "tabsselect", function(event,ui) {
			var table = $(ui.panel).children("table");
			var loadFunc = table.data("loadContentFunction") || $.noop;
			table.removeData("loadContentFunction");
			setTimeout(function() {
				$(ui.panel).children("div.Loading").remove();
				loadFunc(table);
			}, 100);
		}, [ { panel: $("#TeamTimeTab") } ]);

// FIXME - is there better way to avoid key controls when a dialog is visible?
	_crgKeyControls.addCondition(function() { return !$("body>div.ui-dialog").is(":visible"); });
// FIXME - maybe use something else to check if user is typing into a text input...
// FIXME - also provide visual feedback that key-control is disabled while typing into input text box?
	_crgKeyControls.addCondition(function() { return !$("#TeamTime input:text.Editing").length; });


	$("<li>").text("Caps Lock is On").attr("id", "capsLockWarning").addClass("Hidden").appendTo("#tabBar");
	$(document).keydown(function(e) {
		if (e.originalEvent.key === "CapsLock") {
			// Assume it'll be toggled. Different OSes actually change
			// the setting at different stages of the keypress, so
			// this is the best we can do. If it is wrong, it'll be
			// fixed at the next non-Caps Lock keypress.
			$("#capsLockWarning").toggleClass("Hidden");
		} else {
			$("#capsLockWarning").toggleClass("Hidden", !e.originalEvent.getModifierState("CapsLock"));
		}
	});
	$("<button>").text("Logout").click(logout).button().css("float", "right").appendTo("#tabBar");
});

function setOperatorSettings(op) {
	var prefix;
	if (op !== "") {
		prefix = "ScoreBoard.Settings.Setting(ScoreBoard.Operator__"+ op + ".";
	} else {
		// Default settings are intentionally separate from settings of the default operator
		// This ensures users logging in for the first time always get the former and not whatever
		// the latter currently happens to be.
		prefix = "ScoreBoard.Settings.Setting(ScoreBoard.Operator_Default.";
	}
	setClockControls($sb(prefix+"StartStopButtons)").$sbIsTrue());
	setReplaceButton($sb(prefix+"ReplaceButton)").$sbIsTrue());
}

// FIXME - this is done after the team/time panel is loaded,
//				 as the button setup needs to happen after that panel creates its buttons...
//				 really, the keycontrol helper lib needs to have a per-tab interface so
//				 each tab can setup its own keycontrol.
function initialLogin() {
	var operator = _windowFunctions.getParam("operator");
	if (operator) {
		setOperatorSettings("");
		login(operator);
	} else {
		logout();
	}
}

function login(name) {
	$("#operatorId").text(name);
	if (window.history.replaceState)
		window.history.replaceState(null, "", "?operator="+$("#operatorId").text());
	_crgKeyControls.setupKeyControls($sb("Pages.Page(operator.html).Operator("+$("#operatorId").text()+")"));
	setOperatorSettings(name);
}

function logout() {
	$("#operatorId").text("");
	if (window.history.replaceState)
		window.history.replaceState(null, "", "?");
	_crgKeyControls.destroyKeyControls();
	setOperatorSettings("");
	_crgUtils.showLoginDialog("Operator Login", "Operator:", "Login", function(value) {
		if (!value)
			return false;
		login(value);
		return true;
	});
}

////////////////////////////////
// Setup jQuery-UI tab structure
////////////////////////////////

function createTab(title, tabId) {
	if (typeof title == "string") title = $("<a>").html(title);
	if (!tabId) tabId = $sb().$sbNewUUID();
	$("<li>").append(title.attr("href", "#"+tabId)).appendTo("#tabsDiv>ul");
	return $("<div>").attr("id", tabId).addClass("TabContent")
		.append($("<div>").addClass("Loading").append("<a>Loading...</a>"))
		.appendTo("#tabsDiv");
}

function createRowTable(n, r) {
	var table = $("<table>").css("width", "100%").addClass("RowTable");
	var w = (100 / n) + "%";
	r = r || 1;
	while (0 < r--) {
		var count = n;
		var row = $("<tr>").appendTo(table);
		while (0 < count--)
			$("<td>").css("width", w).appendTo(row);
	}
	return table;
}


////////////////////////////
// Score & Time operator tab
////////////////////////////

function createScoreTimeTab() {
	var table = $("<table>").attr("id", "TeamTime")
		.appendTo(createTab("Team/Time", "TeamTimeTab"));
	createScoreTimeContent(table);
	var sk1 = $('<div>').addClass('SKSheet').appendTo($('#TeamTimeTab'));
	var sk2 = $('<div>').addClass('SKSheet').appendTo($('#TeamTimeTab'));
	$('<div>').attr('id', 'TripEditor').appendTo($('#TeamTimeTab'));
	prepareSkSheetTable(sk1, 1, 'operator');
	prepareSkSheetTable(sk2, 2, 'operator');
	prepareTripEditor();
}

function createScoreTimeContent(table) {
	$("<tr><td/></tr>").appendTo(table).children("td")
		.append(createMetaControlTable());
	$("<tr><td/></tr>").appendTo(table).children("td")
		.append(createJamControlTable());
	$("<tr><td/></tr>").appendTo(table).children("td")
		.append(createTeamTable());
	$("<tr><td/></tr>").appendTo(table).children("td")
		.append(createTimeTable());
	table.children("tbody").children("tr").children("td").children("table").addClass("TabTable");

	initialLogin();
}

function setClockControls(value) {
	$("#ShowClockControlsButton").prop("checked", value);
	$("label.ShowClockControlsButton").toggleClass("ui-state-active", value);
	$("#TeamTime").find("tr.Control").toggleClass("Show", value);
}

function setReplaceButton(value) {
	$("#EnableReplaceButton").prop("checked", value);
	$("label.EnableReplaceButton").toggleClass("ui-state-active", value);
	$("#ClockUndo").toggleClass("Hidden KeyInactive", value);
	$("#ClockReplace").toggleClass("Hidden KeyInactive", !value);
}

function createMetaControlTable() {
	var table = $("<table><tr><td/></tr><tr><td/></tr><tr><td/></tr></table>")
		.addClass("MetaControl");
	var buttonsTd = createRowTable(1)
		.appendTo(table.find(">tbody>tr:eq(0)").addClass("Buttons").children("td"))
		.find("tr>td");
	var helpTd = createRowTable(1)
		.appendTo(table.find(">tbody>tr:eq(1)").addClass("Help Hidden").children("td"))
		.find("tr>td");
	var periodEndTd = createRowTable(1)
		.appendTo(table.find(">tbody>tr:eq(2)").addClass("PeriodEnd Hidden").children("td"))
		.find("tr>td");

	$("<label>").text("Edit Key Control").attr("for", "EditKeyControlButton")
		.appendTo(buttonsTd);
	$("<input type='checkbox'>").attr("id", "EditKeyControlButton")
		.appendTo(buttonsTd)
		.button()
		.click(function() {
			_crgKeyControls.editKeys(this.checked);
			table.find("tr.Help").toggleClass("Hidden", !this.checked);
		});
	$("<a>").text("Key Control Edit mode enabled.	 Buttons do not operate in this mode.	 Move the mouse over a button, then press a normal key (not ESC, Enter, F1, etc.) to assign.	Backspace/Delete to remove.")
		.appendTo(helpTd);

	$("<label>").addClass("EnableReplaceButton").text("Enable Replace on Undo").attr("for", "EnableReplaceButton")
		.appendTo(buttonsTd);
	$("<input type='checkbox'>").attr("id", "EnableReplaceButton")
		.appendTo(buttonsTd)
		.button()
		.click(function() {
			var value = this.checked;
			setReplaceButton(value);
			var operator = $("#operatorId").text();
			if (operator) {
				$sb("ScoreBoard.Settings.Setting(ScoreBoard.Operator__"+operator+".ReplaceButton)").$sbSet(value);
			}
		});

	$("<label>").addClass("ShowClockControlsButton").text("Show Start/Stop Buttons").attr("for", "ShowClockControlsButton")
		.appendTo(buttonsTd);
	$("<input type='checkbox'>").attr("id", "ShowClockControlsButton")
		.appendTo(buttonsTd)
		.button()
		.click(function() {
			var value = this.checked;
			setClockControls(value);
			var operator = $("#operatorId").text();
			if (operator) {
				$sb("ScoreBoard.Settings.Setting(ScoreBoard.Operator__"+operator+".StartStopButtons)").$sbSet(value);
			}
		});

	$("<button>").attr("id", "GameControl")
		.text("Start New Game")
		.addClass('clickMe')
		.appendTo(buttonsTd)
		.button()
		.click(createGameControlDialog);

	var periodEndControlsLabel = $("<label>").attr("for", "PeriodEndControlsCheckbox")
		.text("End of Period Controls")
		.addClass("PeriodEndControls")
		.appendTo(buttonsTd);
	$("<input type='checkbox'>").attr("id", "PeriodEndControlsCheckbox")
		.appendTo(buttonsTd)
		.button()
		.click(function() {
			table.find("tr.PeriodEnd").toggleClass("Hidden", !this.checked);
		});
	var doPulseFlag = false;
	var doPulse = function() {
		if (doPulseFlag)
			periodEndControlsLabel.fadeTo(500, 0.25).fadeTo(500, 1, doPulse);
		else
			setTimeout(doPulse, 500);
	};
	doPulse();
	var updatePeriodEndDoPulse = function() {
		var pc = $sb("ScoreBoard.Clock(Period)");
		var under30 = (Number(pc.$sb("Time").$sbGet()) < 30000);
		var last = pc.$sb("Number").$sbIs($sb("ScoreBoard.Rulesets.CurrentRule(Period.Number)").$sbGet());
		doPulseFlag = (under30 && last);
	};
	$sb("ScoreBoard.Clock(Period).Time").$sbBindAndRun("sbchange", updatePeriodEndDoPulse);
	$sb("ScoreBoard.Clock(Period).Number").$sbBindAndRun("sbchange", updatePeriodEndDoPulse);

	var confirmedButton = $("<label/><input type='checkbox'/>");
	$sb("ScoreBoard.OfficialScore")
		.$sbControl(confirmedButton, { sbelement: {
			convert: function(value) {
				confirmedButton.filter("input:checkbox")
					.button("option", "label", isTrue(value)?"Official Score":"Unofficial Score");
				return value;
			}
		}, sbcontrol: {
			button: true
		} })
		.appendTo(periodEndTd);
	var periodEndTimeoutDialog = createPeriodEndTimeoutDialog(periodEndTd);
	$("<button>").addClass("PeriodEndTimeout").text("Timeout before Period End")
		.appendTo(periodEndTd)
		.button()
		.click(function() { periodEndTimeoutDialog.dialog("open"); });
	$("<button>").text("Overtime")
		.appendTo(periodEndTd)
		.button()
		.click(createOvertimeDialog);

	return table;
}

function hideEndOfPeriodControls() {
	$("#PeriodEndControlsCheckbox").removeAttr("checked");
	$("#PeriodEndControlsCheckbox").button("refresh");
	$("tr.PeriodEnd").addClass("Hidden");
}

function addDays(date, days) {
	var result = new Date(date);
	result.setDate(result.getDate() + days);
	return result;
}

function createGameControlDialog() {
	var dialog = $("<div>").addClass("GameControl");
	var title = "Start New Game";


	var preparedGame = $("<div>").addClass("section").appendTo(dialog);
	$("<span>").addClass("header").append("Start a prepared game").appendTo(preparedGame);

	var adhocGame = $("<div>").addClass("section").appendTo(dialog);

	var adhocStartGame = function() {
		$('#GameControl').removeClass('clickMe');
		var StartTime = adhocGame.find("input.StartTime").val();
		var IntermissionClock = null;
		if (StartTime != "") {
			var now = new Date();
			var parts = StartTime.split(":");
			StartTime = new Date(now.getFullYear(), now.getMonth(), now.getDate(),
					parts[0], parts[1]);
			if (StartTime < now)
				StartTime = addDays(StartTime, 1);
			IntermissionClock = StartTime - now;
		}
		var game = {
			Team1: adhocGame.find("select.Team1").val(),
			Team2: adhocGame.find("select.Team2").val(),
			Ruleset: adhocGame.find("select.Ruleset").val(),
			IntermissionClock: IntermissionClock
		};
		console.log(game);
		WS.Command("StartNewGame", game);
		dialog.dialog("close");
	};

	$("<span>").addClass("header").append("Start an adhoc game").appendTo(adhocGame);
	$("<div>")
		.append($("<span>").append("Team 1: "))
		.append($("<select>").addClass("Team1"))
		.appendTo(adhocGame);
	$("<div>")
		.append($("<span>").append("Team 2: "))
		.append($("<select>").addClass("Team2"))
		.appendTo(adhocGame);
	$("<div>")
		.append($("<span>").append("Ruleset: "))
		.append($("<select>").addClass("Ruleset"))
		.appendTo(adhocGame);
	$("<div>")
		.append($("<span>").append("Start Time: "))
		.append($("<input>").attr("type", "time").addClass("StartTime"))
		.appendTo(adhocGame);
	$("<button>")
		.addClass("StartGame")
		.append("Start Game")
		.button({ disabled: true })
		.appendTo(adhocGame)
		.click(adhocStartGame);

	updateAdhocName = function() {
		var t1 = adhocGame.find("select.Team1 option:selected");
		var t2 = adhocGame.find("select.Team2 option:selected");
		if (t1.val() != "" && t2.val() != "") {
			adhocGame.find("button.StartGame").button("option", "disabled", false);
		} else {
			adhocGame.find("button.StartGame").button("option", "disabled", true);
		}
	};

	_crgUtils.setupSelect(adhocGame.find("select.Team1"), {
		optionParent: "ScoreBoard",
		optionChildName: "PreparedTeam",
		prependOptions: [
			{ text: "No Team Selected", value: "" },
		]
	}).change(function(e) { updateAdhocName(); });
	_crgUtils.setupSelect(adhocGame.find("select.Team2"), {
		optionParent: "ScoreBoard",
		optionChildName: "PreparedTeam",
		prependOptions: [
			{ text: "No Team Selected", value: "" },
		]
	}).change(function(e) { updateAdhocName(); });


	_crgUtils.setupSelect(adhocGame.find("select.Ruleset"), {
		optionParent: "ScoreBoard.Rulesets",
		optionChildName: "Ruleset",
		optionNameElement: "Name",
	});
	adhocGame.find("select.Ruleset").val($sb("ScoreBoard.Rulesets.CurrentRulesetId").$sbGet());

	dialog.dialog({
		title: title,
		width: "600px",
		modal: true,
		buttons: { Cancel: function() { $(this).dialog("close"); } },
		close: function() { $(this).dialog("destroy").remove(); }
	});
	return dialog;
}

function createPeriodEndTimeoutDialog(td) {
	var dialog = $("<div>");
	var applyDiv = $("<div>").addClass("Apply").appendTo(dialog);
	$("<span>").text("Timeout with ").appendTo(applyDiv);
	var periodSeconds = $("<input type='text' size='3'>").val("1").appendTo(applyDiv);
	$("<span>").text(" seconds left on Period clock:").appendTo(applyDiv);
	$("<button>").addClass("Apply").text("Apply").appendTo(applyDiv).button();
	var waitDiv = $("<div>").addClass("Wait").appendTo(dialog).hide();
	$("<span>").text("Starting Timeout when Period clock to reaches ").appendTo(waitDiv);
	$("<span>").addClass("TargetSeconds").appendTo(waitDiv);
	$("<span>").text(" seconds...").appendTo(waitDiv);
	$("<button>").addClass("Cancel").text("Cancel").appendTo(waitDiv).button();
	var checkTimeFunction = function(event, value) {
		var currentSecs = Number(_timeConversions.msToSeconds(value));
		var targetSecs = Number(waitDiv.find("span.TargetSeconds").text());
		if (currentSecs > targetSecs)
			return;
		if ($sb("ScoreBoard.Clock(Period).Running"))
			$sb("ScoreBoard.Timeout").$sbSet("true");
		if (currentSecs < targetSecs)
			$sb("ScoreBoard.Clock(Period).Time").$sbSet(_timeConversions.secondsToMs(targetSecs));
		$sb("ScoreBoard.Clock(Intermission).Stop").$sbSet("true");
		$(this).unbind(event);
		td.find("button.PeriodEndTimeout").button("option", "label", "Timeout before Period End");
		applyDiv.show();
		waitDiv.hide();
		dialog.dialog("close");
	};
	applyDiv.find("button.Apply").click(function() {
		var secs = Number(periodSeconds.val());
		if (isNaN(secs))
			return;
		var ms = _timeConversions.secondsToMs(secs);
		waitDiv.find("span.TargetSeconds").text(secs);
		td.find("button.PeriodEndTimeout").button("option", "label", "Timeout at "+secs+" Period seconds");
		applyDiv.hide();
		waitDiv.show();
		$sb("ScoreBoard.Clock(Period).Time").$sbBindAndRun("sbchange", checkTimeFunction);
	});
	waitDiv.find("button.Cancel").click(function() {
		$sb("ScoreBoard.Clock(Period).Time").unbind("sbchange", checkTimeFunction);
		td.find("button.PeriodEndTimeout").button("option", "label", "Timeout before Period End");
		applyDiv.show();
		waitDiv.hide();
	});
	dialog.dialog({
		title: "Timeout before End of Period",
		width: "600px",
		modal: true,
		autoOpen: false,
		buttons: { Close: function() { $(this).dialog("close"); } },
	});
	return dialog;
}

function createOvertimeDialog() {
	var dialog = $("<div>");
	$("<span>").text("Note: Overtime can only be started at the end of Period ").appendTo(dialog);
	$sb("ScoreBoard.Rulesets.CurrentRule(Period.Number)").$sbElement("<span>").appendTo(dialog);
	$("<button>").addClass("StartOvertime").text("Start Overtime Lineup clock").appendTo(dialog)
		.click(function() {
			$sb("ScoreBoard.StartOvertime").$sbSet("true");
			dialog.dialog("close");
		});
	dialog.dialog({
		title: "Overtime",
		width: "600px",
		modal: true,
		buttons: { Cancel: function() { $(this).dialog("close"); } },
		close: function() { $(this).dialog("destroy").remove(); }
	});
}

function createJamControlTable() {
	var table = $("<table><tr><td/></tr></table>").addClass("JamControl");
	var replaceInfoTr = createRowTable(1).addClass("ReplaceInfo Hidden").appendTo(table.find("td"));
	var controlsTr = createRowTable(4,1).appendTo(table.find("td")).find("tr:eq(0)").addClass("Controls");

	var replaceInfoText = $("<span>").html("Replace &quot;<span id=\"replacedLabel\"></span>&quot; with").appendTo(replaceInfoTr);
	$sb("ScoreBoard.Settings.Setting(ScoreBoard.Button.ReplacedLabel)").$sbBindAndRun("sbchange", function(event, val) {
		$("#replacedLabel").html(val);
	});
	
	
	var jamStartButton = $sb("ScoreBoard.StartJam").$sbControl("<button>")
		.html("<span class=\"Label\">Start Jam</span>").val("true")
		.attr("id", "StartJam").addClass("KeyControl").button();
	$sb("ScoreBoard.Settings.Setting(ScoreBoard.Button.StartLabel)").$sbBindAndRun("sbchange", function(event, val) {
		jamStartButton.find("span.Label").html(val);
	});
	jamStartButton.appendTo(controlsTr.children("td:eq(0)"));

	var stopButton = $sb("ScoreBoard.StopJam").$sbControl("<button>")
		.html("<span class=\"Label\">Stop Jam/TO</span>").val("true")
		.attr("id", "StopJam").addClass("KeyControl").button();
	$sb("ScoreBoard.Settings.Setting(ScoreBoard.Button.StopLabel)").$sbBindAndRun("sbchange", function(event, val) {
		stopButton.find("span.Label").html(val);
	});
	stopButton.appendTo(controlsTr.children("td:eq(1)"));


	var timeoutButton = $sb("ScoreBoard.Timeout").$sbControl("<button>")
		.html("<span class=\"Label\">Timeout</span>").val("true")
		.attr("id", "Timeout").addClass("KeyControl").button();
	$sb("ScoreBoard.Settings.Setting(ScoreBoard.Button.TimeoutLabel)").$sbBindAndRun("sbchange", function(event, val) {
		timeoutButton.find("span.Label").html(val);
		});
	timeoutButton.appendTo(controlsTr.children("td:eq(2)"));
	
	var undoButton = $sb("ScoreBoard.ClockUndo").$sbControl("<button>")
		.html("<span class=\"Label\">Undo</span>").val("true")
		.attr("id", "ClockUndo").addClass("KeyControl").button();
	$sb("ScoreBoard.Settings.Setting(ScoreBoard.Button.UndoLabel)").$sbBindAndRun("sbchange", function(event, val) {
		undoButton.find("span.Label").html(val);
		});
	var replaceButton = $sb("ScoreBoard.ClockReplace").$sbControl("<button>")
		.html("<span class=\"Label\">Undo</span>").val("true")
		.attr("id", "ClockReplace").addClass("KeyControl Hidden KeyInactive").button();
	$sb("ScoreBoard.Settings.Setting(ScoreBoard.Button.UndoLabel)").$sbBindAndRun("sbchange", function(event, val) {
		replaceButton.find("span.Label").html(val);
		if (!replaceButton.hasClass("Hidden")) {
			var rep = (val == "No Action");
			$("#TeamTime").find(".TabTable:not(.JamControl)").toggleClass("Faded", rep);
			$("#TeamTime").find(".ReplaceInfo").toggleClass("Hidden", !rep);
		}});
	undoButton
		.bind("mouseenter mouseleave", function(event) {replaceButton.toggleClass("hover", (event.type == "mouseenter"));})
		.appendTo(controlsTr.children("td:eq(3)"));
	replaceButton
		.bind("mouseenter mouseleave", function(event) {undoButton.toggleClass("hover", (event.type == "mouseenter"));})
		.appendTo(controlsTr.children("td:eq(3)"));
	
	return table;
}

var timeoutDialog;
function createTeamTable() {
	var table = $("<table>").addClass("Team");
	var row = $("<tr></tr>");
	var nameRow = row.clone().addClass("Name").appendTo(table);
	var scoreRow = row.clone().addClass("Score").appendTo(table);
	var speedScoreRow = row.clone().addClass("SpeedScore").appendTo(table);
	var timeoutRow = row.clone().addClass("Timeout").appendTo(table);
	var jammer1Row = row.clone().addClass("Jammer").appendTo(table);
	var jammer2Row = row.clone().addClass("Jammer").appendTo(table);

	$.each( [ "1", "2" ], function() {
		var team = String(this);
		var sbTeam = $sb("ScoreBoard.Team("+team+")");
		var first = (team == "1");

		var nameTr = createRowTable(2).appendTo($("<td>").appendTo(nameRow)).find("tr");
		var scoreTr = createRowTable(3).appendTo($("<td>").appendTo(scoreRow)).find("tr");
		var speedScoreTr = createRowTable(4).appendTo($("<td>").appendTo(speedScoreRow)).find("tr");
		var timeoutTr = createRowTable(6).appendTo($("<td>").appendTo(timeoutRow)).find("tr");
		var jammer1Tr = createRowTable(2).appendTo($("<td>").appendTo(jammer1Row)).find("tr");
		var jammer2Tr = createRowTable(2).appendTo($("<td>").appendTo(jammer2Row)).find("tr");

		var nameTd = nameTr.children("td:eq("+(first?1:0)+")").addClass("Name");
		var nameDisplayDiv = $("<div>").appendTo(nameTd);
		var nameA = $("<a>").appendTo(nameDisplayDiv);
		var altNameA = $("<a>").appendTo(nameDisplayDiv);
		
		var nameEditTable = $("<table><tr><td>Team Name</td><td>Alternate Name</td></tr>" +
			"<tr><td><input class='Name' type='text' size='15' /></td>" +
			"<td><input class='AlternateName' type='text' size='15' /></td></tr></table>").appendTo(nameTd);
		var nameInput = $(nameEditTable).find(".Name");
		var altNameInput = $(nameEditTable).find(".AlternateName");
		
		nameEditTable.hide();
		sbTeam.$sb("Name").$sbElement(nameA);
		sbTeam.$sb("Name").$sbControl(nameInput);
		var nameInputFocus = function() {
			if (nameDisplayDiv.css("display") != "none") {
				nameDisplayDiv.hide();
				nameEditTable.show();
				nameInput.addClass("Editing").trigger("editstart");;
				altNameInput.addClass("Editing").trigger("editstart");;
			}
		};
		var nameInputBlur = function(event) {
			if (event.relatedTarget != nameInput[0] && event.relatedTarget != altNameInput[0]) {
				nameEditTable.hide();
				nameDisplayDiv.show();
				nameInput.removeClass("Editing").trigger("editstop");;
				altNameInput.removeClass("Editing").trigger("editstop");;
			}
		};
		var nameInputKeyup = function(event) {
			var c = $(event.target);
			switch (event.which) {
				case 13: /* RET */ if (c.is("textarea") && !event.ctrlKey) break; c.blur(); break;
				case 27: /* ESC */ c.blur(); break;
			}
		};
		
		nameDisplayDiv.bind("click", function() { nameInput.focus(); });
		nameInput.bind("focus", nameInputFocus);
		altNameInput.bind("focus", nameInputFocus);
		nameInput.bind("blur", nameInputBlur);
		altNameInput.bind("blur", nameInputBlur);
		nameInput.bind("keyup", nameInputKeyup);
		altNameInput.bind("keyup", nameInputKeyup);
		
		altNameInput.bind("change", function() {
			var val = $.trim(altNameInput.val());
			if (val != altNameInput.data("last")) {
				altNameInput.data("last", val);
				if (val == "") {
					sbTeam.$sb("AlternateName(operator)").$sbRemove();
				} else {
					sbTeam.$sb("AlternateName(operator)").$sbSet(val);
				}
			}
		});
		
		sbTeam.$sbBindAddRemoveEach("AlternateName", function(event, node) {
			if ($sb(node).$sbId == "operator")
				$sb(node).$sbBindAndRun("sbchange", function(event, val) {
					altNameA.html($.trim(val));
					altNameInput.val($.trim(val));
					nameA.toggleClass("AlternateName", ($.trim(val) != ""));
				});
		}, function(event, node) {
			if ($sb(node).$sbId == "overlay")
			nameA.removeClass("AlternateName");
		});
	
		_crgUtils.bindColors(sbTeam, "operator", nameA);
		_crgUtils.bindColors(sbTeam, "operator", altNameA);

		var logoTd = nameTr.children("td:eq("+(first?0:1)+")").addClass("Logo");
		var logoNone = $("<a>").html("No Logo").addClass("NoLogo").appendTo(logoTd);
		var logoSelect = sbTeam.$sb("Logo").$sbControl("<select>", { sbelement: {
				optionParent: "ScoreBoard.Media.Format(images).Type(teamlogo)",
				optionChildName: "File",
				optionNameElement: "Name",
				optionValueElement: "Src",
				firstOption: { text: "No Logo", value: "" }
			} }).appendTo(logoTd);
		var logoImg = sbTeam.$sb("Logo").$sbElement("<img>")
			.appendTo(logoTd);

		var logoShowSelect = function(show) {
			var showImg = !!sbTeam.$sb("Logo").$sbGet();
			logoImg.toggle(!show && showImg);
			logoNone.toggle(!show && !showImg);
			logoSelect.toggle(show);
			if (show)
				logoSelect.focus();
		};
		sbTeam.$sb("Logo").$sbBindAndRun("sbchange", function(event,value) { logoShowSelect(false); });
		logoSelect
			.blur(function() { logoShowSelect(false); })
			.keyup(function(event) { if (event.which == 27 /* ESC */) $(this).blur(); });

		logoTd.click(function() { if (!logoSelect.is(":visible")) logoShowSelect(true); });

		var scoreTd = scoreTr.children("td:eq("+(first?"0":"2")+")").addClass("Down");
		sbTeam.$sb("TripScore").$sbControl("<button>", { sbcontrol: { sbSetAttrs: { change: "true" } } })
			.text("Score -1").val("-1")
			.attr("id", "Team"+team+"ScoreDown").addClass("KeyControl BigButton").button()
			.appendTo(scoreTd);
		$("<br />").appendTo(scoreTd);
		sbTeam.$sb("RemoveTrip").$sbControl("<button>").text("Trip -1").val("true")
			.attr("id", "Team"+team+"RemoveTrip").addClass("KeyControl TripButton").button()
			.appendTo(scoreTd);

		var scoreSubTr = createRowTable(3).appendTo(scoreTr.children("td:eq(1)")).find("tr");
		sbTeam.$sb("Score").$sbControl("<a/>").appendTo(scoreSubTr.children("td:eq(1)").addClass("Score"));

		var scoreTd = scoreTr.children("td:eq("+(first?"2":"0")+")").addClass("Up");
		sbTeam.$sb("TripScore").$sbControl("<button>", { sbcontrol: { sbSetAttrs: { change: "true" } } })
			.text("Score +1").val("1")
			.attr("id", "Team"+team+"ScoreUp").addClass("KeyControl BigButton").button()
			.appendTo(scoreTd);
		$("<br />").appendTo(scoreTd);
		sbTeam.$sb("AddTrip").$sbControl("<button>").text("Trip +1").val("true")
			.attr("id", "Team"+team+"AddTrip").addClass("KeyControl TripButton").button()
			.appendTo(scoreTd);

		for (var i = 1; i <= 4; i++) {
			var pos = (i - 1);
			if (!first)
				pos = 3 - pos;
			sbTeam.$sb("TripScore").$sbControl("<button>").text(i).val(i)
				.attr("id", "Team"+team+"TripScore"+i).addClass("KeyControl").button()
				.appendTo(speedScoreTr.find("td:eq("+pos+")"));
		}


		// Note instantaneous score change is always towards the center.	Jam score total is on the outside.
		var scoreChange = $("<a>").css({ opacity: "0" }).appendTo(scoreSubTr.children("td:eq("+(first?"2":"0")+")")).addClass("TripScore");
		var jamScore = $("<a>").appendTo(scoreSubTr.children("td:eq("+(first?"0":"2")+")")).addClass("JamScore");

		var scoreChangeTimeout;
		sbTeam.$sb("TripScore").bind("sbchange", function(event,s) {
			var c = (s<0 ? "#800" : s>0 ? "#080" : "#008");
			var addTrip = $sb("ScoreBoard.InJam").$sbIsTrue();
			scoreChange.stop(true).text(s).last().css({ opacity: "1", color: c });
			if (scoreChangeTimeout)
				clearTimeout(scoreChangeTimeout);
			scoreChangeTimeout = setTimeout(function() {
				scoreChange.last()
					.animate({ color: "#000" }, 1000)
					.animate({ opacity: "0" }, 1000, "easeInExpo");
				scoreChangeTimeout = null;
			}, 2000);
		});

		jamScore.stop(true).text("0").last().css({ opacity: "1", color: "#008" });
		var jamScoreTimeout;
		sbTeam.$sb("JamScore").$sbBindAndRun("sbchange", function(event, s) {
			var c = (s<0 ? "#800" : s>0 ? "#080" : "#008");
			jamScore.stop(true).text(s).last().css({ opacity: "1", color: c });
			if (jamScoreTimeout)
				clearTimeout(jamScoreTimeout);
			jamScoreTimeout = setTimeout(function() {
				jamScore.last()
					.animate({ color: "#008" }, 2000)
			}, 2000);
		});

		var timeout = sbTeam.$sb("Timeout");
		var timeoutButton = timeout.$sbControl("<button>").text("Team TO").val("true")
			.attr("id", "Team"+team+"Timeout").addClass("KeyControl").button();
		var timeoutHighlight = function() {
			var to = $sb("ScoreBoard.TimeoutOwner").$sbGet() == team;
			var or = $sb("ScoreBoard.OfficialReview").$sbIsTrue();
			timeoutButton.toggleClass("Active", to && !or);
		};
		$sb("ScoreBoard.TimeoutOwner").$sbBindAndRun("sbchange", timeoutHighlight);
		$sb("ScoreBoard.OfficialReview").$sbBindAndRun("sbchange", timeoutHighlight);
		timeoutButton.appendTo(timeoutTr.children("td:eq("+(first?"0":"5")+")").addClass("Timeout"));
		sbTeam.$sb("Timeouts").$sbElement("<a/>").click(function() { timeoutDialog.dialog("open"); })
			.appendTo(timeoutTr.children("td:eq("+(first?"1":"4")+")").addClass("Timeouts"));
		var review = sbTeam.$sb("OfficialReview");
		var reviewButton = review.$sbControl("<button>").text("Off Review").val("true")
			.attr("id", "Team"+team+"OfficialReview").addClass("KeyControl").button();
		var reviewHighlight = function() {
			var to = $sb("ScoreBoard.TimeoutOwner").$sbGet() == team;
			var or = $sb("ScoreBoard.OfficialReview").$sbIsTrue();
			reviewButton.toggleClass("Active", to && or);
		};
		$sb("ScoreBoard.TimeoutOwner").$sbBindAndRun("sbchange", reviewHighlight);
		$sb("ScoreBoard.OfficialReview").$sbBindAndRun("sbchange", reviewHighlight);
		reviewButton.appendTo(timeoutTr.children("td:eq("+(first?"2":"3")+")").addClass("OfficialReview"));
		sbTeam.$sb("OfficialReviews").$sbElement("<a/>").click(function() { timeoutDialog.dialog("open"); })
			.appendTo(timeoutTr.children("td:eq("+(first?"3":"2")+")").addClass("OfficialReviews"));
		var retainedOR = sbTeam.$sb("RetainedOfficialReview");
		var retainedORButton = retainedOR.$sbControl("<button>").text("Retained").val("true")
			.attr("id", "Team"+team+"RetainedOfficialReview").addClass("KeyControl").button();
		retainedOR.$sbBindAndRun("sbchange", function(event, value) {
			retainedORButton.val(!isTrue(value));
			retainedORButton.toggleClass("Active", isTrue(value));
		});
		retainedORButton.appendTo(timeoutTr.children("td:eq("+(first?"4":"1")+")").addClass("RetainedOfficialReview"));
		if (first) {
			var oto = $sb("ScoreBoard.OfficialTimeout");
			var otoButton = oto.$sbControl("<button>").text("Official TO").val("true")
				.attr("id", "OfficialTimeout").addClass("KeyControl").button();
			var otoHighlight = function() {
				var to = $sb("ScoreBoard.TimeoutOwner").$sbGet() == "O";
				otoButton.toggleClass("Active", to);
			};
			$sb("ScoreBoard.TimeoutOwner").$sbBindAndRun("sbchange", otoHighlight);
			otoButton.appendTo(timeoutTr.children("td:eq(5)").addClass("OfficialTimeout"));
			otoButton.wrap("<div></div>");
		}
		
		var leadJammerTd = jammer1Tr.children("td:eq("+(first?"0":"1")+")");
		var lost = sbTeam.$sb("Lost");
		var lostButton = lost.$sbControl("<button>").text("Lost").val("true")
			.attr("id", "Team"+team+"Lost").addClass("KeyControl").button();
		lost.$sbBindAndRun("sbchange", function(event, value) {
			lostButton.val(!isTrue(value));
			lostButton.toggleClass("Active", isTrue(value));
		});
		lostButton.appendTo(leadJammerTd);
		var lead = sbTeam.$sb("Lead");
		var leadButton = lead.$sbControl("<button>").text("Lead").val("true")
			.attr("id", "Team"+team+"Lead").addClass("KeyControl").button();
		lead.$sbBindAndRun("sbchange", function(event, value) {
			leadButton.val(!isTrue(value));
			leadButton.toggleClass("Active", isTrue(value));
		});
		leadButton.appendTo(leadJammerTd);
		var calloff = sbTeam.$sb("Calloff");
		var calloffButton = calloff.$sbControl("<button>").text("Call").val("true")
			.attr("id", "Team"+team+"Call").addClass("KeyControl").button();
		calloff.$sbBindAndRun("sbchange", function(event, value) {
			calloffButton.val(!isTrue(value));
			calloffButton.toggleClass("Active", isTrue(value));
		});
		calloffButton.appendTo(leadJammerTd);
		var inj = sbTeam.$sb("Injury");
		var injButton = inj.$sbControl("<button>").text("Inj").val("true")
			.attr("id", "Team"+team+"Inj").addClass("KeyControl").button();
		inj.$sbBindAndRun("sbchange", function(event, value) {
			injButton.val(!isTrue(value));
			injButton.toggleClass("Active", isTrue(value));
		});
		injButton.appendTo(leadJammerTd);
		var ni = sbTeam.$sb("NoInitial");
		var niButton = ni.$sbControl("<button>").text("NI").val("true")
			.attr("id", "Team"+team+"NI").addClass("KeyControl").button();
		ni.$sbBindAndRun("sbchange", function(event, value) {
			niButton.val(!isTrue(value));
			niButton.toggleClass("Active", isTrue(value));
		});
		niButton.appendTo(leadJammerTd);
		
		leadJammerTd.buttonset();

		var starPassTd = jammer2Tr.children("td:eq("+(first?"0":"1")+")");
		var starPass = sbTeam.$sb("StarPass");
		var starPassButton = starPass.$sbControl("<button>").text("Star Pass").val("true")
			.attr("id", "Team"+team+"StarPass").addClass("KeyControl").button();
		starPass.$sbBindAndRun("sbchange", function(event, value) {
			starPassButton.val(!isTrue(value));
			starPassButton.toggleClass("Active", isTrue(value));
		});
		starPassButton.appendTo(starPassTd);
		var noPivot = sbTeam.$sb("NoPivot");
		var noPivotButton = starPass.$sbElement("<button>").text("No Pivot").val("true")
			.attr("id", "Team"+team+"NoPivot").addClass("KeyControl").button();
		noPivot.$sbBindAndRun("sbchange", function(event, value) {
			noPivotButton.val(!isTrue(value));
			noPivotButton.toggleClass("Active", isTrue(value));
		});
		noPivotButton.appendTo(starPassTd);

		var makeSkaterDropdown = function(pos, elem, sort) {
			var sortFunc = _windowFunctions.alphaCompareByProp;
			if (sort == "Num") sortFunc = _windowFunctions.numCompareByProp;
			return sbTeam.$sb("Position("+pos+").Skater").$sbControl("<select>", { sbelement: {
					optionParent: sbTeam,
					optionChildName: "Skater",
					optionNameElement: elem,
					optionFilterElement: "Role",
					optionChildFilter: function(node) { return (node.$sb("Role").$sbGet() != 'NotInGame'); },
					compareOptions: function(a, b) { return sortFunc("text", a, b); },
					firstOption: { text: '?', value: "" }
				} }).addClass(pos+" By"+elem+" "+sort+"Sort");
		};

		var jammerSelectTd = jammer1Tr.children("td:eq("+(first?"1":"0")+")").addClass("Jammer");
		$('<span>').text('Jammer:').appendTo(jammerSelectTd);
		makeSkaterDropdown("Jammer", "Number", "Alpha").appendTo(jammerSelectTd);

		var jammerBox = sbTeam.$sb("Position(Jammer).PenaltyBox");
		var jammerBoxButton = jammerBox.$sbControl("<button>").text("Box").val("true")
			.attr("id", "Team"+team+"JammerBox").addClass("KeyControl Box").button();
		jammerBox.$sbBindAndRun("sbchange", function(event, value) {
			jammerBoxButton.val(!isTrue(value));
			jammerBoxButton.toggleClass("Active", isTrue(value));
		});
		jammerBoxButton.appendTo(jammerSelectTd);

		var pivotSelectTd = jammer2Tr.children("td:eq("+(first?"1":"0")+")").addClass("Pivot");
		$('<span>').text('Piv/4th Bl:').appendTo(pivotSelectTd);
		makeSkaterDropdown("Pivot", "Number", "Alpha").appendTo(pivotSelectTd);

		var pivotBox = sbTeam.$sb("Position(Pivot).PenaltyBox");
		var pivotBoxButton = pivotBox.$sbControl("<button>").text("Box").val("true")
			.attr("id", "Team"+team+"PivotBox").addClass("KeyControl Box").button();
		pivotBox.$sbBindAndRun("sbchange", function(event, value) {
			pivotBoxButton.val(!isTrue(value));
			pivotBoxButton.toggleClass("Active", isTrue(value));
		});
		pivotBoxButton.appendTo(pivotSelectTd);
	});

	return table;
}

function createTimeTable() {
	var table = $("<table>").addClass("Time");
	var row = $("<tr></tr>");
	var nameRow = row.clone().addClass("Name").appendTo(table);
	var numberRow = row.clone().addClass("Number").appendTo(table);
	var controlRow = row.clone().addClass("Control").appendTo(table);
	var timeRow = row.clone().addClass("Time").appendTo(table);

	$.each( [ "Period", "Jam", "Lineup", "Timeout", "Intermission" ], function() {
		var clock = String(this);
		var sbClock = $sb("ScoreBoard.Clock("+clock+")");

		var nameTd = $("<td>").appendTo(nameRow);
		var numberTr = createRowTable(3).appendTo($("<td>").appendTo(numberRow)).find("tr");
		var controlTr = createRowTable(2).appendTo($("<td>").appendTo(controlRow)).find("tr");
		var timeTr = createRowTable(3).appendTo($("<td>").appendTo(timeRow)).find("tr");

		sbClock.$sb("Name").$sbElement("<a>").appendTo(nameTd.addClass("Name"));
		if (clock == "Period" || clock == "Jam") {
			var it = sbClock.$sb("InvertedTime").$sbElement("<a>", { sbelement: { convert: _timeConversions.msToMinSec } }).appendTo(nameTd).addClass("InvertedTime");
			sbClock.$sb("Direction").$sbBindAndRun("sbchange", function(event, value) {
				it.toggleClass("CountDown", isTrue(value));
				it.toggleClass("CountUp", !isTrue(value));
			});
		}
		sbClock.$sb("Running").$sbBindAndRun("sbchange", function(event,value) {
			nameTd.toggleClass("Running", isTrue(value));
		});
		$sb("ScoreBoard.NoMoreJam").$sbBindAndRun("sbchange", function(event,value) {
			nameTd.toggleClass("NoMoreJam", isTrue(value));
		});

		sbClock.$sb("Number").$sbControl("<a>").appendTo(numberTr.children("td:eq(1)")
				.addClass("Number").css("width", "20%"));
		if (clock == "Period") {
			var periodDialog = createPeriodDialog();
			numberTr.children("td:eq(1)").click(function() { periodDialog.dialog("open"); });
		} else if (clock == "Jam") {
			var jamDialog = createJamDialog();
			numberTr.children("td:eq(1)").click(function() { jamDialog.dialog("open"); });			
		} else if (clock == "Timeout") {
			timeoutDialog = createTimeoutDialog();
			numberTr.children("td:eq(1)").click(function() { timeoutDialog.dialog("open"); });
		}

		sbClock.$sb("Start").$sbControl("<button>").text("Start").val("true")
			.attr("id", "Clock"+clock+"Start").addClass("KeyControl").button()
			.appendTo(controlTr.children("td:eq(0)").addClass("Start"));
		sbClock.$sb("Stop").$sbControl("<button>").text("Stop").val("true")
			.attr("id", "Clock"+clock+"Stop").addClass("KeyControl").button()
			.appendTo(controlTr.children("td:eq(1)").addClass("Stop"));

		sbClock.$sb("Time").$sbControl("<button>", { sbcontrol: { sbSetAttrs: { change: true } } })
			.text("-1").val("-1000")
			.attr("id", "Clock"+clock+"TimeDown").addClass("KeyControl").button()
			.appendTo(timeTr.children("td:eq(0)").addClass("Button"));
		sbClock.$sb("Time").$sbElement("<a>", { sbelement: { convert: _timeConversions.msToMinSec } })
			.appendTo(timeTr.children("td:eq(1)").addClass("Time"));
		var timeDialog = createTimeDialog(sbClock);
		timeTr.children("td:eq(1)").click(function() { timeDialog.dialog("open"); });
		sbClock.$sb("Time").$sbControl("<button>", { sbcontrol: { sbSetAttrs: { change: true } } })
			.text("+1").val("1000")
			.attr("id", "Clock"+clock+"TimeUp").addClass("KeyControl").button()
			.appendTo(timeTr.children("td:eq(2)").addClass("Button"));
	});

	return table;
}

function createPeriodDialog() {
	var dialog = $("<div>").addClass("NumberDialog");
	var table = $("<table>").appendTo(dialog);
	var headers = $("<tr><td/><td/><td/><td/><td/></tr>").appendTo(table);
	$("<a>").text("Nr").addClass("Title")
		.appendTo(headers.children("td:eq(0)").addClass("Title"));
	$("<a>").text("Jams").addClass("Title")
		.appendTo(headers.children("td:eq(1)").addClass("Title"));
	$("<a>").text("Duration").addClass("Title")
		.appendTo(headers.children("td:eq(2)").addClass("Title"));

	WS.Register([
			'ScoreBoard.Period(*).CurrentJamNumber',
			'ScoreBoard.Period(*).Duration',
			'ScoreBoard.Period(*).Number',
			'ScoreBoard.Period(*).Running'], function (k, v) {
		var nr = k.Period;
		if (nr == null || nr == 0) { return; }
		var prefix = "ScoreBoard.Period(" + nr + ")";
		var key = k.field;
		if (k.parts.length > 3) { return; }
		if (!(["CurrentJamNumber", "Duration", "Number", "Running"].includes(key))) { return; }

		var row = table.find("tr.Period[nr="+nr+"]");
		if (row.length == 0 && v != null) {
			row = $("<tr>").addClass("Period").attr("nr", nr)
				.append($('<td>').addClass('Number').text(nr))
				.append($('<td>').addClass('Jams').text(0))
				.append($('<td>').addClass('Duration'))
				.append($('<td>').append($("<button>").text("Delete")
						.button().click(function () {
							//TODO: confirmation popup
							WS.Set(prefix + ".Delete", true);
						})))
				.append($('<td>').append($("<button>").text("Insert Before")
						.button().click(function () {
							WS.Set(prefix + ".InsertBefore", true); 
						})));
			var inserted = false;
			table.find("tr.Period").each(function (i, r) {
				r = $(r);
				if (Number(r.attr("nr")) > Number(nr)) {
					r.before(row);
					inserted = true;
					return false;
				}});
			if (!inserted) {
				row.appendTo(table);
			}
		} else if (key == "Number" && v == null && row.length > 0) {
			row.remove();
			return;
		}
		if (v != null) {
			if (key == "CurrentJamNumber") { row.children("td.Jams").text(v); }
			if (key == "Duration" && !isTrue(WS.state[prefix + '.Running'])) { row.children("td.Duration").text(_timeConversions.msToMinSec(v)); }
			if (key == "Running" && isTrue(v)) { row.children("td.Duration").text("running"); }
			if (key == "Running" && !isTrue(v)) { row.children("td.Duration").text(_timeConversions.msToMinSec(WS.state[prefix + '.Duration'])); }
		}
	});

	return dialog.dialog({
		title: "Periods",
		autoOpen: false,
		modal: true,
		width: 500,
		buttons: { Close: function() { $(this).dialog("close"); } }
	});
}

function createJamDialog() {
	var dialog = $("<div>").addClass("NumberDialog");
	var tableTemplate = $("<table>").addClass("Period");
	var headers = $("<tr><td/><td/><td/><td/><td/><td/></tr>").appendTo(tableTemplate);
	$("<a>").text("Nr").addClass("Title")
		.appendTo(headers.children("td:eq(0)").addClass("Title"));
	$("<a>").text("Points").addClass("Title")
		.appendTo(headers.children("td:eq(1)").addClass("Title"));
	$("<a>").text("Duration").addClass("Title")
		.appendTo(headers.children("td:eq(2)").addClass("Title"));
	$("<a>").text("PC at end").addClass("Title")
		.appendTo(headers.children("td:eq(3)").addClass("Title"));
	var currentPeriod;

	WS.Register([
			'ScoreBoard.Period(*).Jam(*).Duration',
			'ScoreBoard.Period(*).Jam(*).Number',
			'ScoreBoard.Period(*).Jam(*).PeriodClockDisplayEnd',
			'ScoreBoard.Period(*).Jam(*).TeamJam(*).JamScore'], function (k, v) {
		var per = k.Period;
		if (per == 0) { return; }
		var nr = k.Jam;
		var prefix = "ScoreBoard.Period(" + per + ").Jam(" + nr + ")";
		var key = k.field;
		
		var table = dialog.find("table.Period[nr="+per+"]");
		if (table.length == 0 && v != null) {
			table = tableTemplate.clone().attr("nr", per).appendTo(dialog);
			if (per == currentPeriod) {
				table.addClass('Show');
			}
		}
		if (table.length == 0) { return; }

		var row = table.find("tr.Jam[nr="+nr+"]");
		if (row.length == 0 && v != null) {
			row = $("<tr>").addClass("Jam").attr("nr", nr)
				.append($('<td>').addClass('Number').text(nr))
				.append($('<td>').addClass('Points').append($('<span>').addClass('1'))
						.append($('<span>').text(" - ")).append($('<span>').addClass('2')))
				.append($('<td>').addClass('Duration'))
				.append($('<td>').addClass('PC'))
				.append($('<td>').append($("<button>").text("Delete")
						.button().click(function () {
							//TODO: confirmation popup
							WS.Set(prefix + ".Delete", true);
						})))
				.append($('<td>').append($("<button>").text("Insert Before")
						.button().click(function () {
							WS.Set(prefix + ".InsertBefore", true); 
						})));
			var inserted = false;
			table.find("tr.Jam").each(function (i, r) {
				r = $(r);
				if (Number(r.attr("nr")) > Number(nr)) {
					r.before(row);
					inserted = true;
					return false;
				}});
			if (!inserted) {
				row.appendTo(table);
			}
		} else if (key == "Number" && v == null && row.length > 0) {
			row.remove();
			return;
		}
		if (v != null) {
			if (key == "JamScore") { row.find("td.Points ."+k.TeamJam).text(v); }
			if (key == "Duration") {
				if (WS.state[prefix + '.WalltimeEnd'] == 0 && WS.state[prefix + '.WalltimeStart'] > 0) {
					row.children("td.Duration").text("running");
				} else {
					row.children("td.Duration").text(_timeConversions.msToMinSec(v));
				}
			}
			if (key == 'PeriodClockDisplayEnd') {
				if (WS.state[prefix + '.WalltimeEnd'] == 0 && WS.state[prefix + '.WalltimeStart'] > 0) {
					row.children("td.PC").text("running");
				} else {
					row.children("td.PC").text(_timeConversions.msToMinSec(v));
				}
			}
		}
	});
	
	WS.Register(['ScoreBoard.CurrentPeriodNumber'], function(k, v) {
		currentPeriod = v;
		dialog.find("table.Period.Show").removeClass("Show");
		dialog.find("table.Period[nr="+v+"]").addClass("Show");
	})

	return dialog.dialog({
		title: "Jams",
		autoOpen: false,
		modal: true,
		width: 550,
		buttons: { Close: function() { $(this).dialog("close"); } }
	});
}

function createTimeoutDialog() {
	var firstJamListed = [0];
	var lastJamListed = [0];
	var periodDropdownTemplate = $('<select>').attr('id', 'PeriodDropdown')
		.append($('<option>').attr('value', 0).text('P0'));
	var jamDropdownTemplate = [$('<select>').attr('id', 'JamDropdown').attr('period', 0)
		.append($('<option>').attr('value', 0).text('J0'))];
	var typeDropdownTemplate = $('<select>').attr('id', 'TypeDropdown')
		.append($('<option>').attr('value', '.false').text('No type'))
		.append($('<option>').attr('value', 'O.false').text('Off. Timeout'))
		.append($('<option>').attr('value', '1.false').text('Team TO left'))
		.append($('<option>').attr('value', '2.false').text('Team TO right'))
		.append($('<option>').attr('value', '1.true').text('Off. Review left'))
		.append($('<option>').attr('value', '2.true').text('Off. Review right'));
	
	var dialog = $("<div>").addClass("NumberDialog");
	var table = $("<table>").appendTo(dialog);
	var headers = $("<tr><td/><td/><td/><td/><td/><td/><td/></tr>").appendTo(table);
	$("<a>").text("Period").addClass("Title")
		.appendTo(headers.children("td:eq(0)").addClass("Title"));
	$("<a>").text("After Jam").addClass("Title")
		.appendTo(headers.children("td:eq(1)").addClass("Title"));
	$("<a>").text("Duration").addClass("Title")
		.appendTo(headers.children("td:eq(2)").addClass("Title"));
	$("<a>").text("Period Clock").addClass("Title")
	.appendTo(headers.children("td:eq(3)").addClass("Title"));
	$("<a>").text("Type").addClass("Title")
		.appendTo(headers.children("td:eq(4)").addClass("Title"));
	$("<a>").text("Retained").addClass("Title")
		.appendTo(headers.children("td:eq(5)").addClass("Title"));
	
	var footer = $("<tr><td/><td colspan=\"3\"/><td/><td/><td/></tr>").attr('id', 'toFooter').appendTo(table);
	periodDropdownTemplate.clone().appendTo(footer.find('td:eq(0)'));
	$('<button>').text('Add Timeout').button().click(function() {
		WS.Set('ScoreBoard.Period('+footer.find('#PeriodDropdown').val()+').InsertTimeout', true);
	}).appendTo(footer.find('td:eq(1)'));
	
	WS.Register(['ScoreBoard.Clock(Period).Time']);
	
	WS.Register(['ScoreBoard.CurrentPeriodNumber'], function(k, v) {
		footer.find('#PeriodDropdown').val(v);
	});
	WS.Register(['ScoreBoard.Period(*).CurrentJamNumber', 'ScoreBoard.Period(*).FirstJamNumber'], processJamNumber);

	WS.Register(['ScoreBoard.Period(*).CurrentJam', 'ScoreBoard.Period(*).Jam(*).Id']);
	WS.Register(['ScoreBoard.Period(*).Timeout'], processTimeout);
	
	function addJam(p, j, append) {
		var option = $('<option>').attr('value', j).text('J'+j);
		if (append) {
			jamDropdownTemplate[p].append(option.clone());
			table.find('#JamDropdown[period='+p+']').append(option);
		} else {
			jamDropdownTemplate[p].prepend(option.clone());
			table.find('#JamDropdown[period='+p+']').prepend(option);
		}
	}
	function removeJam(p, j) {
		jamDropdownTemplate[p].children('option[value='+j+']').remove();
		table.find('#JamDropdown[period='+p+'] option[value='+j+']').remove();
	}
	function clearPeriod(p) {
		table.find('tr.Timeout[period='+p+']').remove();
		jamDropdownTemplate[p].children('option').remove();
		table.find('#JamDropdown[period='+p+'] option').remove();
		firstJamListed[p] = 0;
		lastJamListed[p] = 0;
	}

	function processJamNumber(k, v) {
		var p = Number(k.Period);
		if (v == null) {
			if (jamDropdownTemplate[p] != null) {
				periodDropdownTemplate.children('option[value='+p+']').remove();
				table.find('#PeriodDropdown option[value='+p+']').remove();
				clearPeriod(p);
				delete jamDropdownTemplate[p];
			}
			return;
		}
		if (jamDropdownTemplate[p] == null) {
			firstJamListed[p] = 0;
			lastJamListed[p] = 0;
			jamDropdownTemplate[p] = $('<select>').attr('id', 'JamDropdown').attr('period', p);
			var option = $('<option>').attr('value', p).text('P'+p);
			_windowFunctions.appendAlphaNumSortedByAttr(periodDropdownTemplate, option.clone(), 'value', 0);
			table.find('#PeriodDropdown').each(function(idx, e) {
					_windowFunctions.appendAlphaNumSortedByAttr($(e), option.clone(), 'value', 0);
			});
			footer.find('#PeriodDropdown').val(WS.state['ScoreBoard.CurrentPeriodNumber']);
		}

		newFirst = WS.state['ScoreBoard.Period('+p+').FirstJamNumber'];
		newLast = WS.state['ScoreBoard.Period('+p+').CurrentJamNumber'];
		oldFirst = firstJamListed[p];
		oldLast = lastJamListed[p];
		var j;
		if (newFirst == 0 && oldFirst == 0) {
			return;
		}
		if (newFirst == 0 && oldFirst > 0) {
			clearPeriod(p);
			return;
		}
		if (newFirst > 0 && !(oldFirst > 0)) {
			for (j = newFirst; j <= newLast; j++) { addJam(p, j, true); }
			firstJamListed[p] = newFirst;
			lastJamListed[p] = newLast;
			return;
		}
		for (j = oldFirst; j < newFirst; j++) { removeJam(p, j); }
		for (j = oldFirst; j > newFirst; j--) { addJam(p, j-1, false); }
		for (j = oldLast; j < newLast; j++) { addJam(p, j+1, true); }
		for (j = oldLast; j > newLast; j--) { removeJam(p, j); }
		firstJamListed[p] = newFirst;
		lastJamListed[p] = newLast;
	}
	
	function processTimeout(k, v) {
		var id = k.Timeout;
		if (id == 'noTimeout') { return; }
		var p = Number(k.Period);
		var prefix = 'ScoreBoard.Period('+k.Period+').Timeout('+id+')';
		var row = table.find('tr.Timeout[toId='+id+']');
		if (k.field == 'Id' && v == null && row.length > 0) {
			row.remove();
			return;
		}
		if (k.field == 'PrecedingJamNumber') {
			row.remove();
			row = [];
		}
		if (v != null && row.length == 0) {
			var jam = Number(WS.state[prefix+'.PrecedingJamNumber']);
			var dur = isTrue(WS.state[prefix+'.Running']) ? 'Running' : _timeConversions.msToMinSec(WS.state[prefix+'.Duration']);
			var pc = _timeConversions.msToMinSec(isTrue(WS.state[prefix+'.Running']) ?
					WS.state['ScoreBoard.Clock(Period).Time'] :
					WS.state[prefix+'.PeriodClockEnd']);
			var type = WS.state[prefix+'.Owner'] + '.' + WS.state[prefix+'.Review'];
			var review = isTrue(WS.state[prefix+'.Review']);
			var retained = isTrue(WS.state[prefix+'.RetainedReview']);
			row = $("<tr>").addClass("Timeout").attr("toId", id).attr('period', k.Period).attr('jam', jam)
				.append($('<td>').addClass('Period').append(periodDropdownTemplate.clone().val(p).change(function() {
					WS.Set(prefix+'.PrecedingJam', WS.state['ScoreBoard.Period('+$(this).val()+').CurrentJam']);
				})))
				.append($('<td>').addClass('Jam').append(jamDropdownTemplate[p].clone().val(jam).change(function() {
					WS.Set(prefix+'.PrecedingJam', WS.state['ScoreBoard.Period('+p+').Jam('+$(this).val()+').Id']);
				})))
				.append($('<td>').addClass('Duration').text(dur))
				.append($('<td>').addClass('PerClock').text(pc))
				.append($('<td>').addClass('Type').append(typeDropdownTemplate.clone().val(type).change(function() {
					var parts = $(this).val().split('.');
					WS.Set(prefix+'.Owner', parts[0]);
					WS.Set(prefix+'.Review', isTrue(parts[1]));
				})))
				.append($('<td>').addClass('Retained').append($('<button>').toggleClass('Hide', !review).toggleClass('Active', retained)
						.text('Retained').button().click(function () {
							WS.Set(prefix+'.RetainedReview', !isTrue(WS.state[prefix+'.RetainedReview']));
						})))
				.append($('<td>').append($("<button>").text("Delete")
						.button().click(function () {
							//TODO: confirmation popup
							WS.Set(prefix + ".Delete", true);
						})));
			var inserted = false;
			table.find("tr.Timeout").each(function (i, r) {
				r = $(r);
				if (Number(r.attr('period')) > p ||
						(Number(r.attr('period')) == p && Number(r.attr('jam')) > jam)) {
					r.before(row);
					inserted = true;
					return false;
				}
			});
			if (!inserted) {
				table.find('#toFooter').before(row);
			}
		}
		switch(k.field) {
			case 'Running':
			case 'PeriodClockEnd':
				row.find('.PerClock').text(_timeConversions.msToMinSec(isTrue(WS.state[prefix+'.Running']) ?
						WS.state['ScoreBoard.Clock(Period).Time'] :
						WS.state[prefix+'.PeriodClockEnd']));
				break;
			case 'Running':
			case 'Duration':
				row.find('.Duration').text(isTrue(WS.state[prefix+'.Running']) ?
						'Running' :
						_timeConversions.msToMinSec(WS.state[prefix+'.Duration']));
				break;
			case 'Review':
				row.find('.Retained button').toggleClass('Hide', !isTrue(v));
				//no break
			case 'Owner':
				row.find('#TypeDropdown').val(WS.state[prefix+'.Owner'] + '.' + WS.state[prefix+'.Review']);
				break;
			case 'RetainedReview':
				row.find('.Retained button').toggleClass('Active', isTrue(v));
				break;
		}
	}
	
	return dialog.dialog({
		title: "Timeouts",
		autoOpen: false,
		modal: true,
		width: 750,
		buttons: { Close: function() { $(this).dialog("close"); } }
	});
}

function createTimeDialog(clock) {
	var dialog = $("<div>");
	var table = $("<table>").appendTo(dialog).addClass("TimeDialog");
	var row = $("<tr><td/></tr>");
	row.clone().appendTo(table).addClass("Time");
	row.clone().appendTo(table).addClass("MinimumTime");
	row.clone().appendTo(table).addClass("MaximumTime");
	row.clone().appendTo(table).addClass("Direction");

	$.each( [ "Time", "MinimumTime", "MaximumTime" ], function() {
		var e = clock.$sb(this);
		var rowTable = createRowTable(3).appendTo(table.find("tr."+this+">td"));
		rowTable.find("tr:eq(0)").before("<tr><td colspan='3'/></tr>");

		$("<a>").text(this+": ").addClass("Title")
			.appendTo(rowTable.find("tr:eq(0)>td").addClass("Title"));
		e.$sbElement("<a>", { sbelement: { convert: _timeConversions.msToMinSec } })
			.addClass("Time")
			.appendTo(rowTable.find("tr:eq(0)>td"));
		e.$sbControl("<button>", { sbcontrol: { sbSetAttrs: { change: "true" } } })
			.text("-sec").val("-1000").button()
			.appendTo(rowTable.find("tr:eq(1)>td:eq(0)"));
		e.$sbControl("<button>", { sbcontrol: { sbSetAttrs: { change: "true" } } })
			.text("+sec").val("1000").button()
			.appendTo(rowTable.find("tr:eq(1)>td:eq(2)"));
		$("<input type='text' size='5'>").appendTo(rowTable.find("tr:eq(1)>td:eq(1)"));
		$("<button>").text("Set").addClass("Set").button().appendTo(rowTable.find("tr:eq(1)>td:eq(1)"));
		e.$sbControl(rowTable.find("tr:eq(1)>td:eq(1)").children(), { sbcontrol: {
			convert: _timeConversions.minSecToMs,
			delayupdate: true,
			noSetControlValue: true
		}});
	});

	$("<tr><td/><td/><td/></tr>").insertAfter(table.find("tr.Time table tr:eq(0)"));
	$.each( [ "Start", "ResetTime", "Stop" ], function(i) {
		clock.$sb(String(this)).$sbControl("<button>").text(String(this)).val("true").button()
			.appendTo(table.find("tr.Time table tr:eq(1)>td:eq("+i+")"));
	});

	var rowTable = createRowTable(1,2).appendTo(table.find("tr.Direction>td"));
	clock.$sb("Direction").$sbElement("<a>", { sbelement: {
			convert: function(value) { return "Count "+(isTrue(value)?"Down":"Up"); }
		} }).appendTo(rowTable.find("tr:eq(0)>td").addClass("Title"));
	clock.$sb("Direction").$sbControl("<button>", { sbcontrol: {
			getButtonValue: function() { return String(!isTrue($(this).val())); },
			setButtonValue: function(value) { $(this).val(value); }
		} }).text("Switch Direction").button().appendTo(rowTable.find("tr:eq(1)>td"));

	return dialog.dialog({
		title: clock.$sb("Name").$sbGet()+" Time",
		autoOpen: false,
		modal: true,
		width: 400,
		buttons: { Close: function() { $(this).dialog("close"); } }
	});
}

var timeSetWarningAck = false;
var timeSetWarningRefocusing = false;
function createTimeSetWarningDialog(source) {
	if (timeSetWarningAck || timeSetWarningRefocusing )
		return;
	var dialog = $("<div>");
	$("<p>").text("Warning: key control is still enabled while entering the time!")
		.appendTo(dialog);
	$("<p>").html("Any keys you press while entering the time <b>will affect</b> any buttons they are assigned to!")
		.appendTo(dialog);
	$("<p>").html("If this is a problem for you, edit the time using the time dialog instead,<br>by clicking on the time display itself.")
		.appendTo(dialog);
	dialog.dialog({
		title: "Caution",
		modal: true,
		width: "700px",
		close: function() {
			timeSetWarningRefocusing = true;
			dialog.dialog("destroy").remove();
			source.focus();
			timeSetWarningRefocusing = false;
		},
		buttons: [
			{
				text: "Don't remind me again",
				click: function() { timeSetWarningAck = true; $(this).dialog("close"); }
			},
			{
				text: "Ok",
				click: function() { $(this).dialog("close"); }
			}
		]
	});
}


//////////////////////
// ScoreBoard View tab
//////////////////////

function createScoreBoardViewTab() {
	var table = $("<table>").attr("id", "ScoreBoardView")
		.appendTo(createTab("ScoreBoard View", "ScoreBoardViewTab"))

		var usePreviewButton = $("<label for='UsePreviewButton'/><input type='checkbox' id='UsePreviewButton'/>");
	usePreviewButton.last().button();
	_crgUtils.bindAndRun(usePreviewButton.filter("input:checkbox"), "change", function() {
		$(this).button("option", "label", (isTrue(this.checked)?"Editing Live ScoreBoard":"Editing Preview"));
		table.toggleClass("UsePreview", !isTrue(this.checked));
	});
	var applyPreviewButton = $("<button>Apply Preview</button>").button()
		.click(function() {
			var done = {};
			table.find(".Preview [ApplyPreview]").each(function(_, e) {
				var name = $(e).attr("ApplyPreview");
				if (done[name]) return;
				WS.Set("ScoreBoard.Settings.Setting(ScoreBoard.View_" + name + ")",
						WS.state["ScoreBoard.Settings.Setting(ScoreBoard.Preview_" + name + ")"]);
				done[name] = true;
			});
		});

	$("<tr><td/></tr>").appendTo(table)
		.find("td").addClass("Header NoChildren PreviewControl")
		.append(createRowTable(3))
		.find("td:first")
		.next().append(usePreviewButton)
		.next().append(applyPreviewButton);

	$.each( [ "View", "Preview" ], function(i,p) {
		createScoreBoardViewPreviewRows(table, p);
	});

	$("<tr><td/></tr>").appendTo(table)
		.find("td").addClass("ViewFrames Header")
		.append(createRowTable(2))
		.find("td")
		.first().append("<a>Current</a>")
		.next().append("<a>Preview</a>");

	var previewButton = $("<button>").html("Show Preview").click(function() {
		$("<iframe>Your browser does not support iframes.</iframe>")
			.attr({ scrolling: "no", frameborder: "0", src: $(this).attr("src") })
			.replaceAll(this);
	});

	var sbUrl = "/views/standard/index.html?videomuted=true&videocontrols=true";
	$("<tr><td/></tr>").appendTo(table)
		.find("td").addClass("ViewFrames Footer")
		.append(createRowTable(2))
		.find("td").append(previewButton)
		.find("button")
		.first().attr("src", sbUrl).end()
		.last().attr("src", sbUrl+"&preview=true");
}

function createScoreBoardViewPreviewRows(table, type) {
	var currentViewTd = $("<tr><td/></tr>").addClass(type).appendTo(table)
		.children("td").addClass("Header NoChildren CurrentView")
		.attr("ApplyPreview", "CurrentView")
		.append("<label >ScoreBoard</label><input type='radio' value='scoreboard'/>")
		.append("<label >WhiteBoard</label><input type='radio' value='whiteboard'/>")
		.append("<label >Image</label><input type='radio' value='image'/>")
		.append("<label >Video</label><input type='radio' value='video'/>")
		.append("<label >Custom Page</label><input type='radio' value='html'/>");

	currentViewTd.children("input")
		.attr("name", "createScoreBoardViewPreviewRows" + type)
		.each(function(_, e) {
			e = $(e);
			e.attr("id", "createScoreBoardViewPreviewRows" + e.attr("value") + type);
			e.prev().attr("for", "createScoreBoardViewPreviewRows" + e.attr("value") + type);
		})
	.change(function(e) {
		WS.Set("ScoreBoard.Settings.Setting(ScoreBoard." + type + "_CurrentView)", e.target.value);
	});

	currentViewTd.buttonset({items: "input"}).prepend("<a>Current View : </a>");

	WS.Register("ScoreBoard.Settings.Setting(ScoreBoard." + type + "_CurrentView)", function(k, v) {
		currentViewTd.children("input[value=" + v + "]").prop("checked", true).button("refresh");
	});


	$("<tr><td><a>ScoreBoard Options</a></td></tr>").addClass(type).appendTo(table)
		.find("td").addClass("ScoreBoardOptions Header");

	var intermissionControlDialog = createIntermissionControlDialog();
	var intermissionControlButton = $("<button>Intermission Labels</button>").button().addClass("ui-button-small")
		.click(function() { intermissionControlDialog.dialog("open"); });
	var toggleButton = function(key, trueText, falseText) {
		var button = $("<label/><input type='checkbox'/>").addClass("ui-button-small");
		var id = _crgScoreBoard.newUUID(true);
		button.first().attr("for", id);
		var input = button.last().attr("id", id).button();
		input.change(function(e) { WS.Set(key, input.prop("checked")); });
		WS.Register(key, function(k, v) {
			input.button("option", "label", isTrue(v)?trueText:falseText)
				.prop("checked", isTrue(v))
				.button("refresh");
		});
		return button;
	}

	var syncClocksButton = toggleButton("ScoreBoard.Settings.Setting(ScoreBoard.Clock.Sync)", "Clocks Synced", "Clocks Unsynced");
	var forceServedButton = toggleButton("ScoreBoard.Settings.Setting(ScoreBoard.Penalties.ForceServed)", "Assume Penalties Served", "Track Penalty Serving");
	var swapTeamsButton = toggleButton("ScoreBoard.Settings.Setting(ScoreBoard." + type + "_SwapTeams)", "Team sides swapped", "Team sides normal");
	swapTeamsButton.attr("ApplyPreview", "SwapTeams");

	var WSControl = function(key, element) {
		element.change(function() { WS.Set(key, element.val()); });
		WS.Register(key, function(k, v) { element.val(v); });
		return element;
	}

	var clockAfterTimeout = $("<label>Clock shown after Timeout: </label>").add(WSControl("ScoreBoard.Settings.Setting(ScoreBoard.ClockAfterTimeout)",
				$("<select>")
				.append("<option value='Lineup'>Lineup</option>")
				.append("<option value='Timeout'>Timeout</option>")));

	var boxStyle = $("<label>Box Style: </label>").add(WSControl("ScoreBoard.Settings.Setting(ScoreBoard." + type + "_BoxStyle)",
				$("<select>").attr("ApplyPreview", "BoxStyle")
				.append("<option value=''>Rounded</option>")
				.append("<option value='box_flat'>Flat</option>")
				.append("<option value='box_flat_bright'>Flat & Bright</option>")));
	var sidePadding = $("<label>Side Padding: </label>").add(WSControl("ScoreBoard.Settings.Setting(ScoreBoard." + type + "_SidePadding)",
				$("<select>").attr("ApplyPreview", "SidePadding")
				.append("<option value=''>None</option>")
				.append("<option value='2'>2%</option>")
				.append("<option value='4'>4%</option>")
				.append("<option value='6'>6%</option>")
				.append("<option value='8'>8%</option>")
				.append("<option value='10'>10%</option>")));

	var mediaSelect = function(key, format, type, humanName) {
		var select = $("<select>").append($("<option value=''>No " + humanName + "</option>"));
		WS.Register("ScoreBoard.Media.Format(" + format + ").Type(" + type + ").File(*).Name", function(k, v) {
			select.children("[value='"+k.File+"']").remove();
			if (v != null) {
				var option = $("<option>").attr("name", v).val("/" + format + "/" + type + "/" + k.File).text(v);
				_windowFunctions.appendAlphaSortedByAttr(select, option, "name", 1);
				select.val(WS.state[key]);
			}
		});
		WSControl(key, select);
		return select;
	}

	var imageViewSelect = $("<label>Image View: </label>")
		.add(mediaSelect("ScoreBoard.Settings.Setting(ScoreBoard." + type + "_Image)", "images", "fullscreen", "Image"))
		.attr("ApplyPreview", "Image");
	var videoViewSelect = $("<label>Video View: </label>")
		.add(mediaSelect("ScoreBoard.Settings.Setting(ScoreBoard." + type + "_Video)", "videos", "fullscreen", "Video"))
		.attr("ApplyPreview", "Video");
	var customPageViewSelect = $("<label>Custom Page View: </label>")
		.add(mediaSelect("ScoreBoard.Settings.Setting(ScoreBoard." + type + "_CustomHtml)", "customhtml", "fullscreen", "Page"))
		.attr("ApplyPreview", "CustomHtml");

	var optionsTable = $("<table/>")
		.addClass(type)
		.addClass("RowTable")
		.css("width", "100%");
	$("<tr><td></td></tr>").addClass(type).appendTo(table).find("td").append(optionsTable);
	$("<tr><td/><td/><td/></tr>").addClass(type).appendTo(optionsTable)
		.find("td").addClass("ScoreBoardOptions Footer")
		.first().append(intermissionControlButton)
		.next().append(swapTeamsButton)
		.next().append(imageViewSelect);
	$("<tr><td/><td/><td/></tr>").addClass(type).appendTo(optionsTable)
		.find("td").addClass("ScoreBoardOptions Footer")
		.first().append(syncClocksButton)
		.next().append(boxStyle)
		.next().append(videoViewSelect);
	$("<tr><td/><td/><td/></tr>").addClass(type).appendTo(optionsTable)
		.find("td").addClass("ScoreBoardOptions Footer")
		.first().append(clockAfterTimeout)
		.next().append(sidePadding)
		.next().append(customPageViewSelect);
	$("<tr><td/><td/><td/></tr>").addClass(type).appendTo(optionsTable)
		.find("td").addClass("ScoreBoardOptions Footer")
		.first().append(forceServedButton);

}

function createIntermissionControlDialog() {
	var table = $("<table>").addClass("IntermissionControlDialog");

	var fields = [
	{ id: "ScoreBoard.Intermission.PreGame", display: "Pre Game"},
	{ id: "ScoreBoard.Intermission.Intermission", display: "Intermission"},
	{ id: "ScoreBoard.Intermission.Unofficial", display: "Unofficial Score"},
	{ id: "ScoreBoard.Intermission.Official", display: "Official Score"},
	];
	$.each( fields, function(i, field) {
		var path = "ScoreBoard.Settings.Setting(" + field.id + ")";
		var row = $("<tr>").appendTo(table);
		$("<td>").addClass("Name").text(field.display).appendTo(row);
		var input = $("<input>").attr("type", "text").val(WS.state[path])
			.bind("input", function(e) { WS.Set(path, e.target.value); })
			.appendTo($("<td>").addClass("Value").appendTo(row));
		WS.Register(path, function(k, v) { input.val(v); } );
	});

	return $("<div>").append(table).dialog({
		title: "Intermission Display Labels",
		autoOpen: false,
		width: 700,
		modal: true,
		buttons: { Close: function() { $(this).dialog("close"); } }
	});
}



////////////
// Teams tab
////////////

function createTeamsTab() {
	$("<table>").attr("id", "Teams")
		.appendTo(createTab("Teams", "TeamsTab"));

	$("#Teams")
		.append($("<tr><td/></tr>").addClass("Selection NoneSelected"))
		.append($("<tr><td/></tr>").addClass("Team"));
	createRowTable(2).appendTo("#Teams>tbody>tr.Selection>td").addClass("Selection");

	var selectTeam = $("<select>").appendTo("#Teams table.Selection td:eq(0)");
	$("<option value=''>No Team Selected</option>").appendTo(selectTeam);
	$("<option value='(Current Team 1)'>Current Team 1</option>").appendTo(selectTeam);
	$("<option value='(Current Team 2)'>Current Team 2</option>").appendTo(selectTeam);
	var newTeamName = $("<input type='text' size='30'/>")
		.appendTo("#Teams table.Selection td:eq(1)");
	var createNewTeam = $("<button>").text("New Team").attr("disabled", true).button()
		.appendTo("#Teams table.Selection td:eq(1)");
	var isCurrentTeam = function(){ return selectTeam.val().startsWith("(Current Team");}
	var getPrefix = function() {
		if (isCurrentTeam()) {
			return "ScoreBoard.Team("+selectTeam.val()[14]+")";
		} else {
			return "ScoreBoard.PreparedTeam("+selectTeam.val()+")";
		}
	}

	newTeamName.keyup(function(event) {
		createNewTeam.button("option", "disabled", (!$(this).val()));
		if (!createNewTeam.button("option", "disabled") && (13 == event.which)) { // Enter
			createNewTeam.click();
		}
	});
	var waitingOnNewTeam = "";
	createNewTeam.click(function(event) {
		var teamname = newTeamName.val();
		var teamid = _crgUtils.checkSbId(teamname);
		WS.Set("ScoreBoard.PreparedTeam("+teamid+").Name", teamname);
		waitingOnNewTeam = teamid;
		newTeamName.val("").keyup().focus();
		// If team already exists, switch to it.
		selectTeam.val(teamid);
		selectTeam.change();
	});

	WS.Register("ScoreBoard.PreparedTeam(*).Id", function(k, v) {
		if (v == null) {
			selectTeam.children("option[value='"+k.PreparedTeam+"']").remove();
			return;
		}
		if (selectTeam.children("option[value='"+k.PreparedTeam+"']").length == 0) {
			var option = $("<option>").attr("value", v).text(v);
			_windowFunctions.appendAlphaSortedByAttr(selectTeam, option, 'value', 3);
		}
		if (v == waitingOnNewTeam) {
			selectTeam.val(v);
			selectTeam.change();
			waitingOnNewTeam = "";
		}
	});

	var teamTable = $("<table>").addClass("Team")
		.append($("<tr><td/></tr>").addClass("Control"))
		.append($("<tr><td/></tr>").addClass("Skaters"))
		.appendTo($("#Teams .Team td"));
	var controlTable = createRowTable(5).appendTo(teamTable.find("tr.Control>td")).addClass("Control");
	var teamName = $("<input type='text' class='Name'>").appendTo(controlTable.find("td:eq(0)"));
	teamName.bind("input", function() {
		WS.Set(getPrefix() + ".Name", teamName.val());
	});
	var waitingOnUpload = "";
	var logoSelect = $("<select>").append($("<option value=''>No Logo</option>"))
		.appendTo(controlTable.find("td:eq(1)"));
	logoSelect.change(function() {
		WS.Set(getPrefix() + ".Logo", logoSelect.val() == "" ? "": "/images/teamlogo/" + logoSelect.val())
	});
	WS.Register("ScoreBoard.Media.Format(images).Type(teamlogo).File(*).Name", function(k, v) {
		var val = logoSelect.val();	// Record this before we potentially remove and re-add it.
		if (v != null) {
			if (waitingOnUpload == k.File) {
				val = k.File;
				waitingOnUpload = "";
			}
			logoSelect.children("[value='"+k.File+"']").remove();
			var option = $("<option>").attr("name", v).attr("value", k.File).text(v);
			_windowFunctions.appendAlphaSortedByAttr(logoSelect, option, "name", 1);
			logoSelect.val(val);
		}
	});
	$("<input type='file' id='teamLogoUpload'>").fileupload({
		url: "/Media/upload",
		formData: [{name: "media", value: "images"}, {name: "type", value: "teamlogo"}],
		add: function(e, data) {
			var fd = new FormData();
			fd.append("f", data.files[0], (isCurrentTeam() ? "current" : selectTeam.val())+ "_" + data.files[0].name);
			data.files[0] = fd.get("f");
			data.submit();
			waitingOnUpload = fd.get("f").name;
		},
		fail: function(e, data) {
			console.log("Failed upload", data.errorThrown);
		}
	}).css("display", "none").appendTo(controlTable.find("td:eq(1)"));
	$("<button>").text("Upload...").appendTo(controlTable.find("td:eq(1)")).click(function(){controlTable.find("#teamLogoUpload").click();});
	var alternameNameDialog = createAlternateNamesDialog();
	$("<button>").text("Alternate Names").button()
		.click(function() {alternameNameDialog.dialog("open");} )
		.appendTo(controlTable.find("td:eq(2)"));
	var colorsDialog = createColorsDialog();
	$("<button>").text("Colors").button()
		.click(function() { colorsDialog.dialog("open"); })
		.appendTo(controlTable.find("td:eq(3)"));
	var removeTeam = $("<button>").text("Remove Team").button()
		.click(function() { createTeamsRemoveDialog(selectTeam.val()); })
		.appendTo(controlTable.find("td:eq(4)"));

	WS.Register(["ScoreBoard.PreparedTeam(*)"], function(k, v) {
		var teamId = selectTeam.val();
		if (k.PreparedTeam == teamId) {
			handleTeamUpdate(k, v);
		}
	});
	WS.Register(["ScoreBoard.Team(*).AlternateName",
			"ScoreBoard.Team(*).Color",
			"ScoreBoard.Team(*).Logo",
			"ScoreBoard.Team(*).Name",
			"ScoreBoard.Team(*).Skater(*).Flags",
			"ScoreBoard.Team(*).Skater(*).Name",
			"ScoreBoard.Team(*).Skater(*).Number"], function(k, v) {
				var teamId = selectTeam.val();
				if ("(Current Team "+k.Team+")" == teamId) {
					handleTeamUpdate(k, v);
				}
			});

	var skatersTable = $("<table>").addClass("Skaters Empty")
		.appendTo(teamTable.find("tr.Skaters>td"))
		.append("<col class='Number'>")
		.append("<col class='Name'>")
		.append("<col class='Flags'>")
		.append("<col class='Button'>")
		.append("<thead/><tbody/>")
		.children("thead")
		.append("<tr><th></th><th class='Title'>Skaters</th><th id='skaterCount'></th><th></th></tr>")
		.append("<tr><th>Number</th><th>Name</th><th>Flags</th><th>Add</th>")
		.append("<tr class='AddSkater'><th/><th/><th/><th/><th/></tr>")
		.append("<tr><th colspan='4'><hr/></th></tr>")
		.end();

	var addSkater = function(number, name, flags, id) {
		id = id || _crgScoreBoard.newUUID(true);
		WS.Set(getPrefix() + ".Skater("+id+").Number", number);
		WS.Set(getPrefix() + ".Skater("+id+").Name", name);
		WS.Set(getPrefix() + ".Skater("+id+").Flags", flags);
	}

	var newSkaterNumber = $("<input type='text' size='10'>").addClass("Number")
		.appendTo(skatersTable.find("tr.AddSkater>th:eq(0)"));
	var newSkaterName = $("<input type='text' size='30'>").addClass("Name")
		.appendTo(skatersTable.find("tr.AddSkater>th:eq(1)"));
	var newSkaterFlags = $("<select>").addClass("Flags")
		.appendTo(skatersTable.find("tr.AddSkater>th:eq(2)"));
	var newSkaterButton = $("<button>").text("Add Skater").button({ disabled: true }).addClass("AddSkater")
		.appendTo(skatersTable.find("tr.AddSkater>th:eq(3)"))
		.click(function() {
			addSkater(newSkaterNumber.val(), newSkaterName.val(), newSkaterFlags.val());
			newSkaterNumber.val("").focus();
			newSkaterFlags.val("");
			newSkaterName.val("");
			$(this).blur();
			newSkaterButton.button("option", "disabled", true);
		});
	newSkaterName.add(newSkaterNumber).keyup(function(event) {
		newSkaterButton.button("option", "disabled", (!newSkaterName.val() && !newSkaterNumber.val()));
		if (!newSkaterButton.button("option", "disabled") && (13 == event.which)) // Enter
			newSkaterButton.click();
	});
	newSkaterFlags.append($("<option>").attr("value", "").text("Skater"));
	newSkaterFlags.append($("<option>").attr("value", "C").text("Captain"));
	newSkaterFlags.append($("<option>").attr("value", "AC").text("Alt Captain"));
	newSkaterFlags.append($("<option>").attr("value", "BC").text("Bench Alt Captain"));
	newSkaterFlags.append($("<option>").attr("value", "ALT").text("Not Skating"));
	var pasteHandler = function(e){
		var text = e.originalEvent.clipboardData.getData("text");
		var lines = text.split("\n");
		if (lines.length <= 1) {
			// Not pasting in many values, so paste as usual.
			return true;
		}

		// Treat as a tab-seperated roster.
		var knownNumbers = {};
		team.find('Skater Number').map( function(_, n) {
			n = $(n)
				knownNumbers[n.text()] = n.parent().attr("Id");
		});

		for (var i = 0; i < lines.length; i++) {
			var cols = lines[i].split("\t");
			if (cols.length < 2) {
				continue;
			}
			var number = $.trim(cols[0]);
			if (number == "") {
				continue;
			}
			var name = $.trim(cols[1]);
			// Assume same number means same skater.
			var id = knownNumbers[number];
			addSkater(number, name, "", id);
		}
		return false;
	}
	newSkaterNumber.bind("paste", pasteHandler);
	newSkaterName.bind("paste", pasteHandler);

	var updateSkaterCount = function() {
		var count = 0;
		skatersTable.find("tr.Skater td.Flags select").each(function(_, f) {
			if (f.value != "BC" && f.value != "ALT") {
				count++;
			}
		});
		skatersTable.find("#skaterCount").text("(" + count + " skating)");
	};
	updateSkaterCount();

	var handleTeamUpdate = function(k, v) {
		if (k.Skater != null) {
			// For a current team, could be a penalty.
			if (k.parts.length != 4) return;
			var skaterRow = skatersTable.find("tr[skaterid='"+k.Skater+"']");
			if (v == null) {
				skaterRow.remove();
				if (!skatersTable.find("tr[skaterid]").length) {
					skatersTable.children("tbody").addClass("Empty");
				}
				updateSkaterCount();
				return;
			}
			var prefix = getPrefix() + ".Skater("+k.Skater+")";

			if (skaterRow.length == 0) {
				skatersTable.removeClass("Empty");
				skaterRow = $("<tr class='Skater'>").attr("skaterid", k.Skater)
					.append("<td class='Number'>")
					.append("<td class='Name'>")
					.append("<td class='Flags'>")
					.append("<td class='Remove'>");
				var numberInput = $("<input type='text' size='10'>")
					.appendTo(skaterRow.children("td.Number"));
				var nameInput = $("<input type='text' size='30'>")
					.appendTo(skaterRow.children("td.Name"));
				nameInput.change(function() {
					WS.Set(prefix + ".Name", nameInput.val());
				});
				$("<button>").text("Remove").addClass("RemoveSkater").button()
					.click(function() { createTeamsSkaterRemoveDialog(selectTeam.val(), prefix); })
					.appendTo(skaterRow.children("td.Remove"));
				numberInput.change(function() {
					WS.Set(prefix + ".Number", numberInput.val());
					skaterRow.attr("skaternum", WS.state[prefix + ".Number"]);
				});
				var skaterFlags = $("<select>").appendTo(skaterRow.children("td.Flags"));
				skaterFlags.append($("<option>").attr("value", "").text("Skater"));
				skaterFlags.append($("<option>").attr("value", "C").text("Captain"));
				skaterFlags.append($("<option>").attr("value", "AC").text("Alt Captain"));
				skaterFlags.append($("<option>").attr("value", "BC").text("Bench Alt Captain"));
				skaterFlags.append($("<option>").attr("value", "ALT").text("Not Skating"));
				skaterFlags.change(function() {
					WS.Set(prefix + ".Flags", skaterFlags.val());
				});
				_windowFunctions.appendAlphaSortedByAttr(skatersTable.children("tbody"), skaterRow, "skaternum");
			}

			skaterRow.children("td." + k.field).children().val(v);
			if (k.field == "Flags") {
				updateSkaterCount();
			} else if (k.field == "Number") {
				skaterRow.attr("skaternum", v);
				_windowFunctions.appendAlphaSortedByAttr(skatersTable.children("tbody"), skaterRow, "skaternum");
			}
		} else { // Team update.
			// For a current team, could be a Position.
			if (k.parts.length != 3) return;
			switch (k.field) {
				case "Logo":
					logoSelect.val(v.substring(v.lastIndexOf("/") + 1));
					break;
				case "Name":
					teamName.val(v);
					break;
				case "AlternateName":
					if (v == null) {
						alternameNameDialog.removeFunc(k.AlternateName);
						return;
					}
					alternameNameDialog.updateFunc(k.AlternateName, v);
					break;
				case "Color":
					var colorId = k.Color.substring(0, k.Color.lastIndexOf("_"));
					if (v == null) {
						colorsDialog.removeFunc(colorId);
						return;
					}
					colorsDialog.addFunc(colorId);
					colorsDialog.updateFunc(colorId, k.Color.substring(k.Color.lastIndexOf("_")+1), v);
					break;
			}
		}
	}

	selectTeam.change(function(event) {
		var teamId = selectTeam.val();
		$("#Teams tr.Selection").toggleClass("NoneSelected", teamId == "");
		teamTable.toggleClass("Hide", teamId == "");
		removeTeam.button("option", "disabled", isCurrentTeam() || teamId == "");
		skatersTable.addClass("Empty");
		skatersTable.children("tbody").empty();
		alternameNameDialog.attr("prefix", getPrefix());
		alternameNameDialog.find("tbody").empty();
		colorsDialog.attr("prefix", getPrefix());
		colorsDialog.find("tbody").empty();
		for (var key in WS.state) {
			var prefix = getPrefix();
			if (key.startsWith(prefix)) {
				handleTeamUpdate(WS._enrichProp(key), WS.state[key]);
			}
		}
	});
	selectTeam.change();
}

function createAlternateNamesDialog() {
	var dialog = $("<div>").addClass("AlternateNamesDialog");

	$("<a>").text("Type:").appendTo(dialog);
	var newIdInput = $("<input type='text'>").appendTo(dialog);
	$("<a>").text("Name:").appendTo(dialog);
	var newNameInput = $("<input type='text'>").appendTo(dialog);

	var newFunc = function() {
		var newId = newIdInput.val();
		var newName = newNameInput.val();
		WS.Set(dialog.attr("prefix") + ".AlternateName("+newId+")", newName);
		newNameInput.val("");
		newIdInput.val("").focus();
	};

	newNameInput.keypress(function(event) {
		if (event.which == 13) // Enter
			newFunc();
	});
	$("<button>").button({ label: "Add" }).click(newFunc)
		.appendTo(dialog);

	var table = $("<table>").appendTo(dialog);
	var thead = $("<thead>").appendTo(table);
	$("<tr>")
		.append("<th class='X'>X</th>")
		.append("<th class='Id'>Id</th>")
		.append("<th class='Name'>Name</th>")
		.appendTo(thead);
	var tbody = $("<tbody>").appendTo(table);

	dialog.updateFunc = function(id, v) {
		var tr = tbody.find("tr#" + id);
		if (tr.length == 0) {
			var prefix = dialog.attr("prefix");
			tr = $("<tr>").attr("id", id)
				.append("<td class='X'>")
				.append("<td class='Id'>")
				.append("<td class='Name'>");
			$("<button>").button({ label: "X" })
				.click(function() { WS.Set(prefix + ".AlternateName("+id+")", null); })
				.appendTo(tr.children("td.X"));
			$("<input type='text' size='20'>")
				.bind("input", function(e) { WS.Set(prefix + ".AlternateName("+id+")", e.target.value); })
				.appendTo(tr.children("td.Name"));
			_windowFunctions.appendAlphaSortedByAttr(tbody, tr, "id");
		}
		tr.children("td.Id").text(id);
		tr.find("td.Name input").val(v);
	}
	dialog.removeFunc = function(id) {
		tbody.children("tr#" + id).remove();
	}

	newIdInput.autocomplete({
		minLength: 0,
		source: [
		{ label: "operator (Operator Controls)", value: "operator" },
		{ label: "overlay (Video Overlay)", value: "overlay" },
		{ label: "scoreboard (Scoreboard Display)", value: "scoreboard" },
		{ label: "twitter (Twitter)", value: "twitter" }
		]
	}).focus(function() { $(this).autocomplete("search", ""); });

	dialog.dialog({
		title: "Alternate Names",
		modal: true,
		width: 700,
		autoOpen: false,
		buttons: { Close: function() { $(this).dialog("close"); } }
	});

	return dialog;

}

function createColorsDialog() {
	var dialog = $("<div>").addClass("ColorsDialog");

	$("<a>").text("Type:").appendTo(dialog);
	var newIdInput = $("<input type='text' size='10'>").appendTo(dialog);

	var newFunc = function() {
		var newId = newIdInput.val();
		var prefix = dialog.attr("prefix");

		WS.Set(prefix + ".Color(" + newId + "_fg)", "");
		WS.Set(prefix + ".Color(" + newId + "_bg)", "");
		WS.Set(prefix + ".Color(" + newId + "_glow)", "");

		newIdInput.val("").focus();
	};

	$("<button>").button({ label: "Add" }).click(newFunc)
		.appendTo(dialog);

	var table = $("<table>").appendTo(dialog);
	var thead = $("<thead>").appendTo(table);
	$("<tr>")
		.append("<th class='X'></th>")
		.append("<th class='Id'>Id</th>")
		.append("<th class='fg'>Foreground</th>")
		.append("<th class='bg'>Background</th>")
		.append("<th class='glow'>Glow/Halo</th>")
		.appendTo(thead);
	var tbody = $("<tbody>").appendTo(table);

	dialog.addFunc = function(colorId) {
		var tr = tbody.children("[id='" + colorId + "']");
		if (tr.length == 0 && colorId != "") {
			 tr = $("<tr>").attr("id", colorId)
				.append("<td class='X'>")
				.append("<td class='Id'>")
				.append("<td class='fg'>")
				.append("<td class='bg'>")
				.append("<td class='glow'>");
			tr.children("td.Id").text(colorId);
			$("<input type='color' cleared='true' value='#666666' suffix='fg'>")
				.appendTo(tr.children("td.fg"));
			$("<input type='color' cleared='true' value='#666666' suffix='bg'>")
				.appendTo(tr.children("td.bg"));
			$("<input type='color' cleared='true' value='#666666' suffix='glow'>")
				.appendTo(tr.children("td.glow"));
			tr.find("input").bind("input", function(e) {
				var prefix = dialog.attr("prefix");
				WS.Set(prefix + ".Color(" + colorId + "_"+ $(e.target).attr("suffix") + ")", e.target.value);
			});
			tr.find("input").after($("<button class='ClearPrev'>X</button>").click(function(e) {
				var prefix = dialog.attr("prefix");
				WS.Set(prefix + ".Color(" + colorId + "_" + $(e.target).prev().attr("suffix") + ")", "");
			}));

			$("<button>").button({ label: "X" })
				.click(function() {
					var prefix = dialog.attr("prefix");
					WS.Set(prefix + ".Color(" + colorId + "_fg)", null);
					WS.Set(prefix + ".Color(" + colorId + "_bg)", null);
					WS.Set(prefix + ".Color(" + colorId + "_glow)", null);
				})
				.appendTo(tr.children("td.X"));

			_windowFunctions.appendAlphaSortedByAttr(tbody, tr, "id");
		}
	};
	dialog.removeFunc = function(colorId) {
		tbody.children("tr[id='" + colorId + "']").remove();
	};
	dialog.updateFunc = function(colorId, suffix, v) {
		if (v == null || v == "") {
			tbody.children("tr[id='" + colorId + "']").children("td." + suffix).children("input").attr("cleared", "true").val("#666666");
		} else {
			tbody.children("tr[id='" + colorId + "']").children("td." + suffix).children("input").attr("cleared", "false").val(v);
		}
	};

	newIdInput.autocomplete({
		minLength: 0,
		source: [
			{ label: "operator (Operator Colors)", value: "operator" },
			{ label: "overlay (Video Overlay Colors)", value: "overlay" },
			{ label: "scoreboard (Scoreboard Colors)", value: "scoreboard" },
			{ label: "scoreboard_dots (Scoreboard Dot Colors)", value: "scoreboard_dots" },
		]
	}).focus(function() { $(this).autocomplete("search", ""); });

	dialog.dialog({
		title: "Team Colors",
		modal: true,
		width: 800,
		autoOpen: false,
		buttons: { Close: function() { $(this).dialog("close"); } }
	});

	return dialog;
}

function createTeamsRemoveDialog(teamId) {
	var dialog = $("<div>").addClass("TeamsRemoveDialog");

	$("<a>").addClass("Remove").text("Remove Entire Team: ").appendTo(dialog);
	$("<a>").addClass("Target").text(teamId).appendTo(dialog);
	$("<br>").appendTo(dialog);

	$("<hr>").appendTo(dialog);
	$("<a>").addClass("AreYouSure").text("Are you sure?").appendTo(dialog);
	$("<br>").appendTo(dialog);

	$("<button>").addClass("No").text("No, keep this team.").appendTo(dialog).click(function() {
		dialog.dialog("close");
	}).button();
	$("<button>").addClass("Yes").text("Yes, remove!").appendTo(dialog).click(function() {
		WS.Set("ScoreBoard.PreparedTeam("+teamId+")", null);
		dialog.dialog("close");
	}).button();

	dialog.dialog({
		title: "Remove Team",
		modal: true,
		width: 700,
		close: function() { $(this).dialog("destroy").remove(); }
	});
}

function createTeamsSkaterRemoveDialog(teamId, prefix) {
	var dialog = $("<div>").addClass("TeamsRemoveDialog");

	$("<a>").addClass("Title").text("Team: "+teamId).appendTo(dialog);
	$("<br>").appendTo(dialog);

	var skaterName = WS.state[prefix + ".Name"];
	var skaterNumber = WS.state[prefix + ".Number"];
	$("<a>").addClass("Remove").text("Remove Skater: ").appendTo(dialog);
	$("<a>").addClass("Target").text(skaterNumber).appendTo(dialog);
	$("<br>").appendTo(dialog);
	if (skaterName) {
		$("<a>").addClass("Name").text("(Skater Name: "+skaterName+")").appendTo(dialog);
		$("<br>").appendTo(dialog);
	}

	$("<hr>").appendTo(dialog);
	$("<a>").addClass("AreYouSure").text("Are you sure?").appendTo(dialog);
	$("<br>").appendTo(dialog);

	$("<button>").addClass("No").text("No, keep this skater.").appendTo(dialog).click(function() {
		dialog.dialog("close");
	}).button();
	$("<button>").addClass("Yes").text("Yes, remove!").appendTo(dialog).click(function() {
		WS.Set(prefix, null);
		dialog.dialog("close");
	}).button();

	dialog.dialog({
		title: "Remove Skater",
		modal: true,
		width: 700,
		close: function() { $(this).dialog("destroy").remove(); }
	});
}


////////////////
// Save/Load tab
////////////////

function createSaveLoadTab() {
	$("<table>").attr("id", "SaveLoad")
		.appendTo(createTab("Save/Load", "SaveLoadTab"))
		.data("loadContentFunction", createSaveLoadContent);
}

function createSaveLoadContent() {
	// Download table
	var sbDownloadTable = $("<table>").addClass("Download")
		.appendTo($("<td>").appendTo($("<tr>").appendTo("#SaveLoad")));
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
		.appendTo($("<td>").appendTo($("<tr>").appendTo("#SaveLoad")));
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

//# sourceURL=controls\operator.js
