

_crgScoreBoardControl = {
	create: function(sbElement, type, attributes, className) {
//FIXME - consolidate _crgScoreBoard.create() and this better.
		/* specifying attributes is optional */
		if (typeof attributes == "string") {
			className = attributes;
			attributes = {};
		} else if (!attributes)
			attributes = {};
		var sbcontrol = $.extend(true, {}, attributes.sbcontrol);
		var sbelement = $.extend(true, {}, attributes.sbelement);
		attributes.sbcontrol = undefined;
		attributes.sbelement = undefined;
		var controls = $(type);
		controls.find("*").andSelf()
			.data("sbcontrol", sbcontrol).data("sbelement", sbelement)
			.attr($.extend({ "data-sbcontrol": _crgScoreBoard.getPath(sbElement), "data-UUID": _crgScoreBoard.newUUID() }, attributes))
			.addClass(className)
			.each(function(index) { _crgScoreBoardControl.addControlFunction($(this), sbElement, index); });
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

		c.bind({ focus: function() { $(this).addClass("isFocused"); }, blur: function() { $(this).removeClass("isFocused"); } });

		var controlValueToElementValue = function(value) {
			var p = sbC.prefix, s = sbC.suffix, u = sbC.useNumber, t = sbC.trueString, f = sbC.falseString;
			if (sbC.convert) value = sbC.convert(value);
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
				_crgScoreBoard.setHtmlValue(c, elementValueToControlValue(value));
		};

		if (c.is("input:text,input:password,textarea")) {
			var updateControlIfUnfocused = function(value) {
				if (!getGroup().hasClass("isFocused")) setControlValue(value);
			};
			sbElement.bind("content", function(event, value) { updateControlIfUnfocused(value); });
			setControlValue(sbElement.$sbGet());
			c.bind("mouseup keyup change", function() { setElementValue(c.val()); });
			c.bind("blur", function() {
// FIXME - total kludge; this adds a small delay to give the next focused element time to get focus,
// in case it's part of this group.  Otherwise this text area will be cleared out even if
// changing focus over to a button to actually submit.
				setTimeout(function () { updateControlIfUnfocused(sbElement.$sbGet()) }, 500);
			});
		} else if (c.is("a")) {
			sbElement.bind("content", function(event, value) { setControlValue(value); });
			setControlValue(sbElement.$sbGet());
		} else if (c.is("input:button,button")) {
			sbElement.bind("content", function(event, value) { setControlValue(value); });
			setControlValue(sbElement.$sbGet());
			var buttonClick = function() {
//FIXME - can this check get pushed into the _crgKeyControls code instead?
				if (c.is(".KeyControl.Editing")) // If this button is a KeyControl that's being edited, ignore clicks.
					return;
				var associateText = getGroup("input:text,input:password,textarea");
				if (associateText.length) {
					setElementValue(associateText.val(), true);
					$(this).blur();
				} else if (c.data("sbcontrol").getButtonValue)
//FIXME - not sure if I like getButtonValue API
					setElementValue(c.data("sbcontrol").getButtonValue.call(this));
				else if (c.val())
					setElementValue(c.val());
			};
			c.bind("click", buttonClick);
		} else if (c.is("input:checkbox")) {
			sbElement.bind("content", function(event, value) { setControlValue(value); });
			setControlValue(sbElement.$sbGet());
			c.bind("change", function() { setElementValue(String(this.checked)); this.checked = !this.checked; });
		} else if (c.is("input:radio")) {
			if (!c.attr("name"))
				c.attr("name", c.attr("data-UUID")+"-name");
			sbElement.bind("content", function(event, value) { setControlValue(value); });
			setControlValue(sbElement.$sbGet());
			c.bind("change", function() { setElementValue($(this).val()); });
		} else if (c.is("label")) {
			// This requires the target checkbox or radio to immediately follow its label
			var target = c.next("input:checkbox,input:radio");
			if (!target.attr("id"))
				target.attr("id", sbElement.$sbNewUUID());
			c.attr("for", target.attr("id"));
		} else if (c.is("select")) {
			if (!c.data("sbcontrol").optionComparator)
				c.data("sbcontrol").optionComparator = function(a, b) {
					return _windowFunctions.alphaSortByAttr(a, b, "text");
				};
			if (!c.data("sbcontrol").addOption)
				c.data("sbcontrol").addOption = function(o, id) {
					o.attr("data-optionid", id);
					_windowFunctions.appendSorted(c, o, c.data("sbcontrol").optionComparator, (c.data("sbcontrol").firstOption?1:0));
				};
			if (!c.data("sbcontrol").removeOption)
				c.data("sbcontrol").removeOption = function(id) {
					c.find("option[data-optionid='"+id+"']").remove();
				};

			if (c.data("sbcontrol").firstOption)
				c.append(c.data("sbcontrol").firstOption);

			if (c.data("sbcontrol").sbelementSelect) {
//FIXME - this should use sbcontrol.firstOption instead of hardcoding a first one
				c.append("<option>");
//FIXME - add/remove is normally recursive and this should not need recursive adding/removing here
				var addOptions, removeOptions;
				addOptions = function(node) {
					c.data("sbcontrol").addOption(node.$sbPath);
					node.children().each(function() { addOptions($sb(this)); });
				};
				removeOptions = function(node) {
					node.children().each(function() { removeOptions($sb(this)); });
					c.data("sbcontrol").removeOption(node.$sbPath);
				};
				$sb().children().each(function() { addOptions($sb(this)); });
				$sb().bind("add", function(event, node) { addOptions(node); });
				$sb().bind("remove", function(event, node) { removeOptions(node); });
			}

			/* This sets up the select option list with values from all children
			 * of a specific parent in the $sb() tree.
			 *   optionParent:
			 *     this is the main node in the tree, e.g. ScoreBoard.Team(1)
			 *   optionChildName:
			 *     this is the name of which children to use, e.g. Skater
			 *   optionChildFilter:
			 *     an optional function to filter out children.
             *     it takes a single parameter which is the child $sb() element,
             *     and its return value is a boolean indicating if it should
             *     be included in the list or not.
			 *   optionNameElement:
			 *     if set, this is the name of each child subelement value to use
             *     for the option html, e.g. Name
			 *     if not set, the child.$sbId will be used.
			 *   optionValueElement:
			 *     if set, this is the name of each child subelement value to use
             *     for the option value, e.g. Name
			 *     if not set, the child.$sbId will be used.
			 */
			var parent = c.data("sbcontrol").optionParent;
			var child = c.data("sbcontrol").optionChildName;
			var childFilter = (c.data("sbcontrol").optionChildFilter || function() { return true; });
			var nameElement = c.data("sbcontrol").optionNameElement;
			var valueElement = c.data("sbcontrol").optionValueElement;
			if (parent && child) {
				var getO = function(node) {
					var o = $("<option>").html(node.$sbId).val(node.$sbId);
					if (nameElement) {
						o.html(node.$sb(nameElement).$sbGet());
						node.$sb(nameElement).bind("content", function(value) {
							o.html(value);
							c.data("sbcontrol").addOption(o, node.$sbPath);
						});
					}
					if (valueElement) {
						o.data("sbcontrol", { noSetOptionValue: true });
						o.val(node.$sb(valueElement).$sbGet());
						node.$sb(valueElement).bind("content", function(value) {
							o.val(value);
							if (o[0].selected)
								o.parent().change();
						});
					}
					return o;
				};
//FIXME - need to add $sb<function> to do this, handling each child and setting up live() calls.
				parent.children(child).each(function() {
					if (childFilter($sb(this)))
						c.data("sbcontrol").addOption(getO($sb(this)), $sb(this).$sbPath);
				});
				parent.live("add:"+child, function(e, node) {
					if (childFilter(node))
						c.data("sbcontrol").addOption(getO(node), node.$sbPath);
				});
				parent.live("remove:"+child, function(e, node) {
					c.data("sbcontrol").removeOption(node.$sbPath);
				});

			}

			sbElement.bind("content", function(event, value) { setControlValue(value); });
			setControlValue(sbElement.$sbGet());
			c.bind("keyup change", function() { setElementValue($(this).find("option:selected").val()); });
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
				.bind("slide slidestop", function(event, ui) { $.each(elements, function(i) { elements[i].$sbSet(controlValueToElementValue(ui.values[i])); });	});
			$.each(elements, function(i) { this.bind("content", function(event, value) { c.slider("values", i, elementValueToControlValue(value)); }); });
		}
		if (c.data("sbcontrol").colorPicker) {
			c.ColorPicker({	color: c.val(), onChange: function(hsb, hex, rgb) { c.val(hex).change(); } });
			c.bind("keyup mouseup change", function() { c.ColorPickerSetColor(c.val()); });
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
				c.bind("blur", function() { c.hide().removeClass("Editing").trigger("editstop"); getGroup(true).show(); });
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
