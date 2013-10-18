
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */


//FIXME - need to allow setting up groups, maybe custom class to indicate grouping, so per-tab keycontrols are possible
_crgKeyControls = {
	/* This selector should be used to match key control buttons. */
	keySelector: ":button.KeyControl,label.KeyControl",

	/* Setup all key control buttons.
	 * This finds all button-type elements with the class KeyControl
	 * and calls setupKeyControl, using the given controlParent.
	 */
	setupKeyControls: function(controlParent) {
		_crgKeyControls.setupKeyControl($(_crgKeyControls.keySelector), controlParent);
	},
	/* Destroy all key control buttons.
	 * This finds all button-type elements with the class KeyControl
	 * and calls destroyKeyControl.
	 */
	destroyKeyControls: function() {
		_crgKeyControls.destroyKeyControl($(_crgKeyControls.keySelector));
	},

	/* Setup the button for key control.
	 * This sets up the given button as a key control.
	 * The button must have a (unique) id, and
	 * the id must conform to the restrictions of
	 * the ScoreBoard id restrictions (e.g. no (, ), ', or ")
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
		_crgKeyControls.destroyKeyControl(button)
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
				key.$sbBindAndRun("sbchange", contentChange);
				button.data("_crgKeyControls:unbind", function() { key.unbind("sbchange", contentChange); });
				button.data("_crgKeyControls:Key", key);
				_crgKeyControls._start();
			});
		return button;
	},
	_hoverFunction: function(event) { $(this).toggleClass("hover", (event.type == "mouseenter")); },

	/* Destroy a key control button.
	 * This undoes the key control setup. If destroyButton
	 * is true, is destroys the jQuery-UI button.
	 * It returns the button element.
	 * Note this does not remove the KeyControl class from the button.
	 */
	destroyKeyControl: function(button) {
		button.each(function() { try { $(this).data("_crgKeyControls:unbind")(); } catch(err) { } });
		button.removeData("_crgKeyControls:unbind").removeData("_crgKeyControls:Key");
		button.unbind("mouseenter mouseleave", _crgKeyControls._hoverFunction);
		button.find("span.Indicator").remove();
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
		$(_crgKeyControls.keySelector).toggleClass("Editing", edit);
	},

	addCondition: function(condition) {
		_crgKeyControls._conditions.push(condition);
	},
	_conditions: [ function() { return !$("div.MultipleKeyAssignDialog").length; } ],

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
	_validKey: function(keycode) {
		/* For reference see http://en.wikipedia.org/wiki/List_of_Unicode_characters */
		if (keycode < 0x20) // Low control chars
			return false;
		if (0x7e < keycode && keycode < 0xa1) // Higher control chars
			return false;
		return true;
	},
	_existingKeyLast: undefined,
	_existingKeyCount: 0,
	_keyControlPress: function(event) {
		if (!_crgKeyControls._checkConditions())
			return;
		if (!_crgKeyControls._validKey(event.which))
			return;

		var key = String.fromCharCode(event.which);

		var controls = $(_crgKeyControls.keySelector);
		var active = controls.filter(":not(.Editing):visible");
		var editing = controls.filter(".Editing");

		// Perform the corresponding button's action
		var target = active.has("span.Key[data-keycontrol='"+event.which+"']").click();
		// FIXME - workaround seemingly broken jQuery-UI
		// which does not fire change event for radio buttons when click() is called on their label...
		if (target.is("label"))
			target.filter("label").each(function() { $("#"+$(this).attr("for")).change(); });

		// Update the hovered button if in edit mode
		var editingTarget = editing.filter(".hover");
		if (editingTarget.length) {
			var existingControl = editing.filter(":not(.hover)").has("span.Key[data-keycontrol='"+event.which+"']");
			if (existingControl.length) {
				if (_crgKeyControls._existingKeyLast != key)
					_crgKeyControls._existingKeyCount = 1;
				else
					_crgKeyControls._existingKeyCount++;
				_crgKeyControls._existingKeyLast = key;
				if (_crgKeyControls._existingKeyCount > 2) {
					_crgKeyControls._showMultipleKeyAssignDialog(existingControl, editingTarget, key);
					_crgKeyControls._existingKeyCount = 0;
				} else {
					existingControl.effect("highlight", { color: "#f00" }, 300);
				}
			} else {
				_crgKeyControls._setKey(editingTarget, key);
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
			_crgKeyControls._clearKey($(_crgKeyControls.keySelector).filter(".Editing.hover"));
		}
	},
	_clearKey: function(targets) { _crgKeyControls._setKey(targets, ""); },
	_setKey: function(targets, key) {
		targets.each(function() {
			var sbKey = $(this).data("_crgKeyControls:Key");
			if (sbKey)
				sbKey.$sbSet(key);
		});
	},
	_showMultipleKeyAssignDialog: function(existing, target, key) {
		var div = $("<div>").addClass("MultipleKeyAssignDialog");
		var n = existing.length;
		var s = (n == 1 ? "" : "s");
		$("<p>").text("The key '"+key+"' is assigned to "+n+" other control"+s+", what do you want to do?")
			.appendTo(div);
		$("<hr>").appendTo(div);
		$("<button>")
			.text("Assign '"+key+"' to only this control, remove from the other "+n+" control"+s)
			.button()
			.appendTo(div)
			.click(function() {
				_crgKeyControls._clearKey(existing);
				_crgKeyControls._setKey(target, key);
				div.dialog("close");
			});
		$("<br>").appendTo(div);
		$("<button>")
			.text("Assign '"+key+"' to this control and the other "+n+" control"+s)
			.button()
			.appendTo(div)
			.click(function() {
				_crgKeyControls._setKey(target, key);
				div.dialog("close");
			});
		div.dialog({
			modal: true,
			width: "700px",
			buttons: { Cancel: function() { div.dialog("close"); } },
			close: function() { div.dialog("destroy").remove(); }
		});
	}
};
