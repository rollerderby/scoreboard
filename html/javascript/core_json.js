var $rulesets = {
	List: function(callback) {
		$.getJSON('/json/Ruleset/List', callback);
	},

	ListDefinitions: function(callback) {
		$.getJSON('/json/Ruleset/ListDefinitions', callback);
	},

	New: function(obj, callback, error) {
		$.ajax({
			type: "POST",
			url: '/json/Ruleset/New',
			data: JSON.stringify(obj),
			success: callback,
			error: error,
			dataType: "json"
		});
	},

	Update: function(obj, callback, error) {
		$.ajax({
			type: "POST",
			url: '/json/Ruleset/Update',
			data: JSON.stringify(obj),
			success: callback,
			error: error,
			dataType: "json"
		});
	},
};

var $game = {
	Adhoc: function(obj, callback, error) {
		$.ajax({
			type: "POST",
			url: '/json/Game/Adhoc',
			data: JSON.stringify(obj),
			success: callback,
			error: error,
			dataType: "json"
		});
	}
}
