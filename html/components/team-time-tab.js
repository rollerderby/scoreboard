function createTeamTimeTab(tab) {
	var table = $("<table>").attr("id", "TeamTime")
	.appendTo(tab);

	$("<tr><td/></tr>").appendTo(table).children("td")
		.append(createMetaControlTable());
	$("<tr><td/></tr>").appendTo(table).children("td")
		.append(createJamControlTable());
	$("<tr><td/></tr>").appendTo(table).children("td")
		.append(createTeamTable());
	$("<tr><td/></tr>").appendTo(table).children("td")
		.append(createTimeTable());
	table.children("tbody").children("tr").children("td").children("table").addClass("TabTable");

	var sk1 = $('<div>').addClass('SKSheet').appendTo(tab);
	var sk2 = $('<div>').addClass('SKSheet').appendTo(tab);
	$('<div>').attr('id', 'TripEditor').appendTo(tab);
	$('<div>').attr('id', 'skaterSelector').appendTo(tab);
	prepareSkSheetTable(sk1, 1, 'operator');
	prepareSkSheetTable(sk2, 2, 'operator');
	prepareTripEditor();
	prepareSkaterSelector();

	initialLogin();
}

function setClockControls(value) {
	$("#ShowClockControlsButton").prop("checked", value);
	$("label.ShowClockControlsButton").toggleClass("ui-state-active", value);
	$("#TeamTime").find("tr.Control").toggleClass("Show", value);
}

