
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

/*
 * The main scoreboard API is the $sb() function.	 Its use can be:
 *	 $sb(function)
 *		 The function will be run after the scoreboard has been loaded.
 *	 $sb(name)
 *		 The name must be the name of a single scoreboard element,
 *		 which is in the form of one or more hierarchal element names
 *		 (alphanumeric only, no spaces) separated by a period, and
 *		 optionally including each element's "Id" attribute value
 *		 (any character except paranthesis and single quotes) inside
 *		 paranthesis.	 For example:
 *			 "ScoreBoard.Team(1).Skater(Some Skater name).Number"
 *		 This will return an extended jQuery object representing a
 *		 single XML document element node which corresponds to the
 *		 given element name.	If no named element exists, it is
 *		 automatically created.
 *	 $sb(jQuery object) or $sb(XML element)
 *		 This returns an extended jQuery object, the same as the $sb(name)
 *		 function, but corresponding to the passed in unextended jQuery object
 *		 or XML element reference.	The XML element must be one from the
 *		 scoreboard's XML document structure, and the jQuery object must
 *		 represent an XML element from the scoreboard's XML document structure.
 *		 An already-extended jQuery object may be passed in, and it will be
 *		 returned unmodified.
 *
 * The extended jQuery object returned from the $sb(name) function
 * contains these extended scoreboard-specific functions and fields:
 *	 $sb(name)
 *		 This is the same as the global $sb(name) function, but it only
 *		 searches relative to the object it's called from.	For example:
 *			 $sb("ScoreBoard.Team(1).Name")
 *		 is equivalent to
 *			 $sb("ScoreBoard").$sb("Team(1)").$sb("Name")
 *	 $sbName
 *		 This field contains the XML element node name.
 *		 This does not include the "Id" attribute value.
 *	 $sbId
 *		 This field contains the XML element "Id" attribute value.
 *		 The value may be null if there is no "Id" attribute for this element.
 *	 $sbFullName
 *		 This field contains the name and "Id" attribute value,
 *		 as name(Id).	 If "Id" is null, this is identical to $sbName.
 *	 $sbPath
 *		 This field contains the path, which includes parent elements.
 *		 The path is what you pass to the global $sb(name) function
 *		 to get this specific element.
 *	 $sbExtended
 *		 This field is true if the jQuery object has been extended with
 *		 the scoreboard-specific fields/functions.
 *	 $sbGet()
 *		 This function returns the current value of the XML element.
 *	 $sbIs(value)
 *		 This function returns the boolean value of comparing the
 *		 current value of this XML element to the parameter.
 *	 $sbIsTrue()
 *		 This function returns the boolean value of the
 *		 current value of this XML element.
 *	 $sbSet(value, [attributes])
 *		 This function sets this element's value in the core scoreboard
 *		 program.	 The attributes parameter is an optional parameter
 *		 that can be used to pass attributes to the scoreboard program,
 *		 for example the "change" attribute, when set to "true", has
 *		 the effect when called on e.g. a Team Score of adding the
 *		 parameter to the existing score instead of replacing the
 *		 existing score.	The attribute parameter must be a javascript
 *		 object, i.e. { }, with zero or more key-value pairs, or it can be
 *		 null or undefined.
 *	 $sbRemove()
 *		 This function removes the element from the core scoreboard program.
 *		 Note that elements may not always be able to be removed, and the
 *		 scoreboard program may ignore this remove operation if used on
 *		 an element that cannot be removed.
 *	 $sbBindAndRun(eventType, eventData, handler, initParams)
 *		 This is similar to the jQuery bind() function, but also runs the
 *		 handler function immediately once.	 See _crgUtils.bindAndRun
 *		 for details on the function.
 *	 $sbOnAndRun(eventType, eventData, handler, initParams)
 *		 This is the same as $sbBindAndRun, but it uses on().
 *	 $sbBindAddRemoveEach(childName, add, remove)
 *		 This binds functions to the add and remove events for the specified
 *		 children of this element, and runs the add function once for each
 *		 of the existing matched children.	See _crgUtils.bindAddRemoveEach
 *		 for details on the function.
 *	 $sbElement(type, [attributes], [className])
 *		 This function creates HTML element(s) using the type parameter,
 *		 by calling the jQuery $(type) function.	The attributes parameter is
 *		 an optional parameter that, if used, must be a javascript object
 *		 (i.e. { }) with zero or more key-value pairs.	The attributes
 *		 are set on all of the created HTML element(s).	 The className parameter
 *		 is also an optional parameter that must be a string with zero or more
 *		 space-separated class names that are added to all the created HTML
 *		 element(s).	The attributes parameter may be omitted and the className
 *		 parameter used if desired.
 *		 Note that the attributes object can contain a special key-value pair
 *		 with the key of "sbelement", whose value must be a javascript object
 *		 also.	This key will be removed from the attributes object before
 *		 setting attributes on the HTML element(s).	 See the "sbelement"
 *		 section below for details.
 *	 $sbControl(type, [attributes], [className])
 *		 This function is the same as $sbElement but it also allows changes
 *		 to the HTML element(s) to effect the XML element value.	For example,
 *		 if the "type" field is "<input type='text'>" then changes to the
 *		 text will directly, and immediately, change the XML element value.
 *		 The optional attributes javascript object also supports the
 *		 special "sbelement" object, and it also supports a "sbcontrol" object
 *		 that is explained in the "sbcontrol" section below.
 *
 * sbelement
 * The special "sbelement" object can contain any of these fields which
 * control various aspects of the created HTML element(s) operation.
 *	 setColor: boolean
 *		 If true, the HTML element css 'color' will be set to the
 *		 XML element value.  The HTML element text will not be set.
 *	 setBackground: boolean
 *		 If true, the HTML element css 'background' will be set to the
 *		 XML element value.  The HTML element text will not be set.
 *	 boolean: boolean
 *		 If this is true, the XML element value will be passed through
 *		 the isTrue() function, before the convert function/object
 *		 (if applicable) is used.
 *	 convert: function(value) || object
 *		 If a function, it will be used to convert from the XML element
 *		 value into the HTML element value.	 In the function, 'this'
 *		 points to the $sb(XML element) whose value is being converted.
 *		 If an object, it will be checked for the XML element value
 *		 (after converting to a String), and if the value exists
 *		 as a member of the object, that member value will be used.
 *	 convertOptions: object
 *		 This object controls specific operation of the convert
 *		 function/object.	 Its parameters can be:
 *		 default: HTML-element-specific value
 *			 If a converted XML element value is undefined, this value will
 *			 be used.
 *		 onlyMatch: boolean
 *			 If true, any value that is undefined after conversion (including
 *			 default) will be reset to the pre-conversion value (after
 *			 conversion to boolean, if applicable).
 *	 autoFitText: boolean || {}
 *		 If true, the HTML element text will be auto-fit to its immediate parent,
 *		 using _autoFit.enableAutoFitText() with no options.
 *		 If set to an object, that object will be used as the options.
 *		 Note that if the container/parent is already is enabled for auto-fit,
 *		 it may ignore any new options and continue to use its initial options
 *		 (see that function for specific details on its behavior).
// FIXME - this isn't optimal, would be better to figure something else out
 *		 Note if the element has no container/parent (yet), the auto-fit
 *		 enablement is deferred to setTimeout() which will allow the
 *		 current code to add the element to a parent; however if the element
 *		 still has no parent in the deferred call auto-fit will not be enabled.
// END FIXME
 *	 autoFitTextTarget: string selector
 *		 If specified, this will restrict the auto-fit to only elements
 *		 matching the selector.
 *	 autoFitTextContainer: jQuery object || string selector || function
 *		 If specified, this will be used instead of the immediate parent
 *		 of the target element.
 *
 * sbcontrol
 * The special "sbcontrol" object can contain any of these fields which
 * control various aspects of the created HTML element(s) operation.
 *	 convert: function(value)
 *		 This function should convert from the HTML element value into
 *		 the XML element value.	 In the function, 'this' points to the
 *		 $(HTML element) whose value is being converted.
 *	 
 * Global variables
 *	 XML_ELEMENT_SELECTOR
 *		 This optional selector can be used to filter which scoreboard
 *		 XML top-level elements will be processed.	Using this can
 *		 help avoid unnecessary processing of XML elements that the
 *		 page will never use.	 This can be useful e.g. in mobile browsers.
 *		 If used, this should be set to a standard selector string;
 *		 for example "ScoreBoard,Teams,Pages".	Only top level elements
 *		 can be selected.	 By default all top level elements are processed.
 */

