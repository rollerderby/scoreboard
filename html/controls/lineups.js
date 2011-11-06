

$(function() { $("body").mousedown(function(event) { if (!$(event.target).is("input,text,select")) event.preventDefault(); }); });

$sb(function() {
  createCurrentJamLineupsTab();
  createEditJamLineupsTab();

  $("div#tabsDiv").tabs();

  $("<div>")
    .append("<center><a>This page is in development - functions may be incomplete or broken.</a></center>")
    .dialog({
      modal: true,
      width: "600px",
      title: "This page is in development",
      close: function() { $(this).dialog("destroy").remove(); },
      buttons: { Ok: function() { $(this).dialog("close"); } }
    });
});

/*******************************
 * Setup jQuery-UI tab structure
 ******************************/

function createTab(title, tabId) {
  if (typeof title == "string") title = $("<a>").html(title);
  if (!tabId) tabId = $sb().$sbNewUUID();
  $("<li>").append(title.attr("href", "#"+tabId)).appendTo("div#tabsDiv>ul");
  return $("<div>").attr("id", tabId).appendTo("div#tabsDiv");
}

function createTwoTeamTab(createTableFunction, defaultVertical, title, tabId) {
  var tab = createTab(title, tabId).css("overflow","hidden");
  var header = $("<div>").addClass("tabHeader").appendTo(tab);
  var team1 = $("<div>").css({ float: "left" }).append(createTableFunction("1")).appendTo(tab);
  var team2 = $("<div>").css({ float: "right" }).append(createTableFunction("2")).appendTo(tab);

  var teams = team1.add(team2);
  tabId = tab.attr("id");

  var alignment = $("<span>").appendTo(header);
  var setHorizontal = function() { teams.css({ width: "50%" }); };
  var setVertical = function() { teams.css({ width: "100%" }); };
  $("<a>").html("Alignment: ").appendTo(alignment);
  $("<label for='"+tabId+"AlignmentV'>Vertical</label>").appendTo(alignment);
  $("<input type='radio' name='"+tabId+"Alignment' id='"+tabId+"AlignmentV'>").appendTo(alignment)
    .prop("defaultChecked", defaultVertical)
    .click(setVertical);
  $("<label for='"+tabId+"AlignmentH'>Horizontal</label>").appendTo(alignment);
  $("<input type='radio' name='"+tabId+"Alignment' id='"+tabId+"AlignmentH'>").appendTo(alignment)
    .prop("defaultChecked", !defaultVertical)
    .click(setHorizontal);
  if (defaultVertical)
    setVertical();
  else
    setHorizontal();

  var show = $("<span>").appendTo(header);
  var showTeams = function() {
    var one = show.children("input:checkbox:eq(0)").prop("checked");
    var two = show.children("input:checkbox:eq(1)").prop("checked");
    var alignInput = alignment.children("input:radio");
    if (one == two) {
      alignInput.prop("disabled", false);
      if (alignInput.eq(0).prop("checked"))
        setVertical();
      else if (alignInput.eq(1).prop("checked"))
        setHorizontal();
    } else {
      alignInput.prop("disabled", true);
      setVertical();
    }
    if (one) team1.show(); else team1.hide();
    if (two) team2.show(); else team2.hide();
  };
  $("<a>").html("Show Team: ").appendTo(show);
  $("<label for='"+tabId+"ShowTeam1'>1</label>").appendTo(show);
  $("<input type='checkbox' id='"+tabId+"ShowTeam1'>").appendTo(show)
    .prop("defaultChecked", "true")
    .click(showTeams);
  $("<label for='"+tabId+"ShowTeam2'>2</label>").appendTo(show);
  $("<input type='checkbox' id='"+tabId+"ShowTeam2'>").appendTo(show)
    .prop("defaultChecked", "true")
    .click(showTeams);

  return tab;
}

function createCurrentJamLineupsTab() {
  var tab = createTwoTeamTab(createCurrentJamLineupsTable, true, "Current Jam Lineups");

  var header = tab.children("div.tabHeader");

  var showAddSkater = function() {
    if (this.checked)
      $(".addSkaterControls").show();
    else
      $(".addSkaterControls").hide();
  };
  $("<span>")
    .append("<label for='"+tab.attr("id")+"EditLineups'>Edit Lineups: </label>")
    .append($("<input type='checkbox' id='"+tab.attr("id")+"EditLineups'>").click(showAddSkater))
    .appendTo(header)
}
function createEditJamLineupsTab() {
  createTwoTeamTab(createEditJamLineupsTable, false, "Edit Jam Lineups");
}


