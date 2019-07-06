
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */


$.fx.interval = 33;


$(function() {
	createTeamTimeTab(createTab("Team/Time", "TeamTimeTab"));
	createRulesetsTab(createTab('Rulesets', 'RulesetsTab'))
	createScoreBoardSettingsTab(createTab("Settings", "ScoreBoardSettingsTab"));
	createTeamsTab(createTab("Teams", "TeamsTab"));
	createDataManagementTab(createTab("Up-/Download", "DataManagementTab"));
	// Only connect after any registrations from the above are in place.
	// This avoids repeating work on the initial load.
	WS.AutoRegister();
	WS.Connect();

	$("#tabsDiv").tabs();

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
	setClockControls(isTrue(WS.state[prefix+"StartStopButtons)"]));
	setReplaceButton(isTrue(WS.state[prefix+"ReplaceButton)"]));
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
	WS.Register("ScoreBoard.Settings.Setting(ScoreBoard.*)", function(k, v) {
		setOperatorSettings(_windowFunctions.getParam("operator"));
	});
}

function login(name) {
	$("#operatorId").text(name);
	if (window.history.replaceState)
		window.history.replaceState(null, "", "?operator="+$("#operatorId").text());
	_crgKeyControls.setupKeyControls(name);
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

function createTab(title, tabId) {
	if (typeof title == "string") title = $("<a>").html(title);
	$("<li>").append(title.attr("href", "#"+tabId)).appendTo("#tabsDiv>ul");
	return $("<div>").attr("id", tabId).addClass("TabContent")
		.appendTo("#tabsDiv");
}

//# sourceURL=nso\sbo.js
