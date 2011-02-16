

_crgUtils = {
	/* Bind and run a function.
	 * This is more restrictive than the actual bind() function,
	 * as only one eventType can be specified, and this does
	 * not support a map as the jQuery bind() function does.
	 * The eventData and initialParams parameters are optional.
	 * The initialParams, if provided, is an array of the parameters
	 * to supply to the initial call of the handler.
	 * The handler is initially run once for each element
	 * in the jQuery target object.
	 * As a special case, if the eventType is "content", and
	 * the initialParams are not defined, and the target
	 * is a $sb() node, the target.$sbGet() value is passed as the
	 * first and second initial parameters to the handler.
	 */
//FIXME - add parameter for initial param(s) to handler call?
	bindAndRun: function(target, eventType, eventData, handler, initialParams) {
		target.bind(eventType, eventData, handler);
		if (typeof eventData == "function") {
			initialParams = handler;
			handler = eventData;
			eventData = undefined;
		}
		target.each(function() {
			var params = [ ];
			if (initialParams)
				params = initialParams;
			else if (eventType.trim() == "content" && $sb(this))
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

	/* Bind functions to the addition/removal of specific children.
	 * The add function is also called for each of the existing matched children.
	 * This works ONLY when using a single $sb() element as the target.
	 *
	 * Calling api is one of:
	 *   bindAddRemoveEach(target, childname, add, remove);
	 *   bindAddRemoveEach(target, parameters);
	 *
	 * Individual parameters:
	 * target: This is the $sb element, or value which can be passed to $sb(),
	 *         to which the add/remove functions are bound.
	 * childname: This is the name of the child elements to monitor
	 *            (use null/undefined/"" to match all children)
	 * add: The function to call when a child is added (use null/undefined to ignore).
	 * remove: The function to call when a child is removed (use null/undefined to ignore).
	 *
	 * If an object is used instead, the above parameters can be included plus
	 * these addition parameters:
	 * subChildren: Optional boolean to indicate if events from non-immediate
	 *              children should be processed (defaults to only immediate children).
	 * callback: A callback function that is called after this is finished (i.e.
	 *           after the add function is called for all matching children)
	 */
	bindAddRemoveEach: function(target, childname, add, remove) {
		var options = { childname: childname, add: add, remove: remove };
		if (typeof childname == "object")
			options = childname;
		target = $sb(target);
		childname = options.childname || "";
		add = options.add || $.noop;
		remove = options.remove || $.noop;
		var subChildren = options.subChildren || false;
		var callback = options.callback || $.noop;
		var addEventType = "add"+(childname?":"+childname:"");
		var removeEventType = "remove"+(childname?":"+childname:"");
		target.bind(addEventType, function(event,node) {
			if (subChildren || (event.target == this)) add(event,node);
		});
		target.bind(removeEventType, function(event,node) {
			if (subChildren || (event.target == this)) remove(event,node);
		});
		var currentChildren = (subChildren ? target.find(childname||"*") : target.children(childname||"*"));
		currentChildren.each(function() {
			var event = jQuery.Event(addEventType);
			event.target = $(this).parent()[0];
			event.currentTarget = target[0];
			add(event,$sb(this));
		});
		callback();
		return target;
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
			buttons: [ { text: buttonText, click: login } ]
		});
	},
};
