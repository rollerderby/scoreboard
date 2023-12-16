let WS = {
  connectCallback: null,
  connectTimeout: null,
  _registerOnConnect: [],
  _afterLoadCallbacks: [],
  Connected: false,
  _connectionStatus: $('<div>').attr('id', 'sbConnectionStatus').attr('status', 'loading').text('Loading'),
  _started: false,
  _preRegisterDone: false,
  state: {},
  heartbeat: null,
  debug: false,

  Connect: function (callback) {
    'use strict';
    WS.connectCallback = callback;
    WS._connect();
    if (!WS._started) {
      WS._started = true;
      WS._connectionStatus.appendTo('body');
    }
  },

  _connect: function () {
    'use strict';
    WS.connectTimeout = null;
    let url = (document.location.protocol === 'http:' ? 'ws' : 'wss') + '://';
    url += document.location.host + '/WS/';
    // This is not required, but helps figure out which device is which.
    url += '?source=' + encodeURIComponent(document.location.pathname + document.location.search);
    let platform = '';
    if (navigator.userAgent || false) {
      const match = navigator.userAgent.match(/\((.*)\).*\(.*\)/);
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
        WS._connectionStatus.attr('status', 'loading').text('Loading...');
        $.each(Object.keys(WS.state), function (idx, k) {
          WS._nullCallbacks(k);
        });
        WS.state = {};
        if (WS._preRegisterDone) {
          let req = {
            action: 'Register',
            paths: WS._registerOnConnect,
          };
          WS.send(JSON.stringify(req));
        }
        if (WS.connectCallback != null) {
          WS.connectCallback();
        }
        // Hearbeat every 30s so the connection is kept alive.
        WS.heartbeat = setInterval(WS.Command, 30000, 'Ping');
      };
      WS.socket.onmessage = function (e) {
        const json = JSON.parse(e.data);
        if (WS.debug) {
          console.log('WS', json);
        }
        if (json.authorization != null) {
          alert(json.authorization);
        }
        if (json.state != null) {
          WS.processUpdate(json.state);
          if (!json.state['WS.Device.Id']) {
            // skip initial pseudo update
            if (WS._afterLoadCallbacks.length) {
              $.each(WS._afterLoadCallbacks, function (idx, func) {
                func();
              });
              WS._afterLoadCallbacks = [];
            }
            WS._connectionStatus.attr('status', 'ready').text('');
          }
        }
      };
      WS.socket.onclose = function (e) {
        WS.Connected = false;
        console.log('WS', 'Websocket: Close', e);
        WS._connectionStatus.attr('status', 'error').text('Not connected');
        if (WS.connectTimeout == null) {
          WS.connectTimeout = setTimeout(WS._connect, 1000);
        }
        clearInterval(WS.heartbeat);
      };
      WS.socket.onerror = function (e) {
        console.log('WS', 'Websocket: Error', e);
        WS._connectionStatus.attr('status', 'error').text('Not connected');
        if (WS.connectTimeout == null) {
          WS.connectTimeout = setTimeout(WS._connect, 1000);
        }
        clearInterval(WS.heartbeat);
      };
    } else {
      // run the post connect callback if we didn't need to connect
      if (WS.connectCallback != null) {
        WS.connectCallback();
      }
    }
  },

  AfterLoad: function (func) {
    WS._afterLoadCallbacks.push(func);
  },

  send: function (data) {
    'use strict';
    if (WS.socket != null && WS.socket.readyState === 1) {
      WS.socket.send(data);
    }
  },

  Command: function (command, data) {
    'use strict';
    const req = {
      action: command,
      data: data,
    };
    WS.send(JSON.stringify(req));
  },

  Set: function (key, value, flag) {
    'use strict';
    const req = {
      action: 'Set',
      key: key,
      value: value,
      flag: typeof flag !== 'undefined' ? flag : '',
    };
    WS.send(JSON.stringify(req));
  },

  _nullCallbacks: function (k) {
    'use strict';
    k = WS._enrichProp(k);
    WS._getMatchesFromTrie(k).plain.forEach(function (v) {
      try {
        v.func(k, null, v.elem);
      } catch (err) {
        console.error(err.message);
        console.log(err.stack);
      }
    });
  },

  processUpdate: function (state) {
    'use strict';
    // update all incoming properties before triggers
    // dependency issues causing problems
    let plainNull = [];
    let plain = [];
    // Batch functions are only called once per update.
    // This is useful to avoid n^2 operations when
    // every callback redraws everything.
    let batched = new Set();
    $.each(state, function (k, v) {
      k = WS._enrichProp(k);
      const callbacks = WS._getMatchesFromTrie(k);

      if (v == null) {
        delete WS.state[k];
        callbacks.plain.forEach(function (c) {
          plainNull.push({ callback: c.func, path: k, elem: c.elem });
        });
        // Clean cache to avoid a memory leak for long running screens.
        delete WS._enrichPropCache[k];
      } else {
        WS.state[k] = v;
        callbacks.plain.forEach(function (c) {
          plain.push({ callback: c.func, path: k, value: v, elem: c.elem });
        });
      }
      callbacks.batched.forEach(function (c) {
        batched.add(c);
      });
    });
    plainNull.forEach(function (el) {
      try {
        el.callback(el.path, null, el.elem);
      } catch (err) {
        console.error(err.message);
        console.log(err.stack);
      }
    });
    plain.forEach(function (el) {
      try {
        el.callback(el.path, el.value, el.elem);
      } catch (err) {
        console.error(err.message);
        console.log(err.stack);
      }
    });
    batched.forEach(function (el) {
      try {
        el();
      } catch (err) {
        console.error(err.message);
        console.log(err.stack);
      }
    });
  },

  _enrichPropCache: {},

  // Parse property name, and make it easily accessible.
  _enrichProp: function (prop) {
    'use strict';
    if (WS._enrichPropCache[prop] != null) {
      return WS._enrichPropCache[prop];
    }
    prop = new String(prop);
    let i = prop.length - 1;
    let parts = [];
    while (i >= 0) {
      let dot;
      let key;
      let val = '';
      if (prop[i] === ')') {
        const open = prop.lastIndexOf('(', i);
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
    prop.upTo = function (key) {
      return prop.substring(0, prop.indexOf('.', prop.indexOf(key)));
    };
    WS._enrichPropCache[prop] = prop;
    return prop;
  },

  Register: function (paths, options, isPreRegister) {
    'use strict';
    if ($.isFunction(options)) {
      options = { triggerFunc: options };
    }

    let callback = null;
    let batchCallback = null;
    let elem = null;
    if (options == null) {
      callback = null;
    } else {
      elem = options.element;
      if (options.triggerFunc != null) {
        callback = options.triggerFunc;
      } else if (options.triggerBatchFunc != null) {
        batchCallback = options.triggerBatchFunc;
      } else {
        if (options.css != null) {
          callback = function (k, v) {
            elem.css(options.css, v == null ? '' : v);
          };
        } else if (options.attr != null) {
          callback = function (k, v) {
            elem.attr(options.attr, v);
          };
        } else if (options.prop != null) {
          callback = function (k, v) {
            elem.prop(options.prop, v);
            if (options.prop === 'disabled' && elem.hasClass('ui-button')) {
              elem.toggleClass('ui-state-disabled', v);
            }
          };
        } else if (options.toggleClass != null) {
          callback = function (k, v) {
            elem.toggleClass(options.toggleClass, v);
          };
        } else if (options.set) {
          callback = function (k, v) {
            elem.val(v);
          };
        } else if (options.html) {
          callback = function (k, v) {
            elem.html(v);
          };
        } else {
          if (elem.hasClass('AutoFit')) {
            elem.empty();
            const div = $('<div>').css('width', '100%').css('height', '100%').appendTo(elem);
            elem = $('<a>').appendTo(div);
            const autofit = _autoFit.enableAutoFitText(div);

            callback = function (k, v) {
              elem.text(v);
              setTimeout(autofit, 50); // delay so other elements are updated first
            };
          } else if (elem.parent().hasClass('AutoFit')) {
            const parenAutofit = _autoFit.enableAutoFitText(elem.parent());
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
        const origCallback = callback;
        callback = function (k, v, el) {
          origCallback(k, options.modifyFunc(k, v, el), el);
        };
      }
    }

    if (!$.isArray(paths)) {
      paths = [paths];
    }

    if ($.isFunction(batchCallback)) {
      $.each(paths, function (idx, path) {
        WS._addToTrie(path, batchCallback, 'batched');
      });
    }

    if ($.isFunction(callback)) {
      $.each(paths, function (idx, path) {
        WS._addToTrie(path, { func: callback, elem: elem }, 'plain');
      });
      if (options && options.preRegistered) {
        if (paths.length) {
          if (paths.length === 1 && paths[0].includes('*)')) {
            const regexp = new RegExp(
              paths[0].replaceAll('.', '\\.').replaceAll('(', '\\(').replaceAll(')', '\\)').replaceAll('*', '[^)]*')
            );
            $.each(WS.state, function (k, v) {
              if (regexp.test(k)) {
                callback(WS._enrichProp(k), v, elem);
              }
            });
          } else {
            callback(WS._enrichProp(paths[0]), WS.state[paths[0]], elem);
          }
          paths = []; // they have been registered with the backend during pre registration
        } else {
          callback();
        }
      }
    }

    WS._preRegisterDone = WS._preRegisterDone || isPreRegister;
    if (paths.length) {
      WS._registerOnConnect.push(...paths);
      if (WS.Connected && WS._preRegisterDone) {
        const req = {
          action: 'Register',
          paths: isPreRegister ? WS._registerOnConnect : paths,
        };
        WS.send(JSON.stringify(req));
      }
    }
  },

  _callbackTrie: {},
  _addToTrie: function (key, value, type) {
    'use strict';
    let t = WS._callbackTrie;
    const p = key.split(/[.(]/);
    for (let i = 0; i < p.length; i++) {
      const c = p[i];
      t[c] = t[c] || {};
      t = t[c];
    }
    t._values = t._values || { plain: [], batched: [] };
    t._values[type].push(value);
  },

  _getMatchesFromTrie: function (key) {
    'use strict';
    function findMatches(t, p, i, result) {
      result.merge(t._values);
      for (; i < p.length; i++) {
        if (t['*)'] != null) {
          // Allow Blah(*) as a wildcard.
          let j = i;
          // id captured by * might contain . and thus be split - find the end
          while (j < p.length && !p[j].endsWith(')')) {
            j++;
          }
          findMatches(t['*)'], p, j + 1, result);
        }
        t = t[p[i]];
        if (t == null) {
          break;
        }
        result.merge(t._values);
      }
    }

    let result = {
      plain: [],
      batched: [],
      merge: function (v) {
        if (v) {
          this.plain.push(...v.plain);
          this.batched.push(...v.batched);
        }
      },
    };
    findMatches(WS._callbackTrie, key.split(/[.(]/), 0, result);
    return result;
  },

  Forget: function (elements) {
    'use strict';
    function filter(trie, elem) {
      if (trie._values && trie._values.plain) {
        trie._values.plain = trie._values.plain.filter(function (v) {
          return !elements.is(v.elem);
        });
      }
      Object.entries(trie).forEach(function (entry) {
        if (entry[0] !== '_values') {
          filter(entry[1], elem);
        }
      });
    }

    filter(
      WS._callbackTrie,
      elements
        .find('*')
        .addBack()
        .each(function () {
          WS._pathCache.delete(this);
          WS._prefixCache.delete(this);
        })
    );
  },

  _getParameters: function (elem, attr, pathIndex, isPreRegister) {
    'use strict';
    pathIndex = pathIndex || 0;
    return WS._replacePathComponents(elem, attr, isPreRegister)[0]
      .split('|')
      .map(function (part) {
        let list = part.split(':').map((s) => s.trim());
        const basePath = WS._getContext(elem, attr === 'sbForeach', isPreRegister);
        const prefixes = WS._getPrefixes(elem, isPreRegister);
        list[pathIndex] = list[pathIndex].split(',').map((item) => WS._combinePaths(basePath, item.trim(), prefixes));
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
    'use strict';
    return selector === '[sbForeach]'
      ? root.find('[sbForeach]:not([sbForeach] [sbForeach]):not(#sbTemplates [sbForeach])')
      : root.find(selector + ':not([sbForeach]):not([sbForeach] ' + selector + '):not(#sbTemplates ' + selector + ')').addBack(selector);
  },

  _getModifyFunc: function (paths, func, isBool) {
    'use strict';
    if (!$.isFunction(func)) {
      if (isBool) {
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
          func = WS._buildBoolFunc(func);
        }
      } else {
        func = window[func] || WS._buildNonBoolFunc(func);
      }
    }
    if (paths.length < 2) {
      return func;
    } else {
      return function (path, v, elem) {
        v = null;
        for (let i = 0; i < paths.length; i++) {
          path = paths[i];
          if (WS.state[path] != null) {
            v = WS.state[path];
            break;
          }
        }
        if ($.isFunction(func)) {
          v = func(WS._enrichProp(path), v, elem);
        }
        return v;
      };
    }
  },

  _boolFuncCache: {},
  _buildBoolFunc: function (body) {
    WS._boolFuncCache[body] = WS._boolFuncCache[body] || Function('k', 'v', 'elem', 'return v' + body);
    return WS._boolFuncCache[body];
  },
  _nonBoolFuncCache: {},
  _buildNonBoolFunc: function (body) {
    WS._nonBoolFuncCache[body] = WS._nonBoolFuncCache[body] || Function('k', 'v', 'elem', 'return ' + (body || 'v'));
    return WS._nonBoolFuncCache[body];
  },

  _isInputElement: function (elem) {
    'use strict';
    return elem.prop('tagName') === 'INPUT' || elem.prop('tagName') === 'SELECT';
  },

  _runningIncludes: 1, // core.js includes
  _loadIncludes: function (root) {
    'use strict';
    $('[sbInclude]', root).each(function (idx, elem) {
      WS._runningIncludes++;
      elem = $(elem);
      const selector = elem.attr('sbInclude');
      elem.attr('sbInclude', null);
      elem.load(selector, function () {
        WS.Process(selector.split(' ', 1)[0], elem);
      });
    });
  },

  Process: function (url, elem) {
    'use strict';
    _includeJsAndCss(url, function () {
      WS._loadIncludes(elem);
      WS._runningIncludes--;
      if (WS._runningIncludes === 0) {
        WS.AutoRegister();
      }
    });
  },

  AutoRegister: function (root) {
    'use strict';
    if (!root) {
      WS._preRegister();
      $('[sbForeach]').each(function () {
        const elem = $(this);
        const prev = elem.prev('[sbForeach]');
        elem.attr('sbSubId', Number(prev.attr('sbSubId') || -1) + 1);
      });
      // run the button conversion before items are cloned as the operation is expensive
      $(
        '[sbButton], button[sbCall]:not(.ToggleSwitch), button[sbControl]:not(.ToggleSwitch), button[sbSet]:not(.ToggleSwitch), button[sbToggle]:not(.ToggleSwitch)'
      ).button();
      $('[sbButtonGroup').controlgroup();
      $(
        '.sbShowOnSk, .sbShowOnPt, .sbShowOnPurePt, .sbShowOnLt, .sbShowOnPureLt, .sbShowOnPlt, .sbShowOnSheet, .sbShowOnWhiteboard, .sbShowOnOperator'
      ).addClass('sbShowBySheetStyle');
      root = $('html');
    }

    $.each(WS._getElements('.AutoFit:not([sbDisplay]):not([sbControl]):not(:has(*))', root), function (idx, elem) {
      elem = $(elem);
      const text = elem.text();
      const mf = function () {
        return text;
      };
      WS.Register(WS._getAutoFitPaths(elem), { preRegistered: true, element: elem, modifyFunc: mf });
    });
    $.each(WS._getElements('[sbOn]', root), function (idx, elem) {
      elem = $(elem);
      elem
        .attr('sbOn')
        .split('|')
        .map(function (entry) {
          const [event, listener] = entry
            .trim()
            .split(':')
            .map((s) => s.trim());
          elem.on(event, function (e) {
            return window[listener](WS._enrichProp(WS._getContext(elem)), elem.val(), elem, e);
          });
        });
    });
    $.each(WS._getElements('[sbDisplay]', root), function (idx, elem) {
      elem = $(elem);
      const [paths, func, attr] = WS._getParameters(elem, 'sbDisplay')[0];
      WS.Register(paths.concat(WS._getAutoFitPaths(elem)), {
        preRegistered: true,
        element: elem,
        html: attr === 'html',
        modifyFunc: WS._getModifyFunc(paths, func, false),
      });
    });
    $.each(WS._getElements('[sbSet]', root), function (idx, elem) {
      elem = $(elem);
      const entries = WS._getParameters(elem, 'sbSet');
      elem.on(WS._isInputElement(elem) ? 'change' : 'click', function (event) {
        entries.map(function (entry) {
          const writeFunc = WS._getModifyFunc([], entry[1], false);
          const [prefix, suffix] = WS._getPrefixes(elem);
          entry[0].map(function (path) {
            if (prefix[path[0]]) {
              path = prefix[path[0]] + path.substring(1) + (suffix[path[0]] || '');
            }
            WS.Set(path, writeFunc(WS._enrichProp(path), WS._isInputElement(elem) ? elem.val() : true, elem, event), entry[2]);
          });
        });
      });
    });
    $.each(WS._getElements('[sbControl]', root), function (idx, elem) {
      elem = $(elem);
      let [paths, readFunc, writeFunc] = WS._getParameters(elem, 'sbControl')[0];
      writeFunc = WS._getModifyFunc([], writeFunc, false);
      WS.Register(paths, { preRegistered: true, element: elem, set: true, modifyFunc: WS._getModifyFunc(paths, readFunc, false) });
      elem.on('change', function (event) {
        WS.Set(paths[0], writeFunc(paths[0], elem.val(), elem, event));
      });
    });
    $.each(WS._getElements('[sbToggle]', root), function (idx, elem) {
      elem = $(elem);
      let [paths, usedClass, func] = WS._getParameters(elem, 'sbToggle')[0];
      usedClass = usedClass || 'sbActive';
      WS.Register(paths, {
        preRegistered: true,
        element: elem,
        toggleClass: usedClass,
        modifyFunc: function (k, v, elem) {
          return isTrue(v);
        },
      });
      elem.on('click', function () {
        WS.Set(paths[0], !elem.hasClass(usedClass));
      });
    });
    $.each(WS._getElements('[sbClass]', root), function (idx, elem) {
      elem = $(elem);
      WS._getParameters(elem, 'sbClass', 1).map(function (toggle) {
        const paths = toggle[1];

        WS.Register(paths, {
          preRegistered: true,
          element: elem,
          toggleClass: toggle[0],
          modifyFunc: WS._getModifyFunc(paths, toggle[2], true),
        });
      });
    });
    $.each(WS._getElements('[sbCss]', root), function (idx, elem) {
      elem = $(elem);
      WS._getParameters(elem, 'sbCss', 1).map(function (entry) {
        const paths = entry[1];

        WS.Register(paths, { preRegistered: true, element: elem, css: entry[0], modifyFunc: WS._getModifyFunc(paths, entry[2], false) });
      });
    });
    $.each(WS._getElements('[sbAttr]', root), function (idx, elem) {
      elem = $(elem);
      WS._getParameters(elem, 'sbAttr', 1).map(function (entry) {
        const paths = entry[1];

        WS.Register(paths, {
          preRegistered: true,
          element: elem,
          attr: entry[0],
          modifyFunc: WS._getModifyFunc(paths, entry[2], false),
        });
      });
    });
    $.each(WS._getElements('[sbProp]', root), function (idx, elem) {
      elem = $(elem);
      WS._getParameters(elem, 'sbProp', 1).map(function (entry) {
        const paths = entry[1];

        WS.Register(paths, { preRegistered: true, element: elem, prop: entry[0], modifyFunc: WS._getModifyFunc(paths, entry[2], true) });
      });
    });
    $.each(WS._getElements('[sbCall]', root), function (idx, elem) {
      elem = $(elem);
      const entries = elem
        .attr('sbCall')
        .split('|')
        .map((s) => s.trim());
      elem.on(WS._isInputElement(elem) ? 'change' : 'click', function (event) {
        entries.map(function (entry) {
          window[entry](WS._enrichProp(WS._getContext(elem)), elem.val(), elem, event);
        });
      });
    });
    let preForeachItem;
    $.each(WS._getElements('[sbForeach]', root), function (idx, elem) {
      elem = $(elem);
      const paren = elem.parent();
      const subId = elem.attr('sbSubId');
      if (subId == 0) {
        preForeachItem = elem.prev();
      }
      let [paths, fixedKeys, sortFunction, optionsString] = WS._getParameters(elem, 'sbForeach')[0];
      if (fixedKeys) {
        fixedKeys = fixedKeys.split(',').map((s) => s.trim());
      }
      let blockedKeys = {};
      let options = {};
      if (optionsString) {
        optionsString.split(',').map(function (option) {
          option = option.split('=', 2).map((s) => s.trim());
          options[option[0]] = option[1] || true;
        });
      }
      let context = elem.attr('sbContext') ? (elem.attr('sbContext') + ':').split(':', 2) : ['', ''];
      if (context[0] != '' && !context[0].endsWith('^')) {
        context[0] = context[0] + '.';
      }
      if (context[1] != '') {
        context[1] = '.' + context[1];
      }
      elem.detach().removeAttr('sbForeach');
      $.each(paths, function (idx, path) {
        const field = path.substring(path.lastIndexOf('.') + 1);
        if (options.filter === '^') {
          options.filter = paren
            .closest('[' + field + ']')
            .attr(field)
            .slice(0, -2); // cut off .*
        }
        $.each(fixedKeys, function (idx, key) {
          if (key.startsWith('-')) {
            blockedKeys[key.substring(1)] = true;
          } else {
            if (options.filter) {
              key = options.filter + '.' + key;
            }
            const newElem = elem.clone(true).attr(field, key).addClass('sbFixed');
            if (!options.noContext) {
              newElem.attr('sbContext', context[0] + field + '(' + key + ')' + context[1]);
            }
            let prev = paren.children('[' + field + '="' + key + '"][sbSubId="' + (subId - 1) + '"]');
            if (!prev.length) {
              prev = preForeachItem.nextUntil(':not(.sbFixed)').addBack().last();
            }
            if (!prev.length) {
              prev = paren.children(':first-child.sbFixed').nextUntil(':not(.sbFixed)').addBack().last();
            }
            if (prev.length) {
              newElem.insertAfter(prev);
            } else {
              paren.prepend(newElem);
            }
            WS.AutoRegister(newElem);
          }
        });
        if (sortFunction !== 'only') {
          path = path + '(' + (options.filter ? options.filter + '.' : '') + '*)' + (options.noId ? '' : '.Id');
          WS.Register(path, {
            preRegistered: true,
            element: elem,
            triggerFunc: function (k, v) {
              const key = options.part ? k[field].split('.').slice(0, options.part).join('.') + '.*' : k[field];
              const subfieldId = 'data-' + k[field].replaceAll('.', '-');
              if (blockedKeys[key]) {
                return;
              } else if (v == null) {
                if (key !== k[field]) {
                  const target = paren.children('[' + field + '="' + key + '"][' + subfieldId + '][sbSubId="' + subId + '"]:not(.sbFixed)');
                  const removed = target
                    .attr(subfieldId, null)
                    .attr('sbCount', target.attr('sbCount') - 1)
                    .filter('[sbCount="0"]');
                  if (removed.length) {
                    WS.Forget(removed);
                    removed.remove();
                  }
                } else {
                  const removed = paren.children('[' + field + '="' + key + '"][sbSubId="' + subId + '"]:not(.sbFixed)');
                  if (removed.length) {
                    WS.Forget(removed);
                    removed.remove();
                  }
                }
              } else if (!paren.children('[' + field + '="' + key + '"][sbSubId="' + subId + '"]').length) {
                const newElem = elem.clone(true).attr(field, key).appendTo(paren);
                if (!options.noContext) {
                  newElem.attr('sbContext', context[0] + field + '(' + key + ')' + context[1]);
                }
                if (key !== k[field]) {
                  newElem.attr('sbCount', 1).attr(subfieldId, k[field]);
                }
                WS.AutoRegister(newElem);
                if (options.resort) {
                  WS.Register(WS._getContext(newElem) + '.' + options.resort, {
                    preRegistered: true,
                    element: newElem,
                    triggerFunc: function (k, v, elem) {
                      if (v != null) {
                        elem.detach();
                        _windowFunctions.appendSorted(paren, elem, window[sortFunction], preForeachItem.index() + 1);
                      }
                    },
                  });
                } else {
                  newElem.detach();
                  _windowFunctions.appendSorted(
                    paren,
                    newElem,
                    window[sortFunction] ||
                      function (a, b) {
                        return _compareAttrThenSubId(field, a, b);
                      },
                    preForeachItem.index() + 1
                  );
                }
              } else if (
                key !== k[field] &&
                !paren.children('[' + field + '="' + key + '"][' + subfieldId + '][sbSubId="' + subId + '"]').length
              ) {
                const target = paren.children('[' + field + '="' + key + '"][sbSubId="' + subId + '"]:not(.sbFixed)');
                target.attr(subfieldId, k[field]).attr('sbCount', Number(target.attr('sbCount')) + 1);
              }
            },
          });
        }
      });
    });
  },

  _preRegister: function () {
    'use strict';
    let paths = [];
    const preRegisterAttribute = function (attr, pathIndex) {
      $('[' + attr + ']').each(function (idx, elem) {
        $.each(WS._getParameters($(elem), attr, pathIndex, true), function (idx, params) {
          paths = paths.concat(params[pathIndex]);
        });
      });
    };

    $('[sbForeach]').each(function (idx, elem) {
      $.each(WS._getParameters($(elem), 'sbForeach', 0, true), function (idx, params) {
        paths = paths.concat(params[0].map((s) => s + '(*)' + ((params[3] || '').includes('noId') ? '' : '.Id')));
      });
    });

    $.each(['sbDisplay', 'sbControl', 'sbToggle'], function (idx, attr) {
      preRegisterAttribute(attr, 0);
    });
    $.each(['sbClass', 'sbCss', 'sbAttr', 'sbProp'], function (idx, attr) {
      preRegisterAttribute(attr, 1);
    });

    WS.Register(paths, null, true);
  },

  _replacePathRe: /\[([^\]]*)\]/g,
  _replacePathComponents: function (elem, attr, isPreRegister, skipCopyContext) {
    'use strict';
    let clean = true;
    elem = $(elem);
    if (!elem.attr(attr)) {
      return [elem.attr(attr), true];
    }
    elem.attr(
      attr,
      elem.attr(attr).replaceAll(WS._replacePathRe, function (match, field) {
        if (field === '*') {
          if (skipCopyContext) {
            // we're trying to determine the proper replacement
            return match;
          }
          let cached = WS._pathCache.get(elem[0]);
          if (!cached) {
            WS._getContext(elem, false, isPreRegister, attr === 'sbPrefix');
            cached = WS._pathCache.get(elem[0]);
          }
          return cached.reevaluate ? match : cached.path;
        } else {
          const src = elem.closest(match + ', [sbForeach^="' + match + '"]:not([sbForeach*="noContext"])');
          return src.length ? src.attr(field) || match : _windowFunctions.getParam(field) || match;
        }
      })
    );

    const result = elem.attr(attr).replaceAll(WS._replacePathRe, function (match, field) {
      clean = false;
      return field === '*' ? (skipCopyContext ? '[*]' : WS._pathCache.get(elem[0]).path) : '*';
    });
    return [result, clean];
  },

  _pathCache: new Map(),
  _getContext: function (elem, skipSuffix, isPreRegister, skipCopyContext) {
    'use strict';
    const cached = WS._pathCache.get(elem[0]);
    if (cached && (!cached.reevaluate || isPreRegister)) {
      return skipSuffix ? cached.noSuffixPath : cached.path;
    }
    let reevaluate = false;
    let path = '';
    const parent = elem.parent();
    if (parent.length > 0) {
      path = WS._getContext(parent, false, isPreRegister);
      reevaluate = WS._pathCache.get(parent[0]).reevaluate;
    }
    let [value, clean] = WS._replacePathComponents(elem, 'sbContext', isPreRegister);
    reevaluate = reevaluate || !clean;
    let suffix;
    const prefixes = WS._getPrefixes(elem, isPreRegister, skipCopyContext);
    if (value) {
      const parts = value.split(':', 2);
      path = WS._combinePaths(path, parts[0], prefixes);
      suffix = parts[1];
    }
    const noSuffixPath = path;
    const foreach = elem.attr('sbForeach');
    if (foreach != null && !foreach.includes('noContext')) {
      reevaluate = true;
      path = (path !== '' ? path + '.' : '') + foreach.split(':', 1)[0].trim() + '(*)';
    }
    if (suffix) {
      path = WS._combinePaths(path, suffix, prefixes);
    }
    WS._pathCache.set(elem[0], { reevaluate: reevaluate, path: path, noSuffixPath: noSuffixPath });
    return skipSuffix ? noSuffixPath : path;
  },

  _combinePaths: function (basePath, subPath, prefixes) {
    if (subPath === '') {
      return basePath;
    } else if (subPath.startsWith('/')) {
      return subPath.substring(1);
    } else if (prefixes[0][subPath[0]]) {
      return prefixes[0][subPath[0]] + subPath.substring(1) + (prefixes[1][subPath[0]] || '');
    } else if (subPath[1] === '.') {
      // using a dynamic prefix that's not set yet
    } else {
      while (basePath && subPath.startsWith('^')) {
        subPath = subPath.substring(1);
        basePath = basePath.substring(0, basePath.lastIndexOf('.'));
      }
      return basePath + (basePath && subPath ? '.' : '') + subPath;
    }
  },

  _prefixCache: new Map(),
  _getPrefixes: function (elem, isPreRegister, skipCopyContext) {
    'use strict';
    const cached = WS._prefixCache.get(elem[0]);
    if (cached && (!cached.reevaluate || isPreRegister)) {
      return [cached.prefixes, cached.suffixes];
    }
    let reevaluate = false;
    let prefixes = {};
    let suffixes = {};
    $.each(elem.parents('[sbPrefix]').addBack('[sbPrefix]'), function (idx, paren) {
      const [value, clean] = WS._replacePathComponents(paren, 'sbPrefix', isPreRegister, skipCopyContext);
      reevaluate = reevaluate || !clean;
      value.split('|').map(function (entry) {
        entry = entry.split(':').map((s) => s.trim());
        prefixes[entry[0]] = entry[1];
        suffixes[entry[0]] = entry[2];
      });
    });
    WS._prefixCache.set(elem[0], { reevaluate: reevaluate, prefixes: prefixes, suffixes: suffixes });
    return [prefixes, suffixes];
  },

  SetupDialog: function (elem, context, options) {
    'use strict';
    elem = elem.clone().attr('sbContext', '/' + context);
    options = options || {};
    WS.AutoRegister(elem);
    const origCloseHandler = options.close;
    options.close = function (event, ui) {
      WS.Forget(elem);
      if (origCloseHandler) {
        origCloseHandler(event, ui, elem);
      }
      elem.dialog('destroy').remove();
    };
    elem.dialog(options);
    return elem;
  },
};
