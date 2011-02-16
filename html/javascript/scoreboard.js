
/* This file requires base jQuery; other required jQuery plugins are automatically included below. */
if (typeof $ == "undefined") {
	alert("You MUST include jQuery before this file!");
	throw("You MUST include jQuery before this file!");
}

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

/* Core functionality */
_include("/javascript", [
	"core.js",
	"controls.js",
	"timeconversions.js",
	"keycontrols.js",
	"utils.js",
	"windowfunctions.js" ]);


/* Start ScoreBoard server polling */
$(_crgScoreBoard.scoreBoardRegister);
