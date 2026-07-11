if (typeof $ === 'undefined') {
  alert('You MUST include jQuery before this file!');
  throw 'You MUST include jQuery before this file!';
}

var _alreadyIncludedScripts = {};

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
const theme = new URL(window.location).searchParams.get('theme');
if (theme) {
  _include(theme);
}

function _addThemeToUrl(href) {
  if (!theme || !href) {
    return href;
  }

  const trimmedHref = href.trim();
  
  // Do not alter links to non URLs.
  if (
    trimmedHref.startsWith('#') ||
    trimmedHref.startsWith('javascript:') ||
    trimmedHref.startsWith('mailto:') ||
    trimmedHref.startsWith('tel:') ||
    trimmedHref.startsWith('data:')
  ) {
    return href;
  }

  let url;

  try {
    url = new URL(href, window.location.href);
  } catch (error) {
    console.warn('Unable to add theme to URL:', href, error);
    return href;
  }

  // Do not alter links to other websites or hosts.
  if (url.origin !== window.location.origin) {
    return href;
  }

  url.searchParams.set('theme', theme);

  return url.pathname + url.search + url.hash;
}


function _propagateThemeToLinks(root) {
  if (!theme) {
    return;
  }

  const container = root instanceof Element || root instanceof Document
    ? root
    : document;


  const links = [];

  if (container instanceof Element && container.matches('a[href]')) {
    links.push(container);
  }

  container.querySelectorAll('a[href]').forEach(function (link) {
    links.push(link);
  });

  links.forEach(function (link) {
    const href = link.getAttribute('href');
    const themedHref = _addThemeToUrl(href);

    if (themedHref !== href) {
      link.setAttribute('href', themedHref);
    }
  });
}

if (theme) {
  $(function () {
    _propagateThemeToLinks(document);

    const themeLinkObserver = new MutationObserver(function (mutations) {
      mutations.forEach(function (mutation) {

        mutation.addedNodes.forEach(function (node) {
          if (node.nodeType === Node.ELEMENT_NODE) {
            _propagateThemeToLinks(node);
          }
        });

        if (
          mutation.type === 'attributes' &&
          mutation.target instanceof Element &&
          mutation.target.matches('a[href]')
        ) {
          _propagateThemeToLinks(mutation.target);
        }
      });
    });

    themeLinkObserver.observe(document.documentElement, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['href']
    });
  });
}
