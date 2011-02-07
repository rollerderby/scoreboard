
var ScoreBoardNode_ScoreBoard = Class.create(ScoreBoardNode, {
  initialize: function($super, parent, name, id) {
    $super(parent, name, id);
  },

  startJam: function() {
    var e = this.toNewElement();
    e.setAttribute("call", "startJam");
    sendDocument(e.ownerDocument);
  },

  stopJam: function() {
    var e = this.toNewElement();
    e.setAttribute("call", "stopJam");
    sendDocument(e.ownerDocument);
  },

  timeout: function() {
    var e = this.toNewElement();
    e.setAttribute("call", "timeout");
    sendDocument(e.ownerDocument);
  },

  unStartJam: function() {
    var e = this.toNewElement();
    e.setAttribute("call", "unStartJam");
    sendDocument(e.ownerDocument);
  },

  unStopJam: function() {
    var e = this.toNewElement();
    e.setAttribute("call", "unStopJam");
    sendDocument(e.ownerDocument);
  },

  unTimeout: function() {
    var e = this.toNewElement();
    e.setAttribute("call", "unTimeout");
    sendDocument(e.ownerDocument);
  },

  updateImages: function() {
    var e = this.toNewElement();
    e.setAttribute("call", "updateImages");
    sendDocument(e.ownerDocument);
  },
});

var ScoreBoardNode_Team = Class.create(ScoreBoardNode, {
  initialize: function($super, parent, name, id) {
    $super(parent, name, id);
  },

  timeout: function() {
    var e = this.toNewElement();
    e.setAttribute("call", "timeout");
    sendDocument(e.ownerDocument);
  },

  addSkater: function(id, name, number) {
    var e = this.toNewElement();
    var s = setElement(e, "Skater", id);
    s.setAttribute("add", "true");
    if (null != name && "" != name)
      setElement(s, "Name", null, name);
    if (null != number && "" != number)
      setElement(s, "Number", null, number);
    sendDocument(e.ownerDocument);
  },

  removeSkater: function(id) {
    var e = this.toNewElement();
    var s = setElement(e, "Skater", id);
    s.setAttribute("remove", "true");
    sendDocument(e.ownerDocument);
  },
});

ScoreBoardNode_Time.addMethods({
  reset: function () {
    var e = this.toNewElement();
    e.setAttribute("reset", "true");
    sendDocument(e.ownerDocument);
  },
});

var ScoreBoardNode_Clock = Class.create(ScoreBoardNode, {
  initialize: function($super, parent, name, id) {
    $super(parent, name, id);
  },

  start: function () {
    this.Running.setContent("true");
  },

  stop: function () {
    this.Running.setContent("false");
  },
});

var ScoreBoardNodeHelper_text = Class.create(ScoreBoardNodeHelper, {
  initialize: function($super, node, element) {
    $super(node, element);

    this.element.observe("keyup", this.elementChange.bindAsEventListener(this));
    this.element.observe("mouseup", this.elementChange.bindAsEventListener(this));
  },

  elementChange: function(event) {
    var e = Event.element(event);
    if (e.hasClassName("updateimmediate"))
      this.setContent(e.value);
  },

  setElementContent: function(value) {
    this.element.value = value;
  },

  contentChange: function($super, event) {
    if (!this.element.hasClassName("isFocused"))
      $super(event);
  },
});
var ScoreBoardNodeHelper_textarea = Class.create(ScoreBoardNodeHelper_text, {
  initialize: function($super, node, element) {
    $super(node, element);
  },
});

var ScoreBoardNodeAttributesHelper_text = Class.create(ScoreBoardNodeAttributesHelper, {
  initialize: function($super, node, element) {
    $super(node, element);
  },

  setElementContent: function(attributes) {
    this.element.value = attributes.inject("", function(acc, n) { return acc+n+": "+this.node.getAttribute(n)+", "; }.bind(this));
  },

  attributeChange: function($super, event) {
    if (!this.element.hasClassName("isFocused"))
      $super(event);
  },
});

function GetImageFilter(id) {
  return function(e) { return (e.Type && (e.Type.getContent() == id)); };
}

