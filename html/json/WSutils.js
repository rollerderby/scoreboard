
function toggleButton(key, trueText, falseText) {
	var button = $("<label/><input type='checkbox'/>").addClass("ui-button-small");
	var id = newUUID();
	button.first().attr("for", id);
	var input = button.last().attr("id", id).button();
	input.change(function(e) { WS.Set(key, input.prop("checked")); });
	input.button("option", "label", falseText);
	WS.Register(key, function(k, v) {
		input.button("option", "label", isTrue(v)?trueText:falseText)
			.prop("checked", isTrue(v))
			.button("refresh");
	});
	return button;
}

function mediaSelect(key, format, type, humanName) {
	var select = $("<select>").append($("<option value=''>No " + humanName + "</option>"));
	WS.Register("ScoreBoard.Media.Format(" + format + ").Type(" + type + ").File(*).Name", function(k, v) {
		select.children("[value='" + escape("/" + format + "/" + type + "/" + k.File) + "']").remove();
		if (v != null) {
			var option = $("<option>").attr("name", v).val(escape("/" + format + "/" + type + "/" + k.File)).text(v);
			_windowFunctions.appendAlphaSortedByAttr(select, option, "name", 1);
			select.val(WS.state[key]);
		}
	});
	WSControl(key, select);
	return select;
}


function WSActiveButton(key, button) {
	button.click(function() { WS.Set(key, !button.hasClass("Active"));} );
	WS.Register(key, function (k, v) {
		button.toggleClass("Active", isTrue(v));
	});
	return button;
}

function WSControl(key, element) {
	element.change(function() { WS.Set(key, element.val()); });
	WS.Register(key, function(k, v) { element.val(v); });
	return element;
}
