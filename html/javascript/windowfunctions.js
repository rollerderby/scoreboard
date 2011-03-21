

_windowFunctions = {
	/* Display area dimensions */
	getAspectDimensions: function(aspect, overflow) {
		var width, height, top, bottom, left, right;
		if ((aspect > ($(window).width()/$(window).height())) == (overflow==true)) {
			width = Math.round((aspect * $(window).height()));
			height = $(window).height();
			top = bottom = 0;
			left = right = (($(window).width() - width) / 2);
		} else {
			width = $(window).width();
			height = Math.round(($(window).width() / aspect));
			top = bottom = (($(window).height() - height) / 2);
			left = right = 0;
		}
		return { width: width, height: height, top: top, bottom: bottom, left: left, right: right };
	},
	get4x3Dimensions: function(overflow) { return this.getAspectDimensions(4/3, overflow); },
	get16x9Dimensions: function(overflow) { return this.getAspectDimensions(16/9, overflow); },

	/* This enables the specified element for auto-fitting its text into the parent
	 * container.  See the autoFitText() function description for details on the
	 * options.  This returns a reference to a function that should be called,
	 * with no parameters, if the autoFitText() function needs to be called manually,
	 * e.g. if the actual element text changes or its parent container size changes.
	 * The text will only be auto-fit automatically when the browser window resizes.
	 * The auto-fit function can also be accessed via element.data("AutoFit").
	 */
	enableAutoFitText: function(e, options) {
		if (!e)
			return null;
		e = $(e);
		if (!e.length)
			return null;
		if (e.data("AutoFit"))
			return e.data("AutoFit");
		e.data("AutoFit", function() { return _windowFunctions.autoFitText(e, options); });
		$(window).bind("resize", e.data("AutoFit"));
		e.data("AutoFit").call();      // auto-fit the text now,
		setTimeout(e.data("AutoFit")); // and also later as page may not be laid out fully yet
		return e.data("AutoFit");
	},
	disableAutoFitText: function(e) {
		if (!e.data("AutoFit"))
			return;
		$(window).unbind("resize", e.data("AutoFit"));
		e.removeData("AutoFit");
	},
	/* This should be called each time the text content changes and/or the window resizes.
	 * It resizes the text element to fit, and returns the current relevant css properties.
	 * The options are:
	 *   max: number (default: 100)
	 *     This sets the maximum font size, in %.
	 *   overage: number (default: 0)
	 *     This sets the overage, in %.  The overage increases the vertical height allowed,
	 *     so the auto-sized text element can be vertically larger than its parent container.
	 *     Usually with text elements, there is blank space vertically surrounding the text,
	 *     so this allows pushing that vertical blank space outside the container, which
	 *     allows the actual text font to fill up more of the container.
	 *   percentHeight: number (default: 98)
	 *     This sets the minimum height value, in %, that indicates a successful fit.
	 *     Once the element's height is at least this percent of the container's height,
	 *     after accounting for overage, the auto fitting will stop at the current settings.
	 *   iterations: number (default: 10)
	 *     This is the maximum number of iterations to fine-tune the fit.
	 *     Unless the font size required is a very small number (in %),
	 *     this should not need adjustment.
	 */
	autoFitText: function(text, options) {
		var container = text.parent();
		var getTextCss = function() {
			return { fontSize: text.css("fontSize"), top: text.css("top"), position: text.css("position") };
		};

		if (!text.text())
			return getTextCss();

		if (!options)
			options = { };

		var max = options.max || 100;
		var iterations = options.iterations || 10;
		var percentHeight = (options.percentHeight || 98) / 100;
		var overage = (options.overage || 0) / 100;

		var maxW = container.innerWidth();
		var maxH = container.innerHeight();
		var overH = overage * maxH;
		var targetH = maxH + overH;

		var topSize = max, bottomSize = max;
		var overSize = function() {
			return (text.outerWidth(true) > maxW) || (text.outerHeight(true) > targetH);
		};
		var atPercentHeight = function() {
			return (text.outerHeight(true) > (percentHeight * targetH));
		};
		var updateFontSize = function(size) {
			text.css("fontSize", ((size * $(window).height()) / 100)+"px");
		};

		text.css({ position: "absolute", top: "0px" });
		updateFontSize(topSize);

		while (overSize() && (bottomSize > 0)) {
			topSize = bottomSize;
			updateFontSize(bottomSize--);
		}

		if (topSize != bottomSize) {
			for (var i=0; i<iterations; i++) {
				var newSize = ((bottomSize + topSize) / 2);
				updateFontSize(newSize);
				if (overSize())
					topSize = newSize;
				else if (atPercentHeight())
					break;
				else
					bottomSize = newSize;
			}
			if (overSize())
				updateFontSize(bottomSize);
		}

		text.css("top", ((maxH - text.outerHeight(true)) / 2)+"px");
		return getTextCss();
	},

	/* URL parameters */
	getParam: function(param) {
		var value = $.string(window.location.search).toQueryParams()[param];
		return ($.isArray(value) ? value[0] : value);
	},
	hasParam: function(param) {
		return $.string(window.location.search).toQueryParams().hasOwnProperty(param);
	},
	checkParam: function(param, value) { return value == getParam(param); },

	/* DOM element sorting */
	appendSorted: function(parent, newChild, comparator, startIndex) {
		var child = null;
		parent.children().each(function(i) {
			if ((startIndex <= i) && comparator(this, newChild[0])) {
				child = this;
				return false;
			}
		});
		if (child)
			newChild.insertBefore(child);
		else
			parent.append(newChild);
		return parent;
	},
	appendAlphaSorted: function(parent, newChild, startIndex) { return _windowFunctions.appendSorted(parent, newChild, _windowFunctions.alphaSortByNodeName, startIndex); },
	appendAttrAlphaSorted: function(parent, newChild, attrName, startIndex) { return _windowFunctions.appendSorted(parent, newChild, function(a,b) { return _windowFunctions.alphaSortByAttr(a, b, attrName); }, startIndex); },
	appendAttrNumericSorted: function(parent, newChild, attrName, startIndex) { return _windowFunctions.appendSorted(parent, newChild, function(a,b) { return _windowFunctions.numericSortByAttr(a, b, attrName); }, startIndex); },
	alphaSortByNodeName: function(dom1, dom2) { return dom1.nodeName > dom2.nodeName; },
	alphaSortByAttr: function(dom1, dom2, name) { return $(dom1).attr(name) > $(dom2).attr(name); },
	numericSortByAttr: function(dom1, dom2, name) { return Number($(dom1).attr(name)) > Number($(dom2).attr(name)); }
};
