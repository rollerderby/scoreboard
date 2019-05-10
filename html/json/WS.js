
var WS = {

	connectCallback: null,
	connectTimeout: null,
	callbacks: new Array(),
	callbackTrie: {},
	batchCallbacks: new Array(),
	Connected: false,
	state: { },
	heartbeat: null,
	debug: false,

	Connect: function(callback) {
		WS.connectCallback = callback;
		WS._connect();
	},

	_connect: function() {
		WS.connectTimeout = null;
		var url = (document.location.protocol == "http:" ? "ws" : "wss") + "://";
		url += document.location.host + "/WS/";
	
		if(WS.Connected != true || !WS.socket) {
			if(WS.debug) console.log("WS", "Connecting the websocket at " + url);

			WS.socket = new WebSocket(url);
			WS.socket.onopen = function(e) {
				WS.Connected = true;
				if(WS.debug) console.log("WS", "Websocket: Open");
				$(".ConnectionError").addClass("Connected");
				req = {
					action: "Register",
					paths: new Array()
				};
				$.each(Object.keys(WS.state), function(idx, k) {
					WS.triggerCallback(k, null);
				});
				WS.state = {};
				$.each(WS.callbacks, function (idx, c) {
					req.paths.push(c.path);
				});
				$.each(WS.batchCallbacks, function (idx, c) {
					req.paths.push(c.path);
				});
				if (req.paths.length > 0) {
					WS.send(JSON.stringify(req));
				}
				if (WS.connectCallback != null)
					WS.connectCallback();
				// Hearbeat every 30s so the connection is kept alive.
				WS.heartbeat = setInterval(WS.Command, 30000, "Ping");
			};
			WS.socket.onmessage = function(e) {
				json = JSON.parse(e.data);
				if (WS.debug) console.log("WS", json);
				if (json.authorization != null)
					alert(json.authorization);
				if (json.state != null)
					WS.processUpdate(json.state);
			};
			WS.socket.onclose = function(e) {
				WS.Connected = false;
				console.log("WS", "Websocket: Close", e);
				$(".ConnectionError").removeClass("Connected");
				if (WS.connectTimeout == null)
					WS.connectTimeout = setTimeout(WS._connect, 1000);
				clearInterval(WS.heartbeat);
			};
			WS.socket.onerror = function(e) {
				console.log("WS", "Websocket: Error", e);
				$(".ConnectionError").removeClass("Connected");
				if (WS.connectTimeout == null)
					WS.connectTimeout = setTimeout(WS._connect, 1000);
				clearInterval(WS.heartbeat);
			}
		} else {
			// better run the callback on post connect if we didn't need to connect
			if(WS.connectCallback != null) WS.connectCallback();
		}
	},

	send: function(data) {
		if (WS.socket != null && WS.socket.readyState === 1) {
			WS.socket.send(data);
		}
	},

	Command: function(command, data) {
		req = {
			action: command,
			data: data
		};
		WS.send(JSON.stringify(req));
	},

	Set: function(key, value, flag) {
		req = {
			action: 'Set',
			key: key,
			value: value,
			flag: typeof flag !== 'undefined' ? flag : ''
		};
		WS.send(JSON.stringify(req));
	},

	triggerCallback: function (k, v) {
		var callbacks = WS._getMatchesFromTrie(WS.callbackTrie, k);
		for (idx = 0; idx < callbacks.length; idx++) {
			c = callbacks[idx];
			if (c == null)
				continue;
			try {
				c(k, v);
			} catch (err) {
				console.log(err.message, err.stack);
			}
		}
	},

	processUpdate: function (state) {
		for (var prop in state) {
			// update all incoming properties before triggers 
			// dependency issues causing problems
			WS.state[prop] = state[prop];
		}

		for (var prop in state) {
			if (state[prop] == null)
				WS.triggerCallback(prop, state[prop]);
		}
		for (var prop in state) {
			if (state[prop] != null)
				WS.triggerCallback(prop, state[prop]);
		}

		// Batch functions are only called once per update.
		// This is useful to avoid n^2 operations when
		// every callback redraws everything.
		var batched = {};
		$.each(WS.batchCallbacks, function(idx, c) {
			if (c.callback == null) {
				return;
			}
			for (var prop in state) {
				if (prop.indexOf(c.path) == 0) {
					batched[c.callback] = c.callback;
					return;
				}
			}
		});
		$.each(batched, function(idx, c) {
			try {
				c();
			} catch (err) {
				console.log(err.message, err.stack);
			}
		});

	},

	Register: function(paths, options) {
		if ($.isFunction(options))
			options = { triggerFunc: options };

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
					callback = function(k, v) { elem.css(options.css, v); };
				} else if (options.attr != null) {
					callback = function(k, v) { elem.attr(options.attr, v); };
				} else {
					if (elem.hasClass("AutoFit")) {
						elem.empty();
						var div = $("<div>").css("width", "100%").css("height", "100%").appendTo(elem);
						elem = $("<a>").appendTo(div);
						var autofit = _autoFit.enableAutoFitText(div);

						callback = function(k, v) {
							elem.text(v);
							if (elem.data("lastText") != v) {
								elem.data("lastText", v);
								autofit();
							}
						};
					} else if (elem.parent().hasClass("AutoFit")) {
						var autofit = _autoFit.enableAutoFitText(elem.parent());
						callback = function(k, v) {
							elem.text(v);
							if (elem.data("lastText") != v) {
								elem.data("lastText", v);
								autofit();
							}
						};
					} else {
						callback = function(k, v) { elem.text(v); };
					}
				}
			}

			if (options.modifyFunc != null) {
				var origCallback = callback;
				callback = function(k, v) { origCallback(k, options.modifyFunc(k, v)); };
			}
		}

		if (!$.isArray(paths)) {
			paths = [ paths ];
		}

		$.each(paths, function(idx, path) {
			WS.callbacks.push( { path: path, callback: callback } );
			WS._addToTrie(WS.callbackTrie, path, callback);
			WS.batchCallbacks.push( { path: path, callback: batchCallback } );
		});

		req = {
			action: "Register",
			paths: paths
		};
		WS.send(JSON.stringify(req));
	},

	_addToTrie: function(t, key, value) {
		for (var i = 0; i < key.length; i++) {
			var c = key.charAt(i);
			t[c] = t[c] || {};
			t = t[c];
		}
		t.values = t.values || [];
		t.values.push(value);
	},

	_getMatchesFromTrie: function(t, key) {
		var result = t.values || [];
		for (var i = 0; i < key.length; i++) {
			var c = key.charAt(i);
			t = t[c];
			if (t == null) {
				break;
			}
			result = result.concat(t.values || []);
		}
		return result;
	},

	getPaths: function(elem, attr) {
		var list = elem.attr(attr).split(",");
		var path = WS._getContext(elem);
		paths = new Array();
		$.each(list, function(idx, item) {
			item = $.trim(item);
			if (path != null)
				item = path + '.' + item;
			paths.push(item);
		});
		return paths;
	},

	AutoRegister: function() {
		$.each($("[sbDisplay]"), function(idx, elem) {
			elem = $(elem);
			var paths = WS.getPaths(elem, "sbDisplay");
			if (paths.length > 0) {
				// When there's multiple names, use the
				// first non-empty one.
				var mf = function() {
					var v = null;
					var path;
					for (var i = 0; i < paths.length; i++) {
						path = paths[i];
						if (WS.state[path] != null ) {
							v = WS.state[path];
							break;
						}
					}
					if (window[elem.attr("sbModify")] != null) {
						v = window[elem.attr("sbModify")](path, v);
					}
					return v;
				}
				WS.Register(paths, { element: elem, modifyFunc: mf });
			}
		});
		$.each($("[sbTrigger]"), function(idx, elem) {
			elem = $(elem);
			var sbTrigger = window[elem.attr("sbTrigger")];
			if (sbTrigger == null)
				return;

			var paths = WS.getPaths(elem, "sbTriggerOn");
			if (paths.length > 0)
				WS.Register(paths, { triggerFunc: sbTrigger } );
		});
	},

	_getContext: function(elem, attr) {
		if (attr == null)
			attr = "sbContext";

		var parent = elem.parent();
		var ret = '';
		if (parent.length > 0)
			ret = WS._getContext(parent, "sbContext");
		var context = elem.attr(attr);
		if (context != null)
			ret = (ret != '' ? ret + '.' : '') + context;
		return ret;
	},
};

//# sourceURL=json\WS.js