function setTabBar(value) {
	$("#ShowTabBarButton").prop("checked", value);
	$("label.ShowTabBarButton").toggleClass("ui-state-active", value);
	$("#tabBar").toggle(value);
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
	var buttonsTd = _crgUtils.createRowTable(1)
		.appendTo(table.find(">tbody>tr:eq(0)").addClass("Buttons").children("td"))
		.find("tr>td");
	var helpTd = _crgUtils.createRowTable(1)
		.appendTo(table.find(">tbody>tr:eq(1)").addClass("Help Hidden").children("td"))
		.find("tr>td");
	var periodEndTd = _crgUtils.createRowTable(1)
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
				WS.Set("ScoreBoard.Settings.Setting(ScoreBoard.Operator__"+operator+".ReplaceButton)", value);
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
				WS.Set("ScoreBoard.Settings.Setting(ScoreBoard.Operator__"+operator+".StartStopButtons)", value);
			}
		});

	$("<label>").addClass("ShowTabBarButton").text("Show Tab Bar").attr("for", "ShowTabBarButton")
		.appendTo(buttonsTd);
	$("<input type='checkbox'>").attr("id", "ShowTabBarButton")
		.appendTo(buttonsTd)
		.button()
		.click(function() {
			var value = this.checked;
			setTabBar(value);
			var operator = $("#operatorId").text();
			if (operator) {
				WS.Set("ScoreBoard.Settings.Setting(ScoreBoard.Operator__"+operator+".TabBar)", value);
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
	WS.Register(["ScoreBoard.Clock(Period).Time",
			"ScoreBoard.Clock(Period).Number",
			"ScoreBoard.Rulesets.CurrentRule(Period.Number)"], function(k, v) {
				var under30 = (Number(WS.state["ScoreBoard.Clock(Period).Time"]) < 30000);
				var last = (WS.state["ScoreBoard.Rulesets.CurrentRule(Period.Number)"] == WS.state["ScoreBoard.Clock(Period).Number"]);
				doPulseFlag = (under30 && last);
			});

	var confirmedButton = toggleButton("ScoreBoard.OfficialScore", "Official Score", "Unofficial Score");
	confirmedButton.appendTo(periodEndTd);
	var periodEndTimeoutDialog = createPeriodEndTimeoutDialog(periodEndTd);
	$("<button>").addClass("PeriodEndTimeout").text("Timeout before Period End")
		.appendTo(periodEndTd)
		.button()
		.click(function() { periodEndTimeoutDialog.dialog("open"); });
	WS.Register("ScoreBoard.Rulesets.CurrentRule(Period.Number)");
	$("<button>").text("Overtime")
		.appendTo(periodEndTd)
		.button()
		.click(function() {createOvertimeDialog(WS.state["ScoreBoard.Rulesets.CurrentRule(Period.Number)"])});

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
			IntermissionClock = "" + (StartTime - now);
		}
		var game = {
			Team1: adhocGame.find("select.Team1").val(),
			Team2: adhocGame.find("select.Team2").val(),
			Ruleset: adhocGame.find("select.Ruleset").val(),
			IntermissionClock: IntermissionClock
		};
		WS.Command("StartNewGame", game);
		dialog.dialog("close");
	};

	$("<span>").addClass("header").append("Start an adhoc game").appendTo(adhocGame);
	$("<div>")
		.append($("<span>").append("Team 1: "))
		.append($("<select>").addClass("Team1").append("<option value=''>No Team Selected</option"))
		.appendTo(adhocGame);
	$("<div>")
		.append($("<span>").append("Team 2: "))
		.append($("<select>").addClass("Team2").append("<option value=''>No Team Selected</option"))
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
	};

	WS.Register("ScoreBoard.PreparedTeam(*).Id", function(k, v) {
		if (v == null) {
			adhocGame.find("option[value='"+k.PreparedTeam+"']").remove();
			return;
		}
		if (adhocGame.find("option[value='"+k.PreparedTeam+"']").length == 0) {
			var option = $("<option>").attr("value", v).text(v);
			_windowFunctions.appendAlphaSortedByAttr(adhocGame.find("select.Team1"), option, 'value', 1);
			_windowFunctions.appendAlphaSortedByAttr(adhocGame.find("select.Team2"), option.clone(), 'value', 1);
		}
	});
	adhocGame.find("select.Team1, select.Team2").change(function(e) {
		var t1 = adhocGame.find("select.Team1 option:selected");
		var t2 = adhocGame.find("select.Team2 option:selected");
		if (t1.val() != "" && t2.val() != "") {
			adhocGame.find("button.StartGame").button("option", "disabled", false);
		} else {
			adhocGame.find("button.StartGame").button("option", "disabled", true);
		}
	});


	WS.Register("ScoreBoard.Rulesets.CurrentRulesetId", function(k, v) {
		adhocGame.find("select.Ruleset").val(v);
	});
	WS.Register("ScoreBoard.Rulesets.Ruleset(*).Name", function(k, v) {
		var select = adhocGame.find("select.Ruleset");
		select.children("option[value='"+k.Ruleset+"']").remove();
		if (v == null) {
			return;
		}
		var option = $("<option>").attr("value", k.Ruleset).attr("name", v).text(v);
		_windowFunctions.appendAlphaSortedByAttr(select, option, 'name');
		select.val(WS.state["ScoreBoard.Rulesets.CurrentRulesetId"]);
	});

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
	$("<button>").addClass("Cancel").text("Cancel").appendTo(waitDiv).button()
		var applying = false;
	WS.Register("ScoreBoard.Clock(Period).Time", function(k, v) {
		checkTimeFunction(v);
	});
	WS.Register("ScoreBoard.Clock(Period).Running");
	var checkTimeFunction = function(v) {
		if (!applying) return;
		var currentSecs = Number(_timeConversions.msToSeconds(v));
		var targetSecs = Number(waitDiv.find("span.TargetSeconds").text());
		if (currentSecs > targetSecs) {
			return;
		}
		if (currentSecs < targetSecs) {
			WS.Set("ScoreBoard.Clock(Period).Time", _timeConversions.secondsToMs(targetSecs));
		}
		WS.Set("ScoreBoard.Timeout", true);
		applying = false;
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
		applying = true;
		checkTimeFunction(WS.state["ScoreBoard.Clock(Period).Time"]);
	});
	waitDiv.find("button.Cancel").click(function() {
		td.find("button.PeriodEndTimeout").button("option", "label", "Timeout before Period End");
		applying = false;
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

function createOvertimeDialog(numPeriods) {
	var dialog = $("<div>");
	$("<span>").text("Note: Overtime can only be started at the end of Period ").appendTo(dialog);
	$("<span>").text(numPeriods).appendTo(dialog);
	$("<button>").addClass("StartOvertime").text("Start Overtime Lineup clock").appendTo(dialog)
		.click(function() {
			WS.Set("ScoreBoard.StartOvertime", true);
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
	var replaceInfoTr = _crgUtils.createRowTable(1).addClass("ReplaceInfo Hidden").appendTo(table.find("td"));
	var controlsTr = _crgUtils.createRowTable(4,1).appendTo(table.find("td")).find("tr:eq(0)").addClass("Controls");

	var replaceInfoText = $("<span>").html("Replace &quot;<span id=\"replacedLabel\"></span>&quot; with").appendTo(replaceInfoTr);
	WS.Register("ScoreBoard.Settings.Setting(ScoreBoard.Button.ReplacedLabel)", function(k, v) {
		$("#replacedLabel").text(v)
	});

	var jamStartButton = $("<button>")
		.html("<span class=\"Label\">Start Jam</span>")
		.attr("id", "StartJam").addClass("KeyControl").button().click(function() {
			WS.Set("ScoreBoard.StartJam", true);
		});
	WS.Register("ScoreBoard.Settings.Setting(ScoreBoard.Button.StartLabel)", function(k, v) {
		jamStartButton.find("span.Label").text(v);
	});
	jamStartButton.appendTo(controlsTr.children("td:eq(0)"));

	var jamStopButton = $("<button>")
		.html("<span class=\"Label\">Stop Jam</span>")
		.attr("id", "StopJam").addClass("KeyControl").button().click(function() {
			WS.Set("ScoreBoard.StopJam", true);
		});
	WS.Register("ScoreBoard.Settings.Setting(ScoreBoard.Button.StopLabel)", function(k, v) {
		jamStopButton.find("span.Label").text(v);
	});
	jamStopButton.appendTo(controlsTr.children("td:eq(1)"));


	var timeoutButton = $("<button>")
		.html("<span class=\"Label\">Timeout</span>")
		.attr("id", "Timeout").addClass("KeyControl").button().click(function() {
			WS.Set("ScoreBoard.Timeout", true);
		});
	WS.Register("ScoreBoard.Settings.Setting(ScoreBoard.Button.TimeoutLabel)", function(k, v) {
		timeoutButton.find("span.Label").text(v);
	});
	timeoutButton.appendTo(controlsTr.children("td:eq(2)"));

	var undoButton = $("<button>")
		.html("<span class=\"Label\">Undo</span>")
		.attr("id", "ClockUndo").addClass("KeyControl").button().click(function() {
			WS.Set("ScoreBoard.ClockUndo", true);
		});
	WS.Register("ScoreBoard.Settings.Setting(ScoreBoard.Button.UndoLabel)", function(k, v) {
		undoButton.find("span.Label").text(v);
	});
	var replaceButton = $("<button>")
		.html("<span class=\"Label\">Replace</span>")
		.attr("id", "ClockReplace").addClass("KeyControl Hidden KeyInactive").button().click(function() {
			WS.Set("ScoreBoard.ClockReplace", true);
		});
	WS.Register("ScoreBoard.Settings.Setting(ScoreBoard.Button.UndoLabel)", function(k, v) {
		replaceButton.find("span.Label").text(v);
		if (!replaceButton.hasClass("Hidden")) {
			var rep = (v == "No Action");
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
	var flagsRow = row.clone().appendTo(table);
	var jammerRow = row.clone().addClass("Jammer").appendTo(table);
	var pivotRow = row.clone().addClass("Pivot").appendTo(table);

	$.each( [ "1", "2" ], function() {
		var team = String(this);
		var prefix = "ScoreBoard.Team("+team+")";
		var first = (team == "1");

		var nameTr = _crgUtils.createRowTable(2).appendTo($("<td>").appendTo(nameRow)).find("tr");
		var scoreTr = _crgUtils.createRowTable(3).appendTo($("<td>").appendTo(scoreRow)).find("tr");
		var speedScoreTr = _crgUtils.createRowTable(4).appendTo($("<td>").appendTo(speedScoreRow)).find("tr");
		var timeoutTr = _crgUtils.createRowTable(6).appendTo($("<td>").appendTo(timeoutRow)).find("tr");
		var flagsTr = _crgUtils.createRowTable(2).appendTo($("<td>").appendTo(flagsRow)).find("tr");
		var jammerTr = _crgUtils.createRowTable(1).appendTo($("<td>").appendTo(jammerRow)).find("tr");
		var pivotTr = _crgUtils.createRowTable(1).appendTo($("<td>").appendTo(pivotRow)).find("tr");

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
		WS.Register(prefix + ".Name", function(k, v) { nameA.text(v); });
		WSControl(prefix + ".Name", nameInput);
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
			if (val == "") {
				WS.Set(prefix + ".AlternateName(operator)", null);
			} else {
				WS.Set(prefix + ".AlternateName(operator)", val);
			}
		});

		WS.Register(prefix + ".AlternateName(operator)", function(k, v) {
			altNameA.text(v || "");
			altNameInput.val(v || "");
			nameA.toggleClass("AlternateName", v != null);
		});

		var names = nameA.add(altNameA);
		WS.Register(prefix + ".Color(*)", function(k, v) {
			v = v || "";
			switch (k.Color) {
				case "operator_fg":
					names.css("color", v);
					break;
				case "operator_bg":
					names.css("background-color", v);
					break;
				case "operator_glow":
					var shadow = "";
					if (v) {
						shadow = '0 0 0.2em ' + v;
						shadow = shadow + ', ' + shadow + ', ' + shadow;
					}
					names.css("text-shadow", shadow);
					break;
			}
		});

		var logoTd = nameTr.children("td:eq("+(first?0:1)+")").addClass("Logo");
		var logoNone = $("<a>").html("No Logo").addClass("NoLogo").appendTo(logoTd);
		var logoSelect = mediaSelect(prefix + ".Logo", "images", "teamlogo", "Logo")
			.appendTo(logoTd);
		var logoImg = $("<img>").appendTo(logoTd);

		var logoShowSelect = function(show) {
			var showImg = !!(WS.state[prefix + ".Logo"]);
			logoImg.toggle(!show && showImg);
			logoNone.toggle(!show && !showImg);
			logoSelect.toggle(show);
			if (show) {
				logoSelect.focus();
			}
		};
		WS.Register(prefix + ".Logo", function(k, v) {
			logoShowSelect(false);
			logoImg.attr("src", v);
		});
		logoSelect
			.blur(function() { logoShowSelect(false); })
			.keyup(function(event) { if (event.which == 27 /* ESC */) $(this).blur(); });

		logoTd.click(function() { if (!logoSelect.is(":visible")) logoShowSelect(true); });

		var scoreTd = scoreTr.children("td:eq("+(first?"0":"2")+")").addClass("Down");
		$("<button>").text("Score -1")
			.attr("id", "Team"+team+"ScoreDown").addClass("KeyControl BigButton").button()
			.click(function(){WS.Set(prefix + ".TripScore", -1, "change");})
			.appendTo(scoreTd);
		$("<br />").appendTo(scoreTd);
		$("<button>").text("Trip -1").val("true")
			.attr("id", "Team"+team+"RemoveTrip").addClass("KeyControl TripButton").button()
			.click(function(){WS.Set(prefix + ".RemoveTrip", true);})
			.appendTo(scoreTd);

		var scoreSubTr = _crgUtils.createRowTable(3).appendTo(scoreTr.children("td:eq(1)")).find("tr");
		var score = $("<a/>").appendTo(scoreSubTr.children("td:eq(1)").addClass("Score"));
		WS.Register(prefix +".Score", function(k, v) { score.text(v); });

		var scoreTd = scoreTr.children("td:eq("+(first?"2":"0")+")").addClass("Up");

		$("<button>").text("Score +1")
			.attr("id", "Team"+team+"ScoreUp").addClass("KeyControl BigButton").button()
			.click(function(){WS.Set(prefix + ".TripScore", +1, "change");})
			.appendTo(scoreTd);
		$("<br />").appendTo(scoreTd);
		$("<button>").text("Trip +1")
			.attr("id", "Team"+team+"AddTrip").addClass("KeyControl TripButton").button()
			.click(function(){WS.Set(prefix + ".AddTrip", true);})
			.appendTo(scoreTd);

		for (var i = 1; i <= 4; i++) {
			var pos = (i - 1);
			if (!first) {
				pos = 3 - pos;
			}
			(function(i) {
				$("<button>").text(i)
					.attr("id", "Team"+team+"TripScore"+i).addClass("KeyControl").button()
					.click(function(){ WS.Set(prefix + ".TripScore", i); })
					.appendTo(speedScoreTr.find("td:eq("+pos+")"));
			}(i));
		}


		// Note instantaneous score change is always towards the center.	Jam score total is on the outside.
		var scoreChange = $("<a>").css({ opacity: "0" }).appendTo(scoreSubTr.children("td:eq("+(first?"2":"0")+")")).addClass("TripScore");
		var jamScore = $("<a>").appendTo(scoreSubTr.children("td:eq("+(first?"0":"2")+")")).addClass("JamScore");

		var scoreChangeTimeout;
		WS.Register(prefix + ".TripScore", function(k, v) {
			var c = (v>0 ? "#080" : "#008");
			scoreChange.stop(true).text(v).last().css({ opacity: "1", color: c });
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
		WS.Register(prefix + ".JamScore", function(k, v) {
			var c = (v>0 ? "#080" : "#008");
			jamScore.stop(true).text(v).last().css({ opacity: "1", color: c });
			if (jamScoreTimeout)
				clearTimeout(jamScoreTimeout);
			jamScoreTimeout = setTimeout(function() {
				jamScore.last()
					.animate({ color: "#008" }, 2000)
			}, 2000);
		});

		var timeoutButton = $("<button>").text("Team TO")
			.attr("id", "Team"+team+"Timeout").addClass("KeyControl").button()
			.click(function() { WS.Set(prefix + ".Timeout", true);});
		timeoutButton.appendTo(timeoutTr.children("td:eq("+(first?"0":"5")+")").addClass("Timeout"));
		var timeoutCount = $("<a/>").click(function() { timeoutDialog.dialog("open"); })
			.appendTo(timeoutTr.children("td:eq("+(first?"1":"4")+")").addClass("Timeouts"));
		WS.Register(prefix + ".Timeouts", function(k, v) { timeoutCount.text(v);});

		var reviewButton = $("<button>").text("Off Review")
			.attr("id", "Team"+team+"OfficialReview").addClass("KeyControl").button()
			.click(function() { WS.Set(prefix + ".OfficialReview", true);});
		reviewButton.appendTo(timeoutTr.children("td:eq("+(first?"2":"3")+")").addClass("OfficialReview"));
		var officialReviews = $("<a/>").click(function() { timeoutDialog.dialog("open"); })
			.appendTo(timeoutTr.children("td:eq("+(first?"3":"2")+")").addClass("OfficialReviews"));
		WS.Register(prefix + ".OfficialReviews", function(k, v) { officialReviews.text(v);});

		WS.Register(["ScoreBoard.TimeoutOwner", "ScoreBoard.OfficialReview"], function(k, v) {
			var to = WS.state["ScoreBoard.TimeoutOwner"] == team;
			var or = isTrue(WS.state["ScoreBoard.OfficialReview"]);
			timeoutButton.toggleClass("Active", to && !or);
			reviewButton.toggleClass("Active", to && or);
		});

		var retainedORButton = WSActiveButton(prefix + ".RetainedOfficialReview", $("<button>")).text("Retained")
			.attr("id", "Team"+team+"RetainedOfficialReview").addClass("KeyControl").button();
		retainedORButton.appendTo(timeoutTr.children("td:eq("+(first?"4":"1")+")").addClass("RetainedOfficialReview"));

		if (first) {
			var otoButton = $("<button>").text("Official TO")
				.attr("id", "OfficialTimeout").addClass("KeyControl").button()
				.click(function() { WS.Set("ScoreBoard.OfficialTimeout", true);} );
			WS.Register("ScoreBoard.TimeoutOwner", function(k, v) {
				otoButton.toggleClass("Active", v == "O");
			});
			otoButton.appendTo(timeoutTr.children("td:eq(5)").addClass("OfficialTimeout"));
			otoButton.wrap("<div></div>");
		}

		var leadJammerTd = flagsTr.children("td:eq("+(first?"1":"0")+")").css("direction", "ltr");
		WSActiveButton(prefix + ".Lost", $("<button>")).text("Lost")
			.attr("id", "Team"+team+"Lost").addClass("KeyControl").button().appendTo(leadJammerTd);
		WSActiveButton(prefix + ".Lead", $("<button>")).text("Lead")
			.attr("id", "Team"+team+"Lead").addClass("KeyControl").button().appendTo(leadJammerTd);
		WSActiveButton(prefix + ".Calloff", $("<button>")).text("Call")
			.attr("id", "Team"+team+"Call").addClass("KeyControl").button().appendTo(leadJammerTd);
		WSActiveButton(prefix + ".Injury", $("<button>")).text("Inj")
			.attr("id", "Team"+team+"Inj").addClass("KeyControl").button().appendTo(leadJammerTd);
		WSActiveButton(prefix + ".NoInitial", $("<button>")).text("NI")
			.attr("id", "Team"+team+"NI").addClass("KeyControl").button().appendTo(leadJammerTd);

		leadJammerTd.buttonset();

		var starPassTd = flagsTr.children("td:eq("+(first?"0":"1")+")").css("direction", "ltr");
		var starPassButton = WSActiveButton(prefix + ".StarPass", $("<button>")).text("Star Pass")
			.attr("id", "Team"+team+"StarPass").addClass("KeyControl").button().appendTo(starPassTd);
		var noPivotButton = WSActiveButton(prefix + ".NoPivot", $("<button>")).text("No Pivot")
			.attr("id", "Team"+team+"NoPivot").addClass("KeyControl").button().appendTo(starPassTd);
				
		var makeSkaterSelector = function(pos) {
			var container = $('<span class="skaterSelector">')

			var none = $('<button>').text("?").attr("skater", "").button();
			container.append(none).buttonset();
			none.click(function(){WS.Set(prefix + '.Position('+pos+').Skater', "") });

			function setValue(v) {
				container.children().removeClass("Active");
				v = v || "";
				container.children('[skater="'+v+'"]').addClass("Active");
			}
			WS.Register([prefix + '.Skater(*).Number', prefix + '.Skater(*).Role'], function(k, v) {
				container.children('[skater="'+k.Skater+'"]').remove();
				if (v != null && WS.state[prefix + '.Skater('+k.Skater+').Role'] != 'NotInGame') {
					var number = WS.state[prefix + '.Skater('+k.Skater+').Number'];
					var button = $('<button>').attr('number', number).attr('skater', k.Skater).text(number)
							.click(function() {
						WS.Set(prefix + '.Position('+pos+').Skater', k.Skater);
					}).button();
					_windowFunctions.appendAlphaSortedByAttr(container, button, 'number', 1);
				}
				setValue(WS.state[prefix + '.Position('+pos+').Skater']);
			});
			WS.Register(prefix + '.Position('+pos+').Skater', function(k, v) {
				setValue(v);
			});
			return container;
		};

		var jammerSelectTd = jammerTr.children("td");
		$('<span>').text('Jammer:').appendTo(jammerSelectTd);
		makeSkaterSelector("Jammer").appendTo(jammerSelectTd);

		WSActiveButton(prefix + ".Position(Jammer).PenaltyBox", $("<button>")).text("Box")
			.attr("id", "Team"+team+"JammerBox").addClass("KeyControl Box").button().appendTo(jammerSelectTd);

		var pivotSelectTd = pivotTr.children("td");
		$('<span>').text('Piv/4th Bl:').appendTo(pivotSelectTd);
		makeSkaterSelector("Pivot").appendTo(pivotSelectTd);

		WSActiveButton(prefix + ".Position(Pivot).PenaltyBox", $("<button>")).text("Box")
			.attr("id", "Team"+team+"PivotBox").addClass("KeyControl Box").button().appendTo(pivotSelectTd);
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
		var prefix = "ScoreBoard.Clock("+clock+")";

		var nameTd = $("<td>").appendTo(nameRow);
		var numberTr = _crgUtils.createRowTable(3).appendTo($("<td>").appendTo(numberRow)).find("tr");
		var controlTr = _crgUtils.createRowTable(2).appendTo($("<td>").appendTo(controlRow)).find("tr");
		var timeTr = _crgUtils.createRowTable(3).appendTo($("<td>").appendTo(timeRow)).find("tr");

		var name = $("<a>").appendTo(nameTd.addClass("Name"));
		WS.Register(prefix + ".Name", function(k ,v) {
			name.text(v);
		});
		if (clock == "Period" || clock == "Jam") {
			var it = $("<a>").appendTo(nameTd).addClass("InvertedTime");
			WS.Register(prefix +".InvertedTime", function(k, v) {
				it.text(_timeConversions.msToMinSec(v));
			});
			WS.Register(prefix +".Direction", function(k, v) {
				it.toggleClass("CountDown", isTrue(v));
				it.toggleClass("CountUp", !isTrue(v));
			});
		}
		WS.Register(prefix + ".Running", function(k, v) {
			nameTd.toggleClass("Running", isTrue(v));
		});
		WS.Register("ScoreBoard.NoMoreJam", function(k, v) {
			nameTd.toggleClass("NoMoreJam", isTrue(v));
		});

		var number = $("<a>").appendTo(numberTr.children("td:eq(1)")
				.addClass("Number").css("width", "20%"));
		WS.Register(prefix + ".Number", function(k, v) {
			number.text(v);
		});
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

		$("<button>").text("Start").val("true")
			.attr("id", "Clock"+clock+"Start").addClass("KeyControl").button()
			.appendTo(controlTr.children("td:eq(0)").addClass("Start"))
			.click(function() { WS.Set(prefix + ".Start", true);});
		$("<button>").text("Stop").val("true")
			.attr("id", "Clock"+clock+"Stop").addClass("KeyControl").button()
			.appendTo(controlTr.children("td:eq(1)").addClass("Stop"))
			.click(function() { WS.Set(prefix + ".Stop", true);});

		$("<button>").text("-1")
			.attr("id", "Clock"+clock+"TimeDown").addClass("KeyControl").button()
			.click(function() { WS.Set(prefix + ".Time", -1000, "change");})
			.appendTo(timeTr.children("td:eq(0)").addClass("Button"));
		var time = $("<a>").appendTo(timeTr.children("td:eq(1)").addClass("Time"));
		WS.Register(prefix +".Time", function(k, v) {
			time.text(_timeConversions.msToMinSec(v));
		});
		var timeDialog = createTimeDialog(clock);
		timeTr.children("td:eq(1)").click(function() { timeDialog.dialog("open"); });
		$("<button>").text("+1")
			.attr("id", "Clock"+clock+"TimeUp").addClass("KeyControl").button()
			.click(function() { WS.Set(prefix + ".Time", +1000, "change");})
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
		var prefix = "ScoreBoard.Period("+k.Period+").Timeout("+id+")";
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
	var prefix = "ScoreBoard.Clock("+clock+")";
	var dialog = $("<div>");
	var table = $("<table>").appendTo(dialog).addClass("TimeDialog");
	var row = $("<tr><td/></tr>");
	row.clone().appendTo(table).addClass("Time");
	row.clone().appendTo(table).addClass("MinimumTime");
	row.clone().appendTo(table).addClass("MaximumTime");
	row.clone().appendTo(table).addClass("Direction");

	$.each( [ "Time", "MinimumTime", "MaximumTime" ], function(_, e) {
		var rowTable = _crgUtils.createRowTable(3).appendTo(table.find("tr."+this+">td"));
		rowTable.find("tr:eq(0)").before("<tr><td colspan='3'/></tr>");

		$("<a>").text(this+": ").addClass("Title")
			.appendTo(rowTable.find("tr:eq(0)>td").addClass("Title"));
		var time = $("<a>").addClass("Time").appendTo(rowTable.find("tr:eq(0)>td"));
		WS.Register(prefix + "." + e, function(k, v) {
			time.text(_timeConversions.msToMinSec(v))
		});
		$("<button>").text("-sec").button()
			.click(function() { WS.Set(prefix + "." + e, -1000, "change");})
			.appendTo(rowTable.find("tr:eq(1)>td:eq(0)"));
		$("<button>").text("+sec").button()
			.click(function() { WS.Set(prefix + "." + e, +1000, "change");})
			.appendTo(rowTable.find("tr:eq(1)>td:eq(2)"));
		var input = $("<input type='text' size='5'>").appendTo(rowTable.find("tr:eq(1)>td:eq(1)"));
		$("<button>").text("Set").addClass("Set").button().appendTo(rowTable.find("tr:eq(1)>td:eq(1)"))
			.click(function() {
				WS.Set(prefix + "." + e, _timeConversions.minSecToMs(input.val()));
			});
	});
	$("<tr><td/><td/><td/></tr>").insertAfter(table.find("tr.Time table tr:eq(0)"));
	$.each( [ "Start", "ResetTime", "Stop" ], function(i, t) {
		$("<button>").text(t).button()
			.click(function() {WS.Set(prefix + "." + t, "true");})
			.appendTo(table.find("tr.Time table tr:eq(1)>td:eq("+i+")"));
	});


	var rowTable = _crgUtils.createRowTable(1,2).appendTo(table.find("tr.Direction>td"));
	toggleButton(prefix + ".Direction", "Counting Down", "Counting Up")
		.appendTo(rowTable.find("tr:eq(1)>td"));

	dialog.dialog({
		autoOpen: false,
		modal: true,
		width: 400,
		buttons: { Close: function() { $(this).dialog("close"); } }
	});

	WS.Register(prefix + ".Name", function(k, v){
		dialog.dialog("option", "title", v + " Clock");
	});

	return dialog;
}
