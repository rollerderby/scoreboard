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
		ws = new WebSocket(url);
		ws.onopen = function(e) {
			console.log("OPEN", e);
			$("body").addClass("Connected");
			req = {
				action: "Register",
				paths: new Array()
			};
			$.each(WS.callbacks, function (idx, c) {
				req.paths.push(c.path);
			});
			if (req.paths.length > 0) {
				ws.send(JSON.stringify(req));
			}
			WS.connectCallback();
		};
		ws.onmessage = function(e) {
			json = JSON.parse(e.data);
			WS.processUpdate(json);
		};
		ws.onclose = function(e) {
			console.log("CLOSE", e);
			$("body").removeClass("Connected");
			if (WS.connectTimeout == null)
				WS.connectTimeout = setTimeout(WS._connect, 1000);
		};
		ws.onerror = function(e) {
			console.log("ERROR", e);
			$("body").removeClass("Connected");
			if (WS.connectTimeout == null)
				WS.connectTimeout = setTimeout(WS._connect, 1000);
		}
	},

	processUpdate: function (json) {
		$.each(json, function(k, v) {
			WS.state[k] = v;
			for (idx = 0; idx < WS.callbacks.length; idx++) {
				c = WS.callbacks[idx];
				if (k.indexOf(c.path) == 0) {
					c.callback(k, v);
				}
			}
		});
	},

	Register: function(paths, options) {
		if ($.isFunction(options))
			options = { triggerFunc: options };

		var callback = null;
		if (options.triggerFunc != null) {
			callback = options.triggerFunc;
		} else {
			var elem = options.element;
			if (options.css != null) {
				callback = function(k, v) { elem.css(options.css, v); };
			} else if (options.attr != null) {
				callback = function(k, v) { elem.attr(options.attr, v); };
			} else {
				callback = function(k, v) { elem.text(v); };
			}
		}

		if (options.modifyFunc != null) {
			var origCallback = callback;
			callback = function(k, v) { origCallback(k, options.modifyFunc(k, v)); };
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
		ws.send(JSON.stringify(req));
	},

	AutoRegister: function() {
		$.each($("[sbDisplay]"), function(idx, elem) {
			elem = $(elem);
			var path = WS._getContext(elem, "sbDisplay");
			WS.Register(path, { element: elem, modifyFunc: window[elem.attr("sbModify")] });
		});
		$.each($("[sbTrigger]"), function(idx, elem) {
			elem = $(elem);
			var sbTrigger = window[elem.attr("sbTrigger")];
			if (sbTrigger == null)
				return;

			var sbTriggerOn = elem.attr("sbTriggerOn");
			var path = WS._getContext(elem);
			if (sbTriggerOn != null) {
				paths = new Array();
				$.each(sbTriggerOn.split(","), function(idx, triggerOn) {
					if (path != null)
						triggerOn = path + '.' + triggerOn;
					paths.push(triggerOn);
				});
				WS.Register(paths, { triggerFunc: sbTrigger } );
			}
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