function GetImageFilename(img) {
  return img.Filename.getContent();
}

var ScoreBoardNodeHelper_select = Class.create(ScoreBoardNodeHelper, {
  initialize: function($super, node, element) {
    $super(node, element);

    var optionPrefix = "AddDefaultOption:";
    var defaultOption = $w(this.element.className).find(function (n) { return n.startsWith(optionPrefix); });
    if (defaultOption)
      this.addDefaultOption(defaultOption.sub(optionPrefix, "").sub("_", " "));

    var targetNodePrefix = "TargetNode:";
    var targetNodeId = $w(this.element.className).find(function (n) { return n.startsWith(targetNodePrefix); });
    var targetNode = (targetNodeId ? getScoreBoardNodeFromId(targetNodeId.sub(targetNodePrefix, "")) : null);
    var targetNamePrefix = "TargetName:";
    var targetName = $w(this.element.className).find(function (n) { return n.startsWith(targetNamePrefix); });
    var targetFilterPrefix = "TargetFilter:";
    var targetFilter = $w(this.element.className).find(function (n) { return n.startsWith(targetFilterPrefix); });
    if (targetFilter)
      try { this.useTarget = eval(targetFilter.sub(targetFilterPrefix, "")); } catch (err) { }
    var targetValueFilterPrefix = "TargetValueFilter:";
    var targetValueFilter = $w(this.element.className).find(function (n) { return n.startsWith(targetValueFilterPrefix); });
    if (targetValueFilter)
      try { this.getTargetValue = eval(targetValueFilter.sub(targetValueFilterPrefix, "")); } catch (err) { }
    if (targetNode && targetName)
      this.setupOptions(targetNode, targetName.sub(targetNamePrefix, ""));

    this.element.setup = this.setupOptions;
  },

  addDefaultOption: function(text) {
    var o = Builder.node( "option" );
    o.text = text;
    o.value = "null";
    o.getStorage().set("isDefaultOption", true);
    this.element.insert(o);
    this.element.selectedIndex = 0;
  },

  setupOptions: function(base, name) {
    base.get(name, "").each(this.addOption.bind(this));
    this.updateSelectedIndex();
    base.observe(name+":add", function(event) { this.addOption(event.memo.node); }.bind(this) );
    base.observe(name+":remove", function(event) { this.removeOption(event.memo.node); }.bind(this) );
  },

  useTarget: function(target) {
    return true;
  },

  getTargetValue: function(target) {
    return target.getId();
  },

  addOption: function(target) {
    if (this.useTarget(target)) {
      var newOption = this.createOption(target);
      var beforeOption = $A(this.element.options).detect(function (o) { return (!(o.getStorage().get("isDefaultOption")) && (o.text.toLowerCase() > newOption.text.toLowerCase())); });
      if (Object.isUndefined(beforeOption))
        this.element.insert(newOption);
      else
        this.element.insertBefore(newOption, beforeOption);
      this.updateSelectedIndex();
    }
  },

  createOption: function(target) {
    var o = Builder.node("option");
    o.store("TargetId", target.getId());
    if (target.Name) {
      o.store("NameObserver", function(event) { this.text = event.memo.value; }.bind(o));
      o.text = target.Name.getContent();
      target.Name.observe("content:change", o.retrieve("NameObserver"));
    } else {
      o.text = target.getId();
    }
    o.value = this.getTargetValue(target);
    return o;
  },

  removeOption: function(target) {
    $A(this.element.options).each(function (option) {
      if (o.retrieve("TargetId") == target.getId()) {
        if (target.Name)
          target.Name.stopObserving(o.retrieve("NameObserver"));
        option.remove();
      }
    });
    this.updateSelectedIndex();
  },
 
  updateSelectedIndex: function() {
    if (this.element.hasClassName("ReturnToDefault")) {
      this.element.selectedIndex = 0;
    } else {
      var value = this.node.getContent();
      if (!value)
        value = "null";
      $A(this.element.options).each(function (option) {
        if (value == option.value)
          option.selected = true;
      });
    }
  },

  elementChange: function(event) {
    var e = Event.element(event);
    var value = e.options[e.selectedIndex].value;
    if ((e.selectedIndex == 0) && e.hasClassName("ReturnToDefault"))
      return;
    if (!e.hasClassName("updateimmediate"))
      return;
    if (value != this.node.getContent())
      this.setContent((value == "null" ? "" : value));
    if (e.hasClassName("ReturnToDefault"))
      this.element.selectedIndex = 0;
  },

  setElementContent: function(value) {
    if (this.element.hasClassName("ReturnToDefault"))
      return;
    if (!value)
      value = "null";
    if ((this.element.selectedIndex != -1) && (this.element.options[this.element.selectedIndex].value == value))
      return;
    $A(this.element.options).each(function (option) {
      if (option.value == value)
        option.selected = true;
    });
  },
});

