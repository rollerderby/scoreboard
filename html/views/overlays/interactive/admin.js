
$(initialize);

function initialize() {
	WS.Connect();
	WS.AutoRegister();

	WS.Register(['Custom.Overlay.Clock', 
		     'Custom.Overlay.Score', 
		     'Custom.Overlay.Panel'], function(k,v) { 

		b = $('button[data-setting="' + k +'"]');
		if(b.attr('data-type') == 'Toggle') {
			if(v == 'Off') { b.addClass('current'); } else { b.removeClass('current'); }
			b.val(v);
		} else {
			n = $('button[value=' + v + ']');
			n.addClass('current'); 
		}
		
	});

	WS.Register('Custom.Overlay.LowerThird', function(k,v) { $('input[data-setting="'+k+'"]').val(v); });


}

$('button[data-type=Toggle]').click(function() { 
	c = $(this).attr('data-setting'); 
	dataType = $(this).attr('data-type'); 
	v = $(this).val(); 
	if( dataType == 'Toggle' ) {
		if(v == 'Off') { v = 'On'; } else { v = 'Off'; }
	}
	WS.Set(c, v); 
});

$('input').change(function() { c = $(this).attr('data-setting'); v = $(this).val(); WS.Set(c, v);  });

