var rulesets = {};
var activeRuleset = null;
var definitions = {};
initialize();

function loadRulesets() {
	var rs = $(".rulesets div");
	$("#new_parent").empty();
	rs.empty();
	rs.append(displayRulesetTree(""));
	if (activeRuleset != null) {
		displayRuleset(activeRuleset.Id);
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
					.appendTo($("#new_parent"));
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
	var d = $(".definitions .rules");
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
	WS.Register(['ScoreBoard.Rulesets.RuleDefinition'], {triggerBatchFunc: function() {
		definitions = {};
		for (var prop in WS.state) {
			var k = WS._enrichProp(prop)
			if (k.RuleDefinition) {
				definitions[k.RuleDefinition] = definitions[k.RuleDefinition] || {};
				definitions[k.RuleDefinition][k.field] = WS.state[prop];
				definitions[k.RuleDefinition]['Fullname'] = k.RuleDefinition;
				definitions[k.RuleDefinition]['Group'] = k.RuleDefinition.split(".")[0];
				definitions[k.RuleDefinition]['Name'] = k.RuleDefinition.split(".")[1];
			}
		}
		loadDefinitions();
	}});

	// If the definitions change, we'll have to redraw the rulesets too.
	WS.Register(['ScoreBoard.Rulesets.RuleDefinition', 'ScoreBoard.Rulesets.Ruleset'], {triggerBatchFunc: function() {
		rulesets = {};
		for (var prop in WS.state) {
			var k = WS._enrichProp(prop)
			if (k.Rulesets != null && k.Ruleset != null) {
				rulesets[k.Ruleset] = rulesets[k.Ruleset] || {Rules: {}};
				if (k.field == "Rule") {
					rulesets[k.Ruleset].Rules[k.Rule] = WS.state[prop]
				} else {
					rulesets[k.Ruleset][k.field] = WS.state[prop];
				}
				rulesets[k.Ruleset].Immutable = (k.Ruleset == "00000000-0000-0000-0000-000000000000");
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
		loadRulesets();
	}});


	$(".New").click(New);
	$(".Delete").click(Delete);
	$(".Update").click(Update);
	$(".Cancel").click(Cancel);

	WS.Connect();
	WS.AutoRegister();
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
	var rs = rulesets[id];
	if (!rs) return;
	activeRuleset = rs;
	var definitions = $(".definitions");

	$("#name").val(rs.Name);

	if (!rs.Immutable)
		$(".definition *").prop("disabled", rs.Immutable);

	$.each(rs.Inherited, function(key, val) {
		setDefinition(key, val, true);
	});
	$.each(rs.Rules, function(key, val) {
		setDefinition(key, val, false);
	});

	if (rs.Immutable) {
		$(".definition *").prop("disabled", rs.Immutable);
		$(".Update, .Delete").hide();
	} else {
		$(".Update, .Delete").show();
	}
	definitions.show();
}

function New() {
	var uuid;
	do {
		uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {var r = Math.random()*16|0,v=c=='x'?r:r&0x3|0x8;return v.toString(16);}).toUpperCase();
	} while (rulesets[uuid]);
	WS.Set("ScoreBoard.Rulesets.Ruleset("+uuid+").Name", $("#new_name").val());
	WS.Set("ScoreBoard.Rulesets.Ruleset("+uuid+").ParentId", $("#new_parent").val());
	$("#new_name").val("");
	activeRuleset = uuid;
}

function Update() {
	$(".definitions").hide();
	if (!activeRuleset.immutable) {
		WS.Set("ScoreBoard.Rulesets.Ruleset(" + activeRuleset.Id + ").Name", $("#name").val());
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
	$(".definitions").hide();
	if (!activeRuleset.immutable) {
		WS.Set("ScoreBoard.Rulesets.Ruleset(" + activeRuleset.Id + ")", null);
	}
}

function Cancel() {
	$(".definitions").hide();
}

function setDefinition(key, value, inherited) {
	var def = $(".definition[fullname='" + key + "']");
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
//# sourceURL=controls\rulesets.js
