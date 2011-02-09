
/* This file requires base jQuery; other required jQuery plugins are automatically included below. */
if (typeof $ == "undefined") {
	alert("You MUST include jQuery before this file!");
	throw("You MUST include jQuery before this file!");
}

/*
 * The main scoreboard API is the $sb() function.  Its use can be:
 *   $sb(function)
 *     The function will be run after the scoreboard has been loaded.
 *   $sb(name)
 *     The name must be the name of a single scoreboard element,
 *     which is in the form of one or more hierarchal element names
 *     (alphanumeric only, no spaces) separated by a period, and
 *     optionally including each element's "Id" attribute value
 *     (any character except paranthesis and single quotes) inside
 *     paranthesis.  For example:
 *       "ScoreBoard.Team(1).Skater(Some Skater name).Number"
 *     This will return an extended jQuery object representing a
 *     single XML document element node which corresponds to the
 *     given element name.  If no named element exists, it is
 *     automatically created.
 *   $sb(jQuery object) or $sb(XML element)
 *     This returns an extended jQuery object, the same as the $sb(name)
 *     function, but corresponding to the passed in unextended jQuery object
 *     or XML element reference.  The XML element must be one from the
 *     scoreboard's XML document structure, and the jQuery object must
 *     represent an XML element from the scoreboard's XML document structure.
 *     An already-extended jQuery object may be passed in, and it will be
 *     returned unmodified.
 *
 * The extended jQuery object returned from the $sb(name) function
 * contains these extended scoreboard-specific functions and fields:
 *   $sb(name)
 *     This is the same as the global $sb(name) function, but it only
 *     searches relative to the object it's called from.  For example:
 *       $sb("ScoreBoard.Team(1).Name")
 *     is equivalent to
 *       $sb("ScoreBoard").$sb("Team(1)").$sb("Name")
 *   $sbName
 *     This field contains the XML element node name.
 *     This does not include the "Id" attribute value.
 *   $sbId
 *     This field contains the XML element "Id" attribute value.
 *     The value may be null if there is no "Id" attribute for this element.
 *   $sbFullName
 *     This field contains the name and "Id" attribute value,
 *     as name(Id).  If "Id" is null, this is identical to $sbName.
 *   $sbPath
 *     This field contains the path, which includes parent elements.
 *     The path is what you pass to the global $sb(name) function
 *     to get this specific element.
 *   $sbExtended
 *     This field is true if the jQuery object has been extended with
 *     the scoreboard-specific fields/functions.
 *   $sbGet()
 *     This function returns the current value of the XML element.
 *   $sbIs(value)
 *     This function returns the boolean value of comparing the
 *     current value of this XML element to the parameter.
 *   $sbIsTrue()
 *     This function returns the boolean value of the
 *     current value of this XML element.
 *   $sbSet(value, [attributes])
 *     This function sets this element's value in the core scoreboard
 *     program.  The attributes parameter is an optional parameter
 *     that can be used to pass attributes to the scoreboard program,
 *     for example the "change" attribute, when set to "true", has
 *     the effect when called on e.g. a Team Score of adding the
 *     parameter to the existing score instead of replacing the
 *     existing score.  The attribute parameter must be a javascript
 *     object, i.e. { }, with zero or more key-value pairs, or it can be
 *     null or undefined.
 *   $sbChange(value)
 *     This function is equivalent to $sbSet(value, { change: "true" })
 *   $sbRemove()
 *     This function removes the element from the core scoreboard program.
 *     Note that elements may not always be able to be removed, and the
 *     scoreboard program may ignore this remove operation if used on
 *     an element that cannot be removed.
 *   $sbBindAndRun(eventType, eventData, handler, initParams)
 *     This is similar to the jQuery bind() function, but also runs the
 *     handler function immediately once.  See _crgUtils.bindAndRun
 *     for details on the function.
 *   $sbBindAddRemoveEach(childName, add, remove)
 *     This binds functions to the add and remove events for the specified
 *     children of this element, and runs the add function once for each
 *     of the existing matched children.  See _crgUtils.bindAddRemoveEach
 *     for details on the function.
 *   $sbElement(type, [attributes], [className])
 *     This function creates HTML element(s) using the type parameter,
 *     by calling the jQuery $(type) function.  The attributes parameter is
 *     an optional parameter that, if used, must be a javascript object
 *     (i.e. { }) with zero or more key-value pairs.  The attributes
 *     are set on all of the created HTML element(s).  The className parameter
 *     is also an optional parameter that must be a string with zero or more
 *     space-separated class names that are added to all the created HTML
 *     element(s).  The attributes parameter may be omitted and the className
 *     parameter used if desired.
 *     Note that the attributes object can contain a special key-value pair
 *     with the key of "sbelement", whose value must be a javascript object
 *     also.  This key will be removed from the attributes object before
 *     setting attributes on the HTML element(s).  See the "sbelement"
 *     section below for details.
 *   $sbControl(type, [attributes], [className])
 *     This function is the same as $sbElement but it also allows changes
 *     to the HTML element(s) to effect the XML element value.  For example,
 *     if the "type" field is "<input type='text'>" then changes to the
 *     text will directly, and immediately, change the XML element value.
 *     The optional attributes javascript object also supports the
 *     special "sbelement" object, and it also supports a "sbcontrol" object
 *     that is explained in the "sbcontrol" section below.
 *
 * sbelement
 * The special "sbelement" object can contain any of these fields which
 * control various aspects of the created HTML element(s) operation.
 *   boolean: boolean
 *     If this is true, the XML element value will be passed through
 *     the isTrue() function, before the convert function/object
 *     (if applicable) is used.
 *   convert: function(value) || object
 *     If a function, it will be used to convert from the XML element
 *     value into the HTML element value.
 *     If an object, it will be checked for each XML element value
 *     (after converting the value to a String), and if the value exists
 *     as a member of the object, that member value will be used.
 *   convertOptions: object
 *     This object controls specific operation of the convert
 *     function/object.  Its parameters can be:
 *     default: HTML-element-specific value
 *       If a converted XML element value is undefined, this value will
 *       be used.
 *     onlyMatch: boolean
 *       If true, any value that is undefined after conversion (including
 *       default) will be reset to the pre-conversion value (after
 *       conversion to boolean, if applicable).
 *   autoFitText: boolean || {}
 *     If true, the HTML element text will be auto-fit to the container,
 *     using _windowFunctions.enableAutoFitText() with no options.
 *     If set to an object, that object will be used as the options.
 *
 * sbcontrol
 * The special "sbcontrol" object can contain any of these fields which
 * control various aspects of the created HTML element(s) operation.
 *   convert: function(value)
 *     This function should convert from the HTML element value into
 *     the XML element value.
 *   
 */

