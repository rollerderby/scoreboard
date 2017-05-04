
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

_autoFit = {
	/* This enables the specified element for auto-fitting its contained text.
	 * See the autoFitText() function description for details on the options.
	 * This returns a reference to a function that should be called,
	 * with no parameters, if the autoFitText() function needs to be called manually,
	 * e.g. if the contained text changes or the container size changes.
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
		var doAutoFit = function() { return _autoFit.autoFitText(e, options); };
		e.data("AutoFit", doAutoFit);
		$(window).bind("resize", function(event) {
			if (e.closest("body").length)
				doAutoFit();
			else
				$(window).unbind("resize", event);
		});
		setTimeout(doAutoFit, 100); // run initial autofit deferred
		setTimeout(doAutoFit, 2000); // again in 2 sec; bug workaround FIXME
		return doAutoFit;
	},
	disableAutoFitText: function(e) {
		if (!e.data("AutoFit"))
			return;
		$(window).unbind("resize", e.data("AutoFit"));
		e.removeData("AutoFit");
	},
	/* This should be called each time the text content changes, or the window
	 * or container resizes.	It resizes the text element to fit, and returns the
	 * current relevant css properties. The options are:
	 *	 referenceFontSize: number (default: window height)
	 *		 This is the reference font size, in px.	If not specified,
	 *		 the window height is used.
	 *	 min: number (default: 1)
	 *		 This sets the minimum font size, in % of the reference font size.
	 *	 max: number (default: 1000)
	 *		 This sets the maximum font size, in % of the reference font size.
	 *	 overage: number (default: 0)
	 *		 This sets the overage, in additional % of the container's height.
	 *		 The overage increases the vertical height allowed, so the auto-sized
	 *		 text element can be vertically larger than its parent container.
	 *		 Usually with text elements, there is blank space vertically surrounding the text,
	 *		 so this allows pushing that vertical blank space outside the container, which
	 *		 allows the actual text font to fill up more of the container.
	 *	 percentHeight: number (default: 98)
	 *		 This sets the minimum height value, in % of the container's height,
	 *		 that indicates a successful fit.	 Once the element's height is at least
	 *		 this percent of the container's height, after accounting for overage,
	 *		 the auto fitting will stop at the current settings.
	 *	 useMarginBottom: boolean (default: false)
	 *		 This causes the container's marginBottom property to be used to adjust the
	 *		 vertical position instead of the marginTop property.	 By default, the
	 *		 marginTop property is used to vertically shift the container so the contained
	 *		 text is vertically centered in the container's normal position.
	 *	 noVerticalAdjust: boolean (default: false)
	 *		 If true, this prevents the marginTop (or marginBottom) property from being set.
	 *	 iterations: number (default: 30)
	 *		 This is the maximum number of iterations to fine-tune the fit.
	 *		 It's unlikely this needs to be changed.
	 *
	 * This returns an object with the relevant css properties that were updated.
	 */
	autoFitText: function(container, options) {
		if (!options)
			options = { };

		if (!container.text())
			return _autoFit._cssObject(container, options);

		var contents = container.children();
		if (!contents.length)
			return _autoFit._cssObject(container, options);
		else if (1 < contents.length)
			contents = container.wrapInner("<span>").children().addClass("autoFitTextWrapper");

		container.css({ marginTop: "0px", marginBottom: "0px" });

		var params = {
			min: (options.min || 0.1),
			max: (options.max || 100),
			iterations: (options.iterations || 30),
			percentHeight: ((options.percentHeight || 98) / 100),
			maxW: container.innerWidth(),
			maxH: container.innerHeight(),
			targetH: (((100 + (options.overage || 0)) * container.innerHeight()) / 100)
		};

//FIXME - using window height is wrong, e.g. for fixed-aspect views like the scoreboard,
//				it should be the aspect-corrected height; maybe referenceFontSize should be mandatory param?
		params.referenceFontSize = (options.referenceFontSize || $(window).height());
		params.minFontSize = ((params.min * params.referenceFontSize) / 100);
		params.maxFontSize = ((params.max * params.referenceFontSize) / 100);

		if (!params.maxW || !params.maxH)
			// likely a last call for a removed element; in any case, can't autofit for a 0-size element
			return _autoFit._cssObject(container, options);

		if (_autoFit._currentFontSize(container) > params.maxFontSize)
			container.css("fontSize", params.maxFontSize);
		if (_autoFit._currentFontSize(container) < params.minFontSize)
			container.css("fontSize", params.minFontSize);

		var breakCnt = 0;
		while (0 < params.iterations-- && ++breakCnt < 100) {
			if (_autoFit._overSize(contents, params)) {
				if (!_autoFit._updateMinMaxFontSizes(container, params, params.minFontSize, _autoFit._currentFontSize(container)))
					break;
			} else if (contents.outerHeight(true) > (params.percentHeight * params.targetH))
				break;
			else if (!_autoFit._updateMinMaxFontSizes(container, params, _autoFit._currentFontSize(container), params.maxFontSize))
				break;
		}

		var reduceBy = 1;
		breakCnt = 0;
		while (_autoFit._overSize(contents, params) && ++breakCnt < 100) {
			if (!_autoFit._updateFontSize(container, _autoFit._currentFontSize(container) - reduceBy))
				reduceBy++;
		}

		if (!options.noVerticalAdjust) {
			var vShift = ((params.maxH - contents.outerHeight(true)) / 2);
			// adjust for browser-specific vertical adjustment of text
			vShift -= contents.position().top;
			if (options.useMarginBottom)
				container.css("margin-bottom", (-1*vShift)+"px");
			else
				container.css("margin-top", vShift+"px");
		}
		contents.filter(".autoFitTextWrapper").children().unwrap();
		return _autoFit._cssObject(container, options);
	},
	_currentFontSize: function(container) {
		return Number(container.css("fontSize").replace(/px$/, ""));
	},
	_updateFontSize: function(container, size) {
		var last = _autoFit._currentFontSize(container);
		container.css("fontSize", size+"px");
		return (last != _autoFit._currentFontSize(container));
	},
	_updateMinMaxFontSizes: function(container, params, newMin, newMax) {
		params.minFontSize = Number(newMin);
		params.maxFontSize = Number(newMax);
		return _autoFit._updateFontSize(container, (params.minFontSize + params.maxFontSize) / 2);
	},
	_overSize: function(contents, params) {
		return ((contents.outerWidth(true) > params.maxW) || (contents.outerHeight(true) > params.targetH));
	},
	_cssObject: function(container, options) {
		if (!options.returnCssObject)
			return;
		var obj = { fontSize: container.css("fontSize") };
		if (!options.noVerticalAdjust) {
			if (options.useMarginBottom)
				obj.marginBottom = container.css("marginBottom");
			else
				obj.marginTop = container.css("marginTop");
		}
		return obj;
	}

};
