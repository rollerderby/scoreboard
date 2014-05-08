
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */


_crgScoreBoardControl = {
	create: function(sbElement, type, attributes, className) {
//FIXME - consolidate _crgScoreBoard.create() and this better.
		/* specifying attributes is optional */
		if (typeof attributes == "string") {
			className = attributes;
			attributes = {};
		} else if (!attributes)
			attributes = {};
		attributes = $.extend(true, {}, attributes); // Keep the original attributes object unchanged
		var sbcontrol = $.extend(true, {}, attributes.sbcontrol);
		var sbelement = $.extend(true, {}, attributes.sbelement);
		delete attributes.sbcontrol;
		delete attributes.sbelement;
		var controls = $(type);
		var allControls = controls.find("*").andSelf();
		allControls.data("sbcontrol", sbcontrol).data("sbelement", sbelement).addClass(className)
			.attr($.extend({ "data-sbcontrol": _crgScoreBoard.getPath(sbElement), "data-UUID": _crgScoreBoard.newUUID() }, attributes))
			.each(function(index) {
				_crgScoreBoardControl.addControlFunction($(this), sbElement, index);
			});
		_crgScoreBoard.setupScoreBoardElement(sbElement, allControls, sbelement);
		return controls;
	},

	getControlGroup: function(control, filter, excludeControl) {
		var group = $("[data-UUID="+control.attr("data-UUID")+"]");
		if (filter)
			group = group.filter(filter);
		if (excludeControl)
			group = group.not(control);
		return group;
	},

	addControlFunction: function(c, sbElement, index) {
		var sbC = (c.data("sbcontrol") || {});
		var sbE = (c.data("sbelement") || {});
		var getGroup = function(from, filter, exclude) {
			if (typeof from == "boolean") {
				exclude = from;
				from = null;
			}
			if (typeof filter == "boolean") {
				exclude = filter;
				filter = null;
			}
			if (typeof from == "string") {
				filter = from;
				from = null;
			}
			from = (from || c);
			return _crgScoreBoardControl.getControlGroup(from, filter, exclude);
		};

		c.bind({
			focus: function() { $(this).addClass("isFocused"); },
			blur: function() { $(this).removeClass("isFocused"); },
			mouseenter: function() { $(this).addClass("isMouseFocused"); },
			mouseleave: function() { $(this).removeClass("isMouseFocused"); }
		});

		var controlValueToElementValue = function(value) {
			var p = sbC.prefix, s = sbC.suffix, u = sbC.useNumber, t = sbC.trueString, f = sbC.falseString;
			if (sbC.convert) value = sbC.convert.call(c, value);
			if ((typeof t == "string") && (String(value).toLowerCase() == "true")) value = t; else if ((typeof f == "string") && (String(value).toLowerCase() == "false")) value = f;
			if (u) value = String(Number(value) || 0);
			if (p && !(value==null)) value = p + value;
			if (s && !(value==null)) value = value + s;
			return value;
		};
		var setElementValue = function(value, updateNow) {
			if (!c.data("sbcontrol").delayupdate || updateNow)
				sbElement.$sbSet(controlValueToElementValue(value), sbC.sbSetAttrs);
		};

		var elementValueToControlValue = function(value) {
			var p = sbC.prefix, s = sbC.suffix, u = sbC.useNumber, t = sbC.trueString, f = sbC.falseString;
			if ((typeof t == "string") && (t === value)) value = "true"; else if ((typeof f == "string") && (f === value)) value = "false";
			if (p && $.string(value||'').startsWith(p)) value = value.substr(p.length);
			if (s && $.string(value||'').endsWith(s)) value = value.substr(0, value.length - s.length);
			if (u) value = String(Number(value) || 0);
			return value;
		};
		var setControlValue = function(value) {
			if (!sbC.noSetControlValue)
				_crgScoreBoard.setHtmlValue(sbElement, elementValueToControlValue(value), c);
		};

		var updateControlIfUnfocused = function(value) {
			/* NOTE: only checking isMouseFocused for :button elements (currently) */
			if (!getGroup().hasClass("isFocused") && !getGroup(":button").hasClass("isMouseFocused"))
				setControlValue(value);
		};

		if (sbC.button && c.is("input:checkbox,input:radio,input:submit,input:reset,:button")) {
			if (typeof sbC.button == "boolean")
				c.button();
			else
				c.button(sbC.button);
		}

// FIXME - move all setControlValues out of individual sections;
//				 really this all needs to be cleaned up
//				 and specifically it needs a documented API
		if (c.is("input:text,input[type='number'],input:password,textarea")) {
			sbElement.bind("sbchange", function(event, value) { updateControlIfUnfocused(value); });
			setControlValue(sbElement.$sbGet());
			c.bind("mouseup keyup change", function() { setElementValue(c.val()); });
			c.bind("blur", function() {
// FIXME - can we fix this?
// this defers the update, to give the next focused element time to get focus,
// in case it's part of this group.	 Otherwise this text area will be cleared out even if
// changing focus over to a button to actually submit.
				setTimeout(function () { updateControlIfUnfocused(sbElement.$sbGet()); });
			});
		} else if (c.is("a")) {
			sbElement.bind("sbchange", function(event, value) { setControlValue(value); });
			setControlValue(sbElement.$sbGet());
		} else if (c.is("input:button,button")) {
			sbElement.bind("sbchange", function(event, value) { setControlValue(value); });
			setControlValue(sbElement.$sbGet());
			var buttonClick = function() {
//FIXME - need a generic indicator to prevent action instead of using .KeyControl.Editing
				if (c.is(".KeyControl.Editing")) // If this button is a KeyControl that's being edited, ignore clicks.
					return;
				var associateText = getGroup("input:text,input[type='number'],input:password,textarea");
				if (associateText.length) {
					setElementValue(associateText.val(), true);
					associateText.val("");
					/* Need to include all children, e.g. spans used by jquery-ui */
					c.find("*").andSelf().blur().mouseleave();
				} else if (c.data("sbcontrol").getButtonValue)
//FIXME - not sure if I like getButtonValue API
					setElementValue(c.data("sbcontrol").getButtonValue.call(c));
				else if (c.val())
					setElementValue(c.val());
			};
			c.bind("click", buttonClick);
		} else if (c.is("input:checkbox")) {
			sbElement.bind("sbchange", function(event, value) { setControlValue(value); });
			setControlValue(sbElement.$sbGet());
			c.bind("change", function() { setElementValue(String(this.checked)); this.checked = !this.checked; });
		} else if (c.is("input:radio")) {
			if (!c.attr("name"))
				c.attr("name", c.attr("data-UUID")+"-name");
			sbElement.bind("sbchange", function(event, value) { setControlValue(value); });
			setControlValue(sbElement.$sbGet());
			c.bind("change", function() { setElementValue($(this).val()); });
		} else if (c.is("label")) {
			// This requires the target checkbox or radio to immediately follow *or precede* its label
			var target = c.prev("input:checkbox,input:radio");
			if (!target.length || target.attr("id"))
				target = c.next("input:checkbox,input:radio");
			if (!target.attr("id"))
				target.attr("id", sbElement.$sbNewUUID());
			if (!c.attr("for"))
				c.attr("for", target.attr("id"));
		} else if (c.is("select")) {
			_crgUtils.setupSelect(c);
			sbElement.bind("sbchange", function(event, value) { updateControlIfUnfocused(value); });
			setControlValue(sbElement.$sbGet());
			c.bind("change", function() { setElementValue(c.find("option:selected").val()); c.blur(); });
			c.bind("blur", function() { updateControlIfUnfocused(sbElement.$sbGet()); });
		}

		if (c.data("sbcontrol").slider) {
			var elements = [];
			$.each(c.data("sbcontrol").sliderControls || [], function(i) {
				elements[i] = sbElement.$sb(this);
			});
			if (!elements.length)
				elements = [ sbElement ];
			var eValues = $.map(elements, function(e) { return elementValueToControlValue(e.$sbGet()); });
			c.slider({ values: eValues, range: (eValues.length > 1) })
				.bind("slide slidestop", function(event, ui) { $.each(elements, function(i) { elements[i].$sbSet(controlValueToElementValue(ui.values[i])); }); });
			$.each(elements, function(i) { this.bind("sbchange", function(event, value) { c.slider("values", i, elementValueToControlValue(value)); }); });
		}
		if (c.data("sbcontrol").colorpicker) {
			/* spectrum is dumb, and won't work if the element isn't already in the dom */
			/* whoever's creating this control better put it into the dom before exiting */
			setTimeout(function() {
				_crgUtils.makeColorPicker(c);
				sbElement.bind("sbchange", function(event, value) { c.spectrum("set", value); });
			}, 0);
		}

//FIXME - this is really kludgey, redesign this in a more clean way.
		if (c.data("sbcontrol").editOnClick) {
			if (index == 0) {
				var bindClickTo = (c.data("sbcontrol").bindClickTo || c);
				bindClickTo.bind("click", function(event) {
					if (!c.data("sbcontrol").useDoubleClick || (500 > (event.timeStamp - c.data("sbcontrol").lastClick)))
						getGroup(true).focus();
					c.data("sbcontrol").lastClick = event.timeStamp;
				});
			} else if (index > 0) {
				c.hide();
				c.bind("focus", function() { getGroup(true).hide(); c.show().addClass("Editing").trigger("editstart"); });
				c.bind("blur", function() { getGroup(true).show(); c.hide().removeClass("Editing").trigger("editstop"); });
				c.bind("keyup", function(event) {
					switch (event.which) {
					case 13: /* RET */ if (c.is("textarea") && !event.ctrlKey) break; c.blur(); break;
					case 27: /* ESC */ c.blur(); break;
					}
				});
//FIXME - (above) on exit from text area editing with ESC, should revert to original text instead of updated/new text (only if delayUpdate)
			}
		}
	},
};
