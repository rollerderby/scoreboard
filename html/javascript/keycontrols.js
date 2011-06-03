

//FIXME - need to allow setting up groups, maybe custom class to indicate grouping, so per-tab keycontrols are possible
_crgKeyControls = {
  /* Setup all key control buttons.
   * This finds all button-type elements with the class KeyControl
   * and calls setupKeyControl, using the given controlParent.
   */
  setupKeyControls: function(controlParent) {
    _crgKeyControls.setupKeyControl($(":button.KeyControl"), controlParent);
  },
  /* Destroy all key control buttons.
   * This finds all button-type elements with the class KeyControl
   * and calls destroyKeyControl.
   */
  destroyKeyControls: function(destroyButton) {
    _crgKeyControls.destroyKeyControl($(":button.KeyControl"), destroyButton);
  },

  /* Setup the button for key control.
   * This sets up the given button as a key control.
   * The button must have a (unique) id, and
   * the id must conform to the restrictions of
   * the ScoreBoard id restrictions (e.g. no (, ), ', or ")
   * If the button is not a jQuery-UI button(), it will be made one.
   * Once setup, any button presses corresponding to the
   * button's key control will cause a button click.
   * If this button has already been setup, it will first be
   * destroyed and then re-setup.
   *
   * CSS notes:
   * key control buttons have the class KeyControl
   * there are new span elements added, which are button>span>span
   * the new span elements (under the child span) have the class Indicator
   * the span.Indicator element that stores the control key has class Key
   * when there is a control key the button has class HasControlKey
   */
  setupKeyControl: function(button, controlParent) {
    _crgKeyControls.destroyKeyControl(button, true).button()
      .addClass("KeyControl")
      .bind("mouseenter mouseleave", _crgKeyControls._hoverFunction)
      .children("span")
      .append($("<span>").text(" [").addClass("Indicator"))
      .append($("<span>").addClass("Key Indicator"))
      .append($("<span>").text("]").addClass("Indicator"))
      .end()
      .each(function() {
        var button = $(this);
        var key = controlParent.$sb("KeyControl("+button.attr("id")+").Key");
        key.$sbElement(button.find("span.Key"));
        var contentChange = function(event,value) {
          button.toggleClass("HasControlKey", (value?true:false))
            .find("span.Key")
            .attr("data-keycontrol", String(value?value.charCodeAt(0):""));
        };
        key.$sbBindAndRun("content", contentChange);
        button.data("_crgKeyControls:unbind", function() { key.unbind("content", contentChange); });
        button.data("_crgKeyControls:Key", key);
        _crgKeyControls._start();
      });
    return button;
  },
  _hoverFunction: function() { $(this).toggleClass("hover"); },

  /* Destroy a key control button.
   * This undoes the key control setup. If destroyButton
   * is true, is destroys the jQuery-UI button.
   * It returns the button element.
   * Note this does not remove the KeyControl class from the button.
   */
  destroyKeyControl: function(button, destroyButton) {
    button.each(function() { try { $(this).data("_crgKeyControls:unbind")(); } catch(err) { } });
    button.removeData("_crgKeyControls:unbind").removeData("_crgKeyControls:Key");
    button.unbind("mouseenter mouseleave", _crgKeyControls._hoverFunction);
    button.find("span.Indicator").remove();
    if (destroyButton)
      try { button.button("destroy"); } catch(err) { }
    return button;
  },

  /* Change to key-edit mode.
   * If false, this changes all KeyControls to normal mode,
   * where keypresses will cause the corresponding KeyControl event.
   * If true, this changes all KeyControls to edit mode,
   * where any keypress while the mouse is hovering over a button
   * will cause that button to be assigned the pressed key,
   * unless that key is already assigned to another button, in which
   * case the currently assigned button will flash to indicate a
   * conflict (and this button key assignment will not be changed).
   * To clear the control key assignment, press the Backspace or Delete keys.
   *
   * CSS note: buttons in edit mode have the class "Editing".
   */
  editKeys: function(edit) {
    $(":button.KeyControl").toggleClass("Editing", edit);
  },

  addCondition: function(condition) {
    _crgKeyControls._conditions.push(condition);
  },
  _conditions: [ ],

  _start: function() {
    if (!_crgKeyControls._keyControlStarted) {
      $(document).keypress(_crgKeyControls._keyControlPress);
      $(document).keydown(_crgKeyControls._keyControlDown);
      _crgKeyControls._keyControlStarted = true;
    }
  },
  _keyControlStarted: false,

  _checkConditions: function() {
    var ok = true;
    $.each(_crgKeyControls._conditions, function() {
      if (ok && $.isFunction(this) && !this())
        ok = false;
    });
    return ok;
  },
  _keyControlPress: function(event) {
    if (!_crgKeyControls._checkConditions())
      return;

    var key = String.fromCharCode(event.which);

    // Perform the corresponding button's action
    $(":button.KeyControl:not(.Editing):visible").has("span.Key[data-keycontrol='"+event.which+"']").click();

    // Update the hovered button if in edit mode
    var editControls = $(":button.KeyControl.Editing");
    if (editControls.length) {
      var existingControl = editControls.filter(":not(.hover)").has("span.Key[data-keycontrol='"+event.which+"']");
      if (existingControl.length) {
        existingControl.effect("highlight", { color: "#f00" }, 300);
      } else {
        var sbKey = editControls.filter(".hover").data("_crgKeyControls:Key");
        if (sbKey)
          sbKey.$sbSet(key);
      }
    }
  },
  _keyControlDown: function(event) {
    if (!_crgKeyControls._checkConditions())
      return;

    // Clear control key from button being hovered over
    switch (event.which) {
    case 8: // Backspace
    case 46: // Delete
      var sbKey = $(":button.KeyControl.Editing.hover").data("_crgKeyControls:Key");
      if (sbKey)
        sbKey.$sbSet("");
    }
  }
};
