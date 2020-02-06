
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

_crgUtils = {
	/* Convert a string to a "safe" id.
	 * This removes illegal characters from the string,
	 * so it's safe to use as an element's id.
	 */
	checkSbId: function(s) {
		return s.replace(/['"()]/g, "");
	},

	/* Bind, using on(), and run a function.
	 * This is more restrictive than the actual on() function,
	 * as only one eventType can be specified, and this does
	 * not support a map as the jQuery on() function does.
	 * The eventData and initialParams parameters are optional.
	 * The initialParams, if provided, is an array of the parameters
	 * to supply to the initial call of the handler.
	 * The handler is initially run once for each element
	 * in the jQuery target object.
	 * As a special case, if the eventType is "sbchange", and
	 * the initialParams are not defined, and the target
	 * is a $sb() node, the target.$sbGet() value is passed as the
	 * first and second initial parameters to the handler.
	 * The useBind parameter, if true, will cause bind() to be used
	 * instead of on().
	 */
	onAndRun: function(target, eventType, eventData, handler, initialParams, useBind) {
		if (!$.isjQuery(target))
			target = $(target);
		if ($.isFunction(eventData)) {
			initialParams = handler;
			handler = eventData;
			eventData = undefined;
			if (useBind)
				target.bind(eventType, handler);
			else
				target.live(eventType, handler);
		} else {
			if (useBind)
				target.bind(eventType, eventData, handler);
			else
				target.live(eventType, eventData, handler);
		}
		target.each(function() {
			var params = [ ];
			if (initialParams)
				params = initialParams;
			else if ($.trim(eventType) == "sbchange" && $sb(this))
				params = [ $sb(this).$sbGet(), $sb(this).$sbGet() ];
//FIXME - call once for each eventType after splitting by spaces?
			var event = jQuery.Event(eventType);
			event.target = event.currentTarget = this;
			if (eventData)
				event.data = eventData;
			handler.apply(this, $.merge([ event ], params));
		});
		return target;
	},
	bindAndRun: function(target, eventType, eventData, handler, initialParams) {
		return _crgUtils.onAndRun(target, eventType, eventData, handler, initialParams, true);
	},

	showLoginDialog: function(titleText, nameText, buttonText, callback) {
		var dialog = $("<div>").append($("<a>").html(nameText)).append("<input type='text'/>");
		var login = function() {
			if (callback(dialog.find("input:text").val()))
				dialog.dialog("destroy");
		};
		dialog.find("input:text").keydown(function(event) { if (event.which == 13) login(); });
		dialog.dialog({
			modal: true,
			closeOnEscape: false,
			title: titleText,
			buttons: [ { text: buttonText, click: login } ],
			close: function() { if (callback('default'))
				dialog.dialog("destroy");
			}
		});
		// If we're in period 2, the dialog can end up off screen once all the
		// SK rows finish loading. This ensures the dialog stays up top.
		dialog.dialog("widget").css({"top": "5em", "position": "fixed"});
	},

	createRowTable: function(n, r) {
		var table = $("<table>").css("width", "100%").addClass("RowTable");
		var w = (100 / n) + "%";
		r = r || 1;
		while (0 < r--) {
			var count = n;
			var row = $("<tr>").appendTo(table);
			while (0 < count--)
				$("<td>").css("width", w).appendTo(row);
		}
		return table;
	}

};
//# sourceURL=javascript\utils.js
