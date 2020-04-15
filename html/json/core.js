
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

/* This file requires base jQuery; other required jQuery plugins are automatically included below. */
if (typeof $ === 'undefined') {
  alert('You MUST include jQuery before this file!');
  throw('You MUST include jQuery before this file!');
}

var _alreadyIncludedScripts = {};

function _includeUrl(url) {
  var filename = url.replace(/^.*[\/]/g, '');
  if (/\.[cC][sS][sS](\?.*)?$/.test(url) && !$('head link[href="'+url+'"],head link[href="'+filename+'"]').length) {
    $('<link>').attr({ href: url, type: 'text/css', rel: 'stylesheet'}).appendTo('head');
  } else if (/\.[jJ][sS](\?.*)?$/.test(url) && _alreadyIncludedScripts[url] === null) {
    $.ajax(url, {dataType: 'script', cache: true, async: false}).fail(function(e, s, x) {
      console.error(s + ' for ' + url + ': ' + x);
    });
    _alreadyIncludedScripts[url] = true;
  }
}

function _include(dir, files) {
  if (!files) {
    files = dir;
    dir = undefined;
  }
  if (!$.isArray(files))
    files = [ files ];
  $.each(files, function() { _includeUrl((dir?dir+'/':'')+this); });
}

_include('/external/jquery-ui', [ 'jquery-ui.min.js', 'jquery-ui.min.css' ]);

_include('/external/jquery-plugins/isjquery/jquery.isjquery.js');
_include('/external/jquery-plugins/string/jquery.string.js');
_include('/external/jquery-plugins/fileupload/jquery.fileupload.js');

/* Core functionality */
_include('/javascript', [
  'timeconversions.js',
  'windowfunctions.js',
  'autofit.js',
]);
_include('/json', [
  'WS.js',
]);

$(function() {
  if (/\.html$/.test(window.location.pathname)) {
    _include(window.location.pathname.replace(/\.html$/, '.css'));
    _include(window.location.pathname.replace(/\.html$/, '.js'));
  } else if (/\/$/.test(window.location.pathname)) {
    _include(window.location.pathname + 'index.css');
    _include(window.location.pathname + 'index.js');
  }
});

function isTrue(value) {
  if (typeof value === 'boolean')
    return value;
  else
    return (String(value).toLowerCase() === 'true');
}

function newUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {var r = Math.random()*16|0,v=c==='x'?r:r&0x3|0x8;return v.toString(16);});
}
//# sourceURL=json\core.js
