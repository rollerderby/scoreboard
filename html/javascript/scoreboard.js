
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

/* This file requires base jQuery; other required jQuery plugins are automatically included below. */
if (typeof $ == "undefined") {
	alert("You MUST include jQuery before this file!");
	throw("You MUST include jQuery before this file!");
}

function _includeUrl(url) {
	var filename = url.replace(/^.*[\/]/g, "");
	/* Use HTTP HEAD to verify url exists before adding it to the document */
	if ($.ajax(url, { async: false, type: "HEAD", global: false }).status != 200)
		return;
	if (/\.[cC][sS][sS](\?.*)?$/.test(url) && !$("head link[href='"+url+"'],head link[href='"+filename+"']").length)
		$("<link>").attr({ href: url, type: "text/css", rel: "stylesheet"}).appendTo("head");
	else if (/\.[jJ][sS](\?.*)?$/.test(url) && !$("head script[src='"+url+"'],head script[src='"+filename+"']").length)
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
_include("/external/spectrum", [ "spectrum.js", "spectrum.css" ]);

_include("/external/jquery-plugins/isjquery/jquery.isjquery.js");
_include("/external/jquery-plugins/string/jquery.string.js");
_include("/external/jquery-plugins/fileupload/jquery.fileupload.js");

/* Good places to find fonts are:
 * http://fontspace.com/
 * http://fontsquirrel.com/
 * Also very handy is the @font-face generator at fontsquirrel:
 * http://www.fontsquirrel.com/fontface/generator
 */
_include("/fonts", [
	"liberationsans/stylesheet.css", "roboto/stylesheet.css" ]);

/* Core functionality */
_include("/javascript", [
	"autofit.js",
	"core.js",
	"controls.js",
	"timeconversions.js",
	"keycontrols.js",
	"utils.js",
	"windowfunctions.js",
	"sortedtable.js" ]);

/* Start ScoreBoard server polling */
$(function() {
	_crgUtils.showBrowserWarning(_crgScoreBoard.scoreBoardRegister, {
		msie:
			"Internet Explorer is not supported as it is not w3c standards compliant."+
			"The scoreboard will not work."+
			"You should use either Google Chrome or Mozilla Firefox instead."
	});
});

