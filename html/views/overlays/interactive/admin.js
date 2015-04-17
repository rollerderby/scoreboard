
$(initialize);

function initialize() {
	WS.Connect();
	WS.AutoRegister();

	WS.Register(['Custom.Overlay.Clock', 'Custom.Overlay.Score', 'Custom.Overlay.Panel'], function(k,v) { 
		console.log('Change', k,v); 
		b = $('button[data-setting="' + k +'"]');
		b.removeClass('current'); 
		if(b.hasClass('Toggle')) {
			on = b.attr('data-onstate');
			b.val(v);
			if(v == on) { b.addClass('current'); }
		} else {
			n = $('button[value=' + v + ']');
			n.addClass('current'); 
		}
		
	});

	WS.Register('Custom.Overlay.LowerThird', function(k,v) { $('input[data-setting="'+k+'"]').val(v); });
}

$('button').click(function() { 
	c = $(this).attr('data-setting'); 
	v = $(this).val(); 
	ov = v;
	console.log(c,'was',v);
	if( $(this).hasClass('Toggle') ) {
		on = $(this).attr('data-onstate');
	 	off = $(this).attr('data-offstate');
		if(ov == on) { v = off; } else { v = on; }
		$(this).val(v);
		console.log('checking myself with on',on,'off',off,'but had',ov,'new value',v);
	}
	console.log(c,'=',v);
	WS.Set(c, v); 
});

$('input').change(function() { console.log('change'); c = $(this).attr('data-setting'); v = $(this).val(); WS.Set(c, v); console.log(c,v); });


