var WS = {
	connectCallback: null,
	connectTimeout: null,
	callbacks: new Array(),
	state: new Array(),

	Connect: function(callback) {
		WS.connectCallback = callback;
		WS._connect();
	},

	_connect: function() {
		WS.connectTimeout = null;
		var url = (document.location.protocol == "http:" ? "ws" : "wss") + "://";
		url += document.location.host + "/WS/";
		console.log("Connecting the websocket at " + url);
		WS.socket = new WebSocket(url);
		WS.socket.onopen = function(e) {
			console.log("Websocket: Open");
			$(".ConnectionError").addClass("Connected");
			req = {
				action: "Register",
				paths: new Array()
			};
			$.each(Object.keys(WS.state), function(idx, k) {
				WS.triggerCallback(k, null);
			});
			WS.state = new Array();
			$.each(WS.callbacks, function (idx, c) {
				req.paths.push(c.path);
			});
			if (req.paths.length > 0) {
				WS.send(JSON.stringify(req));
			}
			if (WS.connectCallback != null)
				WS.connectCallback();
		};
		WS.socket.onmessage = function(e) {
			json = JSON.parse(e.data);
			console.log(json);
			if (json.authorization != null)
				alert(json.authorization);
			if (json.state != null)
				WS.processUpdate(json.state);
		};
		WS.socket.onclose = function(e) {
			console.log("Websocket: Close", e);
			$(".ConnectionError").removeClass("Connected");
			if (WS.connectTimeout == null)
				WS.connectTimeout = setTimeout(WS._connect, 1000);
		};
		WS.socket.onerror = function(e) {
			console.log("Websocket: Error", e);
			$(".ConnectionError").removeClass("Connected");
			if (WS.connectTimeout == null)
				WS.connectTimeout = setTimeout(WS._connect, 1000);
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

	triggerCallback: function (k, v) {
		WS.state[k] = v;
		for (idx = 0; idx < WS.callbacks.length; idx++) {
			c = WS.callbacks[idx];
			if (c.callback == null)
				continue;
			if (k.indexOf(c.path) == 0) {
				try {
					c.callback(k, v);
				} catch (err) {
					console.log(err.message, err.stack);
				}
			}
		}
	},

	processUpdate: function (state) {
		for (var prop in state) {
			WS.triggerCallback(prop, state[prop]);
		}
	},

	Register: function(paths, options) {
		if ($.isFunction(options))
			options = { triggerFunc: options };

		var callback = null;
		if (options == null) {
			callback = null;
		} else {
			if (options.triggerFunc != null) {
				callback = options.triggerFunc;
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
		});

		req = {
			action: "Register",
			paths: paths
		};
		WS.send(JSON.stringify(req));
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
			if (paths.length > 0)
				WS.Register(paths, { element: elem, modifyFunc: window[elem.attr("sbModify")] });
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
