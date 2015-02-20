initialize();
var rulesets = array();
var definitions = array();

function initialize() {
	var rs = $(".rulesets ul");
	$rulesets.List(function (data) {
		rulesets = data;
	 	$.each(rulesets, function(idx, val) {
			$("<li>")
				.append(
					$("<a>")
						.click(function() {
							displayRuleset(val.id);
						}
						).append($("<span>").append(val.name)
					)
				).appendTo(rs);
	 	});
	});

	var d = $(".definitions .rules");
	$rulesets.ListDefinitions(function (data) {
		definitions = data;
		var findSection = function(def) {
			var newSection = function(def) {
				var name = def.group;
				if (def.subgroup != "")
					name = name + " - " + def.subgroup;
				var section = $("<div>")
					.addClass("section")
					.attr("group", def.group)
					.attr("subgroup", def.subgroup);
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
					section.before(s);
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

	$(".Save").click(save);
	$(".Cancel").click(cancel);
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
			console.log("Displaying RuleSet id: " + id + "  name: " + rs.name);

			activeRuleSet = rs;
			var definitions = $(".definitions");
			var name = rs.name;
			if (rs.parent != "")
				name = name + " (" + rs.parent + ")";
			definitions.find("h1").empty().append(name);

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
				$(".Save").hide();
			} else {
				$(".Save").show();
			}
			definitions.show();
		}
	});
}

function save() {
	$(".definitions").hide();
}

function cancel() {
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
