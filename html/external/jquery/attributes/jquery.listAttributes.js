/*!
 * listAttributes jQuery Plugin v1.1.0
 *
 * Copyright 2010, Michael Riddle
 * Licensed under the MIT
 * http://jquery.org/license
 *
 * Date: Sun Mar 28 05:49:39 2010 -0900
 */
if(jQuery) {
	jQuery(document).ready(function() {
		jQuery.fn.listAttributes = function(prefix) {
			var list = [];
			$(this).each(function() {
				console.info(this);
				var attributes = [];
				for(var key in this.attributes) {
					if(!isNaN(key)) {
						if(!prefix || this.attributes[key].name.substr(0,prefix.length) == prefix) {
							attributes.push(this.attributes[key].name);
						}
					}
				}
				list.push(attributes);
			});
			return (list.length > 1 ? list : list[0]);
		}
	});
}