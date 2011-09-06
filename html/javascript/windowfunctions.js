

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
  /* This should be called each time the text content changes, or the window
   * or container resizes.  It resizes the text element to fit, and returns the
   * current relevant css properties. The options are:
   *   referenceFontSize: number (default: window height)
   *     This is the reference font size, in px.  If not specified,
   *     the window height is used.
   *   min: number (default: 1)
   *     This sets the minimum font size, in % of the reference font size.
   *   max: number (default: 1000)
   *     This sets the maximum font size, in % of the reference font size.
   *   overage: number (default: 0)
   *     This sets the overage, in additional % of the container's height.
   *     The overage increases the vertical height allowed, so the auto-sized
   *     text element can be vertically larger than its parent container.
   *     Usually with text elements, there is blank space vertically surrounding the text,
   *     so this allows pushing that vertical blank space outside the container, which
   *     allows the actual text font to fill up more of the container.
   *   percentHeight: number (default: 98)
   *     This sets the minimum height value, in % of the container's height,
   *     that indicates a successful fit.  Once the element's height is at least
   *     this percent of the container's height, after accounting for overage,
   *     the auto fitting will stop at the current settings.
   *   useMarginBottom: boolean (default: false)
   *     This causes the container's marginBottom property to be used to adjust the
   *     vertical position instead of the marginTop property.  By default, the
   *     marginTop property is used to vertically shift the container so the contained
   *     text is vertically centered in the container's normal position.
   *   noVerticalAdjust: boolean (default: false)
   *     If true, this prevents the marginTop (or marginBottom) property from being set.
   *   iterations: number (default: 30)
   *     This is the maximum number of iterations to fine-tune the fit.
   *     It's unlikely this needs to be changed.
   *
   * This returns an object with the relevant css properties that were updated.
   */
  autoFitText: function(container, options) {
    if (!options)
      options = { };

    var cssObject = function() {
      var obj = { fontSize: container.css("fontSize") };
      if (!options.noVerticalAdjust) {
        if (options.useMarginBottom)
          obj.marginBottom = container.css("marginBottom");
        else
          obj.marginTop = container.css("marginTop");
      }
      return cssObject;
    };

    if (!container.text())
      return cssObject();

    var contents = container.children();
    if (!contents.length)
      return cssObject();
    else if (1 < contents.length)
      contents = container.wrapInner("<span>").children().addClass("autoFitTextWrapper");

    var min = options.min || 0.1;
    var max = options.max || 100;
    var iterations = options.iterations || 30;
    var percentHeight = (options.percentHeight || 98) / 100;
    var overage = options.overage || 0;

    var maxW = container.innerWidth();
    var maxH = container.innerHeight();
    var targetH = (((100 + overage) / 100) * maxH);

//FIXME - using window height is wrong, e.g. for fixed-aspect views like the scoreboard,
//        it should be the aspect-corrected height; maybe referenceFontSize should be mandatory param?
    var referenceFontSize = options.referenceFontSize || $(window).height();
    var minFontSize = ((min * referenceFontSize) / 100);
    var maxFontSize = ((max * referenceFontSize) / 100);

    var overSize = function() {
      return (contents.outerWidth(true) > maxW) || (contents.outerHeight(true) > targetH);
    };
    var atPercentHeight = function() {
      return (contents.outerHeight(true) > (percentHeight * targetH));
    };
    var checkSize = function() {
      if (overSize())
        return 1;
      else if (atPercentHeight())
        return 0;
      else
        return -1;
    };
    var lastFontSize = 0;
    var fontSizeChanged = function() {
      var changed = (lastFontSize != currentFontSize());
      lastFontSize = currentFontSize();
      return changed;
    };
    var updateSizes = function(minNewSize, maxNewSize) {
      minFontSize = Number(minNewSize);
      maxFontSize = Number(maxNewSize);
      updateFontSize((minFontSize + maxFontSize) / 2);
      return fontSizeChanged();
    };
    var updateFontSize = function(size) {
      container.css("fontSize", size+"px");
      updateTop();
    };
    var updateTop = function() {
      if (options.noVerticalAdjust)
        return;
      var vShift = ((maxH - contents.outerHeight(true)) / 2);
      if (options.useMarginBottom)
        container.css("margin-bottom", (-1*vShift)+"px");
      else
        container.css("margin-top", vShift+"px");
    };
    var currentFontSize = function() {
      return Number(container.css("fontSize").replace(/px$/, ""));
    };
    var shrinkToFit = function() {
      var reduceBy = 1;
      while (overSize()) {
        updateFontSize(currentFontSize() - reduceBy);
        if (!fontSizeChanged())
          reduceBy++;
      }
    };

    updateTop();
    if (currentFontSize() > maxFontSize)
      container.css("fontSize", maxFontSize);
    if (currentFontSize() < minFontSize)
      container.css("fontSize", minFontSize);

    while (0 < iterations--) {
      var check = checkSize();
      if ((-1 == check) && !updateSizes(currentFontSize(), maxFontSize))
        break;
      if (0 == check)
        break;
      if ((1 == check) && !updateSizes(minFontSize, currentFontSize()))
        break;
    }
    shrinkToFit();

    contents.filter(".autoFitTextWrapper").children().unwrap();
    return cssObject();
  },

  /* URL parameters */
  getParam: function(param) {
    var value = $.string(window.location.search).toQueryParams()[param];
    return ($.isArray(value) ? value[0] : value);
  },
  hasParam: function(param) {
    return $.string(window.location.search).toQueryParams().hasOwnProperty(param);
  },
  checkParam: function(param, value) { return value == _windowFunctions.getParam(param); },

  /* DOM element sorting
   *
   * This inserts the provided newChild directly under the parent,
   * using the specified comparator to insert at the correct spot.
   * If startIndex (which is 0-based) is specified, the newChild
   * will be inserted no earlier than that index (unless there
   * are not enough children to reach that index, in which case the
   * newChild will be appended).  The comparator should accept two
   * parameters (both actual DOM elements, not jQuery objects),
   * the first being the existing child to compare, and the second
   * being the newChild that is being inserted, and it should return 
   * true to indicate the newChild is "before" the existing child,
   * and false to indicate the newChild is "after" the existing child.
   * This insertion function does not attempt to maintain ordering
   * for "equal" children, and so the comparator does not provide
   * for indicating equality; it can return either true or false
   * for "equal" children, which will be sorted in the order they
   * are inserted.
   */
  appendSorted: function(parent, newChild, comparator, startIndex) {
    var child = null;
    startIndex = (startIndex || 0);
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

  /* Comparator functions
   *
   * These are convenience functions to use for various
   * types of sorting.  The "Alpha" sorting does an
   * alphabetical sorting using a "greater than" comparison.
   * The "AlphaNum" sorting puts all pure numbers first
   * (using the "Alpha" sort but with the params converted to
   * Numbers) sorted in numerical order, then the rest are given
   * to the "Alpha" sort.
   */
  appendAlphaSorted: function(parent, newChild, startIndex) {
    var comp = _windowFunctions.alphaCompareByNodeName;
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaSortedByAttr: function(parent, newChild, attrName, startIndex) {
    var comp = function(a, b) { return _windowFunctions.alphaCompareByAttr(attrName, a, b); };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaNumSortedByAttr: function(parent, newChild, attrName, startIndex) {
    var comp = function(a, b) { return _windowFunctions.numCompareByAttr(attrName, a, b); };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaSortedByProp: function(parent, newChild, propName, startIndex) {
    var comp = function(a, b) { return _windowFunctions.alphaCompareByProp(propName, a, b); };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaNumSortedByProp: function(parent, newChild, propName, startIndex) {
    var comp = function(a, b) { return _windowFunctions.numCompareByProp(propName, a, b); };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaSortedByData: function(parent, newChild, dataName, startIndex) {
    var comp = function(a, b) { return _windowFunctions.alphaCompareByData(dataName, a, b); };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaNumSortedByData: function(parent, newChild, dataName, startIndex) {
    var comp = function(a, b) { return _windowFunctions.numCompareByData(dataName, a, b); };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },

  alphaCompare: function(a, b) { return (a > b); },
  alphaCompareByNodeName: function(a, b) {
    return _windowFunctions.alphaCompare(a.nodeName, b.nodeName);
  },
  alphaCompareByAttr: function(n, a, b) {
    return _windowFunctions.alphaCompare($(a).attr(n), $(b).attr(n));
  },
  alphaCompareByProp: function(n, a, b) {
    return _windowFunctions.alphaCompare($(a).prop(n), $(b).prop(n));
  },
  alphaCompareByData: function(n, a, b) {
    return _windowFunctions.alphaCompare($(a).data(n), $(b).data(n));
  },
  numCompare: function(a, b) {
    var numA = Number(a), numB = Number(b);
    if (!isNaN(numA) && !isNaN(numB)) // both numbers
      return _windowFunctions.alphaCompare(numA, numB);
    else if (isNaN(numA) && isNaN(numB)) // both non-numbers
      return _windowFunctions.alphaCompare(a, b);
    else  // b num, a non-num? a>b (true).  a num, b non-num? a<b (false).
      return (isNaN(numA));
  },
  numCompareByAttr: function(n, a, b) {
    return _windowFunctions.numCompare($(a).attr(n), $(b).attr(n));
  },
  numCompareByProp: function(n, a, b) {
    return _windowFunctions.numCompare($(a).prop(n), $(b).prop(n));
  },
  numCompareByData: function(n, a, b) {
    return _windowFunctions.numCompare($(a).data(n), $(b).data(n));
  }
};
