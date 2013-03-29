
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */


$.fx.interval = 33;

$sb(function() {
  createScoreTimeTab();
  createPoliciesTab();
  createScoreBoardViewTab();
  createTeamsTab();
  createSaveLoadTab();

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

  $("<button>").text("Logout").click(logout).button().css("float", "right").appendTo("#tabBar");
});

// FIXME - this is done after the team/time panel is loaded,
//         as the button setup needs to happen after that panel creates its buttons...
//         really, the keycontrol helper lib needs to have a per-tab interface so
//         each tab can setup its own keycontrol.
function initialLogin() {
  var operator = _windowFunctions.getParam("operator");
  if (operator) {
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
}

function logout() {
  $("#operatorId").text("");
  if (window.history.replaceState)
    window.history.replaceState(null, "", "?");
  _crgKeyControls.destroyKeyControls();
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
  $("<table>").attr("id", "TeamTime")
    .appendTo(createTab("Team/Time", "TeamTimeTab"))
    .data("loadContentFunction", createScoreTimeContent);
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
  $("<a>").text("Key Control Edit mode enabled.  Buttons do not operate in this mode.  Move the mouse over a button, then press a normal key (not ESC, Enter, F1, etc.) to assign.")
    .appendTo(helpTd);

  $("<label>").text("Show UNDO Controls").attr("for", "ShowUndoControlsButton")
    .appendTo(buttonsTd);
  $("<input type='checkbox'>").attr("id", "ShowUndoControlsButton")
    .appendTo(buttonsTd)
    .button()
    .click(function() {
      $("#TeamTime table.JamControl tr.UndoControls").toggleClass("ShowUndo", this.checked);
    });

  var selectJammerSpan = $("<span>").appendTo(buttonsTd);
  var selectByLabel = $("<label>").attr("for", "SelectJammerBy")
    .appendTo(selectJammerSpan);
  var selectByButton = $("<input type='checkbox'>").attr("id", "SelectJammerBy")
    .appendTo(selectJammerSpan).button();
  _crgUtils.bindAndRun(selectByButton, "click", function() {
    $("#TeamTime table.Team td.Jammer")
      .toggleClass("ByNumber", this.checked)
      .toggleClass("ByName", !this.checked);
    $(this).button("option", "label", (this.checked?"Select Jammer by Number":"Select Jammer by Name"));
  });
  var selectSortLabel = $("<label>").attr("for", "SelectJammerSort")
    .appendTo(selectJammerSpan);
  var selectSortButton = $("<input type='checkbox'>").attr("id", "SelectJammerSort")
    .appendTo(selectJammerSpan).button();
  _crgUtils.bindAndRun(selectSortButton, "click", function() {
    $("#TeamTime table.Team td.Jammer")
      .toggleClass("NumSort", this.checked)
      .toggleClass("AlphaSort", !this.checked);
    $(this).button("option", "label", (this.checked?"Numerically":"Alphabetically"));
  });
  // FIXME - jquery-ui seems buggy by defaulting to rtl if "direction" not specified
  selectJammerSpan.css("direction", "ltr").buttonset();

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
    var last = pc.$sb("Number").$sbIs(pc.$sb("MaximumNumber").$sbGet());
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

function createPeriodEndTimeoutDialog(td) {
  var dialog = $("<div>");
  var applyDiv = $("<div>").addClass("Apply").appendTo(dialog);
  $("<span>").text("Timeout with ").appendTo(applyDiv);
  var periodSeconds = $("<input type='text' size='3'>").val("1").appendTo(applyDiv);
  $("<span>").text(" seconds left on Period clock:").appendTo(applyDiv);
  $("<button>").addClass("Apply").text("Apply").appendTo(applyDiv).button()
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
  $("<span>").text("Overtime can only be started at the end of Period ").appendTo(dialog);
  $sb("ScoreBoard.Clock(Period).MaximumNumber").$sbElement("<span>").appendTo(dialog);
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
  var controlsTr = createRowTable(3,2).appendTo(table.find("td")).find("tr:eq(0)").addClass("Controls");
  var undoTr = controlsTr.next().addClass("UndoControls");

  $sb("ScoreBoard.StartJam").$sbControl("<button>").text("Start Jam").val("true")
    .attr("id", "StartJam").addClass("KeyControl").button()
    .appendTo(controlsTr.children("td:eq(0)"));
  $sb("ScoreBoard.StopJam").$sbControl("<button>").text("Stop Jam").val("true")
    .attr("id", "StopJam").addClass("KeyControl").button()
    .appendTo(controlsTr.children("td:eq(1)"));
  $sb("ScoreBoard.Timeout").$sbControl("<button>").text("Timeout").val("true")
    .attr("id", "Timeout").addClass("KeyControl").button()
    .appendTo(controlsTr.children("td:eq(2)"));

  $sb("ScoreBoard.UnStartJam").$sbControl("<button>").text("UN-Start Jam").val("true")
    .attr("id", "UnStartJam").addClass("KeyControl").button()
    .appendTo(undoTr.children("td:eq(0)"));
  $sb("ScoreBoard.UnStopJam").$sbControl("<button>").text("UN-Stop Jam").val("true")
    .attr("id", "UnStopJam").addClass("KeyControl").button()
    .appendTo(undoTr.children("td:eq(1)"));
  $sb("ScoreBoard.UnTimeout").$sbControl("<button>").text("UN-Timeout").val("true")
    .attr("id", "UnTimeout").addClass("KeyControl").button()
    .appendTo(undoTr.children("td:eq(2)"));

  return table;
}

function createTeamTable() {
  var table = $("<table>").addClass("Team");
  var row = $("<tr></tr>");
  var nameRow = row.clone().addClass("Name").appendTo(table);
  var scoreRow = row.clone().addClass("Score").appendTo(table);
  var timeoutRow = row.clone().addClass("Timeout").appendTo(table);
  var jammerRow = row.clone().addClass("Jammer").appendTo(table);
  var passRow = row.clone().addClass("Pass").appendTo(table);

  $.each( [ "1", "2" ], function() {
    var team = String(this);
    var sbTeam = $sb("ScoreBoard.Team("+team+")");
    var first = (team == "1");

    var nameTr = createRowTable(2).appendTo($("<td>").appendTo(nameRow)).find("tr");
    var scoreTr = createRowTable(3).appendTo($("<td>").appendTo(scoreRow)).find("tr");
    var timeoutTr = createRowTable(2).appendTo($("<td>").appendTo(timeoutRow)).find("tr");
    var jammerTr = createRowTable(2).appendTo($("<td>").appendTo(jammerRow)).find("tr");
    var passTr = createRowTable(3).appendTo($("<td>").appendTo(passRow)).find("tr");

    var nameTd = nameTr.children("td:eq("+(first?1:0)+")").addClass("Name");
    sbTeam.$sb("Name").$sbControl("<a/><input type='text' size='15'/>", { sbcontrol: {
        editOnClick: true,
        bindClickTo: nameTd
      } }).appendTo(nameTd).filter("a").wrap("<div>");

    var logoTd = nameTr.children("td:eq("+(first?0:1)+")").addClass("Logo");
    var logoNone = $("<a>").html("No Logo").addClass("NoLogo").appendTo(logoTd);
    var logoSelect = sbTeam.$sb("Logo").$sbControl("<select>", { sbelement: {
        optionParent: "Images.Type(teamlogo)",
        optionChildName: "Image",
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

    logoTd.click(function() { if (!logoSelect.is(":visible")) logoShowSelect(true); })

    sbTeam.$sb("Score").$sbControl("<button>", { sbcontrol: { sbSetAttrs: { change: "true" } } })
      .text("Score -1").val("-1")
      .attr("id", "Team"+team+"ScoreDown").addClass("KeyControl").button()
      .appendTo(scoreTr.children("td:eq("+(first?"0":"2")+")").addClass("Down"));
    sbTeam.$sb("Score").$sbControl("<a/><input type='text' size='4'/>", { sbcontrol: {
        editOnClick: true,
        bindClickTo: scoreTr.children("td:eq(1)")
      } }).appendTo(scoreTr.children("td:eq(1)").addClass("Score"));
    sbTeam.$sb("Score").$sbControl("<button>", { sbcontrol: { sbSetAttrs: { change: "true" } } })
      .text("Score +1").val("1")
      .attr("id", "Team"+team+"ScoreUp").addClass("KeyControl").button()
      .appendTo(scoreTr.children("td:eq("+(first?"2":"0")+")").addClass("Up"));
    // Note there is a left and right scoreChange, so the main score remains centered,
    // but only the right one is made visible.
    $("<a>").addClass("Change").css({ opacity: "0" })
      .appendTo(scoreTr.children("td:eq(1)"))
      .clone().prependTo(scoreTr.children("td:eq(1)"));
    var scoreChange = scoreTr.find("td:eq(1)>a.Change");
    var lastScore = sbTeam.$sb("Score").$sbGet();
    var scoreChangeTimeout;
    sbTeam.$sb("Score").bind("sbchange", function(event,value) {
      var s = (value - lastScore);
      var c = (s<0 ? "#800" : s>0 ? "#080" : "#008");
      scoreChange.stop(true).text(s<0?s:"+"+s).last().css({ opacity: "1", color: c });
      if (scoreChangeTimeout)
        clearTimeout(scoreChangeTimeout);
      scoreChangeTimeout = setTimeout(function() {
        scoreChange.last()
          .animate({ color: "#000" }, 2000)
          .animate({ opacity: "0" }, 6000, "easeInExpo", function() { lastScore = value; });
        scoreChangeTimeout = null;
      }, 2000);
    });

    sbTeam.$sb("Timeout").$sbControl("<button>").text("Timeout").val("true")
      .attr("id", "Team"+team+"Timeout").addClass("KeyControl").button()
      .appendTo(timeoutTr.children("td:eq("+(first?"0":"1")+")").addClass("Timeout"));
    sbTeam.$sb("Timeouts").$sbControl("<a/><input type='text' size='2'/>", { sbcontrol: {
        editOnClick: true,
        bindClickTo: timeoutTr.children("td:eq("+(first?"1":"0")+")")
      } }).appendTo(timeoutTr.children("td:eq("+(first?"1":"0")+")").addClass("Timeouts"));

    var leadJammerTd = jammerTr.children("td:eq("+(first?"0":"1")+")")
      .append("<label id='Team"+team+"Lead'>Lead</label><input type='radio' value='true'/>")
      [first?"append":"prepend"]("<label id='Team"+team+"NoLead'>No Lead</label><input type='radio' value='false'/>");
    sbTeam.$sb("LeadJammer").$sbControl(leadJammerTd.children())
      .addClass("KeyControl");
    /* some strange bug, css direction is unset for leadJammerTd
     * so need to explicitly specify to style the buttonset as ltr
     */
    leadJammerTd.css("direction", "ltr").buttonset();
    var jammerSelectTd = jammerTr.children("td:eq("+(first?"1":"0")+")").addClass("Jammer ByName AlphaSort");
    sbTeam.$sb("Position(Jammer).Id").$sbControl("<select>", { sbelement: {
        optionParent: sbTeam,
        optionChildName: "Skater",
        optionNameElement: "Name",
        compareOptions: function(a, b) { return _windowFunctions.alphaCompareByProp("text", a, b); },
        firstOption: { text: "No Jammer", value: "" }
      } }).addClass("Jammer ByName AlphaSort")
        .appendTo(jammerSelectTd);
    sbTeam.$sb("Position(Jammer).Id").$sbControl("<select>", { sbelement: {
        optionParent: sbTeam,
        optionChildName: "Skater",
        optionNameElement: "Number",
        compareOptions: function(a, b) { return _windowFunctions.alphaCompareByProp("text", a, b); },
        firstOption: { text: "No Jammer", value: "" }
      } }).addClass("Jammer ByNumber AlphaSort")
        .appendTo(jammerSelectTd);
    sbTeam.$sb("Position(Jammer).Id").$sbControl("<select>", { sbelement: {
        optionParent: sbTeam,
        optionChildName: "Skater",
        optionNameElement: "Name",
        compareOptions: function(a, b) { return _windowFunctions.numCompareByProp("text", a, b); },
        firstOption: { text: "No Jammer", value: "" }
      } }).addClass("Jammer ByName NumSort")
        .appendTo(jammerSelectTd);
    sbTeam.$sb("Position(Jammer).Id").$sbControl("<select>", { sbelement: {
        optionParent: sbTeam,
        optionChildName: "Skater",
        optionNameElement: "Number",
        compareOptions: function(a, b) { return _windowFunctions.numCompareByProp("text", a, b); },
        firstOption: { text: "No Jammer", value: "" }
      } }).addClass("Jammer ByNumber NumSort")
        .appendTo(jammerSelectTd);

    sbTeam.$sb("Pass").$sbControl("<button>", { sbcontrol: { sbSetAttrs: { change: "true" } } })
      .text("Pass -1").val("-1")
      .attr("id", "Team"+team+"PassDown").addClass("KeyControl").button()
      .appendTo(passTr.children("td:eq("+(first?"0":"2")+")").addClass("Down"));
    sbTeam.$sb("Pass").$sbControl("<a/><input type='text' size='4'/>", { sbcontrol: {
        editOnClick: true,
        bindClickTo: passTr.children("td:eq(1)")
      } }).appendTo(passTr.children("td:eq(1)").addClass("Pass"));
    sbTeam.$sb("Pass").$sbControl("<button>", { sbcontrol: { sbSetAttrs: { change: "true" } } })
      .text("Pass +1").val("1")
      .attr("id", "Team"+team+"PassUp").addClass("KeyControl").button()
      .appendTo(passTr.children("td:eq("+(first?"2":"0")+")").addClass("Up"));

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
  var timeSetRow = row.clone().addClass("TimeSet").appendTo(table);
  var timeResetRow = row.clone().addClass("TimeReset").appendTo(table);

  $.each( [ "Period", "Jam", "Lineup", "Timeout", "Intermission" ], function() {
    var clock = String(this);
    var sbClock = $sb("ScoreBoard.Clock("+clock+")");

    var nameTd = $("<td>").appendTo(nameRow);
    var numberTr = createRowTable(3).appendTo($("<td>").appendTo(numberRow)).find("tr");
    var controlTr = createRowTable(2).appendTo($("<td>").appendTo(controlRow)).find("tr");
    var timeTr = createRowTable(3).appendTo($("<td>").appendTo(timeRow)).find("tr");
    var timeSetTr = createRowTable(2).appendTo($("<td>").appendTo(timeSetRow)).find("tr");
    var timeResetTd = $("<td>").appendTo(timeResetRow);

    sbClock.$sb("Name").$sbElement("<a>").appendTo(nameTd.addClass("Name"));
    sbClock.$sb("Running").$sbBindAndRun("sbchange", function(event,value) {
      nameTd.toggleClass("Running", isTrue(value));
    });

    sbClock.$sb("Number").$sbControl("<button>", { sbcontrol: { sbSetAttrs: { change: "true" } } })
      .text("-1").val("-1")
      .attr("id", "Clock"+clock+"NumberDown").addClass("KeyControl").button()
      .appendTo(numberTr.children("td:eq(0)").addClass("Down").css("width", "40%"));
    sbClock.$sb("Number").$sbControl("<a/><input type='text' size='2'/>", { sbcontrol: {
        editOnClick: true,
        bindClickTo: numberTr.children("td:eq(1)")
      } }).appendTo(numberTr.children("td:eq(1)").addClass("Number").css("width", "20%"));
    sbClock.$sb("Number").$sbControl("<button>", { sbcontrol: { sbSetAttrs: { change: "true" } } })
      .text("+1").val("1")
      .attr("id", "Clock"+clock+"NumberUp").addClass("KeyControl").button()
      .appendTo(numberTr.children("td:eq(2)").addClass("Up").css("width", "40%"));

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

    $("<input type='text'/>")
      .attr("size", "6")
      .appendTo(timeSetTr.children("td:eq(0)").addClass("Text"))
      .focus(function() { createTimeSetWarningDialog($(this)); });
    $("<button/>").text("Set").button()
      .appendTo(timeSetTr.children("td:eq(1)").addClass("Button"));
    sbClock.$sb("Time").$sbControl(timeSetTr.find("td"), { sbcontrol: {
      convert: _timeConversions.minSecToMs,
      delayupdate: true,
      noSetControlValue: true
    }});

    sbClock.$sb("ResetTime").$sbControl("<button>")
      .text("Reset Time").val("true")
      .attr("id", "Clock"+clock+"ResetTime").addClass("KeyControl").button()
      .appendTo(timeResetTd);
  });

  return table;
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


///////////////
// Policies tab
///////////////

function createPoliciesTab() {
  $("<table>").attr("id", "Policies")
    .appendTo(createTab("Policies", "PoliciesTab"))
    .data("loadContentFunction", createPoliciesContent);
}

function createPoliciesContent(table) {
  $("<td>").attr("colspan", "2")
    .appendTo($("<tr>").addClass("ExpandCollapseAll").appendTo(table))
    .append("<a>Click to Expand/Collapse all</a>")
    .click(function() {
      if ($("#Policies tr.Name>td.Hide").length)
        $("#Policies tr.Name>td.Hide").click();
      else
        $("#Policies tr.Name>td:not(.Hide)").click();
    });

  $sb("ScoreBoard").$sbBindAddRemoveEach("Policy", 
    function(event, node) { addPolicy(node); },
    function(event, node) { removePolicy(node); }
  );
}

function addPolicy(policy) {
  var nameTr = $("<tr>").addClass("Name")
    .attr("data-policy", policy.$sbId)
    .append($("<td>").attr("colspan", "2"));
  _windowFunctions.appendAlphaSortedByAttr($("#Policies>tbody"), nameTr, "data-policy", 1);
  policy.$sb("Name").$sbElement("<a>").appendTo(nameTr.children("td"));

  var contentTr = $("<tr>").addClass("Content").insertAfter(nameTr)
    .attr("data-policy", policy.$sbId)
    .append($("<td>").addClass("Description"))
    .append($("<td>").addClass("Controls"));
  $("<div>").append(policy.$sb("Description").$sbElement("<a>"))
    .appendTo(contentTr.children("td.Description"));
  var controls = $("<div>").appendTo(contentTr.children("td.Controls"));

  $("<span>")
    .append(policy.$sb("Enabled").$sbControl("<label>Enabled</label><input type='checkbox'/>"))
    .append("<br>").append("<br>")
    .appendTo(controls);

  var toggleContent = function() {
    if ($(this).toggleClass("Hide").hasClass("Hide"))
      contentTr.find("td>div").hide("blind", 250);
    else
      contentTr.find("td>div").show("blind", 250);
  };
  _crgUtils.bindAndRun(nameTr.children("td"), "click", toggleContent);

  policy.$sbBindAddRemoveEach("Parameter",
    function(event, node) {
      var span = $("<span>").attr("data-parameter", node.$sbId)
        .append(node.$sb("Name").$sbElement("<a>"))
        .append("<br>")
        .appendTo(controls);
      var type = node.$sb("Type").$sbGet();
      if (type == "Boolean")
        node.$sb("Value").$sbControl("<input type='checkbox'/>").insertAfter(span.children("a"));
      else
        node.$sb("Value").$sbControl("<input type='text' size='10'/>").insertAfter(span.children("a"));
    },
    function(event, node) {
      controls.children("span[data-parameter='"+node.$sbId+"']").remove();
    }
  );
}

function removePolicy(policy) {
  $("#Policies tr[data-policy='"+policy.$sbId+"']").remove();
}


//////////////////////
// ScoreBoard View tab
//////////////////////

function createScoreBoardViewTab() {
  $("<table>").attr("id", "ScoreBoardView")
    .appendTo(createTab("ScoreBoard View", "ScoreBoardViewTab"))
    .data("loadContentFunction", createScoreBoardViewContent);
}

function createScoreBoardViewContent(table) {
  var usePreviewButton = $("<label for='UsePreviewButton'/><input type='checkbox' id='UsePreviewButton'/>")
    .button();
  _crgUtils.bindAndRun(usePreviewButton.filter("input:checkbox"), "click", function() {
    $(this).button("option", "label", (isTrue(this.checked)?"Edit Live ScoreBoard":"Edit Preview"));
    table.toggleClass("UsePreview", !isTrue(this.checked));
  });
  var applyPreviewButton = $("<button>Apply Preview</button>").button()
    .click(function() {
      $sb("Pages.Page(scoreboard.html).PreviewOptions").find("*").each(function() {
        var path = String($sb(this).$sbPath).replace("PreviewOptions","ViewOptions");
        $sb(path).$sbSet($sb(this).$sbGet());
      });
    });
  var viewOptions = $sb("Pages.Page(scoreboard.html)").children("PreviewOptions,ViewOptions");
  _crgUtils.bindAndRun(viewOptions, "sbchange", function() {
    var disableApplyButton = true;
    $sb("Pages.Page(scoreboard.html).PreviewOptions").find("*").each(function() {
      var viewPath = String($sb(this).$sbPath).replace("PreviewOptions","ViewOptions");
      if (!$sb(this).$sbIs($sb(viewPath).$sbGet())) {
        disableApplyButton = false;
        return false;
      }
    });
    applyPreviewButton.button("option", "disabled", disableApplyButton);
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

  var sbUrl = "/views/scoreboard.html?videomuted=true&videocontrols=true";
  $("<tr><td/></tr>").appendTo(table)
    .find("td").addClass("ViewFrames Footer")
    .append(createRowTable(2))
    .find("td").append(previewButton)
    .find("button")
    .first().attr("src", sbUrl).end()
    .last().attr("src", sbUrl+"&preview=true");
}

function createScoreBoardViewPreviewRows(table, type) {
  var pageSb = $sb("Pages.Page(scoreboard.html)."+type+"Options");

  var currentViewTd = $("<tr><td/></tr>").addClass(type).appendTo(table)
    .find("td").addClass("Header NoChildren CurrentView")
    .append("<label>ScoreBoard</label><input type='radio' value='scoreboard'/>")
    .append("<label>Image</label><input type='radio' value='image'/>")
    .append("<label>Video</label><input type='radio' value='video'/>")
    .append("<label>Custom Page</label><input type='radio' value='html'/>");

  pageSb.$sb("CurrentView").$sbControl(currentViewTd.children());
  currentViewTd.buttonset()
    .prepend("<a>Current View : </a>");

  $("<tr><td><a>ScoreBoard Options</a></td></tr>").addClass(type).appendTo(table)
    .find("td").addClass("ScoreBoardOptions Header");

//FIXME - this intermission control should not be on the View tab,
//FIXME - it should be on the policy or scoreboard operation/behavior tab
  var intermissionControlDialog = createIntermissionControlDialog();
  var intermissionControlButton = $("<button>Intermission Control</button>").button()
    .click(function() { intermissionControlDialog.dialog("open"); });
  var swapTeamsButton = $("<label/><input type='checkbox'/>");
  pageSb.$sb("SwapTeams").$sbControl(swapTeamsButton, { sbcontrol: {
      button: true
    }, sbelement: {
      convert: function(value) {
        swapTeamsButton.filter("input:checkbox")
          .button("option", "label", (isTrue(value)?"Team sides swapped":"Team sides normal"));
        return value;
      }
    } });

  $("<tr><td/></tr>").addClass(type).appendTo(table)
    .find("td").addClass("ScoreBoardOptions Footer")
    .append(createRowTable(2))
    .find("td")
    .first().append(intermissionControlButton)
    .next().append(swapTeamsButton);

  $("<tr><td><a>Alternate View Options</a></td></tr>").addClass(type).appendTo(table)
    .find("td").addClass("AlternateViewOptions Header");

  var imageViewSelect = pageSb.$sb("View(Image).Src").$sbControl("<select>", { sbelement: {
      optionParent: "Images.Type(fullscreen)",
      optionChildName: "Image",
      optionNameElement: "Name",
      optionValueElement: "Src",
      firstOption: { text: "No Image", value: "" }
    } });
  var videoViewSelect = pageSb.$sb("View(Video).Src").$sbControl("<select>", { sbelement: {
      optionParent: "Videos.Type(fullscreen)",
      optionChildName: "Video",
      optionNameElement: "Name",
      optionValueElement: "Src",
      firstOption: { text: "No Video", value: "" }
    } });
  var customPageViewSelect = pageSb.$sb("View(CustomHtml).Src").$sbControl("<select>", { sbelement: {
      optionParent: "CustomHtml.Type(fullscreen)",
      optionChildName: "Html",
      optionNameElement: "Name",
      optionValueElement: "Src",
      firstOption: { text: "No Page", value: "" }
    } });

  $("<tr><td><a>Image View : </a></td></tr>").addClass(type).appendTo(table)
    .find("td").addClass("AlternateViewOptions")
    .append(imageViewSelect);
  $("<tr><td><a>Video View : </a></td></tr>").addClass(type).appendTo(table)
    .find("td").addClass("AlternateViewOptions")
    .append(videoViewSelect);
  $("<tr><td><a>Custom Page View : </a></td></tr>").addClass(type).appendTo(table)
    .find("td").addClass("AlternateViewOptions Footer")
    .append(customPageViewSelect);
}

function createIntermissionControlDialog() {
  var table = $("<table>").addClass("IntermissionControlDialog");
  $.each( [ "Pregame", "Halftime", "Final" ], function(i,title) {
    var intermissionControl = $sb("Pages.Page(scoreboard.html).Intermission("+i+")");
    $("<td>").attr("colspan", "3").addClass("Name")
      .append($("<a>").text(title+" (Intermission "+i+")"))
      .appendTo($("<tr>").addClass("Name").appendTo(table));
    var controlRow = $("<tr>").addClass("Control")
      .append($("<td><label><span/></label><input type='checkbox'/></td>").addClass("ShowUnofficial"))
      .append($("<td><a>Text:</a><input type='text'/></td>").addClass("Text"))
      .append($("<td><label><span/></label><input type='checkbox'/></td>").addClass("HideClock"))
      .appendTo(table);
    intermissionControl.$sb("ShowUnofficial").$sbBindAndRun("sbchange", function(event,value) {
        controlRow.find(">td.ShowUnofficial>label>span").text(isTrue(value)?"'Unofficial' Showing":"'Unofficial' Hidden");
      }).$sbControl(controlRow.find(">td.ShowUnofficial>label,>td.ShowUnofficial>input:checkbox"), { sbcontrol: {
        button: true
      } });
    intermissionControl.$sb("Text").$sbControl(controlRow.find(">td.Text>input:text"), { size: "12" });
    intermissionControl.$sb("HideClock").$sbBindAndRun("sbchange", function(event,value) {
        controlRow.find(">td.HideClock>label>span").text(isTrue(value)?"Clock Hidden":"Clock Showing");
      }).$sbControl(controlRow.find(">td.HideClock>label,>td.HideClock>input:checkbox"), { sbcontrol: {
        button: true
      } });
  });
  return $("<div>").append(table).dialog({
    title: "Intermission Display Controls",
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
    .appendTo(createTab("Teams", "TeamsTab"))
    .data("loadContentFunction", createTeamsContent);
}

function createTeamsContent() {
  $("#Teams")
    .append($("<tr><td/></tr>").addClass("Selection NoneSelected"))
    .append($("<tr><td/></tr>").addClass("Team"));
  createRowTable(2).appendTo("#Teams>tbody>tr.Selection>td").addClass("Selection");

  var selectTeam = _crgUtils.setupSelect("<select>", {
    optionParent: "Teams",
    optionChildName: "Team",
    prependOptions: [
      { text: "No Team Selected", value: "" },
      { text: "Current Team 1", value: "(Current Team 1)" },
      { text: "Current Team 2", value: "(Current Team 2)" }
    ]
  }).appendTo("#Teams table.Selection td:eq(0)");
  var newTeamName = $("<input type='text' size='30'/>")
    .appendTo("#Teams table.Selection td:eq(1)");
  var createNewTeam = $("<button>").text("New Team").button()
    .appendTo("#Teams table.Selection td:eq(1)");

  _crgUtils.bindAndRun(newTeamName, "keyup", function(event) {
    createNewTeam.button("option", "disabled", (!$(this).val()));
    if (!createNewTeam.button("option", "disabled") && (13 == event.which)) // Enter
      createNewTeam.click();
  });
  createNewTeam.click(function(event) {
    var teamname = newTeamName.val();
    var teamid = _crgUtils.checkSbId(teamname);
    $sb("Teams.Team("+teamid+").Name").$sbSet(teamname);
    newTeamName.val("").keyup().focus();
  });

  createNewTeamTable($sb("ScoreBoard.Team(1)"), "(Current Team 1)")
    .appendTo("#Teams>tbody>tr.Team>td");
  createNewTeamTable($sb("ScoreBoard.Team(2)"), "(Current Team 2)")
    .appendTo("#Teams>tbody>tr.Team>td");

  _crgUtils.bindAddRemoveEach($sb("Teams"), "Team", function(event, node) {
    if (node.$sbId) /* skip any invalid team with no id */
      createNewTeamTable(node).appendTo("#Teams>tbody>tr.Team>td");
  }, function(event, node) {
    $("table.Team", "#Teams")
      .filter(function() { return (node.$sbId == $(this).data("id")); })
      .remove();
  });

  _crgUtils.bindAndRun(selectTeam, "change", function(event) {
    var teamid = selectTeam.val();
    var selectedTeam = $("#Teams table.Team").addClass("Hide")
      .filter(function() { return (teamid == $(this).data("id")); })
      .removeClass("Hide");
    $("#Teams tr.Selection").toggleClass("NoneSelected", !selectedTeam.length);
  });
}

function createNewTeamTable(team, teamid) {
  if (!teamid)
    teamid = team.$sbId;
  var isCurrentTeam = team.parent().is("ScoreBoard");
  var teamTable = $("<table>").addClass("Team Hide").data("id", teamid)
    .append($("<tr><td/></tr>").addClass("Control"))
    .append($("<tr><td/></tr>").addClass("Skaters"));
  var controlTable = createRowTable(5).appendTo(teamTable.find("tr.Control>td")).addClass("Control");

  team.$sb("Name").$sbControl("<input type='text'>")
    .appendTo(controlTable.find("td:eq(0)"));
  team.$sb("Logo").$sbControl("<select>", { sbelement: {
    optionParent: "Images.Type(teamlogo)",
    optionChildName: "Image",
    optionNameElement: "Name",
    optionValueElement: "Src",
    firstOption: { text: "No Logo", value: "" }
  } }).appendTo(controlTable.find("td:eq(1)"));
  $("<button>").text("Alternate Names").button()
    .click(function() { createAlternateNamesDialog(team); })
    .appendTo(controlTable.find("td:eq(2)"));
  $("<button>").text("Assign Team").button({ disabled: isCurrentTeam })
    .click(function() { createTeamsAssignDialog(teamid); })
    .appendTo(controlTable.find("td:eq(3)"));
  $("<button>").text("Remove Team").button({ disabled: isCurrentTeam })
    .click(function() { createTeamsRemoveDialog(teamid); })
    .appendTo(controlTable.find("td:eq(4)"));

  var skatersTable = $("<table>").addClass("Skaters Empty")
    .appendTo(teamTable.find("tr.Skaters>td"))
    .append("<col class='Id'>")
    .append("<col class='Name'>")
    .append("<col class='Number'>")
    .append("<col class='Button'>")
    .append("<thead/><tbody/>")
    .children("thead")
    .append("<tr><th colspan='4' class='Title'>Skaters</th></tr>")
    .append("<tr><th>Id</th><th>Name</th><th>Number</th><th>Add</th>")
    .append("<tr class='AddSkater'><th/><th/><th/><th/></tr>")
    .append("<tr><th colspan='4'><hr/></th></tr>")
    .end();

  var newSkaterName = $("<input type='text' size='20'>").addClass("Name")
    .appendTo(skatersTable.find("tr.AddSkater>th:eq(1)"));
  var newSkaterNumber = $("<input type='text' size='20'>").addClass("Number")
    .appendTo(skatersTable.find("tr.AddSkater>th:eq(2)"));
  var newSkaterButton = $("<button>").text("Add Skater").button({ disabled: true }).addClass("AddSkater")
    .appendTo(skatersTable.find("tr.AddSkater>th:eq(3)"))
    .click(function() {
      var id = _crgUtils.checkSbId(newSkaterName.val());
      team.$sb("Skater("+id+").Name").$sbSet(newSkaterName.val());
      team.$sb("Skater("+id+").Number").$sbSet(newSkaterNumber.val());
      newSkaterNumber.val("");
      newSkaterName.val("").focus();
      $(this).blur();
      newSkaterButton.button("option", "disabled", true);
    });
  newSkaterName.add(newSkaterNumber).keyup(function(event) {
    newSkaterButton.button("option", "disabled", (!newSkaterName.val() && !newSkaterNumber.val()));
    if (!newSkaterButton.button("option", "disabled") && (13 == event.which)) // Enter
      newSkaterButton.click();
  });

  team.$sbBindAddRemoveEach("Skater", function(event,node) {
    var skaterid = node.$sbId;
    if (skatersTable.find("tr[data-skaterid='"+skaterid+"']").length)
      return;
    skatersTable.removeClass("Empty");

    var skaterRow = $("<tr>").attr("data-skaterid", skaterid)
      .append("<td class='Id'>")
      .append("<td class='Name'>")
      .append("<td class='Number'>")
      .append("<td class='Remove'>");
    $("<a>").text(skaterid).appendTo(skaterRow.children("td.Id"));
    node.$sb("Name").$sbControl("<input type='text' size='20'>")
      .appendTo(skaterRow.children("td.Name"));
    node.$sb("Number").$sbControl("<input type='text' size='20'>")
      .appendTo(skaterRow.children("td.Number"));
    $("<button>").text("Remove").addClass("RemoveSkater").button()
      .click(function() { createTeamsSkaterRemoveDialog(team, teamid, node); })
      .appendTo(skaterRow.children("td.Remove"));

    _windowFunctions.appendAlphaNumSortedByAttr(skatersTable.children("tbody"), skaterRow, "data-skaterid");
  }, function(event,node) {
    skatersTable.find("tr[data-skaterid='"+node.$sbId+"']").remove();
    if (!skatersTable.find("tr[data-skaterid]").length)
      skatersTable.addClass("Empty");
  });

  return teamTable;
}

function createAlternateNamesDialog(team) {
  var dialog = $("<div>").addClass("AlternateNamesDialog");

  $("<a>").text("Type:").appendTo(dialog);
  var newIdInput = $("<input type='text'>").appendTo(dialog);
  $("<a>").text("Name:").appendTo(dialog);
  var newNameInput = $("<input type='text'>").appendTo(dialog);

  var newFunc = function() {
    var newId = newIdInput.val();
    var newName = newNameInput.val();
    team.$sb("AlternateName("+newId+").Name").$sbSet(newName);
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
  $("<tr>")
    .append("<th class='X'>X</th>")
    .append("<th class='Id'>Id</th>")
    .append("<th class='Name'>Name</th>")
    .appendTo(table);

  var doExit = 0;
  var addFunc = function(event, node) {
    if (doExit) {
      $(this).unbind(event);
      return;
    }
    var tr = $("<tr>").attr("data-id", node.$sbId)
      .append("<td class='X'>")
      .append("<td class='Id'>")
      .append("<td class='Name'>");
    $("<button>").button({ label: "X" })
      .click(function() { node.$sbRemove(); })
      .appendTo(tr.children("td.X"));
    tr.children("td.Id").text(node.$sbId);
    node.$sb("Name").$sbControl("<input type='text' size='20'>").appendTo(tr.children("td.Name"));
    tr.appendTo(table);
  };
  var removeFunc = function(event, node) {
    if (doExit)
      $(this).unbind(event);
    else
      table.find("tr[data-id='"+node.$sbId+"']").remove();
  };

  team.$sbBindAddRemoveEach("AlternateName", addFunc, removeFunc);

  newIdInput.autocomplete({
    minLength: 0,
    source: [
      { label: "overlay (Video Overlay)", value: "overlay" },
      { label: "twitter (Twitter)", value: "twitter" }
    ]
  }).focus(function() { $(this).autocomplete("search", ""); });

  dialog.dialog({
    title: "Alternate Names",
    modal: true,
    width: 700,
    close: function() { doExit = 1; $(this).dialog("destroy").remove(); },
    buttons: { Close: function() { $(this).dialog("close"); } }
  });
}

function createTeamsAssignDialog(teamId) {
  var dialog = $("<div>").addClass("TeamsAssignDialog");
  $("<h4>").text("Load Selected Team's information to ScoreBoard").appendTo(dialog);
  $("<label>").attr("for", "TeamsAssignDialogCheckboxTo").appendTo(dialog);
  $("<input type='checkbox'>").attr("id", "TeamsAssignDialogCheckboxTo").appendTo(dialog).button();
  $("<a>").text(" team info to the Scoreboard as:").appendTo(dialog);
  $("<button>").text("Team 1").data({ team: "1", dir: "To" }).button().appendTo(dialog);
  $("<button>").text("Team 2").data({ team: "2", dir: "To" }).button().appendTo(dialog);
  $("<br>").appendTo(dialog);

  $("<h4>").text("Get Selected Team's information from ScoreBoard").appendTo(dialog);
  $("<label>").attr("for", "TeamsAssignDialogCheckboxFrom").appendTo(dialog);
  $("<input type='checkbox'>").attr("id", "TeamsAssignDialogCheckboxFrom").appendTo(dialog).button();
  $("<a>").text(" team info from the Scoreboard from:").appendTo(dialog);
  $("<button>").text("Team 1").data({ team: "1", dir: "From" }).button().appendTo(dialog);
  $("<button>").text("Team 2").data({ team: "2", dir: "From" }).button().appendTo(dialog);

  $.each( [ "To", "From" ], function() {
    var label = dialog.children("label[for='TeamsAssignDialogCheckbox"+this+"']");
    var box = dialog.children("input:checkbox#TeamsAssignDialogCheckbox"+this);
    _crgUtils.bindAndRun(box, "click", function() {
      label.children("span").text(this.checked ? "Merge" : "Transfer");
    });
  });

  dialog.find("button").click(function() {
    var dir = $(this).data("dir");
    var type = dialog.find(">label[for='TeamsAssignDialogCheckbox"+dir+"']>span").text();
    var target = "Team"+$(this).data("team");
    $sb("Teams").$sb(type).$sb(dir).$sb(target).$sbSet(teamId);
    dialog.dialog("close");
  });
//FIXME - add way to register on specific element removal (not child removal) and destroy this dialog if removed,
//FIXME - do same in TeamsRemoveDialog below
  dialog.dialog({
    title: "Assign Team",
    modal: true,
    width: 700,
    close: function() { $(this).dialog("destroy").remove(); },
    buttons: { Close: function() { $(this).dialog("close"); } }
  });
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
    $sb("Teams.Team("+teamId+")").$sbRemove();
    dialog.dialog("close");
  }).button();

  dialog.dialog({
    title: "Remove Team",
    modal: true,
    width: 700,
    close: function() { $(this).dialog("destroy").remove(); }
  });
}

function createTeamsSkaterRemoveDialog(team, teamId, skater) {
  var dialog = $("<div>").addClass("TeamsRemoveDialog");

  $("<a>").addClass("Title").text("Team: "+teamId).appendTo(dialog);
  $("<br>").appendTo(dialog);

  var skaterId = skater.$sbId;
  var skaterName = skater.$sb("Name").$sbGet();
  $("<a>").addClass("Remove").text("Remove Skater: ").appendTo(dialog);
  $("<a>").addClass("Target").text(skaterId).appendTo(dialog);
  $("<br>").appendTo(dialog);
  if (skaterId != skaterName) {
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
    skater.$sbRemove();
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
    .append("<td colspan='4'>Download ScoreBoard XML</td>");
  $("<tr>").addClass("Instruction").appendTo(sbDownloadTable)
    .append("<td colspan='4'>To download, right-click and Save - to view XML, left-click</td>");
  var contentRow = $("<tr>").addClass("Content").appendTo(sbDownloadTable);

  var links = [
    { name: "All data", url: "" },
    { name: "ScoreBoard", url: "scoreboard.xml?path=ScoreBoard" },
    { name: "Teams", url: "teams.xml?path=Teams" },
    { name: "Pages", url: "pages.xml?path=Pages" }
  ];
  $.each( links, function() {
    $("<td><a/></td>").appendTo(contentRow)
      .children("a").html(this.name)
      .attr({ href: "/SaveXml/"+this.url, target: "_blank" });
  });
  var allDataA = contentRow.find(">td:eq(0)>a");
  var updateAllUrl = function() {
    var d = new Date();
    var name = $.datepicker.formatDate("yy-mm-dd_", d);
    name += _timeConversions.twoDigit(d.getHours());
    name += _timeConversions.twoDigit(d.getMinutes());
    name += _timeConversions.twoDigit(d.getSeconds());
    allDataA.attr("href", "/SaveXml/scoreboard-"+name+".xml");
  };
  setInterval(updateAllUrl, 1000);


  // Upload table
  var sbUploadTable = $("<table>").addClass("Upload")
    .appendTo($("<td>").appendTo($("<tr>").appendTo("#SaveLoad")));
  $("<tr>").addClass("Name").appendTo(sbUploadTable)
    .append("<td>Upload ScoreBoard XML</td>");
  var contentTd = $("<td>")
    .appendTo($("<tr>").addClass("Content").appendTo(sbUploadTable));

  var iframeId = "SaveLoadUploadHiddenIframe";
  var uploadForm = $("<form method='post' enctype='multipart/form-data' target='"+iframeId+"'/>")
    .append("<iframe id='"+iframeId+"' name='"+iframeId+"' style='display: none'/>")
    .append("<input type='file' name='xmlFile'/>")
    .appendTo(contentTd);
  $("<button>").html("Add/Merge").attr("data-method", "merge").appendTo(uploadForm).button();
  $("<button>").html("Replace running scoreboard").attr("data-method", "load").appendTo(uploadForm).button();
  uploadForm.children("button").click(function() {
    uploadForm.attr("action", "/LoadXml/"+$(this).attr("data-method")).submit();
  });
  _crgUtils.bindAndRun(uploadForm.children("input:file").button(), "change", function() {
    uploadForm.children("button").button(this.value ? "enable" : "disable");
  });

  // Reset table
  var sbResetTable = $("<table>").addClass("Reset")
    .appendTo($("<td>").appendTo($("<tr>").appendTo("#SaveLoad")));
  $("<tr>").addClass("Name").appendTo(sbResetTable)
    .append("<td>Reset ScoreBoard</td>");
  var resetTd = $("<td>")
    .appendTo($("<tr>").addClass("Content").appendTo(sbResetTable));

  $("<button>").text("Reset everything").button()
    .click(function() {
      showResetDialog("This will completely reset the ScoreBoard to the defaults.<br/>You will LOSE all current information!<br/>This will also reload all scoreboard pages.", ", reset everything to defaults", ", do not reset", $sb("Reset"));
    }).appendTo(resetTd);
  $("<button>").text("Reset scoreboard only").button()
    .click(function() {
      showResetDialog("This will reset the ScoreBoard elements, like the team and clock values.", ", reset the scoreboard", ", do not reset", $sb("ScoreBoard.Reset"));
    }).appendTo(resetTd);
}

function showResetDialog(descriptionHtml, yesText, noText, sbReset) {
  var div = $("<div>").addClass("ResetDialog");

  $("<p>").html(descriptionHtml).appendTo(div);
  div.dialog({
    modal: true,
    width: 800,
    buttons: [
      { text: "Yes"+yesText, click: function() { $(this).dialog("close"); } },
      { text: "No"+noText, click: function() { $(this).dialog("close"); } }
    ],
    close: function() { $(this).dialog("destroy").remove(); }
  });
  var yesButton = div.dialog("widget").find("button").filter(function() {
    return /^Yes/.test($(this).button("option", "label"));
  });
  sbReset.$sbControl(yesButton).val(true);
}

