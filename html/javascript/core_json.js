var $rulesets = {
	List: function(callback) {
		$.getJSON('/json/RuleSet/List', callback);
	},

	ListDefinitions: function(callback) {
		$.getJSON('/json/RuleSet/ListDefinitions', callback);
	},
};
