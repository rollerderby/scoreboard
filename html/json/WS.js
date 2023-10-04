var WS = {
  connectCallback: null,
  connectTimeout: null,
  registeredPaths: new Set(),
  callbackTrie: {},
  batchCallbacks: [],
  Connected: false,
  state: {},
  heartbeat: null,
  debug: false,

  Connect: function (callback) {
    'use strict';
    WS.connectCallback = callback;
    WS._connect();
  },

  _connect: function () {
    'use strict';
    WS.connectTimeout = null;
    var url = (document.location.protocol === 'http:' ? 'ws' : 'wss') + '://';
    url += document.location.host + '/WS/';
    // This is not required, but helps figure out which device is which.
    url += '?source=' + encodeURIComponent(document.location.pathname + document.location.search);
    var platform = '';
    if (navigator.userAgent || false) {
      var match = navigator.userAgent.match(/\((.*)\).*\(.*\)/);
      if (match) {
        platform += match[1];
      }
    }
    if (!platform) {
      platform += window.screen.width + 'x' + window.screen.height + ' ';
      if (navigator.maxTouchPoints !== undefined) {
        platform += navigator.maxTouchPoints > 0 ? 'Touchscreen ' : 'NotTouchscreen; ';
      }
    }
    url += '&platform=' + encodeURIComponent(platform);

    if (WS.Connected !== true || !WS.socket) {
      if (WS.debug) {
        console.log('WS', 'Connecting the websocket at ' + url);
      }

      WS.socket = new WebSocket(url);
      WS.socket.onopen = function (e) {
        WS.Connected = true;
        if (WS.debug) {
          console.log('WS', 'Websocket: Open');
        }
        $('.ConnectionError').addClass('Connected');
        var req = {
          action: 'Register',
          paths: [],
        };
        $.each(Object.keys(WS.state), function (idx, k) {
          WS.triggerCallback(k, null);
        });
        WS.state = {};
        for (const path of WS.registeredPaths) {
          req.paths.push(path);
        }
        $.each(WS.batchCallbacks, function (idx, c) {
          req.paths.push(c.path);
        });
        if (req.paths.length > 0) {
          WS.send(JSON.stringify(req));
        }
        if (WS.connectCallback != null) {
          WS.connectCallback();
        }
        // Hearbeat every 30s so the connection is kept alive.
        WS.heartbeat = setInterval(WS.Command, 30000, 'Ping');
      };
      WS.socket.onmessage = function (e) {
        var json = JSON.parse(e.data);
        if (WS.debug) {
          console.log('WS', json);
        }
        if (json.authorization != null) {
          alert(json.authorization);
        }
        if (json.state != null) {
          WS.processUpdate(json.state);
        }
      };
      WS.socket.onclose = function (e) {
        WS.Connected = false;
        console.log('WS', 'Websocket: Close', e);
        $('.ConnectionError').removeClass('Connected');
        if (WS.connectTimeout == null) {
          WS.connectTimeout = setTimeout(WS._connect, 1000);
        }
        clearInterval(WS.heartbeat);
      };
      WS.socket.onerror = function (e) {
        console.log('WS', 'Websocket: Error', e);
        $('.ConnectionError').removeClass('Connected');
        if (WS.connectTimeout == null) {
          WS.connectTimeout = setTimeout(WS._connect, 1000);
        }
        clearInterval(WS.heartbeat);
      };
    } else {
      // better run the callback on post connect if we didn't need to connect
      if (WS.connectCallback != null) {
        WS.connectCallback();
      }
    }
  },

  send: function (data) {
    'use strict';
    if (WS.socket != null && WS.socket.readyState === 1) {
      WS.socket.send(data);
    }
  },

  Command: function (command, data) {
    'use strict';
    var req = {
      action: command,
      data: data,
    };
    WS.send(JSON.stringify(req));
  },

  Set: function (key, value, flag) {
    'use strict';
    var req = {
      action: 'Set',
      key: key,
      value: value,
      flag: typeof flag !== 'undefined' ? flag : '',
    };
    WS.send(JSON.stringify(req));
  },

  triggerCallback: function (k, v) {
    'use strict';
    k = WS._enrichProp(k);
    var callbacks = WS._getMatchesFromTrie(WS.callbackTrie, k);
    for (var idx = 0; idx < callbacks.length; idx++) {
      var c = callbacks[idx];
      if (c == null) {
        continue;
      }
      try {
        c(k, v);
      } catch (err) {
        console.log(err.message, err.stack);
      }
    }
  },

  processUpdate: function (state) {
    'use strict';
    var prop;
    for (prop in state) {
      // update all incoming properties before triggers
      // dependency issues causing problems
      if (state[prop] == null) {
        delete WS.state[prop];
      } else {
        WS.state[prop] = state[prop];
      }
    }

    for (prop in state) {
      if (state[prop] == null) {
        WS.triggerCallback(prop, state[prop]);
      }
    }
    for (prop in state) {
      if (state[prop] != null) {
        WS.triggerCallback(prop, state[prop]);
      }
    }

    // Batch functions are only called once per update.
    // This is useful to avoid n^2 operations when
    // every callback redraws everything.
    var batched = {};
    $.each(WS.batchCallbacks, function (idx, c) {
      if (c.callback == null) {
        return;
      }
      for (prop in state) {
        if (prop.indexOf(c.path) === 0) {
          batched[c.callback] = c.callback;
          return;
        }
      }
    });
    $.each(batched, function (idx, c) {
      try {
        c();
      } catch (err) {
        console.log(err.message, err.stack);
      }
    });

    // Clean cache to avoid a memory leak for long running screens.
    for (prop in state) {
      if (state[prop] == null) {
        delete WS._enrichPropCache[prop];
      }
    }
  },

  _enrichPropCache: {},

  // Parse property name, and make it easily accessible.
  _enrichProp: function (prop) {
    'use strict';
    if (WS._enrichPropCache[prop] != null) {
      return WS._enrichPropCache[prop];
    }
    /* jshint -W053 */
    prop = new String(prop);
    /* jshint +W053 */
    var i = prop.length - 1;
    var parts = [];
    while (i >= 0) {
      var dot;
      var key;
      var val = '';
      if (prop[i] === ')') {
        var open = prop.lastIndexOf('(', i);
        dot = prop.lastIndexOf('.', open);
        key = prop.substring(dot + 1, open);
        val = prop.substring(open + 1, i);
      } else {
        dot = prop.lastIndexOf('.', i);
        key = prop.substring(dot + 1, i + 1);
      }
      prop[key] = val;
      parts.push(key);
      i = dot - 1;
    }
    prop.field = parts[0];
    parts.reverse();
    prop.parts = parts;
    WS._enrichPropCache[prop] = prop;
    return prop;
  },

  Register: function (paths, options) {
    'use strict';
    if ($.isFunction(options)) {
      options = { triggerFunc: options };
    }

    var callback = null;
    var batchCallback = null;
    if (options == null) {
      callback = null;
    } else {
      if (options.triggerFunc != null) {
        callback = options.triggerFunc;
      } else if (options.triggerBatchFunc != null) {
        batchCallback = options.triggerBatchFunc;
      } else {
        var elem = options.element;
        if (options.css != null) {
          callback = function (k, v) {
            elem.css(options.css, v == null ? '' : v);
          };
        } else if (options.attr != null) {
          callback = function (k, v) {
            elem.attr(options.attr, v);
          };
        } else if (options.toggleClass != null) {
          callback = function (k, v) {
            elem.toggleClass(options.toggleClass, v);
          };
        } else if (options.set) {
          callback = function (k, v) {
            elem.val(v);
          };
        } else {
          if (elem.hasClass('AutoFit')) {
            elem.empty();
            var div = $('<div>').css('width', '100%').css('height', '100%').appendTo(elem);
            elem = $('<a>').appendTo(div);
            var autofit = _autoFit.enableAutoFitText(div);

            callback = function (k, v) {
              elem.text(v);
              setTimeout(autofit, 50); // delay so other elements are updated first
            };
          } else if (elem.parent().hasClass('AutoFit')) {
            var parenAutofit = _autoFit.enableAutoFitText(elem.parent());
            callback = function (k, v) {
              elem.text(v);
              setTimeout(parenAutofit, 50); // delay so other elements are updated first
            };
          } else {
            callback = function (k, v) {
              elem.text(v);
            };
          }
        }
      }

      if (options.modifyFunc != null) {
        var origCallback = callback;
        callback = function (k, v) {
          origCallback(k, options.modifyFunc(k, v));
        };
      }
    }

    if (!$.isArray(paths)) {
      paths = [paths];
    }

    if ($.isFunction(batchCallback)) {
      $.each(paths, function (idx, path) {
        WS.batchCallbacks.push({ path: path, callback: batchCallback });
      });
    }

    if ($.isFunction(callback)) {
      $.each(paths, function (idx, path) {
        WS._addToTrie(WS.callbackTrie, path, callback);
      });
      if (options && options.element != null) {
        if (paths.length) {
          callback(WS._enrichProp(paths[0]), WS.state[paths[0]]);
          paths = []; // they have been registered with the backend during pre registration
        } else {
          callback();
        }
      }
    }

    $.each(paths, function (idx, path) {
      WS.registeredPaths.add(path);
    });

    if (paths.length) {
      var req = {
        action: 'Register',
        paths: paths,
      };
      WS.send(JSON.stringify(req));
    }
  },

  _addToTrie: function (t, key, value) {
    'use strict';
    var p = key.split(/[.(]/);
    for (var i = 0; i < p.length; i++) {
      var c = p[i];
      t[c] = t[c] || {};
      t = t[c];
    }
    t.values = t.values || [];
    t.values.push(value);
  },

  _getMatchesFromTrie: function (trie, key) {
    'use strict';
    function matches(t, p, i) {
      var result = t.values || [];
      for (; i < p.length; i++) {
        if (t['*)'] != null) {
          // Allow Blah(*) as a wildcard.
          var j;
          // id captured by * might contain . and thus be split - find the end
          for (j = i; j < p.length && !p[j].endsWith(')'); j++) {}
          result = result.concat(matches(t['*)'], p, j + 1) || []);
        }
        t = t[p[i]];
        if (t == null) {
          break;
        }
        result = result.concat(t.values || []);
      }
      return result;
    }

    return matches(trie, key.split(/[.(]/), 0);
  },

  _getParameters: function (elem, attr, pathIndex) {
    'use strict';
    pathIndex = pathIndex || 0;
    return elem
      .attr(attr)
      .split('|')
      .map(function (part) {
        var list = part.split(':').map($.trim);
        var basePath = WS._getContext(elem);
        var prefixes = WS._getPrefixes(elem);
        list[pathIndex] = list[pathIndex].split(',').map(function (item) {
          item = $.trim(item);
          if (item.startsWith('/')) {
            item = item.substring(1);
          } else if (prefixes[item[0]]) {
            item = prefixes[item[0]] + item.substring(1);
          } else if (attr === 'sbForeach') {
            item = basePath + '.Id';
          } else {
            var restPath = basePath;
            while (restPath && item.startsWith('^')) {
              item = item.substring(1);
              restPath = restPath.substring(0, restPath.lastIndexOf('.'));
            }
            if (restPath) {
              item = restPath + '.' + item;
            }
          }
          return item;
        });
        return list;
      });
  },

  _getAutoFitPaths: function (elem) {
    'use strict';
    if (elem.attr('sbAutoFitOn')) {
      return WS._getParameters(elem, 'sbAutoFitOn')[0][0];
    } else {
      return [];
    }
  },

  _getElements: function (selector, root) {
    var filterFunc =
      selector === '[sbForeach]'
        ? function () {
            return !$(this).parent().closest('[sbForeach]').length;
          }
        : function () {
            return !$(this).closest('[sbForeach]').length;
          };
    return $(selector, root)
      .add(root ? root.filter(selector) : $())
      .filter(filterFunc);
  },

  _getModifyFunc: function (paths, func) {
    if (!$.isFunction(func)) {
      func = window[func];
    }
    if (paths.length < 2) {
      return func;
    } else {
      return function () {
        var v = null;
        var path;
        for (var i = 0; i < paths.length; i++) {
          path = paths[i];
          if (WS.state[path] != null) {
            v = WS.state[path];
            break;
          }
        }
        if ($.isFunction(func)) {
          v = func(WS._enrichProp(path), v);
        }
        return v;
      };
    }
  },

  AutoRegister: function (root) {
    'use strict';
    if (!root) {
      WS._preRegister();
    }
    $.each(WS._getElements('[sbInclude]', root), function (idx, elem) {
      elem = $(elem);
      elem.load(elem.attr('sbInclude'), function () {
        elem.attr('sbInclude', null);
        AutoRegister(elem);
      });
    });
    $.each(WS._getElements('.AutoFit:not([sbDisplay]):not(:has(*))', root), function (idx, elem) {
      elem = $(elem);
      const text = elem.text();
      const mf = function () {
        return text;
      };
      WS.Register(WS._getAutoFitPaths(elem), { element: elem, modifyFunc: mf });
    });
    $.each(WS._getElements('[sbDisplay]', root), function (idx, elem) {
      elem = $(elem);
      const [paths, func] = WS._getParameters(elem, 'sbDisplay')[0];
      WS.Register(paths.concat(WS._getAutoFitPaths(elem)), { element: elem, modifyFunc: WS._getModifyFunc(paths, func) });
    });
    $.each(WS._getElements('[sbSet]', root), function (idx, elem) {
      elem = $(elem);
      const entries = WS._getParameters(elem, 'sbSet');
      elem.on('click', function () {
        entries.map(function (entry) {
          const writeFunc = window[entry[1]] || Function('k', 'return ' + entry[1]);
          entry[0].map(function (path) {
            WS.Set(path, writeFunc(WS._enrichProp(path)));
          });
        });
      });
    });
    $.each(WS._getElements('[sbControl]', root), function (idx, elem) {
      elem = $(elem);
      let [paths, readFunc, writeFunc] = WS._getParameters(elem, 'sbControl')[0];
      writeFunc =
        window[writeFunc] ||
        function (v) {
          return v;
        };
      WS.Register(paths, { element: elem, set: true, modifyFunc: WS._getModifyFunc(paths, readFunc) });
      elem.on('change', function () {
        WS.Set(paths[0], writeFunc(elem.val()));
      });
    });
    $.each(WS._getElements('[sbToggleClass]', root), function (idx, elem) {
      elem = $(elem);
      WS._getParameters(elem, 'sbToggleClass', 1).map(function (toggle) {
        const toggledClass = toggle[0];
        const paths = toggle[1];
        let func = toggle[2];
        if ($.isFunction(window[func])) {
          func = window[func];
        } else if (func === '!') {
          func = function (k, v) {
            return !isTrue(v);
          };
        } else if (!func) {
          func = function (k, v) {
            return isTrue(v);
          };
        } else {
          func = Function('k', 'v', 'return v' + func);
        }

        WS.Register(paths, { element: elem, toggleClass: toggledClass, modifyFunc: WS._getModifyFunc(paths, func) });
      });
    });
    $.each(WS._getElements('[sbCss]', root), function (idx, elem) {
      elem = $(elem);
      WS._getParameters(elem, 'sbCss', 1).map(function (entry) {
        const paths = entry[1];

        WS.Register(paths, { element: elem, css: entry[0], modifyFunc: WS._getModifyFunc(paths, entry[2]) });
      });
    });
    $.each(WS._getElements('[sbAttr]', root), function (idx, elem) {
      elem = $(elem);
      WS._getParameters(elem, 'sbAttr', 1).map(function (entry) {
        const paths = entry[1];

        WS.Register(paths, { element: elem, attr: entry[0], modifyFunc: WS._getModifyFunc(paths, entry[2]) });
      });
    });
    $.each(WS._getElements('[sbForeach]', root), function (idx, elem) {
      elem = $(elem);
      let paren = elem.parent();
      let index = elem.index();
      let [paths, fixedKeys, sortFunction] = WS._getParameters(elem, 'sbForeach')[0];
      if (fixedKeys) {
        fixedKeys = fixedKeys.split(',').map($.trim);
      }
      elem.detach().removeAttr('sbForeach');
      $.each(paths, function (idx, path) {
        const field = path.substring(path.lastIndexOf('.', path.length - 6) + 1, path.length - 6); // cut off (*).Id
        $.each(fixedKeys, function (idx, key) {
          const newElem = elem
            .clone(true)
            .attr(field, key)
            .attr('sbContext', (elem.attr('sbContext') ? elem.attr('sbContext') + '.' : '') + field + '(' + key + ')')
            .addClass('Fixed');
          if (index === 0) {
            paren.prepend(newElem);
          } else {
            newElem.insertAfter(paren.children(':nth-child(' + index + ')'));
          }
          index++;
          WS.AutoRegister(newElem);
        });
        WS.Register(path, function (k, v) {
          if (v == null) {
            paren.children(('[' + field + '=' + k[field] + ']:not().Fixed)').remove());
          } else if (!paren.children('[' + field + '=' + k[field] + ']').length) {
            const newElem = elem
              .clone(true)
              .attr(field, k[field])
              .attr('sbContext', (elem.attr('sbContext') ? elem.attr('sbContext') + '.' : '') + field + '(' + k[field] + ')')
              .appendTo(paren);
            WS.AutoRegister(newElem);
            newElem.detach();
            _windowFunctions.appendSorted(
              paren,
              newElem,
              window[sortFunction] ||
                function (a, b) {
                  return _windowFunctions.numCompareByAttr(field, a, b);
                },
              index
            );
          }
        });
      });
    });
  },

  _preRegister: function () {
    let paths = [];
    const preRegisterAttribute = function (attr, pathIndex) {
      $.each($('[' + attr + ']'), function (idx, elem) {
        $.each(WS._getParameters($(elem), attr, pathIndex), function (idx, params) {
          paths = paths.concat(params[pathIndex]);
        });
      });
    };

    $.each(['sbForeach', 'sbDisplay', 'sbControl'], function (idx, attr) {
      preRegisterAttribute(attr, 0);
    });
    $.each(['sbToggleClass', 'sbCss', 'sbAttr'], function (idx, attr) {
      preRegisterAttribute(attr, 1);
    });

    WS.Register(paths);
  },

  _getContext: function (elem) {
    'use strict';
    const parent = elem.parent();
    let ret = '';
    if (parent.length > 0) {
      ret = WS._getContext(parent);
    }
    const context = elem.attr('sbContext');
    const foreach = elem.attr('sbForeach');
    if (context != null) {
      ret = (ret !== '' ? ret + '.' : '') + context;
    }
    if (foreach != null) {
      ret = (ret !== '' ? ret + '.' : '') + foreach.split(':', 1)[0] + '(*)';
    }
    return ret;
  },

  _getPrefixes: function (elem) {
    'use strict';
    let prefixes = {};
    $.each(elem.parents('[sbPrefix]').addBack('[sbPrefix]'), function (idx, paren) {
      $(paren)
        .attr('sbPrefix')
        .split('|')
        .map(function (entry) {
          entry = entry.split('=').map($.trim);
          prefixes[entry[0]] = entry[1];
        });
    });
    return prefixes;
  },
};