function isTrue(value) {
	if (typeof value == "boolean")
		return value;
	else
		return (String(value).toLowerCase() == "true");
}

function _includeUrl(url) {
	if (/\.[cC][sS][sS](\?.*)?$/.test(url))
		$("<link>").attr({ href: url, type: "text/css", rel: "stylesheet"}).appendTo("head");
	else if (/\.[jJ][sS](\?.*)?$/.test(url))
		$("<script>").attr({ src: url, type: "text/javascript" }).appendTo("head");
//		$.ajax({ url: url, dataType: "script", async: false });		
}

function _include(dir, files) {
	if (!$.isArray(files))
		files = [ files ];
	$.each(files, function() { _includeUrl(dir+"/"+this); });
}

_include("/external/jquery-ui", [ "jquery-ui.js", "css/default/jquery-ui.css" ]);

_include("/external/colorpicker", [ "colorpicker.js", "css/colorpicker.css" ]);

_include("/external/jstree", "jquery.tree.js");
$.jstree._themes = "/external/jstree/themes/";

_include("/external/treeview", [ "jquery.treeview.js", "jquery.treeview.css" ]);

_include("/external/jquery/isjquery", "jquery.isjquery.js");
_include("/external/jquery/attributes", [ "jquery.listAttributes.js", "jquery.mapAttributes.js" ]);
_include("/external/jquery/periodicalupdater", "jquery.periodicalupdater.js");
_include("/external/jquery/protify", "jquery.protify.js");
_include("/external/jquery/string", "jquery.string.js");
_include("/external/jquery/xml", [ "jquery.xmldom.js", "jquery.xml.js" ]);


$sb = function(arg) {
	if (!arg)
		arg = "";

	if ($.isFunction(arg))
		_crgScoreBoard.doc.one("load:ScoreBoard", function(event) { arg.call($sb(), event); });
	else if (typeof arg == "string")
		return _crgScoreBoard.findScoreBoardElement(_crgScoreBoard.doc, arg);
	else if (($.isjQuery(arg) || (arg = $(arg))) && arg[0] && $.isXMLDoc(arg[0]) && (arg[0].ownerDocument == _crgScoreBoard.doc[0].ownerDocument))
		return _crgScoreBoard.extendScoreBoard(arg);
	else
		return null; // FIXME - return "empty" sb element instead?
};

