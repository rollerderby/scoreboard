
function getXMLHttpRequest() {
  try {
    return new XMLHttpRequest();
  } catch (e1) {
    try {
      return new ActiveXObject("Msxml2.XMLHTTP");
    } catch (e2) {
      try {
        return new ActiveXObject("Microsoft.XMLHTTP");
      } catch (e3) {
        alert("Your browser does not support AJAX!");
        return null;
      }
    }
  }
  return null;
}


function toDocument(xml) {
  return new DOMParser().parseFromString(xml,"text/xml");
}

function toString(doc) {
  try {
    return new XMLSerializer().serializeToString(doc);
  } catch (err) {
    try {
      return doc.xml;
    } catch (err2) {
      alert("Your browser does not support XML to text conversion!");
    }
  }
}

function createDocument(root) {
  var doc;
  if (document.implementation && document.implementation.createDocument) {
    doc = document.implementation.createDocument("", "document", null);
  } else {
    doc = new ActiveXObject("Microsoft.XMLDOM");
    var text = "<document/>";
    doc.loadXML(text);
  }

  if (null != root)
    setElement(doc.documentElement, root);

  return doc;
}

function getElement(element, name) {
  return getElement(element, name, null);
}

function getElement(element, name, id) {
  var node = element.firstChild;

  if (null == id)
    id = "";
  else
    id = id.replace(/\'/g, '');

  while (null != node) {
    if (node.nodeType == Node.ELEMENT_NODE && node.nodeName == name) {
      var elementId = node.getAttribute('Id');
      if (null == elementId)
        elementId = "";
      if (id == elementId)
        return node;
    }
    node = node.nextSibling;
  }

  return null;
}

function setElement(element, name) {
  return setElement(element, name, null);
}

function setElement(element, name, id) {
  return setElement(element, name, id, null);
}

function setElement(element, name, id, content) {
  if (null == element)
    element = createDocument().documentElement;

  if (null != id)
    id = id.replace(/\'/g, '');

  var doc = element.ownerDocument;
  var e = getElement(element, name, id);

  if (null == e) {
    e = doc.createElement(name);
    if (null != id && "" != id)
      e.setAttribute("Id", id);

    // Add element in alphabetical order (by nodeName and id)
    var child = element.firstChild;
    while (child != null) {
      if (child.nodeType == Node.ELEMENT_NODE) {
        var childId = child.getAttribute("Id");
        if ((child.nodeName > e.nodeName) || ((child.nodeName == e.nodeName) && (childId > id))) {
          element.insertBefore(e, child);
          break;
        }
      }
      child = child.nextSibling;
    }
    if (child == null)
      element.appendChild(e);
  }

  if (null != content)
    setContent(e, content);

  return e;
}

function setContent(element, content) {
  if (null == element)
    return null;

  var node = element.firstChild;
  while (null != node) {
    if (node.nodeType == Node.TEXT_NODE)
      element.removeChild(node);
    node = node.nextSibling;
  }

  try { element.removeAttribute("empty"); } catch (err) { }

  if ("" == content)
    element.setAttribute("empty", "true");
  else if (null != content)
    element.appendChild(element.ownerDocument.createTextNode(content));

  return element;
}

function getContent(element) {
  if (null == element)
    return null;

  var node = element.firstChild;
  while (null != node) {
    if (node.nodeType == Node.TEXT_NODE)
      return node.nodeValue;
    node = node.nextSibling;
  }

  return (element.getAttribute("empty") == "true") ? "" : null;
}

var sendDocumentRefresh = true;
var sendDocumentImmediate = true;

function sendDocument(d) {
  if (sendDocumentImmediate)
    doSendDocument(d);
  else
    setTimeout(doSendDocument, 100, d);
}

function doSendDocument(d) {
  var request = getXMLHttpRequest();
  request.onreadystatechange=function() {
    if (this.readyState == 4 && sendDocumentRefresh)
      getScoreBoard(0, '');
  }

  request.open("POST", "/XmlScoreBoard/set", true);
  request.send(d);
}