var ScoreBoardNodeHelper_button = Class.create(ScoreBoardNodeHelper, {
  initialize: function($super, node, element) {
    $super(node, element);

    this.element.innerHTML = this.element.value = "Set" + this.element.value + this.element.innerHTML;

    var idPrefix = "AssociateId:";
    var idValue = $w(this.element.className).find(function (n) { return n.startsWith(idPrefix); });
    if (idValue) {
      this.associateId = idValue.sub(idPrefix, "");
      this.element.observe("click", this.buttonClick.bindAsEventListener(this));
    }
  },

  buttonClick: function(event) {
    var sourceElement = $(this.associateId);
    if (sourceElement) {
      if (sourceElement.match("input[type='text']"))
        this.setContent(sourceElement.value);
      else if (sourceElement.match("select"))
        this.setContent(sourceElement.options[sourceElement.selectedIndex].value);
    }
  },
});

var ScoreBoardNodeHelper_checkbox = Class.create(ScoreBoardNodeHelper, {
  initialize: function($super, node, element) {
    $super(node, element);

    this.element.checked = (this.node.getContent() == "true");
  },

  elementChange: function(event) {
    this.node.setContent(this.element.checked ? "true" : "false");
  },

  setElementContent: function(value) {
    this.element.checked = (value == "true");
  },
});

var ScoreBoardNodeAttributesHelper_button = Class.create(ScoreBoardNodeAttributesHelper, {
  initialize: function($super, node, element) {
    $super(node, element);

    this.element.innerHTML = this.element.value = "Set";

    var idPrefix = "AssociateId:";
    var idValue = $w(this.element.className).find(function (n) { return n.startsWith(idPrefix); });
    if (idValue) {
      this.associateId = idValue.sub(idPrefix, "");
      this.element.observe("click", this.buttonClick.bindAsEventListener(this));
    }
  },

  buttonClick: function(event) {
    var sourceElement = $(this.associateId);
    if (sourceElement) {
      if (sourceElement.match("input[type='text']")) {
        var h = $H();
        sourceElement.value.split(",").each(function(n) {
          try { h.set(n.split(":")[0].strip(), n.split(":")[1].strip()); } catch(err) { }
        });
        this.setAttributes(h);
      }
    }
  },
});

//FIXME - merge "viewer" and "controller" into single script; little use in splitting them
ScoreBoardNode_Time.addMethods({
  addHelper: function(element) {
    var key = element.nodeName.toLowerCase();
    if (key == "input")
      key = element.type;
    if (window["ScoreBoardNodeHelper_Time_"+key])
      new window["ScoreBoardNodeHelper_Time_"+key](this, element);
    else if (window["ScoreBoardNodeHelper_"+key])
      new window["ScoreBoardNodeHelper_"+key](this, element);
    else
      new ScoreBoardNodeHelper(this, element);
    return element;
  },
});

var ScoreBoardNodeHelper_Time_button = Class.create(ScoreBoardNodeHelper_button, {
  initialize: function($super, node, element) {
    $super(node, element);
  },

  setContent: function($super, value) {
    $super(minSecToMs(value));
  },
});

var ScoreBoardNodeHelper_Time_text = Class.create(ScoreBoardNodeHelper_text, {
  initialize: function($super, node, element) {
    $super(node, element);
  },

  setElementContent: function($super, value) {
    $super(msToMinSec(value));
  },

  setContent: function($super, value) {
    $super(minSecToMs(value));
  },
});