/********************************************
 * Create content for Current Jam Lineups tab
 ********************************************/

function createCurrentJamLineupsTable(team) {
  var table = $("<table>");
  var thead = $("<thead>").appendTo(table);
  $("<th colspan='100%'>")
    .append($sb("ScoreBoard.Team("+team+").Name").$sbElement("<a>"))
    .append($sb("ScoreBoard.Team("+team+").LeadJammer").$sbControl("<button>Lead Jammer</button>", { sbcontrol: {
        getButtonValue: function() { return String(!$(this).hasClass("buttonTrue")); },
        setButtonValue: function(value) { $(this).toggleClass("buttonTrue", (String(value).toLowerCase() == "true")); }
      }}).css("float", "right"))
    .append($sb("ScoreBoard.Team("+team+").Pass").$sbControl("<button>Pass +</button>", { sbcontrol: {
        getButtonValue: function() { return "1"; },
        noSetButtonValue: true,
        sbSetAttrs: { change: "true" }
      }}).css("float", "right"))
    .append($sb("ScoreBoard.Team("+team+").Pass").$sbElement("<a>").css("float", "right"))
    .append($sb("ScoreBoard.Team("+team+").Pass").$sbControl("<button>Pass -</button>", { sbcontrol: {
        getButtonValue: function() { return "-1"; },
        noSetButtonValue: true,
        sbSetAttrs: { change: "true" }
      }}).css("float", "right"))
    .appendTo($("<tr>").appendTo(thead));

  var name = $("<input type='text' size='15'>").attr("id","team"+team+"NewSkaterName");
  var number = $("<input type='text' size='5'>").attr("id","team"+team+"NewSkaterNumber");
  var button = $("<input type='button'>").val("New Skater").attr("id","team"+team+"NewSkaterButton")
    .click(function () {
      var skater = $sb("ScoreBoard.Team("+team+").Skater("+name.val()+")");
      skater.$sb("Name").$sbSet(name.val());
      skater.$sb("Number").$sbSet(number.val());
      name.val("");
      number.val("");
      name.focus();
    });

  $("<th colspan='100%'>").addClass("addSkaterControls").hide()
    .append("<a>Name:</a>").append(name)
    .append("<a>Number:</a>").append(number)
    .append(button)
    .appendTo($("<tr>").appendTo(thead));

  $("<tr>")
    .append($("<th>").text("X"))
    .append($("<th>").text("Name"))
    .append($("<th>").text("#"))
    .append($("<th>").text("Position").attr("colspan", "4"))
    .append($("<th>").text("Box"))
    .appendTo(thead);

  var tbody = $("<tbody>").appendTo(table);

  $sb("ScoreBoard.Team("+team+")")
    .live("add:Skater", function (e, skater) { addCurrentJamLineupsSkater(tbody, skater); })
    .live("remove:Skater", function (e, skater) { removeCurrentJamLineupsSkater(tbody, skater); })
    .children("Skater").each(function() { addCurrentJamLineupsSkater(tbody, $sb(this)); });

  return table;
}

function createPositionTd(skater, position) {
  var td = $("<td>").css("cursor", "default");
  var a = $("<a>").html(position).appendTo(td);
  var positionChange = function(value) {
    var match = $.string(value).startsWith(position);
    td.toggleClass("buttonTrue", match);
    if (position == "Blocker") a.html(match ? value : "Blocker");
  };
  var tdClick = function () {
    var pos = skater.$sb("Position");
    if (position != "Blocker")
      pos.$sbSet(pos.$sbIs(position)?"Bench":position)
    else {
      if ($.string(pos.$sbGet()).startsWith("Blocker"))
        pos.$sbSet("Bench");
      else {
        var emptyIdFunc = function () { return $sb(this).$sb("Id").$sbIs(""); };
        skater.parent().children("Position[Id^=Blocker]").filter(emptyIdFunc).first().each(function () {
          pos.$sbSet($sb(this).$sbId);
        });
      }
    }
  };
  skater.$sb("Position").bind("content", function (e, value) { positionChange(value); });
  positionChange(skater.$sb("Position").$sbGet());
  return td.bind("mousedown", tdClick);
}

function createFieldTd(skater, field, name) {
  var td = $("<td>").css("cursor", "default").append(name);
  var box = skater.$sb(field).$sbControl("<input type=checkbox>").css("display", "none").appendTo(td);
  var fieldChange = function(value) {
    td.toggleClass("buttonTrue", (value == "true"));
  };
  var tdClick = function (event) { box[0].checked = !box[0].checked; box.trigger("change"); };
  skater.$sb(field).bind("content", function (e, value) { fieldChange(value); });
  fieldChange(skater.$sb(field).$sbGet());
  return td.bind("mousedown", tdClick);
}