_crgScoreBoard = {
	scoreBoardRegistrationKey: null,
	POLL_INTERVAL_MIN: 100,
	POLL_INTERVAL_MAX: 500,
	POLL_INTERVAL_INCREMENT: 10,
	pollRate: this.POLL_INTERVAL_MIN,
	doc: $.xmlDOM("<document></document>").find("document"),
	sbExtensions: {
		$sbExtended: true,
		$sb: function(arg) { return _crgScoreBoard.findScoreBoardElement(this, arg); },
/* FIXME - should this paranoid-check for same uuid in specified document, i.e. HTML doc or XML doc? */
		$sbNewUUID: function() { return _crgScoreBoard.newUUID(true); },
		$sbGet: function() { return _crgScoreBoard.getXmlElementText(this); },
		$sbIs: function(value) { return (this.$sbGet() == value); },
		$sbIsTrue: function() { return isTrue(this.$sbGet()); },
		$sbSet: function(value, attrs) { _crgScoreBoard.updateServer(_crgScoreBoard.toNewElement(this, value).attr(attrs||{})); },
		$sbChange: function(value) { this.$sbSet(value, { change: "true" }); },
		$sbRemove: function() { this.$sbSet(undefined, { remove: "true" }); },
		$sbBindAndRun: function(eventType, eventData, handler, initParams) { return _crgUtils.bindAndRun(this, eventType, eventData, handler, initParams); },
		$sbBindAddRemoveEach: function(childName, add, remove) { return _crgUtils.bindAddRemoveEach(this, childName, add, remove); },
		$sbElement: function(type, attributes, className) { return _crgScoreBoard.create(this, type, attributes, className); },
		$sbControl: function(type, attributes, className) { return _crgScoreBoardControl.create(this, type, attributes, className); },
	},

	create: function(sbElement, type, attributes, className) {
		/* specifying attributes is optional */
		if (typeof attributes == "string") {
			className = attributes;
			attributes = {};
		} else if (!attributes)
			attributes = {};
		var sbelement = $.extend(true, {}, attributes.sbelement);
		delete attributes.sbelement;
		var elements = $(type);
		elements.find("*").andSelf()
			.data("sbelement", sbelement)
			.attr($.extend({ "data-sbelement": _crgScoreBoard.getPath(sbElement), "data-UUID": _crgScoreBoard.newUUID() }, attributes))
			.addClass(className);
		_crgScoreBoard.setHtmlValue(elements, sbElement.$sbGet());
		if (sbelement.autoFitText) {
			var autoFitOptions;
			if (typeof sbelement.autoFitText == "object")
				autoFitOptions = sbelement.autoFitText;
			elements.each(function() {
				var autoFitFunction = _windowFunctions.enableAutoFitText($(this), autoFitOptions);
				sbElement.bind("content", autoFitFunction);
			});
		}
		return elements;
	},

	/* From http://www.broofa.com/2008/09/javascript-uuid-function/
	 * With additional super-paranoid checking against UUID of all current HTML elements
	 */
	newUUID: function(notParanoid) {
		var uuid;
		do {
			uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {var r = Math.random()*16|0,v=c=='x'?r:r&0x3|0x8;return v.toString(16);}).toUpperCase();
		} while (!notParanoid && $("[data-UUID="+uuid+"]").length);
		return uuid;
	},

	updateServer: function(e) {
		$.ajax({
				url: "/XmlScoreBoard/set?key="+_crgScoreBoard.scoreBoardRegistrationKey,
				type: "POST",
				processData: false,
				contentType: "text/xml",
				data: e[0].ownerDocument
			});
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
			return $.xmlDOM("<"+e[0].nodeName+"/>").children(e[0].nodeName);

		return _crgScoreBoard.createScoreBoardElement(_crgScoreBoard.toNewElement(e.parent()), e[0].nodeName, e.attr("Id"), newText);
	},

	findScoreBoardElement: function(parent, path, doNotCreate, updateFromServer) {
		var p = path.match(/[\w\d]+(\([^\(\)]*\))?/g);
		var me = parent;
		$.each((p?p:[]), function() {
				var name = this.replace(/\(.*$/, "");
				var id = this.match(/\([^\)]*\)/);
				if (id)
					id = id.toString().replace(/[\(|\)]/g, "")
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
		e.children(function() { removeScoreBoardElement(e, $sb(this)); });
		parent.trigger("remove", [ e ]);
		parent.trigger("remove:"+e.$sbName, [ e ]);
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
		e.contents().filter(function() { return this.nodeType == 3; }).each(function() { text += this.nodeValue; });
		return (text || null);
	},

	setXmlElementText: function(e, text) {
		e.contents().filter(function() { return this.nodeType == 3; }).remove();
		if (text)
			e.removeAttr("empty").append(e[0].ownerDocument.createTextNode(text));
		else if (text === "")
			e.attr("empty", "true");
		else
			e.removeAttr("empty");
		return e;
	},

//FIXME - move this to windowfunctions
	setHtmlValue: function(htmlelements, value) {
		htmlelements.each(function() {
			var e = $(this);
			var v = value; // Don't modify the main value, since we are in $.each()
			var sbC = e.data("sbcontrol") || {};
			var sbE = e.data("sbelement") || {};
			if (sbE["boolean"])
				v = isTrue(v);
			var convertOptions = sbE.convertOptions || {};
			if (sbE.convert) {
				if (typeof sbE.convert == "function")
					v = sbE.convert(value);
				else if (typeof sbE.convert == "object")
					v = sbE.convert[String(value)];
				if (v === undefined)
					v = convertOptions["default"];
				if (v === undefined && isTrue(convertOptions.onlyMatch))
					v = (sbE["boolean"] ? isTrue(value) : value);
			}
			if (e.is("a,span"))
				e.html(v);
			else if (e.is("img"))
				e.attr("src", v);
// FIXME - may need to support multiple video files with multiple source subelements?
// FIXME - need to start video when visible and when changing src; stop when not visible.
			else if (e.is("video"))
				e.attr("src", v);
			else if (e.is("input:text,input:password,textarea"))
				e.val(v);
			else if (e.is("input:checkbox")) {
				e.attr("checked", isTrue(v));
				try { e.button("refresh"); } catch (err) { /* checkbox wasn't a button(), ignore err */ }
			} else if (e.is("input:radio"))
				e.attr("checked", (e.val() == v));
			else if (e.is("input:button,button")) {
				if (sbC && sbC.setButtonValue)
					sbC.setButtonValue.call(this, v);
/* FIXME - uncomment this once any users of $sbControl(<button>) have added noSetButtonValue parameter
 *         or, add parameter automatically if button linked to input text
				else if (!sbC || !sbC.noSetButtonValue)
					e.val(v);
*/
			} else if (e.is("select"))
				e.val(v);
			else
				alert("ADD SUPPORT FOR ME: node type "+this.nodeName);
		});
		return htmlelements;
	},

	processScoreBoardElement: function(parent, element, triggerArray) {
		var $element = $(element);
		var name = element.nodeName;
		var id = $element.attr("Id");
		var remove = isTrue($element.attr("remove"));
		var e = this.findScoreBoardElement(parent, name+(id?"("+id+")":""), remove, true);
		var triggerObj = { node: e };
		if (remove) {
//FIXME - move the "remove" triggers into group below...
			this.removeScoreBoardElement(parent, e);
			return;
		}
		var newContent = _crgScoreBoard.getXmlElementText($element);
		if (newContent !== null) {
			var oldContent = _crgScoreBoard.getXmlElementText(e);
			if (oldContent !== newContent) {
				_crgScoreBoard.setXmlElementText(e, newContent);
				_crgScoreBoard.setHtmlValue($("[data-sbelement="+e.$sbPath+"]"), newContent);
				triggerObj.fireContent = true;
				triggerObj.oldContent = oldContent;
				triggerObj.newContent = newContent;
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
			$.each(triggerArray, function() {
				if (!this.node.data("_crgScoreBoard:addEventTriggered")) {
					this.node.data("_crgScoreBoard:addEventTriggered", true);
					this.node.parent().trigger("add", [ this.node ]);
					this.node.parent().trigger("add:"+this.node.$sbName, [ this.node ]);
				}
				if (this.fireContent)
					this.node.trigger("content", [ this.newContent, this.oldContent ]);
			});
		}
	},

	processScoreBoardXml: function(xml) {
		$(xml).find("document").children().each(function(index) { _crgScoreBoard.processScoreBoardElement(_crgScoreBoard.doc, this); });
		$sbThisPage = $sb("Pages.Page("+/[^\/]*$/.exec(window.location.pathname)+")");
		_crgScoreBoard.doc.triggerHandler("load:ScoreBoard");
	},

	parseScoreBoardResponse: function(xhr, textStatus) {
		try {
			switch (xhr.status) {
			case 304: /* No change since last poll, increase poll rate */
				_crgScoreBoard.pollRate += _crgScoreBoard.POLL_INTERVAL_INCREMENT;
				return;
			case 404:
//FIXME - we could possibly handle this better than reloading the page...
				window.location.reload();
				break;
			case 200:
				_crgScoreBoard.processScoreBoardXml(xhr.responseXML);
				_crgScoreBoard.pollRate = _crgScoreBoard.POLL_INTERVAL_MIN;
				break;
			case 0: /* FIXME - handle network errors? */ break;
			default: /* FIXME - handle other server response? */ break;
			}
		} finally {
			if (_crgScoreBoard.pollRate > _crgScoreBoard.POLL_INTERVAL_MAX)
				_crgScoreBoard.pollRate = _crgScoreBoard.POLL_INTERVAL_MAX;
			setTimeout(this.pollScoreBoard, this.pollRate);
		}
	},

	pollScoreBoard: function() {
		$.ajax({
				url: "/XmlScoreBoard/get",
				data: { key: _crgScoreBoard.scoreBoardRegistrationKey },
				complete: function(xhr,textStatus) { _crgScoreBoard.parseScoreBoardResponse(xhr, textStatus); }
			});
	},

	parseRegistrationKey: function(xml, status) {
		this.scoreBoardRegistrationKey = $(xml).find("document>Key").text();
		this.pollScoreBoard();
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

/* Start ScoreBoard server polling */
$(_crgScoreBoard.scoreBoardRegister);


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

//FIXME - need to allow setting up groups, maybe custom class to indicate grouping, so per-tab keycontrols are possible
_crgKeyControls = {
	/* Setup all key control buttons.
	 * This finds all button-type elements with the class KeyControl
	 * and calls setupKeyControl, using the given controlParent.
	 */
	setupKeyControls: function(controlParent) {
		_crgKeyControls.setupKeyControl($(":button.KeyControl"), controlParent);
	},
	/* Destroy all key control buttons.
	 * This finds all button-type elements with the class KeyControl
	 * and calls destroyKeyControl.
	 */
	destroyKeyControls: function(destroyButton) {
		_crgKeyControls.destroyKeyControl($(":button.KeyControl"), destroyButton);
	},

	/* Setup the button for key control.
	 * This sets up the given button as a key control.
	 * The button must have a (unique) id, and
	 * the id must conform to the restrictions of
	 * the ScoreBoard id restrictions (e.g. no (, ), ', or ")
	 * If the button is not a jQuery-UI button(), it will be made one.
	 * Once setup, any button presses corresponding to the
	 * button's key control will cause a button click.
	 * If this button has already been setup, it will first be
	 * destroyed and then re-setup.
	 *
	 * CSS notes:
	 * key control buttons have the class KeyControl
	 * there are new span elements added, which are button>span>span
	 * the new span elements (under the child span) have the class Indicator
	 * the span.Indicator element that stores the control key has class Key
	 * when there is a control key the button has class HasControlKey
	 */
	setupKeyControl: function(button, controlParent) {
		_crgKeyControls.destroyKeyControl(button, true).button()
			.addClass("KeyControl")
			.bind("mouseenter mouseleave", _crgKeyControls._hoverFunction)
			.children("span")
			.append($("<span>").text(" [").addClass("Indicator"))
			.append($("<span>").addClass("Key Indicator"))
			.append($("<span>").text("]").addClass("Indicator"))
			.end()
			.each(function() {
				var button = $(this);
				var key = controlParent.$sb("KeyControl("+button.attr("id")+").Key");
				key.$sbElement(button.find("span.Key"));
				var contentChange = function(event,value) { 
					button.toggleClass("HasControlKey", (value?true:false));
				};
				key.$sbBindAndRun("content", contentChange, [ key.$sbGet() ]);
				button.data("_crgKeyControls:unbind", function() { key.unbind("content", contentChange); });
				button.data("_crgKeyControls:Key", key);
				_crgKeyControls._start();
			});
		return button;
	},
	_hoverFunction: function() { $(this).toggleClass("hover"); },

	/* Destroy a key control button.
	 * This undoes the key control setup. If destroyButton
	 * is true, is destroys the jQuery-UI button.
	 * It returns the button element.
	 * Note this does not remove the KeyControl class from the button.
	 */
	destroyKeyControl: function(button, destroyButton) {
		button.each(function() { try { $(this).data("_crgKeyControls:unbind")(); } catch(err) { } });
		button.removeData("_crgKeyControls:unbind").removeData("_crgKeyControls:Key");
		button.unbind("mouseenter mouseleave", _crgKeyControls._hoverFunction);
		button.find("span.Indicator").remove();
		if (destroyButton)
			try { button.button("destroy"); } catch(err) { }
		return button;
	},

	/* Change to key-edit mode.
	 * If false, this changes all KeyControls to normal mode,
	 * where keypresses will cause the corresponding KeyControl event.
	 * If true, this changes all KeyControls to edit mode,
	 * where any keypress while the mouse is hovering over a button
	 * will cause that button to be assigned the pressed key,
	 * unless that key is already assigned to another button, in which
	 * case the currently assigned button will flash to indicate a
	 * conflict (and this button key assignment will not be changed).
	 * To clear the control key assignment, press the Backspace or Delete keys.
	 *
	 * CSS note: buttons in edit mode have the class "Editing".
	 */
	editKeys: function(edit) {
		$(":button.KeyControl").toggleClass("Editing", edit);
	},

	addCondition: function(condition) {
		_crgKeyControls._conditions.push(condition);
	},
	_conditions: [ ],

	_start: function() {
		if (!_crgKeyControls._keyControlStarted) {
			$(document).keypress(_crgKeyControls._keyControlPress);
			$(document).keydown(_crgKeyControls._keyControlDown);
			_crgKeyControls._keyControlStarted = true;
		}
	},
	_keyControlStarted: false,

	_checkConditions: function() {
		var ok = true;
		$.each(_crgKeyControls._conditions, function() {
			if (ok && $.isFunction(this) && !this())
				ok = false;
		});
		return ok;
	},
	_keyControlPress: function(event) {
		if (!_crgKeyControls._checkConditions())
			return;

		var key = String.fromCharCode(event.which);

		// Perform the corresponding button's action
		$(":button.KeyControl:not(.Editing):visible").has("span.Key:contains('"+key+"')").click();

		// Update the hovered button if in edit mode
		var editControls = $(":button.KeyControl.Editing");
		if (editControls.length) {
			var existingControl = editControls.filter(":not(.hover)").has("span.Key:contains('"+key+"')");
			if (existingControl.length) {
				existingControl.effect("highlight", { color: "#f00" }, 300);
			} else {
				var sbKey = editControls.filter(".hover").data("_crgKeyControls:Key");
				if (sbKey)
					sbKey.$sbSet(key);
			}
		}
	},
	_keyControlDown: function(event) {
		if (!_crgKeyControls._checkConditions())
			return;

		// Clear control key from button being hovered over
		switch (event.which) {
		case 8: // Backspace
		case 46: // Delete
			var sbKey = $(":button.KeyControl.Editing.hover").data("_crgKeyControls:Key");
			if (sbKey)
				sbKey.$sbSet("");
		}
	}
};

_crgUtils = {
	/* Bind and run a function.
	 * This is more restrictive than the actual bind() function,
	 * as only one eventType can be specified, and this does
	 * not support a map as the jQuery bind() function does.
	 * The eventData and initialParams parameters are optional.
	 * The initialParams, if provided, is an array of the parameters
	 * to supply to the initial call of the handler.
	 * The handler is initially run once for each element
	 * in the jQuery target object.
	 * As a special case, if the eventType is "content", and
	 * the initialParams are not defined, and the target
	 * is a $sb() node, the target.$sbGet() value is passed as the
	 * first and second initial parameters to the handler.
	 */
//FIXME - add parameter for initial param(s) to handler call?
	bindAndRun: function(target, eventType, eventData, handler, initialParams) {
		target.bind(eventType, eventData, handler);
		if (typeof eventData == "function") {
			initialParams = handler;
			handler = eventData;
			eventData = undefined;
		}
		target.each(function() {
			var params = [ ];
			if (initialParams)
				params = initialParams;
			else if (eventType.trim() == "content" && $sb(this))
				params = [ $sb(this).$sbGet(), $sb(this).$sbGet() ];
//FIXME - call once for each eventType after splitting by spaces?
			var event = jQuery.Event(eventType);
			event.target = event.currentTarget = this;
			if (eventData)
				event.data = eventData;
			handler.apply(this, $.merge([ event ], params));
		});
		return target;
	},

	/* Bind functions to the addition/removal of specific children.
	 * The add function is also called for each of the existing matched children.
	 * This works ONLY when using a single $sb() element as the target.
	 *
	 * Calling api is one of:
	 *   bindAddRemoveEach(target, childname, add, remove);
	 *   bindAddRemoveEach(target, parameters);
	 *
	 * Individual parameters:
	 * target: This is the $sb element, or value which can be passed to $sb(),
	 *         to which the add/remove functions are bound.
	 * childname: This is the name of the child elements to monitor
	 *            (use null/undefined/"" to match all children)
	 * add: The function to call when a child is added (use null/undefined to ignore).
	 * remove: The function to call when a child is removed (use null/undefined to ignore).
	 *
	 * If an object is used instead, the above parameters can be included plus
	 * these addition parameters:
	 * subChildren: Optional boolean to indicate if events from non-immediate
	 *              children should be processed (defaults to only immediate children).
	 * callback: A callback function that is called after this is finished (i.e.
	 *           after the add function is called for all matching children)
	 */
	bindAddRemoveEach: function(target, childname, add, remove) {
		var options = { childname: childname, add: add, remove: remove };
		if (typeof childname == "object")
			options = childname;
		target = $sb(target);
		childname = options.childname || "";
		add = options.add || $.noop;
		remove = options.remove || $.noop;
		var subChildren = options.subChildren || false;
		var callback = options.callback || $.noop;
		var addEventType = "add"+(childname?":"+childname:"");
		var removeEventType = "remove"+(childname?":"+childname:"");
		target.bind(addEventType, function(event,node) {
			if (subChildren || (event.target == this)) add(event,node);
		});
		target.bind(removeEventType, function(event,node) {
			if (subChildren || (event.target == this)) remove(event,node);
		});
		var currentChildren = (subChildren ? target.find(childname||"*") : target.children(childname||"*"));
		currentChildren.each(function() {
			var event = jQuery.Event(addEventType);
			event.target = $(this).parent()[0];
			event.currentTarget = target[0];
			add(event,$sb(this));
		});
		callback();
		return target;
	},

	showLoginDialog: function(titleText, nameText, buttonText, callback) {
		var dialog = $("<div>").append($("<a>").html(nameText)).append("<input type='text'/>");
		var login = function() {
			if (callback(dialog.find("input:text").val()))
				dialog.dialog("destroy");
		};
		dialog.find("input:text").keydown(function(event) { if (event.which == 13) login(); });
		dialog.dialog({
			modal: true,
			closeOnEscape: false,
			title: titleText,
			buttons: [ { text: buttonText, click: login } ]
		});
	},
};

_windowFunctions = {
	/* Display area dimensions */
	getAspectDimensions: function(aspect, overflow) {
		var width, height, top, bottom, left, right;
		if ((aspect > ($(window).width()/$(window).height())) == (overflow==true)) {
			width = Math.round((aspect * $(window).height()));
			height = $(window).height();
			top = bottom = 0;
			left = right = (($(window).width() - width) / 2);
		} else {
			width = $(window).width();
			height = Math.round(($(window).width() / aspect));
			top = bottom = (($(window).height() - height) / 2);
			left = right = 0;
		}
		return { width: width, height: height, top: top, bottom: bottom, left: left, right: right };
	},
	get4x3Dimensions: function(overflow) { return this.getAspectDimensions(4/3, overflow); },
	get16x9Dimensions: function(overflow) { return this.getAspectDimensions(16/9, overflow); },

	/* Text/font auto-sizing */
	enableAutoFitText: function(e, options) {
		e = $(e);
		if (e.data("AutoFitFunction"))
			return;
		e.data("AutoFitFunction", function() { return _windowFunctions.autoFitText(e, options); });
		$(window).bind("resize", e.data("AutoFitFunction"));
		setTimeout(e.data("AutoFitFunction")); // use setTimeout as page may not be laid out fully yet
		return e.data("AutoFitFunction");
	},
	disableAutoFitText: function(e) {
		if (!e.data("AutoFitFunction"))
			return;
		$(window).unbind("resize", e.data("AutoFitFunction"));
		e.removeData("AutoFitFunction");
	},
	/* This should be called each time the text content changes and/or the window resizes.
	 * It resizes the text element to fit, and returns the current relevant css properties.
	 * The options are:
	 *   max: number (default: 100)
	 *     This sets the maximum font size, in %.
	 *   overage: number (default: 0)
	 *     This sets the overage, in %.  The overage increases the vertical height allowed,
	 *     so the auto-sized text element can be vertically larger than its parent container.
	 *     Usually with text elements, there is blank space vertically surrounding the text,
	 *     so this allows pushing that vertical blank space outside the container, which
	 *     allows the actual text font to fill up more of the container.
	 *   percentHeight: number (default: 98)
	 *     This sets the minimum height value, in %, that indicates a successful fit.
	 *     Once the element's height is at least this percent of the container's height,
	 *     after accounting for overage, the auto fitting will stop at the current settings.
	 *   iterations: number (default: 10)
	 *     This is the maximum number of iterations to fine-tune the fit.
	 *     Unless the font size required is a very small number (in %),
	 *     this should not need adjustment.
	 */
	autoFitText: function(text, options) {
		var container = text.parent();
		var getTextCss = function() {
			return { fontSize: text.css("fontSize"), top: text.css("top"), position: text.css("position") };
		};

		if (!text.text())
			return getTextCss();

		if (!options)
			options = { };

		var max = options.max || 100;
		var iterations = options.iterations || 10;
		var percentHeight = (options.percentHeight || 98) / 100;
		var overage = (options.overage || 0) / 100;

		var maxW = container.innerWidth();
		var maxH = container.innerHeight();
		var overH = overage * maxH;
		var targetH = maxH + overH;

		var topSize = max, bottomSize = max;
		var overSize = function() {
			return (text.outerWidth(true) > maxW) || (text.outerHeight(true) > targetH);
		};
		var atPercentHeight = function() {
			return (text.outerHeight(true) > (percentHeight * targetH));
		};
		var updateFontSize = function(size) {
			text.css("fontSize", ((size * $(window).height()) / 100)+"px");
		};

		text.css({ position: "absolute", top: "0px" });
		updateFontSize(topSize);

		while (overSize() && (bottomSize > 0)) {
			topSize = bottomSize;
			updateFontSize(bottomSize--);
		}

		if (topSize != bottomSize) {
			for (var i=0; i<iterations; i++) {
				var newSize = ((bottomSize + topSize) / 2);
				updateFontSize(newSize);
				if (overSize())
					topSize = newSize;
				else if (atPercentHeight())
					break;
				else
					bottomSize = newSize;
			}
			if (overSize())
				updateFontSize(bottomSize);
		}

		text.css("top", ((maxH - text.outerHeight(true)) / 2)+"px");
		return getTextCss();
	},

	/* URL parameters */
	getParam: function(param) {
		var value = $.string(window.location.search).toQueryParams()[param];
		return ($.isArray(value) ? value[0] : value);
	},
	hasParam: function(param) {
		return $.string(window.location.search).toQueryParams().hasOwnProperty(param);
	},
	checkParam: function(param, value) { return value == getParam(param); },

	/* DOM element sorting */
	appendSorted: function(parent, newChild, comparator, startIndex) {
		var child = null;
		parent.children().each(function(i) {
			if ((startIndex <= i) && comparator(this, newChild[0])) {
				child = this;
				return false;
			}
		});
		if (child)
			newChild.insertBefore(child);
		else
			parent.append(newChild);
		return parent;
	},
	appendAlphaSorted: function(parent, newChild, startIndex) { return _windowFunctions.appendSorted(parent, newChild, _windowFunctions.alphaSortByNodeName, startIndex); },
	appendAttrAlphaSorted: function(parent, newChild, attrName, startIndex) { return _windowFunctions.appendSorted(parent, newChild, function(a,b) { return _windowFunctions.alphaSortByAttr(a, b, attrName); }, startIndex); },
	appendAttrNumericSorted: function(parent, newChild, attrName, startIndex) { return _windowFunctions.appendSorted(parent, newChild, function(a,b) { return _windowFunctions.numericSortByAttr(a, b, attrName); }, startIndex); },
	alphaSortByNodeName: function(dom1, dom2) { return dom1.nodeName > dom2.nodeName; },
	alphaSortByAttr: function(dom1, dom2, name) { return $(dom1).attr(name) > $(dom2).attr(name); },
	numericSortByAttr: function(dom1, dom2, name) { return Number($(dom1).attr(name)) > Number($(dom2).attr(name)); }
};


_timeConversions = {
	_formatMinSec: function(time) { return ((String(time).indexOf(":") > -1) ? time : ":"+time); },
	_msOnlySec: function(ms) {
		var s = (Math.floor(ms / 1000) % 60);
		if (10 > s) s = "0"+s;
		return String(s);
	},
	_msOnlyMin: function(ms) { return String(Math.floor(ms / 60000)); },
	_timeOnlySec: function(time) { return _timeConversions._formatMinSec(time).split(":")[1]; },
	_timeOnlyMin: function(time) { return _timeConversions._formatMinSec(time).split(":")[0]; },
	msToSeconds: function(ms) { return Math.floor(ms / 1000); },
	secondsToMs: function(sec) { return (sec * 1000); },
	msToMinSec: function(ms) { return _timeConversions._msOnlyMin(ms)+":"+_timeConversions._msOnlySec(ms); },
	minSecToMs: function(time) {
		var min = Number(_timeConversions._timeOnlyMin(time) || 0);
		var sec = Number(_timeConversions._timeOnlySec(time) || 0);
		return ((min * 60) + sec) * 1000;
	},
	msToMinSecNoZero: function(ms) {
		var min = _timeConversions._msOnlyMin(ms);
		var sec = _timeConversions._msOnlySec(ms);
		return ((min == "0" ? "" : min+":") + sec);
	},
	minSecNoZeroToMs: function(time) { return _timeConversions.minSecToMs(time); }
};
