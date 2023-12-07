let WS = {
  connectCallback: null,
  connectTimeout: null,
  registerOnConnect: [],
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
        $('.ConnectionError').addClass('Connected');
        let req = {
          action: 'Register',
          paths: WS.registerOnConnect,
        };
        $.each(Object.keys(WS.state), function (idx, k) {
          WS._nullCallbacks(k);
        });
        WS.state = {};
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
        const json = JSON.parse(e.data);
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
    WS._getMatchesFromTrie(WS.callbackTrie, k).plain.forEach(function (v) {
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
      const callbacks = WS._getMatchesFromTrie(WS.callbackTrie, k);

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
    /* jshint -W053 */
    prop = new String(prop);
    /* jshint +W053 */
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
    WS._enrichPropCache[prop] = prop;
    return prop;
  },

  Register: function (paths, options) {
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
        WS._addToTrie(WS.callbackTrie, path, batchCallback, 'batched');
      });
    }

    if ($.isFunction(callback)) {
      $.each(paths, function (idx, path) {
        WS._addToTrie(WS.callbackTrie, path, { func: callback, elem: elem }, 'plain');
      });
      if (options && options.preRegistered) {
        if (paths.length) {
          if (paths.length > 1) {
            $.each(paths, function (i, path) {
              const val = WS.state[path];
              if (val != null) {
                callback(WS._enrichProp(path), val, elem);
                return false;
              }
            });
          } else if (paths[0].includes('*)')) {
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

    if (paths.length) {
      WS.registerOnConnect.push(...paths);
      if (WS.Connected) {
        const req = {
          action: 'Register',
          paths: paths,
        };
        WS.send(JSON.stringify(req));
      }
    }
  },

  _addToTrie: function (t, key, value, type) {
    'use strict';
    const p = key.split(/[.(]/);
    for (let i = 0; i < p.length; i++) {
      const c = p[i];
      t[c] = t[c] || {};
      t = t[c];
    }
    t._values = t._values || { plain: [], batched: [] };
    t._values[type].push(value);
  },

  _getMatchesFromTrie: function (trie, key) {
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
    findMatches(trie, key.split(/[.(]/), 0, result);
    return result;
  },

  _removeFromTrie: function (trie, elements) {
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

    filter(trie, elements.find('*').add(elements));
  },

  _getParameters: function (elem, attr, pathIndex) {
    'use strict';
    pathIndex = pathIndex || 0;
    return elem
      .attr(attr)
      .split('|')
      .map(function (part) {
        let list = part.split(':').map($.trim);
        const basePath = WS._getContext(elem, attr === 'sbForeach');
        const [prefixes, suffixes] = WS._getPrefixes(elem);
        list[pathIndex] = list[pathIndex].split(',').map(function (item) {
          item = $.trim(item);
          if (item === '') {
            item = basePath;
          } else if (attr === 'sbForeach') {
            item = basePath + '.Id';
          } else if (item.startsWith('/')) {
            item = item.substring(1);
          } else if (prefixes[item[0]]) {
            item = prefixes[item[0]] + item.substring(1) + (suffixes[item[0]] || '');
          } else if (item[1] === '.') {
            // using a dynamic prefix that's not set yet
          } else {
            let restPath = basePath;
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
    'use strict';
    const filterFunc =
      selector === '[sbForeach]'
        ? function () {
            return !$(this).parent().closest('[sbForeach],[sbInclude]').length && $(this).attr('sbInclude') == null;
          }
        : function () {
            return !$(this).closest('[sbForeach],[sbInclude]').length;
          };
    return $(selector, root)
      .add(root ? root.filter(selector) : $())
      .filter(filterFunc);
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
          func = Function('k', 'v', 'elem', 'return v' + func);
        }
      } else {
        func = window[func] || Function('k', 'v', 'elem', 'return ' + (func || 'v'));
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

  AutoRegister: function (root, doPreRegister) {
    'use strict';
    $('.GameContext')
      .attr('sbContext', 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ')')
      .removeClass('GameContext');
    $('.OneTeam .TeamContext')
      .attr('sbContext', 'Team(' + _windowFunctions.getParam('team') + ')')
      .attr('Team', _windowFunctions.getParam('team'))
      .removeClass('TeamContext');
    $('.BothTeams .TeamContext').attr('sbForeach', 'Team: 1,2: only').removeClass('TeamContext');
    $('.PreparedTeam .TeamContext')
      .attr('sbContext', 'PreparedTeam(' + _windowFunctions.getParam('team') + ')')
      .attr('PreparedTeam', _windowFunctions.getParam('team'))
      .removeClass('TeamContext');
    $('[sbInclude]', root).each(function (idx, elem) {
      elem = $(elem);
      const postFunc = elem.attr('sbAfterInclude');
      const selector = elem.attr('sbInclude');
      elem.load(selector, function () {
        _includeJsAndCss(selector.split(' ', 1)[0]);
        elem.attr('sbInclude', null).attr('sbAfterInclude', null);
        WS.AutoRegister(elem, true);
        if (postFunc) {
          window[postFunc]();
        }
      });
    });
    $('[sbContext*="(auto)"]', root).each(function (idx, elem) {
      elem = $(elem);
      elem.attr(
        'sbContext',
        elem.attr('sbContext').replace('TeamJam(auto)', 'TeamJam(' + (elem.closest('[Team]').attr('Team') || 'auto') + ')')
      );
    });
    $.each(WS._getElements('[sbPrefix]', root), function (idx, elem) {
      elem = $(elem);
      elem.attr('sbPrefix', elem.attr('sbPrefix').replace(':cur', ':' + WS._getContext(elem)));
    });
    if (!root || doPreRegister) {
      WS._preRegister(root);
    }

    $.each(WS._getElements('.AutoFit:not([sbDisplay]):not([sbControl]):not(:has(*))', root), function (idx, elem) {
      elem = $(elem);
      const text = elem.text();
      const mf = function () {
        return text;
      };
      WS.Register(WS._getAutoFitPaths(elem), { preRegistered: true, element: elem, modifyFunc: mf });
    });
    $.each(WS._getElements('[sbButton]', root), function (idx, elem) {
      $(elem).button();
    });
    $.each(WS._getElements('[sbCall]', root), function (idx, elem) {
      elem = $(elem);
      const entries = elem.attr('sbCall').split('|').map($.trim);
      if (elem.prop('tagName') === 'INPUT' || elem.prop('tagName') === 'SELECT') {
        elem.on('change', function () {
          entries.map(function (entry) {
            window[entry](WS._getContext(elem), elem.val(), elem);
          });
        });
      } else {
        elem.on('click', function () {
          entries.map(function (entry) {
            window[entry](WS._getContext(elem), null, elem);
          });
        });
        elem.filter('button:not(.ToggleSwitch)').button();
      }
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
      if (elem.prop('tagName') === 'INPUT' || elem.prop('tagName') === 'SELECT') {
        elem.on('change', function () {
          entries.map(function (entry) {
            const writeFunc = WS._getModifyFunc([], entry[1], false);
            const [prefix, suffix] = WS._getPrefixes(elem);
            entry[0].map(function (path) {
              if (prefix[path[0]]) {
                path = prefix[path[0]] + path.substring(1) + (suffix[path[0]] || '');
              }
              WS.Set(path, writeFunc(WS._enrichProp(path), elem.val(), elem));
            });
          });
        });
      } else {
        elem.on('click', function () {
          entries.map(function (entry) {
            const writeFunc = WS._getModifyFunc([], entry[1], false);
            const [prefix, suffix] = WS._getPrefixes(elem);
            entry[0].map(function (path) {
              if (prefix[path[0]]) {
                path = prefix[path[0]] + path.substring(1) + (suffix[path[0]] || '');
              }
              WS.Set(path, writeFunc(WS._enrichProp(path), true, elem));
            });
          });
        });
      }
      elem.filter('button:not(.ToggleSwitch)').button();
    });
    $.each(WS._getElements('[sbControl]', root), function (idx, elem) {
      elem = $(elem);
      let [paths, readFunc, writeFunc] = WS._getParameters(elem, 'sbControl')[0];
      writeFunc = WS._getModifyFunc([], writeFunc, false);
      WS.Register(paths, { preRegistered: true, element: elem, set: true, modifyFunc: WS._getModifyFunc(paths, readFunc, false) });
      elem.on('change', function () {
        WS.Set(paths[0], writeFunc(paths[0], elem.val(), elem));
      });
      elem.filter('button:not(.ToggleSwitch)').button();
    });
    $.each(WS._getElements('[sbToggle]', root), function (idx, elem) {
      elem = $(elem);
      let [paths, usedClass, func] = WS._getParameters(elem, 'sbToggle')[0];
      usedClass = usedClass || 'Active';
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
      elem.filter('button:not(.ToggleSwitch)').button();
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
    let lastForeach;
    let lastSubId = 0;
    $.each(WS._getElements('[sbForeach]', root), function (idx, elem) {
      elem = $(elem);
      const paren = elem.parent();
      let index = elem.index();
      let [paths, fixedKeys, sortFunction, optionsString] = WS._getParameters(elem, 'sbForeach')[0];
      let subId = elem.attr('sbForeach') === lastForeach ? lastSubId + 1 : 0;
      lastForeach = elem.attr('sbForeach');
      lastSubId = subId;
      if (fixedKeys) {
        fixedKeys = fixedKeys.split(',').map($.trim);
      }
      let blockedKeys = {};
      let options = {};
      if (optionsString) {
        optionsString.split(',').map(function (option) {
          option = option.split('=', 2);
          options[$.trim(option[0])] = $.trim(option[1]) || true;
        });
      }
      let context = elem.attr('sbContext') ? (elem.attr('sbContext') + ':').split(':', 2) : ['', ''];
      if (context[0] != '' && !context[0].endsWith('^')) {
        context[0] = context[0] + '.';
      }
      if (context[1] != '') {
        context[1] = '.' + context[1];
      }
      elem.detach().removeAttr('sbForeach').attr('subId', subId);
      $.each(paths, function (idx, path) {
        const field = path.slice(path.lastIndexOf('.', path.length - 6) + 1, -6); // cut off (*).Id
        if (options.filter === '^') {
          options.filter = paren
            .closest('[' + field + ']')
            .attr(field)
            .slice(0, -2);
        }
        $.each(fixedKeys, function (idx, key) {
          if (key.startsWith('-')) {
            blockedKeys[key.substring(1)] = true;
          } else {
            if (options.filter) {
              key = options.filter + '.' + key;
            }
            const newElem = elem.clone(true).attr(field, key).addClass('Fixed');
            if (!options.noContext) {
              newElem.attr('sbContext', context[0] + field + '(' + key + ')' + context[1]);
            }
            if (subId > 0) {
              newElem.insertAfter(paren.children('[' + field + '="' + key + '"][subId="' + (subId - 1) + '"]'));
            } else if (index === 0) {
              paren.prepend(newElem);
            } else {
              newElem.insertAfter(paren.children(':nth-child(' + index + ')'));
            }
            index++;
            WS.AutoRegister(newElem);
          }
        });
        if (sortFunction !== 'only') {
          if (options.filter) {
            path = path.slice(0, -5) + options.filter + '.*).Id';
          }
          if (options.noId) {
            path = path.slice(0, -3);
          }
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
                  const target = paren.children('[' + field + '="' + key + '"][' + subfieldId + '][subId="' + subId + '"]:not(.Fixed)');
                  const removed = target
                    .attr(subfieldId, null)
                    .attr('sbCount', target.attr('sbCount') - 1)
                    .filter('[sbCount="0"]');
                  if (removed.length) {
                    WS._removeFromTrie(WS.callbackTrie, removed);
                    removed.remove();
                  }
                } else {
                  const removed = paren.children('[' + field + '="' + key + '"][subId="' + subId + '"]:not(.Fixed)');
                  if (removed.length) {
                    WS._removeFromTrie(WS.callbackTrie, removed);
                    removed.remove();
                  }
                }
              } else if (!paren.children('[' + field + '="' + key + '"][subId="' + subId + '"]').length) {
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
                        _windowFunctions.appendSorted(paren, elem, window[sortFunction], index);
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
                        return compareAttrThenSubId(field, a, b);
                      },
                    index
                  );
                }
              } else if (
                key !== k[field] &&
                !paren.children('[' + field + '="' + key + '"][' + subfieldId + '][subId="' + subId + '"]').length
              ) {
                const target = paren.children('[' + field + '="' + key + '"][subId="' + subId + '"]:not(.Fixed)');
                target.attr(subfieldId, k[field]).attr('sbCount', Number(target.attr('sbCount')) + 1);
              }
            },
          });
        }
      });
    });
  },

  _preRegister: function (root) {
    let paths = [];
    const preRegisterAttribute = function (attr, pathIndex) {
      $.each($('[' + attr + ']', root), function (idx, elem) {
        $.each(WS._getParameters($(elem), attr, pathIndex), function (idx, params) {
          paths = paths.concat(
            params[pathIndex].map(function (path) {
              return path.replace('TeamJam(auto)', 'TeamJam(*)');
            })
          );
        });
      });
    };

    $.each(['sbForeach', 'sbDisplay', 'sbSet', 'sbControl', 'sbToggle'], function (idx, attr) {
      preRegisterAttribute(attr, 0);
    });
    $.each(['sbClass', 'sbCss', 'sbAttr', 'sbProp'], function (idx, attr) {
      preRegisterAttribute(attr, 1);
    });

    WS.Register(paths);
  },

  _getContext: function (elem, skipSuffix) {
    'use strict';
    const parent = elem.parent();
    let ret = '';
    if (parent.length > 0) {
      ret = WS._getContext(parent);
    }
    let context = elem.attr('sbContext');
    let suffix;
    if (context) {
      const parts = context.split(':', 2);
      context = parts[0];
      suffix = parts[1];
    }
    const foreach = elem.attr('sbForeach');
    const [prefixes, suffixes] = WS._getPrefixes(elem);
    if (context != null) {
      if (context.startsWith('/')) {
        ret = context.substring(1);
      } else if (prefixes[context[0]]) {
        ret = prefixes[context[0]] + context.substring(1) + (suffixes[context[0]] || '');
      } else {
        while (ret && context.startsWith('^')) {
          context = context.substring(1);
          ret = ret.substring(0, ret.lastIndexOf('.'));
        }
        ret = ret + (ret !== '' && context !== '' ? '.' : '') + context;
      }
    }
    if (foreach != null && (!foreach.includes('noContext') || skipSuffix)) {
      ret = (ret !== '' ? ret + '.' : '') + foreach.split(':', 1)[0] + '(*)';
    }
    if (suffix && !skipSuffix) {
      ret = ret + '.' + suffix;
    }
    return ret;
  },

  _getPrefixes: function (elem) {
    'use strict';
    let prefixes = {};
    let suffixes = {};
    $.each(elem.parents('[sbPrefix]').addBack('[sbPrefix]'), function (idx, paren) {
      $(paren)
        .attr('sbPrefix')
        .split('|')
        .map(function (entry) {
          entry = entry.split(':').map($.trim);
          prefixes[entry[0]] = entry[1];
          suffixes[entry[0]] = entry[2];
        });
    });
    return [prefixes, suffixes];
  },
};
