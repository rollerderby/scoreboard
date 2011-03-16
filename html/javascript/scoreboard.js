
/* This file requires base jQuery; other required jQuery plugins are automatically included below. */
if (typeof $ == "undefined") {
	alert("You MUST include jQuery before this file!");
	throw("You MUST include jQuery before this file!");
}

function _includeUrl(url) {
	if (/\.[cC][sS][sS](\?.*)?$/.test(url))
		$("<link>").attr({ href: url, type: "text/css", rel: "stylesheet"}).appendTo("head");
	else if (/\.[jJ][sS](\?.*)?$/.test(url))
		$("<script>").attr({ src: url, type: "text/javascript" }).appendTo("head");
}

function _include(dir, files) {
	if (!files) {
		files = dir;
		dir = undefined;
	}
	if (!$.isArray(files))
		files = [ files ];
	$.each(files, function() { _includeUrl((dir?dir+"/":"")+this); });
}

_include("/external/jquery-ui", [ "jquery-ui.js", "css/default/jquery-ui.css" ]);

_include("/external/colorpicker", [ "colorpicker.js", "css/colorpicker.css" ]);

_include("/external/jstree/jquery.tree.js");
$.jstree._themes = "/external/jstree/themes/";

_include("/external/treeview", [ "jquery.treeview.js", "jquery.treeview.css" ]);

_include("/external/jquery/isjquery/jquery.isjquery.js");
_include("/external/jquery/attributes", [ "jquery.listAttributes.js", "jquery.mapAttributes.js" ]);
_include("/external/jquery/periodicalupdater/jquery.periodicalupdater.js");
_include("/external/jquery/protify/jquery.protify.js");
_include("/external/jquery/string/jquery.string.js");
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
