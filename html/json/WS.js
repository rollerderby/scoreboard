var WS = {
	connectCallback: null,
	connectTimeout: null,
	registeredPaths: new Array(),
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
			$.each(Object.keys(WS.registeredPaths), function (idx, path) {
				req.paths.push(path);
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
			var callbacks = WS.registeredPaths[k];
			if (callbacks != null) {
				for (i = 0; i < callbacks.length; i++)
					callbacks[i](k, v);
			} else if (k != "stateID") {
				// console.log(k, v);
			}
		});
	},

	Register: function(path, arg1, arg2) {
		if (WS.registeredPaths[path] == null) {
			WS.registeredPaths[path] = new Array();
		}

		if ($.isFunction(arg1)) {
			WS.registeredPaths[path].push(arg1);
			if (WS.state[path] != null)
				arg1(path, WS.state[path]);
		} else if ($.isjQuery(arg1)) {
			if (arg2 != null) {
				callback = function(path, value) {
					if ($.isFunction(arg2)) {
						arg1.text(arg2(value));
					} else {
						if (arg2.css != null)
							arg1.css(arg2.css, value);
						if (arg2.attr != null)
							arg1.attr(arg2.attr, value);
					}
				};
				WS.registeredPaths[path].push(callback);
				if (WS.state[path] != null)
					callback(path, WS.state[path]);
			} else {
				callback = function(path, value) {
					arg1.text(value);
				};
				WS.registeredPaths[path].push(callback);
				if (WS.state[path] != null)
					callback(path, WS.state[path]);
			}
		}

		req = {
			action: "Register",
			path: path
		};
		ws.send(JSON.stringify(req));
	}
};