function addCurrentJamLineupsSkater(table, skater) {
  var row = $("<tr>");

  $("<td>").html("X").appendTo(row)
    .bind("mousedown", function(event) {
      if (confirm("Remove Skater '"+skater.$sbId+"'?")) skater.$sbRemove();
    });
  $("<td>").append(skater.$sb("Name").$sbElement("<a>")).appendTo(row);
  $("<td>").append(skater.$sb("Number").$sbElement("<a>")).appendTo(row);
  createPositionTd(skater, "Bench").appendTo(row);
  createPositionTd(skater, "Pivot").appendTo(row);
  createPositionTd(skater, "Blocker").appendTo(row);
  createPositionTd(skater, "Jammer").appendTo(row);
  createFieldTd(skater, "PenaltyBox", "Box").appendTo(row);

  row.data("rowId", skater.$sbId);
  row.data("rowNumber", skater.$sb("Number").$sbGet());

  _windowFunctions.appendSorted(table, row, function(a, b) {
    var aStr = $(a).data("rowNumber").toLowerCase();
    var bStr = $(b).data("rowNumber").toLowerCase();
    var aNum = Number(aStr);
    var bNum = Number(bStr);
    if (isNaN(aNum))
      return (isNaN(bNum) ? (aStr > bStr) : true);
    else
      return (isNaN(bNum) ? false : (aNum > bNum));
  });
}

function removeCurrentJamLineupsSkater(table, skater) {
  table.children().filter(function () { return ($(this).data("rowId") == skater.$sbId); }).remove();
}

/*****************************************
 * Create content for Edit Jam Lineups tab
 ****************************************/

function createEditJamLineupsTable(team) {
  var table = $("<table>");
  var thead = $("<thead>").appendTo(table);
  var showFunction = function(c) { $(".team"+team+"SkaterAll").not($("."+c).show()).hide(); }
  
  $("<tr>")
    .append($("<th colspan='100%'>").append($sb("ScoreBoard.Team("+team+").Name").$sbElement("<a>")))
    .appendTo(thead);

  $("<tr>")
    .append($("<th colspan='100%'>")
      .append("Show: ")
      .append("<label for='team"+team+"SkaterNumberShow'>Number</label>")
      .append($("<input type='radio'>")
        .attr("name", "team"+team+"SkaterShow")
        .attr("id", "team"+team+"SkaterNumberShow")
        .prop("defaultChecked", "true")
        .change(function() { showFunction("team"+team+"SkaterNumber"); }))
      .append("<label for='team"+team+"SkaterNameShow'>Name</label>")
      .append($("<input type='radio'>")
        .attr("name", "team"+team+"SkaterShow")
        .attr("id", "team"+team+"SkaterNameShow")
        .change(function() { showFunction("team"+team+"SkaterName"); }))
      .append("<label for='team"+team+"SkaterAllShow'>Both</label>")
      .append($("<input type='radio'>")
        .attr("name", "team"+team+"SkaterShow")
        .attr("id", "team"+team+"SkaterAllShow")
        .change(function() { showFunction("team"+team+"SkaterAll"); })))
    .appendTo(thead);

  $("<tr>")
    .append($("<th>").text("P#"))
    .append($("<th>").text("J#"))
    .append($("<th>").text("Pivot"))
    .append($("<th>").text("B1"))
    .append($("<th>").text("B2"))
    .append($("<th>").text("B3"))
    .append($("<th>").text("Jammer"))
    .appendTo(thead);

  var tbody = $("<tbody>").appendTo(table);

//FIXME - this should be dynamic instead of statically fixed to period 1 and 2
  $.each([ "1", "2" ], function() {
    var p = String(this);
    $sb("Stats.Interpreted.Period("+p+")")
      .live("add:Jam", function(e, jam) { addEditJamLineupsLine(tbody, jam, team); })
      .live("remove:Jam", function(e, jam) { removeEditJamLineupsLine(tbody, jam); })
      .children("Jam").each(function() { addEditJamLineupsLine(tbody, $sb(this), team); });
  });

  return table;
}

