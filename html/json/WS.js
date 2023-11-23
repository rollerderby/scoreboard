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
        WS.registerOnConnect = [];
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
    WS._getMatchesFromTrie(WS.callbackTrie, k).plain.forEach(function (el) {
      try {
        el(k, null);
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
          plainNull.push({ callback: c, path: k });
        });
        // Clean cache to avoid a memory leak for long running screens.
        delete WS._enrichPropCache[k];
      } else {
        WS.state[k] = v;
        callbacks.plain.forEach(function (c) {
          plain.push({ callback: c, path: k, value: v });
        });
      }
      callbacks.batched.forEach(function (c) {
        batched.add(c);
      });
    });
    plainNull.forEach(function (el) {
      try {
        el.callback(el.path, null);
      } catch (err) {
        console.error(err.message);
        console.log(err.stack);
      }
    });
    plain.forEach(function (el) {
      try {
        el.callback(el.path, el.value);
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
    if (options == null) {
      callback = null;
    } else {
      if (options.triggerFunc != null) {
        callback = options.triggerFunc;
      } else if (options.triggerBatchFunc != null) {
        batchCallback = options.triggerBatchFunc;
      } else {
        const elem = options.element;
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
        WS._addToTrie(WS.callbackTrie, path, batchCallback, 'batched');
      });
    }

    if ($.isFunction(callback)) {
      $.each(paths, function (idx, path) {
        WS._addToTrie(WS.callbackTrie, path, callback, 'plain');
      });
      if (options && options.preRegistered) {
        if (paths.length) {
          if (paths.length > 1) {
            $.each(paths, function (i, path) {
              const val = WS.state[path];
              if (val != null) {
                callback(WS._enrichProp(path), val);
                return false;
              }
            });
          } else if (paths[0].includes('(*)')) {
            const regexp = new RegExp(paths[0].replaceAll('(', '\\(').replaceAll(')', '\\)').replaceAll('*', '[^)]*'));
            $.each(WS.state, function (k, v) {
              if (regexp.test(k)) {
                callback(WS._enrichProp(k), v);
              }
            });
          } else {
            callback(WS._enrichProp(paths[0]), WS.state[paths[0]]);
          }
          paths = []; // they have been registered with the backend during pre registration
        } else {
          callback();
        }
      }
    }

    if (paths.length) {
      if (WS.Connected) {
        const req = {
          action: 'Register',
          paths: paths,
        };
        WS.send(JSON.stringify(req));
      } else {
        WS.registerOnConnect.push(...paths);
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
            return !$(this).parent().closest('[sbForeach],[sbInclude]').length;
          }
        : function () {
            return !$(this).closest('[sbForeach],[sbInclude]').length;
          };
    return $(selector, root)
      .add(root ? root.filter(selector) : $())
      .filter(filterFunc);
  },

  _getModifyFunc: function (paths, func) {
    'use strict';
    if (!$.isFunction(func)) {
      func = window[func];
    }
    if (paths.length < 2) {
      return func;
    } else {
      return function () {
        let v = null;
        let path;
        for (let i = 0; i < paths.length; i++) {
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
    $('[sbInclude]', root).each(function (idx, elem) {
      elem = $(elem);
      const postFunc = elem.attr('sbAfterInclude');
      const selector = elem.attr('sbInclude');
      elem.attr('sbInclude', null).attr('sbAfterInclude', null);
      elem.load(selector, function () {
        _includeJsAndCss(selector.split(' ', 1)[0]);
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
        elem.attr('sbContext').replace('TeamJam(auto)', 'TeamJam(' + (elem.closest('[Team]').attr('Team') || auto) + ')')
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
    $.each(WS._getElements('[sbCall]', root), function (idx, elem) {
      elem = $(elem);
      const entries = elem.attr('sbCall').split('|').map($.trim);
      if (elem.prop('tagName') === 'input' || elem.prop('tagName') === 'select') {
        elem.on('change', function () {
          entries.map(function (entry) {
            window[entry](elem.val(), elem);
          });
        });
      } else {
        elem.on('click', function () {
          entries.map(function (entry) {
            window[entry](elem);
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
        modifyFunc: WS._getModifyFunc(paths, func),
      });
    });
    $.each(WS._getElements('[sbSet]', root), function (idx, elem) {
      elem = $(elem);
      const entries = WS._getParameters(elem, 'sbSet');
      if (elem.prop('tagName') === 'input' || elem.prop('tagName') === 'select') {
        elem.on('change', function () {
          entries.map(function (entry) {
            const writeFunc = window[entry[1]] || Function('k', 'elem', 'return ' + (entry[1] || elem.val()));
            const [prefix, suffix] = WS._getPrefixes(elem);
            entry[0].map(function (path) {
              if (prefix[path[0]]) {
                path = prefix[path[0]] + path.substring(1) + (suffix[path[0]] || '');
              }
              WS.Set(path, writeFunc(WS._enrichProp(path), elem));
            });
          });
        });
      } else {
        elem.on('click', function () {
          entries.map(function (entry) {
            const writeFunc = window[entry[1]] || Function('k', 'elem', 'return ' + (entry[1] || true));
            const [prefix, suffix] = WS._getPrefixes(elem);
            entry[0].map(function (path) {
              if (prefix[path[0]]) {
                path = prefix[path[0]] + path.substring(1) + (suffix[path[0]] || '');
              }
              WS.Set(path, writeFunc(WS._enrichProp(path), elem));
            });
          });
        });
      }
      elem.filter('button:not(.ToggleSwitch)').button();
    });
    $.each(WS._getElements('[sbControl]', root), function (idx, elem) {
      elem = $(elem);
      let [paths, readFunc, writeFunc] = WS._getParameters(elem, 'sbControl')[0];
      writeFunc =
        window[writeFunc] ||
        function (v) {
          return v;
        };
      WS.Register(paths, { preRegistered: true, element: elem, set: true, modifyFunc: WS._getModifyFunc(paths, readFunc) });
      elem.on('change', function () {
        WS.Set(paths[0], writeFunc(elem.val()));
      });
      elem.filter('button:not(.ToggleSwitch)').button();
    });
    $.each(WS._getElements('[sbToggle]', root), function (idx, elem) {
      elem = $(elem);
      let [paths, usedClass] = WS._getParameters(elem, 'sbToggle')[0];
      usedClass = usedClass || 'Active';
      WS.Register(paths, {
        preRegistered: true,
        element: elem,
        toggleClass: usedClass,
        modifyFunc: function (k, v) {
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

        WS.Register(paths, { preRegistered: true, element: elem, toggleClass: toggledClass, modifyFunc: WS._getModifyFunc(paths, func) });
      });
    });
    $.each(WS._getElements('[sbCss]', root), function (idx, elem) {
      elem = $(elem);
      WS._getParameters(elem, 'sbCss', 1).map(function (entry) {
        const paths = entry[1];

        WS.Register(paths, { preRegistered: true, element: elem, css: entry[0], modifyFunc: WS._getModifyFunc(paths, entry[2]) });
      });
    });
    $.each(WS._getElements('[sbAttr]', root), function (idx, elem) {
      elem = $(elem);
      WS._getParameters(elem, 'sbAttr', 1).map(function (entry) {
        const paths = entry[1];

        WS.Register(paths, { preRegistered: true, element: elem, attr: entry[0], modifyFunc: WS._getModifyFunc(paths, entry[2]) });
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
      elem.detach().removeAttr('sbForeach').attr('subId', subId);
      $.each(paths, function (idx, path) {
        const field = path.substring(path.lastIndexOf('.', path.length - 6) + 1, path.length - 6); // cut off (*).Id
        $.each(fixedKeys, function (idx, key) {
          if (key.startsWith('-')) {
            blockedKeys[key.substring(1)] = true;
          } else {
            const newElem = elem
              .clone(true)
              .attr(field, key)
              .attr('sbContext', context[0] + field + '(' + key + ')' + context[1])
              .addClass('Fixed');
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
          WS.Register(options.noId ? path.substring(0, path.length - 3) : path, {
            preRegistered: true,
            triggerFunc: function (k, v) {
              if (blockedKeys[k[field]]) {
                return;
              } else if (v == null) {
                paren.children('[' + field + '=' + k[field] + ']:not(.Fixed)').remove();
              } else if (!paren.children('[' + field + '="' + k[field] + '"][subId="' + subId + '"]').length) {
                const newElem = elem
                  .clone(true)
                  .attr(field, k[field])
                  .attr('sbContext', context[0] + field + '(' + k[field] + ')' + context[1])
                  .appendTo(paren);
                WS.AutoRegister(newElem);
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
    $.each(['sbClass', 'sbCss', 'sbAttr'], function (idx, attr) {
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
    if (foreach != null) {
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
