/**
 * Copyright (C) 2008-2013 Mr Temper <MrTemper@CarolinaRollergirls.com>, Rob Thomas, and WrathOfJon <crgscorespam@sacredregion.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

XML_ELEMENT_SELECTOR = "Pages,ScoreBoard";

$.each( [ "1", "2" ], function(i, team) {
	$sb("ScoreBoard.Team(" + team + ").Name").$sbElement(".Team" + team + ".Name");
	var an = $(".Team" + team + ".AlternateName");
	var addAlternateName = function(event, node) {
		if (node.$sbId == "overlay") {
			node.$sb("Name").$sbBindAndRun("sbchange", function(event,val) {
				if (!an.hasClass("isFocused"))
					an.val(val);
			});
		}
	}

	var removeAlternateName = function(event, node) {
		if (node.$sbId.substring(0, 7) == "overlay") {
			an.val("");
		}
	}

	$sb("ScoreBoard.Team(" + team + ")").$sbBindAddRemoveEach("AlternateName", addAlternateName, removeAlternateName);
	var anUpdate = function() {
		var val = $.trim(an.val());
		if (an.hasClass("isFocused")) {
			var path = "ScoreBoard.Team(" + team + ").AlternateName(overlay)";
			if (val == null || val == "")
				$sb(path).$sbRemove();
			else
				$sb(path + ".Name").$sbSet(val);
		}
	};
	an.bind({
		change: anUpdate,
		keyup: anUpdate,
		focus: function() { $(this).addClass("isFocused"); },
		blur: function() { $(this).removeClass("isFocused"); },
	});

	var addColor = function(event, node) {
		if (node.$sbId.substring(0, 7) == "overlay") {
			var elem = $(".Team" + team + "." + node.$sbId);
			node.$sb("Color").$sbBindAndRun("sbchange", function(event,val) {
				elem.spectrum("set", val);
			});
		}
	}

	var removeColor = function(event, node) {
		if (node.$sbId.substring(0, 7) == "overlay") {
			var elem = $(".Team" + team + "." + node.$sbId);
			elem.spectrum("set", "");
		}
	}

	$.each([ "fg", "bg", "glow" ], function(i, type) {
		var color = $(".Team" + team + ".overlay_" + type);
		_crgUtils.makeColorPicker(color).addClass("ColorPicker");
		var cUpdate = function() {
			var val = $.trim(color.spectrum("get"));
			var path = "ScoreBoard.Team(" + team + ").Color(overlay_" + type + ")";
			if (val == null || $.trim(val) == "")
				$sb(path).$sbRemove();
			else
				$sb(path + ".Color").$sbSet(val);
		};
		color.bind({
			change: cUpdate,
			keyup: cUpdate
		});
	});
	$sb("ScoreBoard.Team(" + team + ")").$sbBindAddRemoveEach("Color", addColor, removeColor);
});

$sb("Pages.Page(Overlay).Logo").$sbControl($(".Logo"), { sbelement: {
	optionParent: "Images.Type(teamlogo)",
	optionChildName: "Image",
	optionNameElement: "Name",
	optionValueElement: "Src",
	firstOption: { text: "No Logo", value: "" }
} });
