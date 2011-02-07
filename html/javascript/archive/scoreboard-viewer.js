

/*
 * ScoreBoard object reference
 *
 * TODO: add here...or see XML file for structure
 */

var ScoreBoardNode_Time = Class.create(ScoreBoardNode, {
  initialize: function($super, parent, name, id) {
    $super(parent, name, id);
  },

  addElement: function($super, element) {
    if (!$w(element.className).grep("contentfilter:").size())
      element.addClassName("contentfilter:msToMinSec");
    return $super(element);
  },
});

var ScoreBoardNode_MinimumTime = ScoreBoardNode_Time;
var ScoreBoardNode_MaximumTime = ScoreBoardNode_Time;


// FIXME - change this ajax stuff to use prototype's Ajax functions

var scoreBoardKey = "register";
var scoreBoardRegisterSource = "";
var scoreBoardRequestDate = new Date();
var scoreBoardResponseDate = new Date();
var scoreBoardTurnaroundMs = 0;

function getExceptionInfo(err) {
  var infoText = "";
  for (var n in err) {
    try {
      infoText += n + ": " + err[n] + "\n";
    } catch (err2) {
    }
  }
  return infoText;
}

function getScoreBoard(rate, callback) {
  var xmlHttp = getXMLHttpRequest();
  xmlHttp.rate = rate;
  xmlHttp.callback = callback;
  xmlHttp.onreadystatechange = function () {
    if (this.readyState != 4)
      return;

    try {
      scoreBoardResponseDate = this.responseDate = new Date();
      scoreBoardTurnaroundMs = this.responseDate.getTime() - this.requestDate.getTime();
    } catch (err) {
      alert("error with request/response times : " + err);
    }

    try {
      if (this.status == 200)
        parseResponseXml(this.responseXML);
      else if (this.status == 404) /* Change to behavior: if our key isn't found, reload the page instead of re-registering; the core could have restarted. */
        window.location.reload();

      if (("" != this.callback) && (typeof(ScoreBoard) != 'undefined')) {
        try {
          eval(this.callback);
        } catch (err) {
          alert("Error when calling '"+this.callback+"'\n"+getExceptionInfo(err)+err);
        }
        this.callback = "";
      }
    } catch (err) {
      if (!confirm("Error parsing XML\n"+getExceptionInfo(err)+err+"\nContinue?"))
        this.rate = 0;
    } finally {
      if (0 < this.rate)
       setTimeout("getScoreBoard("+this.rate+", '"+this.callback+"');", this.rate);
    }
  }

  if (scoreBoardKey == "register") {
    xmlHttp.open("GET", "/XmlScoreBoard/register?source="+scoreBoardRegisterSource, true);
  } else {
    xmlHttp.open("GET", "/XmlScoreBoard/get?key="+scoreBoardKey, true);
  }
  scoreBoardRequestDate = xmlHttp.requestDate = new Date();
  xmlHttp.send(null);
}

function parseResponseXml(doc) {
  if (doc == null || doc.documentElement == null)
    return;
  var response = doc.documentElement.firstChild;
  while (null != response) {
    if (response.nodeType == Node.ELEMENT_NODE) {
      if (response.nodeName == "Register") {
        scoreBoardKey = "register";
      } else if (response.nodeName == "Reload") {
        window.location.reload();
      } else if (response.nodeName == "Key") {
        scoreBoardKey = getContent(response);
      } else {
        handleElement(null, response);
      }
    }

    response = response.nextSibling;
  }
}

function handleElement(parent, e) {
  //FIXME - this design is broken, XML node names can conflict with javascript object names/references.  Work around for now, then dump this completely for new design.
  if (e.nodeName == "name")
    return;

  var o;
  var fireAdd = false;
  if (null == parent) {
    if (Object.isUndefined(o = window[e.nodeName]))
      o = createScoreBoardNode(null, e.nodeName, e.getAttribute("Id"));
  } else {
    fireAdd = !parent._isCreated(e.nodeName, e.getAttribute("Id"));
    o = parent._create(e.nodeName, e.getAttribute("Id"));
  }
  if (e.getAttribute("remove") == "true") {
    o._remove();
    return;
  }

  var attrs = e.attributes;
  for (var i=0; i<attrs.length; i++) {
    var node = attrs.item(i);
    /* Don't copy "add" and "remove" attributes */
    if (node.nodeName != "add" && node.nodeName != "remove")
      o._setAttribute(node.nodeName, node.nodeValue);
  }

  var content = getContent(e);
  if (null != content)
    o._setContent(content);

  var node = e.firstChild;
  while (null != node) {
    if (node.nodeType == Node.ELEMENT_NODE)
      handleElement(o, node);
    node = node.nextSibling;
  }

  if (fireAdd)
    parent._fireAddEvent(o.name, o.getId());
}

/* Convenience functions to convert ms to a normal clock */

function msToClock(time) {
  if (10000 > time)
    return msToSecMs(time);
  else
    return msToMinSec(time);
}

function msToClockNoZeroMin(time) {
  if (10000 > time)
    return msToSecMs(time);
  else
    return msToMinSecNoZeroMin(time);
}

function msToMinSecNoZeroMin(time) {
  if (60000 > time)
    return msToSec(time);
  else
    return msToMinSec(time);
}

function msToMinSec(time) {
  if (time === null)
    return null;
  // round milliseconds down
  var sec = Math.floor(time / 1000);
  var min = Math.floor(sec / 60);
  sec %= 60;
  if (10 > sec) sec = "0"+sec;
  return min+":"+sec;
}

function msToSec(time) {
  if (time === null)
    return null;
  // round milliseconds down
  var sec = Math.floor(time / 1000) % 60;
  return ""+sec;
}

function msToSecMs(time) {
  if (time === null)
    return null;
  // round milliseconds down
  var ms = Math.floor((time % 1000) / 100);
  var sec = Math.floor(time / 1000) % 60;
  return sec+"."+ms;
}

function minSecToMs(time) {
  if (time === null)
    return null;
  var t = time.split(":");
  if (t.length == 1 && (t[0].length > 0))
    return (t[0]*1000).toString();
  else if (t.length == 2)
    return ((new Number(t[0]*60) + new Number(t[1].substr(0,2)))*1000).toString();
  else
    return null;
}

/* User Interface: */

// Call this function when ready to start receiving scoreboard events.
// You can either call it directly from a <script type="text/javascript"> section,
// or you can add onload="startScoreBoard();" to your <body> tag.
// The rate is how often the server is polled for events.
// You should keep this as high (slow) as possible, as faster rates
// may significantly increase CPU and network usage.
function startScoreBoard(source, rate, callback) {
  scoreBoardRegisterSource = source;
  getScoreBoard(rate, "createScoreBoardNodes();"+callback);
}

function startScoreBoardWatchdog(callback) {
  var lastRequest = scoreBoardRequestDate.getTime();
  var lastResponse = scoreBoardResponseDate.getTime();
  var sinceRequest = new Date().getTime() - lastRequest;
  var sinceResponse = new Date().getTime() - lastResponse;
  try { callback(scoreBoardTurnaroundMs, sinceRequest, sinceResponse); } catch (err) { }

  setTimeout("startScoreBoardWatchdog("+callback+");", 100);
}
