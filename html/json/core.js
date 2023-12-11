if (typeof $ === 'undefined') {
  alert('You MUST include jQuery before this file!');
  throw 'You MUST include jQuery before this file!';
}

var _alreadyIncludedScripts = {};

function _includeUrl(url, callback) {
  'use strict';
  var filename = url.replace(/^.*[\/]/g, '');
  if (/\.[cC][sS][sS](\?.*)?$/.test(url) && !$('head link[href="' + url + '"],head link[href="' + filename + '"]').length) {
    $('<link>').attr({ href: url, type: 'text/css', rel: 'stylesheet' }).appendTo('head');
  } else if (/\.[jJ][sS](\?.*)?$/.test(url) && !_alreadyIncludedScripts[url]) {
    _alreadyIncludedScripts[url] = true;
    $.ajax(url, { dataType: 'script', cache: true })
      .fail(function (x, s, e) {
        console.error(s + ' for ' + url + ': ' + e);
      })
      .always(callback);
  }
}

function _include(dir, files, callback) {
  'use strict';
  if (!files) {
    files = dir;
    dir = undefined;
  }
  if (!$.isArray(files)) {
    files = [files];
  }
  $.each(files, function () {
    _includeUrl((dir ? dir + '/' : '') + this, callback);
  });
}

function _includeJsAndCss(path, callback) {
  'use strict';
  if (/\.html$/.test(path)) {
    _include(path.replace(/\.html$/, '.css'));
    _include(path.replace(/\.html$/, '.js'), null, callback);
  } else if (/\/$/.test(path)) {
    _include(path + 'index.css');
    _include(path + 'index.js', null, callback);
  } else {
    callback();
  }
}

_include('/external/jquery-ui', ['jquery-ui.min.js', 'jquery-ui.min.css']);

_include('/external/jquery-plugins/isjquery/jquery.isjquery.js');
_include('/external/jquery-plugins/string/jquery.string.js');
_include('/external/jquery-plugins/fileupload/jquery.fileupload.js');

/* Core functionality */
_include('/javascript', ['timeconversions.js', 'windowfunctions.js', 'autofit.js', 'conversions.js']);
_include('/styles', ['fonts.css', 'common.css']);
_include('/json', ['WS.js'], function () {
  'use strict';
  WS.Connect();
  WS.Process(window.location.pathname);
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