function is$sb(arg) {
	if ($.isjQuery(arg))
		return (arg.$sbExtended == true);
	else
		return false;
}

$sb = function(arg) {
	if (!arg)
		arg = "";

	if ($.isFunction(arg)) {
		var callArg = function() { arg.call($sb()); };
		if (_crgScoreBoard.documentLoaded)
			setTimeout(callArg, 0);
		else
			_crgScoreBoard.documentEvents.one("load:ScoreBoard", callArg);
	} else if (typeof arg == "string") {
		return _crgScoreBoard.findScoreBoardElement(_crgScoreBoard.doc, arg);
	} else if (($.isjQuery(arg) || (arg = $(arg))) && arg[0] && $.isXMLDoc(arg[0]) && (arg[0].ownerDocument == _crgScoreBoard.doc[0].ownerDocument)) {
		return _crgScoreBoard.extendScoreBoard(arg);
	} else {
		return null; // FIXME - return "empty" sb element instead?
	}
};

_crgScoreBoard = {
	POLL_INTERVAL_MIN: 0,
	POLL_INTERVAL_MAX: 500,
	POLL_INTERVAL_INCREMENT: 100,
	pollRate: this.POLL_INTERVAL_MIN,
	doc: $("document", $.parseXML("<?xml version='1.0' encoding='UTF-8'?><document/>")),
	documentLoaded: false,
	documentEvents: $("<div>"),
	addEventTriggered: { },
	sbExtensions: {
		$sbExtended: true,
		$sb: function(arg) { return _crgScoreBoard.findScoreBoardElement(this, arg); },
/* FIXME - should this paranoid-check for same uuid in specified document, i.e. HTML doc or XML doc? */
		$sbNewUUID: function() { return _crgScoreBoard.newUUID(true); },
		$sbGet: function() { return _crgScoreBoard.getXmlElementText(this); },
		$sbIs: function(value) { return (this.$sbGet() == value); },
		$sbIsTrue: function() { return isTrue(this.$sbGet()); },
		$sbSet: function(value, attrs) { _crgScoreBoard.updateServer(_crgScoreBoard.toNewElement(this, value).attr(attrs||{})); },
		$sbRemove: function() { _crgScoreBoard.removeFromServer(this); },
		$sbBindAndRun: function(eventType, eventData, handler, initParams) { return _crgUtils.bindAndRun(this, eventType, eventData, handler, initParams); },
		$sbOnAndRun: function(eventType, eventData, handler, initParams) { return _crgUtils.onAndRun(this, eventType, eventData, handler, initParams); },
		$sbBindAddRemoveEach: function(childName, add, remove) { return _crgUtils.bindAddRemoveEach(this, childName, add, remove); },
		$sbElement: function(type, attributes, className) { return _crgScoreBoard.create(this, type, attributes, className); },
		$sbControl: function(type, attributes, className) { return _crgScoreBoardControl.create(this, type, attributes, className); },
	},

	loadPageCssJs: function() {
		/*
		 * If needed, load the page's corresponding js/css
		 */
		if (/\.html$/.test(window.location.pathname)) {
			_include(window.location.pathname.replace(/\.html$/, ".css"));
			_include(window.location.pathname.replace(/\.html$/, ".js"));
		}
	},

	loadCustom: function() {
		/*
		 * After the main page's $sb() functions have been run,
		 * include any custom js and/or css for the current html
		 */
		if (/\.html$/.test(window.location.pathname)) {
			_include(window.location.pathname.replace(/\.html$/, "-custom.css"));
			_include(window.location.pathname.replace(/\.html$/, "-custom.js"));
		}
	},

	create: function(sbElement, type, attributes, className) {
		/* specifying attributes is optional */
		if (typeof attributes == "string") {
			className = attributes;
			attributes = {};
		} else if (!attributes)
			attributes = {};
		attributes = $.extend(true, {}, attributes); // Keep the original attributes object unchanged
		var sbelement = $.extend(true, {}, attributes.sbelement);
		delete attributes.sbelement;
		var elements = $(type);
		var allElements = elements.find("*").andSelf();
		allElements.data("sbelement", sbelement).addClass(className)
			.attr($.extend({ "data-sbelement": _crgScoreBoard.getPath(sbElement), "data-UUID": _crgScoreBoard.newUUID() }, attributes));
		_crgScoreBoard.setHtmlValue(sbElement, sbElement.$sbGet(), allElements);
		_crgScoreBoard.setupScoreBoardElement(sbElement, allElements, sbelement);
		return elements;
	},

	setupScoreBoardElement: function(sbElement, allElements, sbelement) {
		if (sbelement.autoFitText) {
			var options = sbelement.autoFitText;
			if ($.type(options) != "object")
				options = { };
			allElements.each(function() {
				var e = $(this);
				var targetSelector = sbelement.autoFitTextTarget;
				if (($.type(targetSelector) == "string") && !e.is(targetSelector))
					return;
				var enableAutoFit = function() {
					var container = sbelement.autoFitTextContainer;
					if ($.type(container) == "string")
						container = e.closest(container);
					else if ($.type(container) == "function")
						container = container.call(e);
					else if (!$.isjQuery(container))
						container = e.parent();
					var opts = $.extend({}, options);
					if (!$.isjQuery(container) || !container.length)
						return false;
					var doAutoFit = _autoFit.enableAutoFitText(container, opts);
					sbElement.bind("sbchange", function(event) {
						if (container.closest("body").length)
							doAutoFit();
						else
							sbElement.unbind("sbchange", event);
					});
					return true;
				};
				enableAutoFit() || setTimeout(enableAutoFit);
			});
		}
	},

	/* From http://www.broofa.com/2008/09/javascript-uuid-function/
	 * With additional super-paranoid checking against UUID of all current HTML elements
	 */
	newUUID: function(notParanoid) {
		var uuid;
		do {
			uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {var r = Math.random()*16|0,v=c=='x'?r:r&0x3|0x8;return v.toString(16);}).toUpperCase();
		} while (!notParanoid && $("[data-UUID="+uuid+"]").length);
		return uuid.toLowerCase();
	},

	updateServer: function(e) {
		$.ajax({
				url: "/XmlScoreBoard/set?key="+_crgScoreBoard.pollAjaxParam.data.key,
				type: "POST",
				processData: false,
				contentType: "text/xml;encoding=UTF-8",
				data: e[0].ownerDocument
			});
	},

	removeFromServer: function(e) {
		var newE = _crgScoreBoard.toNewElement(e);
		newE.append(newE[0].ownerDocument.createProcessingInstruction("Remove", ""));
		_crgScoreBoard.updateServer(newE);
	},

	extendScoreBoard: function(e) {
		if (e.$sbExtended)
			return e;

		e.context = _crgScoreBoard.doc[0];
		e.selector = _crgScoreBoard.getSelector(e);
		e.$sbName = e[0].nodeName;
		e.$sbId = $(e).attr("Id");
		e.$sbFullName = e.$sbName + (e.$sbId==undefined?"":"("+e.$sbId+")");
		e.$sbPath = _crgScoreBoard.getPath(e);
		return $.extend(e, _crgScoreBoard.sbExtensions);
	},

	getPath: function(e) {
		if (e[0] == this.doc[0])
			return "";
		var name = e[0].nodeName;
		var id = e.attr("Id");
		var p = this.getPath(e.parent());
		return p+(p?".":"")+name+(id?"("+id+")":"");
	},

	getSelector: function(e) {
		if (e[0] == this.doc[0])
			return "";
		var s = this.toSelector(e[0].nodeName, e.attr("Id"));
		var p = this.getSelector(e.parent());
		return p+(p?">":"")+s;
	},

	toSelector: function(name, id) {
		return name+(id?"[Id='"+id+"']":":not([Id])");
	},

	toNewElement: function(e, newText) {
		if (!e)
			e = this;

		if (e[0] == _crgScoreBoard.doc[0])
			return $($.parseXML("<?xml version='1.0' encoding='UTF-8'?><"+e[0].nodeName+"/>")).children(e[0].nodeName);

		return _crgScoreBoard.createScoreBoardElement(_crgScoreBoard.toNewElement(e.parent()), e[0].nodeName, e.attr("Id"), newText);
	},

	findScoreBoardElement: function(parent, path, doNotCreate, updateFromServer) {
		var p = path.match(/[\w\d]+(\([^\(\)]*\))?/g);
		var me = parent;
		$.each((p?p:[]), function() {
				var name = this.replace(/\(.*$/, "");
				var id = this.match(/\([^\)]*\)/);
				if (id)
					id = id.toString().replace(/[\(|\)]/g, "");
				var child;
				if (!(child = me.children(_crgScoreBoard.toSelector(name,id))).length) {
					if (doNotCreate)
						return (me = false);
					child = _crgScoreBoard.createScoreBoardElement(me, name, id);
					if (!updateFromServer)
						$sb(child).$sbSet();
				}
				me = child;
			});
		if (me === false)
			return null;
		else
			return $sb(me);
	},

	removeScoreBoardElement: function(parent, e) {
		if (!e) return;
		e.children().each(function() {
			_crgScoreBoard.removeScoreBoardElement(e, $sb(this));
		});
		var oldContent = e.$sbGet();
		_crgScoreBoard.setXmlElementText(e, "");
		_crgScoreBoard.setHtmlValue(e, "");
		e.trigger("sbchange", [ "", oldContent ]);
		delete _crgScoreBoard.addEventTriggered[e.$sbPath];
		parent.trigger("sbremove", [ e ]);
		parent.trigger("sbremove:"+e.$sbName, [ e ]);
		e.remove();
	},

	createScoreBoardElement: function(parent, name, id, text) {
		var e = $(parent[0].ownerDocument.createElement(name));
		if (id)
			e.attr("Id", id);
		_crgScoreBoard.setXmlElementText(e, text);
		parent.append(e);
		return e;
	},

	getXmlElementText: function(e) {
		if (isTrue(e.attr("empty")))
			return "";
		var text = "";
		e.contents().filter(function() { return this.nodeType == 4; }).each(function() { text += this.nodeValue; });
		return (text || null);
	},

	setXmlElementText: function(e, text) {
		e.contents().filter(function() { return this.nodeType == 4; }).remove();
		if (text)
			e.removeAttr("empty")[0].appendChild(e[0].ownerDocument.createCDATASection(text));
		else if (text === "")
			e.attr("empty", "true");
		else
			e.removeAttr("empty");
		return e;
	},

	getXmlElementPI: function(e, target) {
		var pi = e.contents().filter(function() { return ((this.nodeType == 7) && (this.nodeName == target)); });
		return (pi.length ? pi[0] : null);
	},
	hasXmlElementPI: function(e, target) { return (null != _crgScoreBoard.getXmlElementPI(e, target)); },

