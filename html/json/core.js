if (typeof $ === 'undefined') {
  /* jshint -W117 */
  alert('You MUST include jQuery before this file!');
  /* jshint +W117 */
  throw 'You MUST include jQuery before this file!';
}

var _alreadyIncludedScripts = {};

function _includeUrl(url) {
  'use strict';
  var filename = url.replace(/^.*[\/]/g, '');
  if (/\.[cC][sS][sS](\?.*)?$/.test(url) && !$('head link[href="' + url + '"],head link[href="' + filename + '"]').length) {
    $('<link>').attr({ href: url, type: 'text/css', rel: 'stylesheet' }).appendTo('head');
  } else if (/\.[jJ][sS](\?.*)?$/.test(url) && _alreadyIncludedScripts[url] == null) {
    $.ajax(url, { dataType: 'script', cache: true, async: false }).fail(function (e, s, x) {
      /* jshint -W117 */
      console.error(s + ' for ' + url + ': ' + x);
      /* jshint +W117 */
    });
    _alreadyIncludedScripts[url] = true;
  }
}

function _include(dir, files) {
  'use strict';
  if (!files) {
    files = dir;
    dir = undefined;
  }
  if (!$.isArray(files)) {
    files = [files];
  }
  $.each(files, function () {
    _includeUrl((dir ? dir + '/' : '') + this);
  });
}

_include('/external/jquery-ui', ['jquery-ui.min.js', 'jquery-ui.min.css']);

_include('/external/jquery-plugins/isjquery/jquery.isjquery.js');
_include('/external/jquery-plugins/string/jquery.string.js');
_include('/external/jquery-plugins/fileupload/jquery.fileupload.js');

/* Core functionality */
_include('/javascript', ['timeconversions.js', 'windowfunctions.js', 'autofit.js']);
_include('/json', ['WS.js']);

$(function () {
  'use strict';
  if (/\.html$/.test(window.location.pathname)) {
    _include(window.location.pathname.replace(/\.html$/, '.css'));
    _include(window.location.pathname.replace(/\.html$/, '.js'));
  } else if (/\/$/.test(window.location.pathname)) {
    _include(window.location.pathname + 'index.css');
    _include(window.location.pathname + 'index.js');
  }
});

function isTrue(value) {
  'use strict';
  if (typeof value === 'boolean') {
    return value;
  } else {
    return String(value).toLowerCase() === 'true';
  }
}

function newUUID() {
  'use strict';
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = (Math.random() * 16) | 0,
      v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
