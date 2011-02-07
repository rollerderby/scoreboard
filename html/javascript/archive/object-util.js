
/**
 *
 */

function getScoreBoardNodeFromId(id) {
  var e = null;
  try {
    id.scan(/[^\.\(]+\([^\)]+\)|[^\.]+/, function(a) {
      var n = a[0];
      if (e == null) {
        if (Object.isUndefined(window[n]))
          e = createScoreBoardNode(null, n);
        else
          e = window[n];
      } else {
        var v = n.replace(/\)/, '').split('(');
        e = e.get(v[0], v[1]);
      }
    });
  } catch (err) {
    return null;
  }
  return e;
}

function createScoreBoardNodes(element) {
  if (element == null) {
    createScoreBoardNodes(document.body);
    return;
  }

  var key = null;
  if ((key = element.getAttribute("sbelement")) || (key = element.getAttribute("sbhelper")))
    getScoreBoardNodeFromId(key);

  $(element).childElements().each(createScoreBoardNodes);
}

function createScoreBoardNode(parent, name, id) {
  if (window["ScoreBoardNode_"+name])
    return new window["ScoreBoardNode_"+name](parent, name, id);
  else
    return new ScoreBoardNode(parent, name, id);
}

var ScoreBoardNode = Class.create({
  initialize: function(parent, name, id) {
    if (id)
      id = id.replace(/\'/g, '');

    this.parentNode = parent;
    this.childNodes = new ScoreBoardNodeHash(this);

    this.name = name;

    this.fullname = (parent?parent.fullname+".":"") + name;
    if (id)
      this.fullname += "("+id+")";

    if (!parent)
      window[name] = this;

    this.xmlElement = setElement(parent?parent.xmlElement:null, name, id);
    document.body.appendChild(this.eventElement = Builder.node("div"));
    this.htmlElements = $A();
    $$("[sbelement='"+this.fullname+"']").each(this.addElement.bind(this));
    $$("[sbhelper='"+this.fullname+"']").each(this.addHelper.bind(this));

    this.observe("content:change", function(event) {
      this.htmlElements.each(function (e) {
        this._updateElementContent(e, event.memo.value);
      }.bind(this));
    });
    this.observe("attribute:change", function(event) {
      this.htmlElements.each(function (e) {
        this._updateElementAttribute(e, event.memo.attribute, event.memo.value);
      }.bind(this));
    });
  },

  createHelper: function(type, attrs, children) {
    return this.addHelper(this._createElement(type, attrs, children));
  },

  addHelper: function(element) {
    var key = element.nodeName.toLowerCase();
    if (key == "input")
      key = element.type;
    if (window["ScoreBoardNodeHelper_"+key])
      new window["ScoreBoardNodeHelper_"+key](this, element);
    else
      new ScoreBoardNodeHelper(this, element);
    return element;
  },

  createAttributesHelper: function(type, attrs, children) {
    return this.addAttributesHelper(this._createElement(type, attrs, children));
  },

  addAttributesHelper: function(element) {
    var key = element.nodeName.toLowerCase();
    if (key == "input")
      key = element.type;
    if (window["ScoreBoardNodeAttributesHelper_"+key])
      new window["ScoreBoardNodeAttributesHelper_"+key](this, element);
    else
      new ScoreBoardNodeAttributesHelper(this, element);
    return element;
  },

  get: function(name, id) {
    return this.childNodes.get(name, id);
  },

  _contains: function(name, id) {
    return this.childNodes._contains(name, id);
  },

  _create: function(name, id) {
    var n = this.childNodes.get(name, id, true);
    n.isCreated = true;
    return n;
  },

  _isCreated: function(name, id) {
    return (this._contains(name, id) && this.get(name, id).isCreated);
  },

  _fireAddEvent: function(name, id) {
    if (id)
      this.eventElement.fire(name+":add", { node: this.get(name, id) });
    this.eventElement.fire("node:add", { node: this.get(name, id) });
  },

  _fireRemoveEvent: function(name, id) {
    if (id)
      this.eventElement.fire(name+":remove", { node: this.get(name, id) });
    this.eventElement.fire("node:remove", { node: this.get(name, id) });
  },

  remove: function() {
    var e = this.toNewElement();
    e.setAttribute("remove", "true");
    sendDocument(e.ownerDocument);
  },

  _remove: function() {
    if (this.parentNode)
      this.parentNode._fireRemoveEvent(this.name, this.getId());
    this.childNodes.keys().each(function(key) { this[key]._remove(); }.bind(this));
    this.childNodes.names.each(function(name) { this[name].invoke("_remove"); }.bind(this));
    if (this.parentNode) {
      this.eventElement.stopObserving();
      this.parentNode._removeChild(this.name, this.getId());
    }
  },

  _removeChild: function(name, id) {
    return this.childNodes.unset(name, id);
  },

  getChildNodes: function() {
    return this.childNodes.getAll();
  },

  setContent: function(value, change) {
    var e = setContent(this.toNewElement(), value);
    if (change)
      e.setAttribute("change", "true");
    sendDocument(e.ownerDocument);
    return this;
  },

  _setContent: function(value) {
    setContent(this.xmlElement, value);
    this.eventElement.fire("content:change", { node: this, value: value });
  },

  getContent: function() {
    return getContent(this.xmlElement);
  },

  setAttribute: function(attr, value) {
    var e = this.toNewElement();
    e.setAttribute(attr, value);
    sendDocument(e.ownerDocument);
    return this;
  },

  _setAttribute: function(attr, value) {
    //FIXME - damn, should not have used Id attribute!  Need to ignore it here as it overwrites the HTML id attribute.
    if (attr == "Id")
      return;
    this.xmlElement.setAttribute(attr, value);
    this.eventElement.fire("attribute:change", { node: this, attribute: attr, value: value });
  },

  getAttribute: function(attr) {
    return this.xmlElement.getAttribute(attr);
  },

  getAttributes: function() {
    return $A(this.xmlElement.attributes).pluck("name");
  },

  _createElement: function(type, attrs, children) {
    return Builder.node(type, attrs, children);
  },

  createElement: function(type, attrs, children) {
    return this.addElement(this._createElement(type, attrs, children));
  },

  _updateElement: function(element) {
    this._updateElementContent(element, this.getContent());
    this._updateElementAttributes(element);
    return element;
  },

  _updateElementContent: function(element, value) {
    if (element.contentfilters)
      value = element.contentfilters.inject(value, function(val, filter) { try { return filter(val); } catch (err) { return val; } });
    if (element.match("img"))
      element.src = value;
    else if (element.match("input[type='text']") && (element.value != value))
      element.value = value;
    else if (element.match("a") && (element.innerHTML != value))
      element.innerHTML = value;
    else if (element.match("video")) {
      $A(element.childNodes).each(function (e) { if (e.match("source")) e.remove(); });
      if (null == value || "" == value) {
        element.style.opacity = 0;
      } else if (value.endsWith("mp4")) {
        var ogv = value.replace(new RegExp("mp4$"), "ogv");
        var i = ogv.lastIndexOf("/");
        if (-1 < i)
          ogv = ogv.substr(0, i+1) + "ogv/" + ogv.substr(i+1);
        delete element.src;
        element.insert(Builder.node("source", { type: "video/mp4", src: value }));
        element.insert(Builder.node("source", { type: "video/ogg", src: ogv }));
        element.style.opacity = 1;
      } else {
        element.insert(Builder.node("source", { src: value }));
        element.style.opacity = 1;
      }
      try { element.load(); } catch (err) { element.style.opacity = 0; /* Browser doesn't support <video> */ }
    }
    return element;
  },

  _updateElementAttribute: function(element, attribute, value) {
    if (element.attributefilters)
      value = element.attributefilters.inject(value, function(attr, val, filter) { try { return filter(attr, val); } catch (err) { return val; } }.curry(attribute));
    //FIXME - Prototype bug, writeAttribute does not handle multi-level attributes like styles (e.g. style.display)
    //element.writeAttribute(attribute, value);
    try { eval("element."+attribute+" = value;"); } catch (err) { }
    return element;
  },

  _updateElementAttributes: function(element) {
    this.getAttributes().each(function (name) {
      this._updateElementAttribute(element, name, this.getAttribute(name));
    }.bind(this));
    return element;
  },

  addElement: function(element) {
    var contentfilters = $w(element.className).grep("contentfilter:").map(function(n) { return eval(n.sub("contentfilter:", "")); }).partition()[0];
    if (contentfilters.size())
      element.contentfilters = contentfilters;
    var attributefilters = $w(element.className).grep("attributefilter:").map(function(n) { return eval(n.sub("attributefilter:", "")); }).partition()[0];
    if (attributefilters.size())
      element.attributefilters = attributefilters;
    if (!this.htmlElements.include(element))
      this.htmlElements.push(element);
    this._updateElement(element);
    return element;
  },

  removeElement: function(element) {
    this.htmlElements = this.htmlElements.without(element);
    return element;
  },

  getId: function() {
    return this.getAttribute("Id");
  },

  toNewElement: function (d) {
    if (this.parentNode)
      return setElement(this.parentNode.toNewElement(d), this.name, this.getId());

    return setElement(d?d.documentElement:null, this.name, this.getId());
  },

  observe: function (name, handler) {
    return this.eventElement.observe(name, this.eventElement.getStorage().set(handler, handler.bindAsEventListener(this)));
  },

  stopObserving: function (name, handler) {
    return this.eventElement.stopObserving(name, this.eventElement.getStorage().unset(handler));
  },
});

var ScoreBoardNodeHash = Class.create(Hash, {
  initialize: function($super, node) {
    $super();
    this.node = node;
    this.names = $A();
  },

  getAll: function() {
    return this.names.inject(this.values(), function(acc, n) { return acc.concat(this.node[n].ids.values()); }.bind(this));
  },

  _contains: function(name, id) {
    if (null != id)
      return (Object.isFunction(this.node[name]) && this.node[name].ids.keys().include(id)); // FIXME - previously declaring as an object will break accessing as an array; this overwrites the old object which clearly isn't desirable...
    else
      return (this.keys().include(name));
  },

//FIXME - this object vs. hash stuff is too complicated from an interface perspective, should be simpler.
  get: function($super, name, id, noSendDoc) {
    if (null != id) {
      if (!Object.isFunction(this.node[name])) {
        this.node[name] = function(name, id, noSend) {
          if (!this[name].ids.keys().include(id)) {
            var o = this[name].ids.set(id, createScoreBoardNode(this, name, id));
            if (!noSend)
              sendDocument(o.toNewElement().ownerDocument);
          }
          return this[name].ids.get(id);
        }.bind(this.node, name);
        this.node[name].ids = new Hash();
        Object.extend(this.node[name], Enumerable);
        Object.extend(this.node[name], { _each: function(iterator) { this.ids.values().sortBy(function (s) { return s.getId().toLowerCase(); }).each(iterator); } });
        this.names.push(name);
      }
      return ("" != id ? this.node[name](id, noSendDoc) : this.node[name]);
    } else {
      if (!this._contains(name, id)) {
        var o = this.set(name, createScoreBoardNode(this.node, name));
        if (!noSendDoc)
          sendDocument(o.toNewElement().ownerDocument);
      }
      return $super(name);
    }
  },

  set: function($super, key, value) {
    this.node[key] = value;
    return $super(key, value);
  },

  unset: function($super, name, id) {
    var o = null;
    try {
      if (id) {
        this.node.xmlElement.removeChild(this.node[name].ids.get(id).xmlElement);
        document.body.removeChild(this.node[name].ids.get(id).eventElement);
        o = this.node[name].ids.unset(id);
      } else if (this.keys().include(name)) {
        this.node.xmlElement.removeChild(this.get(name).xmlElement);
        document.body.removeChild(this.get(name).eventElement);
        o = $super(name);
        delete this.node[name];
      }
    } catch (err) {
    }
    return o;
  },
});

var ScoreBoardNodeHelper = Class.create({
  initialize: function(node, element) {
    this.node = node;
    this.element = element;

    this.setElementContent(this.node.getContent());

    this.element.observe("focus", function (event) { Event.element(event).addClassName("isFocused"); });
    this.element.observe("blur", function (event) { Event.element(event).removeClassName("isFocused"); });
    this.element.observe("change", this.elementChange.bindAsEventListener(this));

    this.node.observe("content:change", this.contentChange.bindAsEventListener(this));
  },

  elementChange: function(event) {
  },

  contentChange: function(event) {
    this.setElementContent(event.memo.value);
  },

  setElementContent: function(value) {
  },

  setContent: function(value) {
    this.node.setContent(value);
  },
});

var ScoreBoardNodeAttributesHelper = Class.create({
  initialize: function(node, element) {
    this.node = node;
    this.element = element;

    this.setElementContent(this.node.getAttributes());

    this.element.observe("focus", function (event) { Event.element(event).addClassName("isFocused"); });
    this.element.observe("blur", function (event) { Event.element(event).removeClassName("isFocused"); });
    this.element.observe("change", this.elementChange.bindAsEventListener(this));

    this.node.observe("attribute:change", this.attributeChange.bindAsEventListener(this));
  },

  elementChange: function(event) {
  },

  attributeChange: function(event) {
    this.setElementContent(this.node.getAttributes());
  },

  setElementContent: function(attributes) {
  },

  setAttributes: function(attributes) {
    attributes.each(function(attr) {
      this.node.setAttribute(attr.key, (attr.value==null?"":attr.value.strip()));
    }.bind(this));
  },
});

