if (typeof $ === 'undefined') {
  alert('You MUST include jQuery before this file!');
  throw 'You MUST include jQuery before this file!';
}

var _alreadyIncludedScripts = {};

function _getUrlParam(param) {
  var entries = window.location.search.substring(1).split('&');
  for (var i = 0; i < entries.length; i++) {
    var separator = entries[i].indexOf('=');
    var key = separator < 0 ? entries[i] : entries[i].substring(0, separator);
    if (decodeURIComponent(key.replace(/\+/g, ' ')) === param) {
      var value = separator < 0 ? '' : entries[i].substring(separator + 1);
      return decodeURIComponent(value.replace(/\+/g, ' '));
    }
  }
  return null;
}

function _urlWithParam(param, value) {
  var entries = window.location.search.substring(1) === '' ? [] : window.location.search.substring(1).split('&');
  var found = false;
  for (var i = 0; i < entries.length; i++) {
    var separator = entries[i].indexOf('=');
    var key = separator < 0 ? entries[i] : entries[i].substring(0, separator);
    if (decodeURIComponent(key.replace(/\+/g, ' ')) === param) {
      entries[i] = encodeURIComponent(param) + '=' + encodeURIComponent(value);
      found = true;
      break;
    }
  }
  if (!found) {
    entries.push(encodeURIComponent(param) + '=' + encodeURIComponent(value));
  }
  var newSearch = entries.length ? '?' + entries.join('&') : '';
  return window.location.pathname + newSearch + window.location.hash;
}

function _includeUrl(url, callback) {
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
  if (!files) {
    files = dir;
    dir = undefined;
  }
  if (!Array.isArray(files)) {
    files = [files];
  }
  $.each(files, function () {
    _includeUrl((dir ? dir + '/' : '') + this, callback);
  });
}

function _includeJsAndCss(path, callback) {
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

_include('/external/jquery-ui', ['jquery-ui.min.js', 'jquery-ui.structure.min.css', 'jquery-ui.theme.css']);

_include('/external/jquery-fileupload/jquery.fileupload.js');

/* Core functionality */
_include('/javascript', [
  'timeconversions.js',
  'windowfunctions.js',
  'autofit.js',
  'conversions.js',
  'boolconversions.js',
  'cssfunctions.js',
  'sortfunctions.js',
  'utils.js',
]);
_include('/fonts/inter', ['inter.css']);
_include('/styles', ['fonts.css', 'common.css']);
_include('/json', ['WS.js'], function () {
  WS.Connect();
  WS.Process(window.location.pathname);
});
const theme = _getUrlParam('theme');
if (theme) {
  _include(theme);
}
