
function getW() { return document.viewport.getWidth(); }
function getH() { return document.viewport.getHeight(); }

function getAspectW(num, den) { return ( getH() * num ) / den; }
function getAspectH(num, den) { return ( getW() * den ) / num; }

function get4x3W() { return getAspectW(4, 3); }
function get4x3H() { return getAspectH(4, 3); }

function get16x9W() { return getAspectW(16, 9); }
function get16x9H() { return getAspectH(16, 9); }


/* Note this is true even for params with no (i.e. null) value */
function hasParam(p) {
  var params = document.location.search.substring(1).split('&');

  for (var i = 0; i < params.length; i++) {
    var param = params[i].split('=');
    var name = param[0];

    if (p == name)
      return true;
  }

  return false;
}

function hasParamContent(p) {
  var content = getParam(p);
  return (content && (content != ""));
}

function getParam(p) {
  var params = document.location.search.substring(1).split('&');

  for (var i = 0; i < params.length; i++) {
    var param = params[i].split('=');
    var name = param[0];
    var value = param[1];

    if (p == name)
      return value;
  }

  return null;
}

function checkParam(p, v) {
  return v == getParam(p);
}


function SortedSelect() {
  var s = document.createElement("select");
  s.origAdd = s.add;
  s.add = function (op) {
    for (var i=0; i<this.length; i++) {
      if (op.text < this.options[i].text) {
        this.origAdd(op, this.options[i]);
        return;
      }
    }
    try {
      this.origAdd(op, null);
    } catch (err) {
      this.origAdd(op);
    }
  }
  return s;
}

function autoSetFontSize(parent, a, fontMax) {
  if (!$(a).getStorage().get("autoSetFontSize_onresize")) {
    var autoSizeOnResize = function() { autoSetFontSize(parent, a, fontMax); };
    $(a).getStorage().set("autoSetFontSize_onresize", autoSizeOnResize);
    Event.observe(window, "resize", autoSizeOnResize);
  }

  /* If there is no text to auto-size, there's nothing to do */
  if (a.innerHTML == "" || a.innerHTML == null)
    return;

  var parentW = parent.getWidth();
  var parentH = parent.getHeight();
  var maxW = parentW;
  var maxH = parentH;

  var txt = a.innerHTML;
  var span = Builder.node("span", [ txt ]);
  a.innerHTML = "";
  a.insert(span);

  var topSize = fontMax;
  var bottomSize = fontMax;
  var iterations = 10;

  var overSize = function() {
    var overH = (span.getHeight() > maxH);
    var overW = (span.getWidth() > maxW);
    return overH || overW;
  };

  a.style.margin = "0px 0px";
  a.style.borderWidth = "0px";
  a.style.padding = "0px 0px";
  a.style.fontSize = topSize+"%";
  while (overSize() && (bottomSize > 0)) {
    topSize = bottomSize;
    bottomSize -= 1;
    a.style.fontSize = bottomSize+"%";
  }

  if (topSize != bottomSize) {
    for (var i=0; i<iterations; i++) {
      var newSize = ((bottomSize + topSize) / 2);
      a.style.fontSize = newSize+"%";
      if (overSize())
        topSize = newSize;
      else if (0.98 < (span.getHeight()/maxH))
        break;
      else
        bottomSize = newSize;
    }
    if (overSize())
      a.style.fontSize = bottomSize+"%";
  }

/*
  var vGap = (Math.floor((((parentH - span.getHeight())/2)/parentH)*10000))/100;
  if (!isNaN(vGap))
    a.style.padding = (vGap*(parentH/parentW))+"% 0%";
*/
  var vGap = Math.floor((parentH - span.getHeight())/2);
  if (!isNaN(vGap))
    a.style.padding = vGap+"px 0px";

  span.remove();
  a.innerHTML = txt;
}


function createPreviewIFrame(url, w, h) {
  var div = document.createElement("div");
  var a = document.createElement("a");
  a.innerHTML = "<br>";

  div.topDiv = document.createElement("div");
  div.topDiv.div = div;
  div.bottomDiv = document.createElement("div");
  div.bottomDiv.div = div;

  div.iframe = document.createElement("iframe");
  div.urltext = document.createElement("input");
  div.submit = document.createElement("input");

  div.iframe.src = url;
  div.iframe.width = w;
  div.iframe.height = h;

  div.urltext.type = "text";
  div.urltext.size = 40;
  div.urltext.style.fontWeight = "normal";
  div.urltext.value = div.iframe.src;
  div.urltext.iframe = div.iframe;
  div.urltext.submit = div.submit;
  div.urltext.onkeyup = function (e) {
    var same = (this.iframe.src == this.value);
    if (this.submit.disabled != same)
      this.submit.disabled = same;

    if (!same && (CODE_RETURN == keyPressEventToKeyCode(e))) {
      this.iframe.src = this.value;
      this.submit.disabled = true;
    }
  }

  div.submit.type = "button";
  div.submit.value = "Go";
  div.submit.iframe = div.iframe;
  div.submit.urltext = div.urltext;
  div.submit.disabled = true;
  div.submit.onclick = function () {
    this.iframe.src = this.urltext.value;
    return false;
  }

  div.topDiv.appendChild(div.urltext);
  div.topDiv.appendChild(div.submit);
  div.appendChild(div.topDiv);
  div.appendChild(a);
  div.bottomDiv.appendChild(div.iframe);
  div.appendChild(div.bottomDiv);

  return div;
}


function keyPressEventToChar(e) {
  return String.fromCharCode(keyPressEventToCharCode(e));
}

function keyPressEventToCharCode(e) {
  //FIXME - IEBUG
  var evt = (e) ? e : window.event;
  return evt.charCode ? evt.charCode : evt.keyCode;
}

function keyPressEventToKeyCode(e) {
  //FIXME - IEBUG
  var evt = (e) ? e : window.event;
  return evt.keyCode;
}

function isKeyPressEventChar(e) {
  isAscii(keyPressEventToChar(e));
}

function isAscii(c) {
  return (c > ' ' && c < '~');
}

var CODE_F1 = 112;
var CODE_F2 = 113;
var CODE_F3 = 114;
var CODE_F4 = 115;
var CODE_F5 = 116;
var CODE_F6 = 117;
var CODE_F7 = 118;
var CODE_F8 = 119;
var CODE_F9 = 120;
var CODE_F10 = 121;
var CODE_F11 = 122;
var CODE_F12 = 123;

var CODE_UP = 38;
var CODE_DOWN = 40;
var CODE_LEFT = 37;
var CODE_RIGHT = 39;

var CODE_TAB = 9;
var CODE_ESC = 27;
var CODE_BACKSPACE = 8;
var CODE_RETURN = 13;

