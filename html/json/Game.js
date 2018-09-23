var Game = {
	Adhoc: function(obj, callback, error) {
		$.ajax({
			type: "POST",
			url: '/JSON/Game/Adhoc',
			data: JSON.stringify(obj),
			success: callback,
			error: error,
			dataType: "json",
			headers: {"Content-Type": "application/json;charset=UTF-8"}
		});
	}
};
//# sourceURL=json\Game.js
