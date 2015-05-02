
$(initialize)

function initialize() {

	WS.Connect();

	WS.Register(['Custom.Overlay.Clock', 
		     'Custom.Overlay.Score', 
		     'Custom.Overlay.ShowJammers',
		     'Custom.Overlay.Panel'], function(k,v) { 

		$('[data-setting="' + k +'"]').each(function(i) {
			$t = $(this);
			if($t.hasClass('ToggleSwitch')) {
				$t.val(v).toggleClass('current', (v != null && v != ""));
			} else {
				if($t.prop('tagName') == 'SELECT') { 	
					$('option[value=' + v + ']', $t).attr('selected', 'selected');
				} else {
					if(!$t.hasClass('NoToggle')) $t.toggleClass('current', $t.val() == v);
				}
			}	
		});
	});

	WS.Register('Custom.Overlay.LowerThird', function(k,v) { $('input[data-setting="'+k+'"]').val(v); });

}

$('#Controls input').change(function() {
	t = $(this).attr('data-setting');
	v = $(this).val();
	if(t) WS.Set(t, v);
});


$('#Controls select').change(function() {
	$t = $(this);
	v = $t.val();

	// if we have an element target, update it
	target = $t.attr('data-target');
	field  = $t.attr('data-field');

	if(target) {
		// we have a target element to write to not data
		ov = $(target).attr(field);
		if(field) 
			$(target).attr(field, v);
		else 
			$(target).val(v).change();

		// flag it as changed
		if(ov != v) $(target).addClass('changed');
	}

	$( $t.attr('data-subforms') ).hide();
	forms = $( 'option[value=' + v + ']', $t ).attr('data-form');
	if(forms) {
		$(forms).show();
		$('input[type=text],textarea', $(forms)).eq(0).select().focus();
	}

});

$('#Controls button').click(function() { 
	$t = $(this);
	v = $t.val();
	$t.removeClass('changed'); 
	if( $t.hasClass('ToggleSwitch') ) {
		if( $t.hasClass('NoAuto') ) {
			nv = $t.attr('data-next');
			if(nv == v) { nv = null; }
			v = nv ? nv : null;
			if(v) $t.val(v).attr('data-next', v);
		} else {
			v = (v == 'On') ? null : 'On';
		}
	}
	WS.Set( $t.attr('data-setting'), v); 
});

$(function() {
    $(document).keyup(function(e) {
	var tag = e.target.tagName.toLowerCase();
	var c = String.fromCharCode(e.keyCode || e.charCode).toUpperCase();
	console.log(c);
	if (e.keyCode == 27) { $('body').focus(); e.preventDefault(); return false; }
        if ( tag != 'input' && tag != 'textarea') {
		$('[data-key="' + c + '"]').each(function() {
			$t = $(this);
			if($t.prop('tagName') == 'OPTION') { $t.attr('selected', 'selected').parent().change(); }
			if($t.prop('tagName') == 'BUTTON') { $t.click(); }
		});
		e.preventDefault();
	}
    });
});


