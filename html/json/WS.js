'use strict';
let WS = {
  _connectCallback: null,
  _connectTimeout: null,
  _registerOnConnect: [],
  _afterLoadCallbacks: [],
  _Connected: false,
  _connectionStatus: $('<div>').attr('id', 'sbConnectionStatus').attr('status', 'loading').text('Loading'),
  _started: false,
  _preRegisterDone: false,
  state: {},
  _heartbeat: null,
  debug: false,

  Connect: function (callback) {
    if (!WS._started) {
      WS._connectCallback = callback;
      WS._connect();
      WS._started = true;
      WS._connectionStatus.appendTo('body');
    }
  },

  _connect: function () {
    WS._connectTimeout = null;
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

    if (WS._Connected !== true || !WS.socket) {
      if (WS.debug) {
        console.log('WS', 'Connecting the websocket at ' + url);
      }

      WS.socket = new WebSocket(url);
      WS.socket.onopen = function (e) {
        WS._Connected = true;
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
          WS._send(JSON.stringify(req));
        }
        if (WS._connectCallback != null) {
          WS._connectCallback();
        }
        // Hearbeat every 30s so the connection is kept alive.
        WS._heartbeat = setInterval(WS.Command, 30000, 'Ping');
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
          WS._processUpdate(json.state);
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
        WS._Connected = false;
        console.log('WS', 'Websocket: Close', e);
        WS._connectionStatus.attr('status', 'error').text('Not connected');
        if (WS._connectTimeout == null) {
          WS._connectTimeout = setTimeout(WS._connect, 1000);
        }
        clearInterval(WS._heartbeat);
      };
      WS.socket.onerror = function (e) {
        console.log('WS', 'Websocket: Error', e);
        WS._connectionStatus.attr('status', 'error').text('Not connected');
        if (WS._connectTimeout == null) {
          WS._connectTimeout = setTimeout(WS._connect, 1000);
        }
        clearInterval(WS._heartbeat);
      };
    } else {
      // run the post connect callback if we didn't need to connect
      if (WS._connectCallback != null) {
        WS._connectCallback();
      }
    }
  },

  AfterLoad: function (func) {
    WS._afterLoadCallbacks.push(func);
  },

  _send: function (data) {
    if (WS.socket != null && WS.socket.readyState === 1) {
      WS.socket.send(data);
    }
  },

  Command: function (command, data) {
    const req = {
      action: command,
      data: data,
    };
    WS._send(JSON.stringify(req));
  },

  Set: function (key, value, flag) {
    const req = {
      action: 'Set',
      key: key,
      value: value,
      flag: typeof flag !== 'undefined' ? flag : '',
    };
    WS._send(JSON.stringify(req));
  },

  _nullCallbacks: function (k) {
    k = WS._enrichProp(k);
    WS._getMatchesFromTrie(WS._callbackTrie, k, 'partialKey').forEach(function (v) {
      if (v.type === 'plain') {
        try {
          v.func(k, null, v.elem);
        } catch (err) {
          console.error(err.message);
          console.log(err.stack);
        }
      }
    });
    WS._removeFromTrie(WS._stateTrie, k, WS._cleanPropCache);
  },

  _processUpdate: function (state) {
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
      const callbacks = WS._getMatchesFromTrie(WS._callbackTrie, k, 'partialKey');

      if (v == null) {
        delete WS.state[k];
        WS._removeFromTrie(WS._stateTrie, k, WS._cleanPropCache);
        callbacks.forEach(function (c) {
          if (c.type === 'plain') {
            plainNull.push({ callback: c.func, path: k, elem: c.elem });
          } else {
            batched.add(c.func);
          }
        });
      } else {
        WS.state[k] = v;
        WS._addToTrie(WS._stateTrie, k, k);
        callbacks.forEach(function (c) {
          if (c.type === 'plain') {
            plain.push({ callback: c.func, path: k, value: v, elem: c.elem });
          } else {
            batched.add(c.func);
          }
        });
      }
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
      return key === prop.field
        ? prop
        : prop.substring(0, prop.indexOf(key + (prop[key] ? '(' : ''))) + key + (prop[key] ? '(' + prop[key] + ')' : '');
    };
    WS._enrichPropCache[prop] = prop;
    return prop;
  },

  _cleanPropCache: function (p, i) {
    delete WS._enrichPropCache[p.slice(0, i + 1).join('')];
  },

  Register: function (paths, options, isPreRegister) {
    if (typeof options === 'function') {
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
            elem.find('button').button();
            elem.children().each(function () {
              WS.AutoRegister($(this));
            });
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

    if (!Array.isArray(paths)) {
      paths = [paths];
    }

    if (typeof batchCallback === 'function') {
      $.each(paths, function (idx, path) {
        WS._addToTrie(WS._callbackTrie, path, { type: 'batched', func: batchCallback });
      });
    }

    if (typeof callback === 'function') {
      $.each(paths, function (idx, path) {
        WS._addToTrie(WS._callbackTrie, path, { type: 'plain', func: callback, elem: elem });
      });
      if (options && options.preRegistered) {
        if (paths.length) {
          if (paths.length === 1 && paths[0].includes('*)')) {
            WS._getMatchesFromTrie(WS._stateTrie, paths[0], 'partialTrie').forEach(function (k) {
              callback(k, WS.state[k], elem);
            });
          } else {
            callback(WS._enrichProp(paths[0]), WS.state[paths[0]], elem);
          }
          if (elem.prop('tagName') === 'SELECT' && options.set) {
            WS._selectCache.set(elem[0], { path: paths[0], callback: callback });
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
      if (WS._Connected && WS._preRegisterDone) {
        const req = {
          action: 'Register',
          paths: isPreRegister ? WS._registerOnConnect : paths,
        };
        WS._send(JSON.stringify(req));
      }
    }
  },

  _callbackTrie: {},
  _stateTrie: {},
  _addToTrie: function (t, key, value) {
    const p = key.split(/(?=[.(])/);
    for (let i = 0; i < p.length; i++) {
      const c = p[i];
      t[c] = t[c] || {};
      t = t[c];
    }
    t._values = t._values || [];
    t._values.push(value);
  },
  _removeFromTrie: function (t, key, onDeletedNode) {
    function remove(t, p, i) {
      if (i === p.length) {
        delete t._values;
        return;
      }
      const c = p[i];
      if (t[c] == null) {
        return; // key not in trie
      }
      remove(t[c], p, i + 1);
      if ($.isEmptyObject(t[c])) {
        delete t[c];
        onDeletedNode(p, i);
      } else {
        return; // no further object will be empty
      }
    }

    remove(t, key.split(/(?=[.(])/), 0);
  },
  _getMatchesFromTrie: function (trie, key, mode) {
    let result = [];
    function findAllMatches(t) {
      if (t._values) {
        result.push(...t._values);
      }
      Object.keys(t).forEach(function (k) {
        if (k !== '_values') {
          findAllMatches(t[k]);
        }
      });
    }
    function findMatches(t, p, i) {
      if (mode === 'partialKey' && t._values) {
        result.push(...t._values);
      }
      for (; i < p.length; i++) {
        if (t['.*)'] || t['(*)']) {
          // Allow Blah(*) as a wildcard.
          let j = i;
          // id captured by * might contain . and thus be split - find the end
          while (j < p.length && !p[j].endsWith(')')) {
            j++;
          }
          if (t['.*)']) {
            findMatches(t['.*)'], p, j + 1, result);
          }
          if (t['(*)']) {
            findMatches(t['(*)'], p, j + 1, result);
          }
        }
        if (p[i] === '.*)' || p[i] === '(*)') {
          Object.keys(t).forEach(function (k) {
            if (k === '_values' || k.endsWith('*)')) {
              return;
            } else if (k.endsWith(')')) {
              findMatches(t[k], p, i + 1);
            } else {
              findMatches(t[k], p, i);
            }
          });
        } else {
          t = t[p[i]];
          if (t == null) {
            return;
          }
          if (mode === 'partialKey' && t._values) {
            result.push(...t._values);
          }
        }
      }
      if (mode === 'partialTrie') {
        findAllMatches(t);
      } else if (mode === 'exact' && t._values) {
        result.push(...t._values);
      }
    }

    findMatches(trie, key.split(/(?=[.(])/), 0, result);
    return result;
  },

  Forget: function (elements) {
    function filter(trie, elem) {
      if (trie._values) {
        trie._values = trie._values.filter(function (v) {
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
          WS._selectCache.delete(this);
        })
    );
  },

  _getParameters: function (
    elem,
    attr,
    pathIndex = -1,
    readFuncIndex = -1,
    writeFuncIndex = -1,
    isBool = false,
    isPreRegister = false,
    alwaysReadState = false
  ) {
    const val = WS._replacePathComponents(elem, attr, isPreRegister)[0];
    return val
      ? val.split('|').map(function (part) {
          let list = part.split(':').map((s) => s.trim());
          if (pathIndex > -1) {
            const prefixes = WS._getPrefixes(elem, isPreRegister);
            const basePath = WS._getContext(elem, attr === 'sbForeach', isPreRegister);
            list[pathIndex] = list[pathIndex].split(',').map((item) => WS._combinePaths(basePath, [item.trim(), false], prefixes)[0]);
          }
          if (readFuncIndex > -1 && (isBool || list[readFuncIndex] != null || list[pathIndex].length > 1 || alwaysReadState)) {
            list[readFuncIndex] = WS._getModifyFunc(list[pathIndex], list[readFuncIndex] || '', isBool, alwaysReadState);
          }
          if (writeFuncIndex > -1) {
            list[writeFuncIndex] = WS._getModifyFunc([], list[writeFuncIndex] || '', isBool);
          }
          return list;
        })
      : [];
  },

  _getAutoFitPaths: function (elem) {
    let value = WS._getParameters(elem, 'sbAutoFitOn', 0);
    return value.length ? value[0][0] : value;
  },

  _getModifyFunc: function (paths, func, isBool, alwaysReadState = false) {
    if (typeof func !== 'function') {
      func = isBool ? WS._buildBoolFunc(func) : WS._buildNonBoolFunc(func);
    }
    if (paths.length < 2 && !alwaysReadState) {
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
        if (typeof func === 'function') {
          v = func(WS._enrichProp(path), v, elem);
        }
        return v;
      };
    }
  },

  _boolFuncCache: {
    '!': function (k, v) {
      return !isTrue(v);
    },
    '': function (k, v) {
      return isTrue(v);
    },
  },
  _buildBoolFunc: function (func) {
    WS._boolFuncCache[func] = WS._boolFuncCache[func] || window[func] || Function('k', 'v', 'elem', 'return v ' + func);
    return WS._boolFuncCache[func];
  },
  _nonBoolFuncCache: {},
  _buildNonBoolFunc: function (func) {
    WS._nonBoolFuncCache[func] = WS._nonBoolFuncCache[func] || window[func] || Function('k', 'v', 'elem', 'return ' + (func || 'v'));
    return WS._nonBoolFuncCache[func];
  },
  _sortFuncCache: {},
  _buildSortFunc: function (func) {
    WS._sortFuncCache[func] =
      WS._sortFuncCache[func] ||
      window[func] ||
      Function('a', 'b', 'return _sb' + (func.split(',')[1] || '') + 'CompareAttrThenSubId("' + func.split(',')[0] + '", a, b);');
    return WS._sortFuncCache[func];
  },

  _isInputElement: function (elem) {
    return elem.prop('tagName') === 'INPUT' || elem.prop('tagName') === 'SELECT';
  },

  _runningIncludes: 1, // core.js includes
  _loadIncludes: function (root) {
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
    _includeJsAndCss(url, function () {
      WS._loadIncludes(elem);
      WS._runningIncludes--;
      if (WS._runningIncludes === 0) {
        WS._preRegister();
        // run the button conversion before items are cloned as the operation is expensive
        $(
          '[sbButton], button[sbCall]:not(.ToggleSwitch), button[sbControl]:not(.ToggleSwitch), button[sbSet]:not(.ToggleSwitch), button[sbToggle]:not(.ToggleSwitch)'
        ).button();
        $('[sbButtonGroup]').controlgroup();
        $(
          '.sbShowOnPbt, .sbShowOnSk, .sbShowOnPt, .sbShowOnPurePt, .sbShowOnLt, .sbShowOnPureLt, .sbShowOnPlt, .sbShowOnSheet, .sbShowOnWhiteboard, .sbShowOnOperator'
        ).addClass('sbShowBySheetStyle');
        $('[sbSet], [sbControl], [sbToggle], [sbCall]').not('input, select').addClass('sbClickable');
        WS.AutoRegister($('html'));
      }
    });
  },

  _selectCache: new Map(),
  AutoRegister: function (elem) {
    if (!elem || elem.hasClass('sbTemplates')) {
      return;
    }
    const forEachEntries = WS._getParameters(elem, 'sbForeach', 0);
    if (forEachEntries.length) {
      const paren = elem.parent();
      const subId = elem.attr('sbSubId') || 0;
      if (subId === 0) {
        elem.attr('sbSubId', 0);
      }
      elem.next('[sbForeach]').attr('sbSubId', subId + 1);
      const preForeachItem = subId === 0 ? elem.prev() : elem.prevUntil(':not([sbSubId])').prev();
      let [paths, fixedKeys, sortFunction, optionsString] = forEachEntries[0];
      fixedKeys = fixedKeys ? fixedKeys.split(',').map((s) => s.trim()) : [];
      let blockedKeys = {};
      let options = {};
      if (optionsString) {
        optionsString.split(',').map(function (option) {
          option = option.split('=', 2).map((s) => s.trim());
          options[option[0]] = option[1] || true;
        });
        if (options.onInsert) {
          options.onInsert = WS._buildNonBoolFunc(options.onInsert);
        }
      }
      let context = elem.attr('sbContext');
      context = context ? (context + ':').split(':', 2) : ['', ''];
      if (context[0] != '' && !context[0].endsWith('^')) {
        context[0] = context[0] + '.';
      }
      elem.detach().removeAttr('sbForeach');
      paths.forEach(function (path) {
        const field = path.substring(path.lastIndexOf('.') + 1);
        if (options.filter === '^') {
          options.filter = paren
            .closest('[' + field + ']')
            .attr(field)
            .slice(0, -2); // cut off .*
        }
        fixedKeys.forEach(function (key) {
          if (key.startsWith('-')) {
            blockedKeys[key.substring(1)] = true;
          } else {
            if (options.filter) {
              key = options.filter + '.' + key;
            }
            const newElem = elem.clone(true).attr(field, key).addClass('sbFixed');
            if (!options.noContext) {
              newElem.attr('sbContext', context[0] + field + '(' + key + '):' + context[1]);
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
            if (options.onInsert) {
              options.onInsert(WS._enrichProp(WS._getContext(newElem)[0]), null, newElem);
            }
          }
        });
        if (sortFunction !== 'only') {
          path = path + '(' + (options.filter ? options.filter + '.' : '') + '*)' + (options.noId ? '' : '.Id');
          const func = WS._buildSortFunc(sortFunction || field);
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
                  newElem.attr(
                    'sbContext',
                    (key === k[field] ? '/' + k.upTo(field) + ':' : context[0] + field + '(' + key + '):') + context[1]
                  );
                }
                if (key !== k[field]) {
                  newElem.attr('sbCount', 1).attr(subfieldId, k[field]);
                }
                WS.AutoRegister(newElem);
                if (elem.prop('tagName') === 'OPTION') {
                  const cached = WS._selectCache.get(paren[0]);
                  if (cached) {
                    cached.callback(WS._enrichProp(cached.path), WS.state[cached.path], paren);
                  }
                }
                if (options.resort) {
                  WS.Register(WS._getContext(newElem)[0] + '.' + options.resort, {
                    preRegistered: true,
                    element: newElem,
                    triggerFunc: function (k, v, elem) {
                      if (v != null) {
                        elem.detach();
                        _windowFunctions.appendSorted(paren, elem, func, preForeachItem.index() + 1);
                        if (options.onInsert) {
                          options.onInsert(WS._enrichProp(WS._getContext(newElem)[0]), null, newElem);
                        }
                      }
                    },
                  });
                } else {
                  newElem.detach();
                  _windowFunctions.appendSorted(paren, newElem, func, preForeachItem.index() + 1);
                  if (options.onInsert) {
                    options.onInsert(WS._enrichProp(WS._getContext(newElem)[0]), null, newElem);
                  }
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
    } else {
      const autoFitPaths = WS._getAutoFitPaths(elem);
      const context = WS._enrichProp(WS._getContext(elem)[0]);
      let autoFitNeeded = elem.hasClass('AutoFit');

      WS._getParameters(elem, 'sbOn', -1, -1, 1).forEach(function ([event, func]) {
        elem.on(event, function (e) {
          return func(context, elem.val(), elem, e);
        });
      });

      WS._getParameters(elem, 'sbDisplay', 0, 1, -1, false, false, true).forEach(function ([paths, func, options]) {
        autoFitNeeded = false;
        WS.Register(paths.concat(autoFitPaths), { preRegistered: true, element: elem, html: options === 'html', modifyFunc: func });
      });

      const sbSetEntries = WS._getParameters(elem, 'sbSet', 0, -1, 1);
      if (sbSetEntries.length) {
        elem.on(WS._isInputElement(elem) ? 'change' : 'click', function (event) {
          const prefixes = WS._getPrefixes(elem);
          sbSetEntries.map(function ([paths, func, flag]) {
            paths.map(function (path) {
              if (prefixes[path[0]]) {
                path = (prefixes[path[0]].prefix || '') + path.substring(1) + (prefixes[path[0]].suffix || '');
              }
              WS.Set(path, func(WS._enrichProp(path), WS._isInputElement(elem) ? elem.val() : true, elem, event), flag);
            });
          });
        });
      }

      WS._getParameters(elem, 'sbControl', 0, 1, 2).forEach(function ([paths, readFunc, writeFunc]) {
        WS.Register(paths, { preRegistered: true, element: elem, set: WS._isInputElement(elem), modifyFunc: readFunc });
        elem.on(WS._isInputElement(elem) ? 'change' : 'click', function (event) {
          WS.Set(paths[0], writeFunc(WS._enrichProp(paths[0]), elem.val(), elem, event));
        });
      });

      WS._getParameters(elem, 'sbToggle', 0).forEach(function ([paths, usedClass = 'sbActive']) {
        WS.Register(paths, { preRegistered: true, element: elem, toggleClass: usedClass, modifyFunc: WS._buildBoolFunc('') });
        elem.on('click', function () {
          WS.Set(paths[0], !elem.hasClass(usedClass));
        });
      });

      WS._getParameters(elem, 'sbClass', 1, 2, -1, true).forEach(function ([toggleClass, paths, func]) {
        WS.Register(paths, { preRegistered: true, element: elem, toggleClass: toggleClass, modifyFunc: func });
      });

      WS._getParameters(elem, 'sbCss', 1, 2).forEach(function ([prop, paths, func]) {
        WS.Register(paths, { preRegistered: true, element: elem, css: prop, modifyFunc: func });
      });

      WS._getParameters(elem, 'sbAttr', 1, 2).forEach(function ([attr, paths, func]) {
        WS.Register(paths, { preRegistered: true, element: elem, attr: attr, modifyFunc: func });
      });

      WS._getParameters(elem, 'sbProp', 1, 2, -1, true).forEach(function ([prop, paths, func]) {
        WS.Register(paths, { preRegistered: true, element: elem, prop: prop, modifyFunc: func });
      });

      WS._getParameters(elem, 'sbCall', -1, -1, 0).forEach(function ([func]) {
        elem.on(WS._isInputElement(elem) ? 'change' : 'click', function (event) {
          func(context, elem.val(), elem, event);
        });
      });

      elem.children().each(function () {
        autoFitNeeded = false;
        WS.AutoRegister($(this));
      });

      if (autoFitNeeded) {
        const text = elem.text();
        WS.Register(autoFitPaths, {
          preRegistered: true,
          element: elem,
          modifyFunc: function () {
            return text;
          },
        });
      }
    }
  },

  _preRegister: function () {
    let paths = [];
    const preRegisterAttribute = function (attr, pathIndex) {
      $('[' + attr + ']').each(function (idx, elem) {
        WS._getParameters($(elem), attr, pathIndex, -1, -1, false, true).forEach(function (params) {
          paths = paths.concat(params[pathIndex]);
        });
      });
    };

    $('[sbForeach]').each(function (idx, elem) {
      WS._getParameters($(elem), 'sbForeach', 0, -1, -1, false, true).forEach(function (params) {
        paths = paths.concat(params[0].map((s) => s + '(*)' + ((params[3] || '').includes('noId') ? '' : '.Id')));
      });
    });

    ['sbDisplay', 'sbControl', 'sbToggle'].forEach(function (attr) {
      preRegisterAttribute(attr, 0);
    });
    ['sbClass', 'sbCss', 'sbAttr', 'sbProp'].forEach(function (attr) {
      preRegisterAttribute(attr, 1);
    });

    WS.Register(paths, null, true);
  },

  _replacePathRe: /\[([^\]]*)\]/g,
  _replacePathComponents: function (elem, attr, isPreRegister, skipCopyContext) {
    let reevaluate = false;
    let value = elem.attr(attr);
    if (value) {
      let changed = false;
      value = value.replaceAll(WS._replacePathRe, function (match, field) {
        changed = true;
        if (field === '*') {
          if (skipCopyContext) {
            // we're called as part of determining the proper replacement - break infinite loop
            return match;
          }
          let context = WS._getContext(elem, false, isPreRegister, attr === 'sbPrefix');
          return context[1] ? match : context[0];
        } else {
          const src = elem.closest(match + ', [sbForeach^="' + match + '"]:not([sbForeach*="noContext"])');
          return src.length ? src.attr(field) || match : _windowFunctions.getParam(field) || match;
        }
      });
      if (changed) {
        elem.attr(attr, value);

        value = value.replaceAll(WS._replacePathRe, function (match, field) {
          reevaluate = true;
          return field === '*' ? (skipCopyContext ? '[*]' : WS._pathCache.get(elem[0]).path[0]) : '*';
        });
      }
      if (isPreRegister && value.includes('*')) {
        reevaluate = true;
      }
    }
    return [value, reevaluate];
  },

  _pathCache: new Map(),
  _getContext: function (elem, skipSuffix, isPreRegister, skipCopyContext) {
    const cached = WS._pathCache.get(elem[0]);
    if (cached) {
      const match = skipSuffix ? cached.noSuffixPath : cached.path;
      if (!match[1] || isPreRegister) {
        return match;
      }
    }
    let path = ['', false];
    const parent = elem.parent();
    if (parent.length > 0) {
      path = WS._getContext(parent, false, isPreRegister);
    }
    const prefixes = WS._getPrefixes(elem, isPreRegister, skipCopyContext);
    let [value, reeval] = WS._replacePathComponents(elem, 'sbContext', isPreRegister);
    let suffix = [undefined, reeval];
    if (value) {
      const parts = value.split(':', 2);
      path = WS._combinePaths(path, [parts[0], reeval], prefixes);
      suffix = [parts[1], reeval];
    } else {
      path = [path[0], path[1]]; // deep copy
    }
    const noSuffixPath = [path[0], path[1]]; // deep copy
    const foreach = elem.attr('sbForeach');
    if (foreach != null && !foreach.includes('noContext')) {
      path[0] = (path[0] !== '' ? path[0] + '.' : '') + foreach.split(':', 1)[0].trim() + '(*)';
      path[1] = true;
    }
    if (suffix[0]) {
      path = WS._combinePaths(path, suffix, prefixes);
    }
    WS._pathCache.set(elem[0], { path: path, noSuffixPath: noSuffixPath });
    return skipSuffix ? noSuffixPath : path;
  },

  _combinePaths: function ([basePath, basePathReeval], [subPath, subPathReeval], prefixes) {
    if (subPath === '') {
      return [basePath, basePathReeval];
    } else if (subPath.startsWith('/')) {
      return [subPath.substring(1), subPathReeval];
    } else if (prefixes[subPath[0]]) {
      const match = prefixes[subPath[0]];
      return [(match.prefix || '') + subPath.substring(1) + (match.suffix || ''), match.reevaluate || subPathReeval];
    } else if (subPath[1] === '.') {
      // using a dynamic prefix that's not set yet
      return [subPath, true];
    } else {
      while (basePath && subPath.startsWith('^')) {
        subPath = subPath.substring(1);
        basePath = basePath.substring(0, basePath.lastIndexOf('.'));
      }
      return [basePath + (basePath && subPath ? '.' : '') + subPath, basePathReeval || subPathReeval];
    }
  },

  _prefixCache: new Map(),
  _getPrefixes: function (elem, isPreRegister, skipCopyContext) {
    if (!elem.length) {
      return {};
    }
    const cached = WS._prefixCache.get(elem[0]);
    if (cached && (!cached.reevaluate || isPreRegister)) {
      return cached.prefixes;
    }

    let prefixes = {};
    Object.entries(WS._getPrefixes(elem.parent(), isPreRegister)).forEach(function ([prefix, value]) {
      prefixes[prefix] = { prefix: value.prefix, suffix: value.suffix, reevaluate: value.reevaluate };
    });
    const [value, reevaluate] = WS._replacePathComponents(elem, 'sbPrefix', isPreRegister, skipCopyContext);
    if (value) {
      value.split('|').map(function (entry) {
        entry = entry.split(':').map((s) => s.trim());
        prefixes[entry[0]] = { prefix: entry[1], suffix: entry[2], reevaluate: reevaluate };
      });
    }
    WS._prefixCache.set(elem[0], { reevaluate: reevaluate, prefixes: prefixes });
    return prefixes;
  },

  SetupDialog: function (elem, context, options) {
    elem = elem.clone().attr('sbContext', '/' + context);
    context = WS._enrichProp(context);
    context.parts.forEach(function (part) {
      if (context[part]) {
        elem.attr(part, context[part]);
      }
    });
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
