var Rulesets = {
	List: function(callback) {
		$.getJSON('/JSON/Ruleset/List', callback);
	},

	ListDefinitions: function(callback) {
		$.getJSON('/JSON/Ruleset/ListDefinitions', callback);
	},

	New: function(obj, callback, error) {
		$.ajax({
			type: "POST",
			url: '/JSON/Ruleset/New',
			data: JSON.stringify(obj),
			success: callback,
			error: error,
			dataType: "json"
		});
	},

	Update: function(obj, callback, error) {
		$.ajax({
			type: "POST",
			url: '/JSON/Ruleset/Update',
			data: JSON.stringify(obj),
			success: callback,
			error: error,
			dataType: "json"
		});
	},

	Delete: function(obj, callback, error) {
		$.ajax({
			type: "POST",
			url: '/JSON/Ruleset/Delete',
			data: JSON.stringify(obj),
			success: callback,
			error: error,
			dataType: "json"
		});
	}
};