function addEditJamLineupsLine(table, jam, teamN) {
  var team = jam.$sb("Team("+teamN+")");
  var periodN = $sb(jam.parent()).$sbId;
  var tr = $("<tr>")
    .attr("data-period", periodN)
    .attr("data-jam", jam.$sbId)
    .append($("<td>").text(jam.$sbId));

  var numberShow = $("#team"+teamN+"SkaterNumberShow");
  var nameShow = $("#team"+teamN+"SkaterNameShow");
  var allShow = $("#team"+teamN+"SkaterAllShow");
  $.each([ "Pivot", "Blocker1", "Blocker2", "Blocker3", "Jammer" ], function() {
    var position = team.$sb("Position("+this+")");
    var td = $("<td>").click(function() { createEditJamLineupsPositionDialog(position); });
    var number = position.$sb("Number").$sbElement("<a>")
      .addClass("team"+teamN+"SkaterNumber team"+teamN+"SkaterAll")
      .hide()
      .appendTo(td);
    var separator = $("<span> : </span>")
      .addClass("team"+teamN+"SkaterAll")
      .hide()
      .appendTo(td);
    var name = position.$sb("Name").$sbElement("<a>")
      .addClass("team"+teamN+"SkaterName team"+teamN+"SkaterAll")
      .hide()
      .appendTo(td);
    tr.append(td);

    if (numberShow[0] ? numberShow[0].checked : true) number.show();
    if (nameShow[0] ? nameShow[0].checked : false) name.show();
    if (allShow[0] ? allShow[0].checked : false) { number.show(); name.show(); separator.show(); }
  });

  if (!table.children("tr[data-period="+periodN+"]").length)
    tr.prepend($("<td>").text(periodN));

  _windowFunctions.appendSorted(table, tr, function(a, b) {
    return ($(a).attr("data-jam") > $(b).attr("data-jam"));
  });

  var rows = table.children("tr[data-period="+periodN+"]");
  rows.first().children("td").first().attr("rowspan", rows.length);
}

function removeEditJamLineupsLine(table, jam) {
  alert("Not implemented!  Implement remove jam.");
}

function createEditJamLineupsPositionDialog(position) {
  var p = $sb(position.parent().parent().parent()).$sbId;
  var j = $sb(position.parent().parent()).$sbId;
  var t = $sb(position.parent()).$sbId;
  var table = $("<table>")
  var tr = $("<tr>").appendTo(table);
  var manualSet = $("<input type='checkbox'>").attr("id", $sb().$sbNewUUID());
  $("<td>").attr("colspan", "3")
    .append($("<label>").attr("for", manualSet.attr("id")).html("Manually Set : "))
    .append(manualSet)
    .appendTo(tr);

  tr = $("<tr>").appendTo(table);
  $("<a>").html("Name").appendTo($("<td>").appendTo(tr));
  var nameInput = position.$sb("Name").$sbControl("<input type='text' size='20'>")
    .appendTo($("<td>").appendTo(tr));
  var nameSelect = position.$sb("Name").$sbControl("<select>", {
    sbcontrol: {
      sbSetAttrs: { clearOnEmpty: "true" }
    }, sbelement: {
      firstOption: "",
      optionParent: $sb("ScoreBoard.Team("+t+")"),
      optionChildName: "Skater",
      optionNameElement: "Name"
    }
  }).appendTo($("<td>").appendTo(tr));

  tr = $("<tr>").appendTo(table);
  $("<a>").html("Number").appendTo($("<td>").appendTo(tr));
  var numberInput = position.$sb("Number").$sbControl("<input type='text' size='10'>")
    .appendTo($("<td>").appendTo(tr));
  var numberSelect = position.$sb("Number").$sbControl("<select>", {
    sbcontrol: {
      sbSetAttrs: { clearOnEmpty: "true" }
    }, sbelement: {
      firstOption: "",
      optionParent: $sb("ScoreBoard.Team("+t+")"),
      optionChildName: "Skater",
      optionNameElement: "Number",
      compareOptions: function(a, b) { return _windowFunctions.numCompareByAttr("value", a, b); }
    }
  }).appendTo($("<td>").appendTo(tr));

  if ((nameSelect.val() != nameInput.val()) || (numberSelect.val() != numberInput.val()))
    manualSet.click();

  manualSet.bind("click", function() {
    nameSelect.add(numberSelect).prop("disabled", this.checked);
    nameInput.data("sbcontrol").sbSetAttrs = numberInput.data("sbcontrol").sbSetAttrs = 
      (this.checked ? { manual: "true" } : undefined );
  });

  $("<div>").append(table).dialog({
      title: "Team "+t+": "+$sb("ScoreBoard.Team("+t+").Name").$sbGet()+"<br>Period "+p+", Jam "+j+" : "+position.$sbId,
      width: 600,
      modal: true,
      close: function(event, ui) { $(this).dialog("destroy").remove(); },
      buttons: {
        'Close': function() { $(this).dialog("close"); }
      }
    });
}
