function createRulesetsTab(tab) {
	initialize();
	var rulesets = {};
	var definitions = {};
	var knownRulesets = {};

	function loadRulesets(displayId) {
		var rs = tab.find(">.rulesets>div");
		tab.find("#new_parent").empty();
		rs.empty();
		rs.append(displayRulesetTree(""));
		if (displayId != null) {
			displayRuleset(displayId);
		}
	}

	function displayRulesetTree(parentId) {
		var list = null;
		 	$.each(rulesets, function(idx, val) {
				if (val.ParentId == parentId) {
					if (list == null)
						list = $("<ul>");

					$("<option>")
						.prop("value", val.Id)
						.append(val.Name)
						.appendTo(tab.find("#new_parent"));
					$("<li>")
						.append(
							$("<a>")
								.attr("href", "#")
								.click(function() {
									displayRuleset(val.Id);
								}
								).append($("<span>").append(val.Name)
							)
						)
						.append(displayRulesetTree(val.Id))
						.appendTo(list);
				}
		 	});
		return list;
	}

	function loadDefinitions() {
		var d = tab.find(">.definitions>.rules");
		d.empty();
		var findSection = function(def) {
			var newSection = function(def) {
				var name = def.Group;
				var section = $("<div>")
					.addClass("section folded")
					.attr("group", def.Group)

				section.append($("<div>").addClass("header")
					.click(function(e) {
						section.toggleClass("folded");
					}).append(name));

				return section;
			};
			var section = null;

			var children = d.find(".section");
			$.each(children, function (idx, s) {
				if (section != null)
					return;
				s = $(s);
				if (s.attr("group") == def.Group) {
					section = s;
				}
			});
			if (section == null) {
				section = newSection(def);
				d.append(section);
			}

			var div = $("<div>")
				.addClass("definition")
				.attr("fullname", def.Fullname);
			section.append(div);
			return div;
		};
		// Keep them in the same order they are in the Java code.
		var sortedValues = $.map(definitions, function(v) {
			return v;
		}).sort(function(a, b){return a.Index - b.Index});
		$.each(sortedValues, function(idx, def) {
			var div = findSection(def);
			var tooltiptext = null;
			if (def.Description != "") {
				tooltiptext = $("<span>").addClass("tooltiptext").append(def.Description);
			}
			$("<div>").addClass("name").appendTo(div)
				.append($("<label>").append($("<input>").attr("type", "checkbox").prop("checked", true).click(definitionOverride))
				.append(def.Name).append(tooltiptext));

			var value = $("<div>").addClass("value").appendTo(div);
			value.append($("<span>").addClass("inherit"));
			if (def.Type == "Boolean") {
				var select = $("<select>").addClass("override");
				select.append($("<option>").attr("value", true).append(def.TrueValue));
				select.append($("<option>").attr("value", false).append(def.FalseValue));

				select.appendTo(value);
			} else if (def.Type == "Integer") {
				value.append($("<input>").addClass("override").attr("type", "text"));
			} else if (def.Type == "Long") {
				value.append($("<input>").addClass("override").attr("type", "text"));
			} else if (def.Type == "Time") {
				value.append($("<input>").addClass("override").attr("type", "text"));
			} else {
				// Treat as string
				value.append($("<input>").addClass("override").attr("type", "text"));
			}
		});
	}

	function initialize() {
		tab.append($('<div>').addClass('rulesets')
			.append($('<h1>').text('Rulesets'))
			.append($('<div>')).append($('<br>')).append($('<br>'))
			.append($('<table>')
				.append($('<tr>')
					.append($('<th>').attr('colspan', '2').text('New Ruleset')))
				.append($('<tr>')
					.append($('<td>').text('Name:'))
					.append($('<td>').append($('<input type="text">').attr('id', 'new_name'))))
					.append($('<tr>')
						.append($('<td>').text('Parent:'))
						.append($('<td>').append($('<select>').attr('id', 'new_parent'))))
				.append($('<tr>').append($('<td>').attr('colspan', '2')
						.append($('<button>').addClass('New').text('Create').click(New))))));
		tab.append($('<div>').addClass('definitions')
			.append($('<p>')
					.append($('<button>').addClass('Cancel').text('Cancel').click(Cancel))
					.append($('<button>').addClass('Update').text('Update').click(Update))
					.append($('<button>').addClass('Delete').text('Delete').click(Delete)))
			.append($('<input type="text">').attr('id', 'name').attr('size', '40'))
			.append($('<div>').addClass('rules'))
			.append($('<p>')
					.append($('<button>').addClass('Cancel').text('Cancel').click(Cancel))
					.append($('<button>').addClass('Update').text('Update').click(Update))
					.append($('<button>').addClass('Delete').text('Delete').click(Delete))));
		tab.children('.definitions').hide();
		
		WS.Register(['ScoreBoard.Rulesets.RuleDefinition'], {triggerBatchFunc: function() {
			definitions = {};
			for (var prop in WS.state) {
				if (WS.state[prop] == null) {
					continue;
				}
				var re = /ScoreBoard.Rulesets.RuleDefinition\(((\w+)\.(\w+))\).(\w+)/;
				var m = prop.match(re);
				if (m != null) {
					var key = m[4];
					definitions[m[1]] = definitions[m[1]] || {};
					definitions[m[1]][key] = WS.state[prop];
					definitions[m[1]]['Fullname'] = m[1];
					definitions[m[1]]['Group'] = m[2];
					definitions[m[1]]['Name'] = m[3];
				}
			}
			loadDefinitions();
		}});

		// If the definitions change, we'll have to redraw the rulesets too.
		WS.Register(['ScoreBoard.Rulesets.RuleDefinition', 'ScoreBoard.Rulesets.Ruleset'], {triggerBatchFunc: function() {
			rulesets = {};
			for (var prop in WS.state) {
				if (WS.state[prop] == null) {
					continue;
				}
				re = /ScoreBoard.Rulesets.Ruleset\(([^)]+)\)\.(\w+)(?:\(([^)]+)\))?/;
				m = prop.match(re);
				if (m != null) {
					rulesets[m[1]] = rulesets[m[1]] || {Rules: {}};
					var key = m[2];
					if (key == "Rule") {
						rulesets[m[1]].Rules[m[3]] = WS.state[prop]
					} else {
						rulesets[m[1]][key] = WS.state[prop];
					}
					rulesets[m[1]].Immutable = (m[1] == "00000000-0000-0000-0000-000000000000");
				}
			}
	 
			// Populate inherited values from parents.
			$.each(rulesets, function(idx, rs) {
				rs.Inherited = {};
				var pid = rs.ParentId;
				while (rulesets[pid]) {
					$.each(rulesets[pid].Rules, function(name, value) {
						if (rs.Inherited[name] == undefined) {
							rs.Inherited[name] = value;
						}
					});
					pid = rulesets[pid].ParentId;
				}
			});
			var toLoad = null;
			if (!$.isEmptyObject(knownRulesets)) {
				// A new ruleset was added by us, find it and display it.
				$.each(rulesets, function(id) {
					if (!knownRulesets[id]) {
						toLoad = id;;
					}
				});
				knownRulesets = {};
			}
			loadRulesets(toLoad);
		}});
	}

	function definitionOverride(e) {
		var elem = $(e.target);
		var value = elem.parents(".definition").find(".value");
		if (!elem.prop("checked")) {
			value.children(".inherit").show();
			value.children(".override").hide();
		} else {
			value.children(".inherit").hide();
			value.children(".override").show();
		}
	}

	function displayRuleset(id) {
		$.each(rulesets, function(idx, rs) {
			if (rs.Id == id) {
				activeRuleset = rs;
				var definitions = tab.children(".definitions");

				definitions.find("#name").val(rs.Name);

				if (!rs.Immutable)
					definitions.find(".definition *").prop("disabled", rs.Immutable);

				$.each(rs.Inherited, function(key, val) {
					setDefinition(key, val, true);
				});
				$.each(rs.Rules, function(key, val) {
					setDefinition(key, val, false);
				});

				if (rs.Immutable) {
					definitions.find(".definition *").prop("disabled", rs.Immutable);
					definitions.find(".Update, .Delete").hide();
				} else {
					definitions.find(".Update, .Delete").show();
				}
				definitions.show();
			}
		});
	}

	function New() {
		// Note what rulesets we know about so we can determine
		// the id of the new one when we get an update.
		knownRulesets = {};
		$.each(rulesets, function(id) {
			knownRulesets[id] = true;
		});
		var uuid;
		do {
			uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {var r = Math.random()*16|0,v=c=='x'?r:r&0x3|0x8;return v.toString(16);}).toUpperCase();
		} while (knownRulesets[uuid]);
		WS.Set("ScoreBoard.Rulesets.Ruleset("+uuid+").Name", tab.find("#new_name").val());
		WS.Set("ScoreBoard.Rulesets.Ruleset("+uuid+").ParentId", tab.find("#new_parent").val());
		$("#new_name").val("");
	}

	function Update() {
		tab.children(".definitions").hide();
		if (!activeRuleset.immutable) {
			WS.Set("ScoreBoard.Rulesets.Ruleset(" + activeRuleset.Id + ").Name", tab.find("#name").val());
			$.each(definitions, function(idx, val) {
				var def = $(".definition[fullname='" + val.Fullname + "']");
				var value = null;
				if (def.find(".name input").prop("checked")) {
					var input = def.find(".value>input").prop("value");
					var select = def.find(".value>select").val();
					if (input != null)
						value = input;
					if (select != null)
						value = select;
				}
				WS.Set("ScoreBoard.Rulesets.Ruleset(" + activeRuleset.Id + ").Rule(" + val.Fullname + ")", value);
			});
		}
	}

	function Delete() {
		tab.children(".definitions").hide();
		if (!activeRuleset.immutable) {
			WS.Set("ScoreBoard.Rulesets.Ruleset(" + activeRuleset.Id + ")", null);
		}
	}

	function Cancel() {
		tab.children(".definitions").hide();
	}

	function setDefinition(key, value, inherited) {
		var def = tab.find(".definition[fullname='" + key + "']");
		def.find(".value>input").prop("value", value);
		def.find(".value>select").val(value);
		value = def.find(".value>select>option:selected").text() || value;
		if (inherited) {
			def.find(".name input").prop("checked", false);
			def.find(".value .inherit").show().empty().append(value);
			def.find(".value .override").hide();
		} else {
			def.find(".name input").prop("checked", true);
			def.find(".value .inherit").hide();
			def.find(".value .override").show();
		}
	}
}