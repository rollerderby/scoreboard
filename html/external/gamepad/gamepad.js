(function(){
	// author: yetzt, license: Unlicense <https://unlicense.org/UNLICENSE>

	var requestAnimationFrame = window.requestAnimationFrame || window.webkitRequestAnimationFrame;
	var gamepads = {};

	// check state
	function update(){

		// stop checking if no gamepads exist
		if (Object.values(gamepads).length === 0) return;

		Object.values(gamepads).forEach(function(gamepad,p){
			// check buttons for presses
			gamepad.device.buttons.forEach(function(b, i){
				var state = (b.pressed || b.touched || b.value > 0);
				if (state !== gamepad.buttons[i]) {
					document.dispatchEvent(new CustomEvent("gamepadbutton", { detail: {
						type: state ? "down" : "up",
						device: p,
						id: "b"+i,
					}}))
					gamepad.buttons[i] = state;
				}
			});
			// check axes (some cheaper controllers pretend buttons are analog sticks)
			gamepad.device.axes.forEach(function(a, i){
				var state = (a < -0.5);
				if (state !== gamepad.axes[i*2]) {
					document.dispatchEvent(new CustomEvent("gamepadbutton", { detail: {
						type: state ? "down" : "up",
						device: p,
						id: "a"+(i*2),
					}}));
					gamepad.axes[i*2] = state;
				}
				var nstate = (a > 0.5);
				if (nstate !== gamepad.axes[i*2+1]) {
					document.dispatchEvent(new CustomEvent("gamepadbutton", { detail: {
						type: nstate ? "down" : "up",
						device: p,
						id: "a"+(i*2+1),
					}}));
					gamepad.axes[i*2+1] = nstate;
				}
			});
		});
		// check state again next frame
		requestAnimationFrame(update);
	};

	// listen for connected gamepads
	window.addEventListener("gamepadconnected", function(event){
		gamepads[event.gamepad.id] = {
			device: event.gamepad,
			buttons: event.gamepad.buttons.map(function(b){ // initial state
				return b.pressed || b.touched || b.value > 0
			}),
			axes: event.gamepad.axes.reduce(function(x,a){ // make two pseudo-buttons out of one axis
				x.push((a < -0.5) ? true : false);
				x.push((a > 0.5) ? true : false);
				return x;
			},[]),
		};
		console.log("Connected", gamepads[event.gamepad.id]);
		if (Object.keys(gamepads).length === 1) update();
	});

	// remove disconnected gamepads
	window.addEventListener("gamepaddisconnected", function(event){
		delete gamepads[event.gamepad.id];
	});

	// integrate with crg
	document.addEventListener("gamepadbutton", function(event){
		if (_crgKeyControls && event.detail.type === "down") { // ignore release events
			// send as "event" to _crgKeyControls
			_crgKeyControls._keyControlPress({
				// map gamepad buttons to unicode symbols; works, but could be better
				which: (event.detail.id.substr(0,1) === "b") ? (event.detail.id === "b0") ? 9450 : 9312 + parseInt(event.detail.id.substr(1),10) : 9398+parseInt(event.detail.id.substr(1),10)
			});
		};
	});

})();