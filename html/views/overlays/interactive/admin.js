
function str_sort(a, b){ return ( $(b).text() < $(a).text() ) ? 1 : -1; }
jQuery.fn.sortOptions = function sortOptions() { $("> option", this[0]).sort(str_sort).appendTo(this[0]); }

Skaters = new DataSet();
Skaters.AddTrigger('UPDATE', '*', { }, function(n,o,k) {

	if(this.Name && this.Team && this.Id) {
		att = { 'data-name': this.Name, 'data-team': this.Team, 'value': this.Skater };
		var $s = $('#Skaters option[value="' + this.Skater + '"]');
		if($s.length == 0) $s = $('<option>').attr(att).appendTo('#Skaters');
		$s.attr(att).text(this.Name);
		$('#Skaters').sortOptions();
	}
});
Skaters.AddTrigger('DELETE', '*', { }, function(n,o,k) { 
	$('#Skaters option[value="' + this.Skater + '"]').remove();
});

$(initialize)


function initialize() {

	WS.Connect();

	WS.Register(['Custom.Overlay.Clock', 
		     'Custom.Overlay.Score', 
		     'Custom.Overlay.ShowJammers',
		     'Custom.Overlay.Alert',
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

	var skaterRegEx = /^Game\.Team\((.+)\)\.Skater\((.+?)\)\.(.+)$/;
	WS.Register('Game.Team', function(k,v) {
		var m = k.match(skaterRegEx);
		if(m) {
			var key = m[3];
			if(!(key == 'Id' || key == 'Name' || key == 'Number' || key == 'Flags')) return;

			var d = {}; d[key] = v; d['Team'] = m[1];
			if(key == 'Id' && v == null) {
				Skaters.Delete({ Skater: m[2] });
			} else {
				Skaters.Upsert(d, { Skater: m[2] });	
			}
		}	
	});

	WS.Register('Custom.Overlay.LowerThird.Line', function(k,v) { $('input[data-setting="'+k+'"]').val(v); });
	WS.Register('Custom.Overlay.LowerThird.Style', function(k,v) { $('#LowerThirdStyle option[value="'+v+'"]').attr('selected', 'selected'); });

}

$('#Controls input').change(function() {
	t = $(this).attr('data-setting');
	v = $(this).val();
	if(t) WS.Set(t, v);
});


$('.SelectUpdator').change(function() {
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

$('select#Skaters').change(function(e) {
	$t = $(this);
	v = $t.val();
	team = $( 'option[value=' + v + ']', $t ).attr('data-team');
	name = $( 'option[value=' + v + ']', $t ).attr('data-name');
	tnam = WS.state['Game.Team(' + team + ').AlternateName(overlay)'];
	tnam = tnam ? tnam : WS.state['Game.Team(' + team + ').Name'];
	f = $( '#LowerThirdStyle option[value=ColourTeam' + team + ']').attr('selected', 'selected').change();
	$('input[data-setting="Custom.Overlay.LowerThird.Line1"]').val(name).change();
	$('input[data-setting="Custom.Overlay.LowerThird.Line2"]').val(tnam).change();
});

$('select#Keepers').change(function(e) {
	var $t = $(this);
	v = $t.val();

	$d = $('option[value="' + v + '"]', this);
	var line1 = $d.attr('data-line1');
	var line2 = $d.attr('data-line2');
	var style = $d.attr('data-style');

	$('#LowerThirdStyle option[value=' + style + ']').attr('selected', 'selected').change();
	$('input[data-setting="Custom.Overlay.LowerThird.Line1"]').val(line1).change();
	$('input[data-setting="Custom.Overlay.LowerThird.Line2"]').val(line2).change();
});

$('#KeeperAdd').click(function() {
	$('#LowerThirdStyle').change();
	var line1 = $('input[data-setting="Custom.Overlay.LowerThird.Line1"]').val();
	var line2 = $('input[data-setting="Custom.Overlay.LowerThird.Line2"]').val();
	var style = $('#LowerStyle').val();

	$('<option>').attr('data-line1', line1)
		     .attr('data-line2', line2)
		     .attr('data-style', style)
		     .attr('value', '_' + Math.random().toString(36).substr(2, 9) )
		     .text( line1 + '/' + line2 + ' (' + style + ')' ).appendTo('#Keepers');
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


