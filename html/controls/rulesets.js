initialize();
var rulesets = array();
var definitions = array();

function loadRulesets(displayID) {
	var rs = $(".rulesets div");
	Rulesets.List(function (data) {
		$("#new_parent").empty();
		rs.empty();
		rulesets = data;
		rs.append(displayRulesetTree(""));
		if (displayID != null)
			displayRuleset(displayID);
	});
}

function displayRulesetTree(parentID) {
	var list = null;
	 	$.each(rulesets, function(idx, val) {
			if (val.parent == parentID) {
				if (list == null)
					list = $("<ul>");

				$("<option>")
					.prop("value", val.id)
					.append(val.name)
					.appendTo($("#new_parent"));
				$("<li>")
					.append(
						$("<a>")
							.attr("href", "#")
							.click(function() {
								displayRuleset(val.id);
							}
							).append($("<span>").append(val.name)
						)
					)
					.append(displayRulesetTree(val.id))
					.appendTo(list);
			}
	 	});
	return list;
}

function loadDefinitions() {
	var d = $(".definitions .rules");
	Rulesets.ListDefinitions(function (data) {
		definitions = data;
		var findSection = function(def) {
			var newSection = function(def) {
				var name = def.group;
				var section = $("<div>")
					.addClass("section")
					.attr("group", def.group)
				if (def.subgroup != null) {
					name = name + " - " + def.subgroup;
					section.attr("subgroup", def.subgroup);
				}

				section.append($("<div>").addClass("header").append(name));

				return section;
			};
			var section = null;

			var children = d.find(".section");
			$.each(children, function (idx, s) {
				if (section != null)
					return;
				s = $(s);
				if (s.attr("group") == def.group && s.attr("subgroup") == def.subgroup) {
					section = s;
				}
				if (s.attr("group") > def.group || (s.attr("group") == def.group && s.attr("subgroup") > def.subgroup)) {
					section = newSection(def);
					section.insertBefore(s);
				}
			});
			if (section == null) {
				section = newSection(def);
				d.append(section);
			}

			var div = $("<div>")
				.addClass("definition")
				.attr("fullname", def.fullname);
			section.append(div);
			return div;
		};
	 	$.each(definitions, function(idx, def) {
			var div = findSection(def);
			$("<div>").addClass("name").appendTo(div)
				.append($("<label>").append($("<input>").attr("type", "checkbox").prop("checked", true).click(definitionOverride)).append(def.name));

			var value = $("<div>").addClass("value").appendTo(div);
			value.append($("<span>").addClass("inherit"));
			if (def.type == "Boolean") {
				var select = $("<select>").addClass("override");
				select.append($("<option>").attr("value", def.trueValue).append(def.trueValue));
				select.append($("<option>").attr("value", def.falseValue).append(def.falseValue));

				select.appendTo(value);
			} else if (def.type == "Integer") {
				value.append($("<input>").addClass("override").attr("type", "text"));
			} else if (def.type == "Long") {
				value.append($("<input>").addClass("override").attr("type", "text"));
			} else if (def.type == "Time") {
				value.append($("<input>").addClass("override").attr("type", "text"));
			} else {
				// Treat as string
				value.append($("<input>").addClass("override").attr("type", "text"));
			}
	 	});
	});
}

function initialize() {
	loadRulesets();
	loadDefinitions();

	$(".New").click(New);
	$(".Delete").click(Delete);
	$(".Update").click(Update);
	$(".Cancel").click(Cancel);
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
		if (rs.id == id) {
			activeRuleset = rs;
			var definitions = $(".definitions");
			// definitions.find("h1").empty().append(rs.name);

			$("#name").val(rs.name);

			if (!rs.immutable)
				$(".definition *").prop("disabled", rs.immutable);

			$.each(rs.inherit_values, function(key, val) {
				setDefinition(key, val, true);
			});
			$.each(rs.values, function(key, val) {
				setDefinition(key, val, false);
			});

			if (rs.immutable) {
				$(".definition *").prop("disabled", rs.immutable);
				$(".Update, .Delete").hide();
			} else {
				$(".Update, .Delete").show();
			}
			definitions.show();
		}
	});
}

function New() {
	var o = {
		parent: $("#new_parent").val(),
		name: $("#new_name").val()
	};
	Rulesets.New(o, function(rs) {
		loadRulesets(rs.id);
	});
}

function Update() {
	if (!activeRuleset.immutable) {
		var o = {
			name: $("#name").val(),
			id: activeRuleset.id,
			values: {},
		};
		var values = new Array();
		$.each(definitions, function(idx, val) {
			var def = $(".definition[fullname='" + val.fullname + "']");
			if (def.find(".name input").prop("checked")) {
				var input = def.find(".value>input").prop("value");
				var select = def.find(".value>select").val();
				if (input != null)
					o.values[val.fullname] = input;
				if (select != null)
					o.values[val.fullname] = select;
			}
		});
		Rulesets.Update(o, function() {
			loadRulesets();
			$(".definitions").hide();
		}, function(err) {
			alert(err.responseText);
		});
	} else {
		$(".definitions").hide();
	}
}

function Delete() {
	if (!activeRuleset.immutable) {
		var o = {
			id: activeRuleset.id,
		};
		Rulesets.Delete(o, function() {
			loadRulesets();
			$(".definitions").hide();
		}, function(err) {
			alert(err.responseText);
		});
	} else {
		$(".definitions").hide();
	}
}

function Cancel() {
	$(".definitions").hide();
}

function setDefinition(key, value, inherited) {
	var def = $(".definition[fullname='" + key + "']");
	def.find(".value>input").prop("value", value);
	def.find(".value>select").val(value);
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
