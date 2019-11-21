function createTeamsTab(tab) {
	var table = $("<table>").attr("id", "Teams").appendTo(tab);
	
	table.append($("<tr><td/></tr>").addClass("Selection NoneSelected"))
		.append($("<tr><td/></tr>").addClass("Team"));
	_crgUtils.createRowTable(2).appendTo(table.find("tbody>tr.Selection>td")).addClass("Selection");

	var selectTeam = $("<select>").appendTo(table.find("table.Selection td:eq(0)"));
	$("<option value=''>No Team Selected</option>").appendTo(selectTeam);
	$("<option value='(Current Team 1)'>Current Team 1</option>").appendTo(selectTeam);
	$("<option value='(Current Team 2)'>Current Team 2</option>").appendTo(selectTeam);
	var newTeamName = $("<input type='text' size='30'/>")
		.appendTo(table.find("table.Selection td:eq(1)"));
	var createNewTeam = $("<button>").text("New Team").attr("disabled", true).button()
		.appendTo(table.find("table.Selection td:eq(1)"));
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
		.appendTo(table.find(".Team td"));
	var controlTable = _crgUtils.createRowTable(5).appendTo(teamTable.find("tr.Control>td")).addClass("Control");
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
			logoSelect.change();
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
		id = id || newUUID();
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
	newSkaterFlags.append($("<option>").attr("value", "ALT").text("Not Skating"));
	newSkaterFlags.append($("<option>").attr("value", "C").text("Captain"));
	newSkaterFlags.append($("<option>").attr("value", "AC").text("Alt Captain"));
	newSkaterFlags.append($("<option>").attr("value", "BC").text("Bench Alt Captain"));
	var pasteHandler = function(e){
		var text = e.originalEvent.clipboardData.getData("text");
		var lines = text.split("\n");
		if (lines.length <= 1) {
			// Not pasting in many values, so paste as usual.
			return true;
		}

		// Treat as a tab-seperated roster.
		var knownNumbers = {};
		teamTable.find('.Skater').map( function(_, n) {
			n = $(n)
			knownNumbers[n.attr("skaternum")] = n.attr("skaterid");
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
			// For a current team, could be a penalty or position.
			if (k.parts.length != 4 || k.parts[2] != "Skater") return;
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
				skaterFlags.append($("<option>").attr("value", "ALT").text("Not Skating"));
				skaterFlags.append($("<option>").attr("value", "C").text("Captain"));
				skaterFlags.append($("<option>").attr("value", "AC").text("Alt Captain"));
				skaterFlags.append($("<option>").attr("value", "BC").text("Bench Alt Captain"));
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
		table.find("tr.Selection").toggleClass("NoneSelected", teamId == "");
		teamTable.toggleClass("Hide", teamId == "");
		removeTeam.button("option", "disabled", isCurrentTeam() || teamId == "");
		skatersTable.addClass("Empty");
		skatersTable.children("tbody").empty();
		updateSkaterCount();  // In case there's no skaters.
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