//FIXME - move this to windowfunctions
	setHtmlValue: function(sbElement, value, htmlelements) {
		if (!htmlelements)
			htmlelements = $("[data-sbelement='"+sbElement.$sbPath+"']");
		htmlelements.each(function() {
			var e = $(this);
			var v = value; // Don't modify the main value, since we are in $.each()
			var sbC = e.data("sbcontrol") || {};
			var sbE = e.data("sbelement") || {};
			if (sbE["boolean"])
				v = isTrue(v);
			var convertOptions = sbE.convertOptions || {};
			if (sbE.convert) {
				var tmpV = v;
				if ($.type(sbE.convert) == "function")
					tmpV = sbE.convert.call(sbElement, tmpV);
				else if ($.type(sbE.convert) == "object")
					tmpV = sbE.convert[String(tmpV)];
				if (tmpV === undefined)
					tmpV = convertOptions["default"];
				if (!(tmpV === undefined && isTrue(convertOptions.onlyMatch)))
					v = tmpV;
			}
			if (sbE.setColor) {
				e.css("color", v);
			} else if (sbE.setBackground) {
				e.css("background", v);
			} else if (e.is("a,span,div")) {
				if (e.html() != v)
					e.html(v);
			} else if (e.is("img")) {
				if (v == "")
					e.attr("src", "/images/blank.png");
				else
					e.attr("src", v);
			} else if (e.is("video")) {
				var currentlyPlaying = !this.paused;
				e.attr("src", v);
				this.load();
				if (currentlyPlaying)
					this.play();
			} else if (e.is("iframe"))
				e.attr("src", v);
			else if (e.is("input:text,input[type='number'],input:password,textarea"))
				e.val(v);
			else if (e.is("input:checkbox"))
				e.prop("checked", isTrue(v));
			else if (e.is("input:radio"))
				e.prop("checked", (e.val() == v));
			else if (e.is("input:button,button")) {
				if (sbC && sbC.setButtonValue)
					sbC.setButtonValue.call(this, v);
/* FIXME - uncomment this once any users of $sbControl(<button>) have added noSetButtonValue parameter
 *				 or, add parameter automatically if button linked to input text
				else if (!sbC || !sbC.noSetButtonValue)
					e.val(v);
*/
			} else if (e.is("select"))
				e.val(v);
			else
				alert("ADD SUPPORT FOR ME: node type "+this.nodeName);

			if (e.is(":checkbox,:radio,:button"))
				try { e.button("refresh"); } catch (err) { /* wasn't a button() */ }
			else if (e.parent().is(":checkbox,:radio,:button"))
				try { e.parent().button("refresh"); } catch (err) { /* wasn't a button() */ }
		});
		return htmlelements;
	},

	processScoreBoardElement: function(parent, element, triggerArray) {
		var $element = $(element);
		var name = element.nodeName;
		var id = $element.attr("Id");
		var remove = _crgScoreBoard.hasXmlElementPI($element, "Remove");
		var once = _crgScoreBoard.hasXmlElementPI($element, "Once");
		var e = this.findScoreBoardElement(parent, name+(id?"("+id+")":""), remove, true);
		var triggerObj = { parent: parent, node: e, remove: (remove || once) };
		if (!remove) {
			var newContent = _crgScoreBoard.getXmlElementText($element);
			if (newContent !== null) {
				var oldContent = _crgScoreBoard.getXmlElementText(e);
				if (oldContent !== newContent) {
					_crgScoreBoard.setXmlElementText(e, newContent);
					_crgScoreBoard.setHtmlValue(e, newContent);
					triggerObj.fireChange = true;
					triggerObj.oldContent = oldContent;
					triggerObj.newContent = newContent;
				}
			}
		}
		var fireEvents = false;
		if (!triggerArray) {
			triggerArray = [];
			fireEvents = true;
		}
		triggerArray.push(triggerObj);
		$element.children().each(function() { _crgScoreBoard.processScoreBoardElement(e, this, triggerArray); });
		if (fireEvents) {
			$.each(triggerArray, function(i,obj) {
				if (!_crgScoreBoard.addEventTriggered[obj.node.$sbPath]) {
					_crgScoreBoard.addEventTriggered[obj.node.$sbPath] = true;
					obj.parent.trigger("sbadd", [ obj.node ]);
					obj.parent.trigger("sbadd:"+obj.node.$sbName, [ obj.node ]);
				}
				if (obj.fireChange)
					obj.node.trigger("sbchange", [ obj.newContent, obj.oldContent ]);
				if (obj.remove)
					_crgScoreBoard.removeScoreBoardElement(obj.parent, obj.node);
			});
		}
	},

	processScoreBoardXml: function(xml) {
		$(xml).children("document")
			.children("ReloadViewers").each(function(i,e) {
				if (_crgScoreBoard.hasXmlElementPI($(e), "Reload"))
					window.location.reload();
			}).end()
			.children(window.XML_ELEMENT_SELECTOR).each(function(i,e) {
				_crgScoreBoard.processScoreBoardElement(_crgScoreBoard.doc, e);
			});
		if (!_crgScoreBoard.documentLoaded) {
			$sbThisPage = $sb("Pages.Page("+/[^\/]*$/.exec(window.location.pathname)+")");
			_crgScoreBoard.documentLoaded = true;
			_crgScoreBoard.documentEvents.triggerHandler("load:ScoreBoard");
			_crgScoreBoard.loadPageCssJs();
			_crgScoreBoard.loadCustom();
		}
	},

	pollAjaxParam: {
		global: false,
		cache: false,
		url: "/XmlScoreBoard/get",
		data: { key: null },
		complete: function(data, textStatus, jqxhr) {
			if (data.status == 200) {
				_crgScoreBoard.processScoreBoardXml(data.responseXML);
				_crgScoreBoard.pollRate = 0;
			} else if (data.status == 304) {
				_crgScoreBoard.pollRate = 0;
			} else if (data.status == 404) {
				//FIXME - we could possibly handle this better than reloading the page...
				window.location.reload();
			} else {
				// Some other error occured.  Poll longer
				_crgScoreBoard.pollRate += _crgScoreBoard.POLL_INTERVAL_INCREMENT;
			}
			if (_crgScoreBoard.pollRate > _crgScoreBoard.POLL_INTERVAL_MAX)
				_crgScoreBoard.pollRate = _crgScoreBoard.POLL_INTERVAL_MAX;
			setTimeout(_crgScoreBoard.pollScoreBoard, _crgScoreBoard.pollRate);
		}
	},

	pollScoreBoard: function() {
		$.ajax(_crgScoreBoard.pollAjaxParam);
	},

	parseRegistrationKey: function(xml, status) {
		_crgScoreBoard.pollAjaxParam.data.key = $(xml).find("document>Key").text();
		_crgScoreBoard.pollScoreBoard();
	},

	scoreBoardRegister: function() {
		$.ajax({
				url: "/XmlScoreBoard/register",
				success: function(xml,status) { _crgScoreBoard.parseRegistrationKey(xml, status); },
				//FIXME - really handle error
				error: function() { alert("error registering with scoreboard"); }
			});
	}
};
