$(initialize);
/*
 Inside whiteboard screen for use with CRG Penalty Tracking funcitonality.
 Basically just the original PT javascript with a few tweaks.

*/
var penaltyEditor = null;
var period = null;
var jam = null;
var teamId = null;
var skaterId = null;
var penaltyId = null;
var fo_exp = null;

function setupMainDiv(div) {
  div.css({ position: "fixed" });
  div.bind("click", function() { _windowFunctions.fullscreenRequest(true); });
  div.bind("dblclick", function() { _windowFunctions.fullscreenRequest(false); });


  _crgUtils.bindAndRun($(window), "resize", function() {
    var aspect16x9 = _windowFunctions.get16x9Dimensions();
    div.css(aspect16x9).css("fontSize", aspect16x9.height);
  });
}


function initialize() {
	WS.Connect();
	WS.AutoRegister();
	setupMainDiv($("#mainDiv"));

	$.each([1, 2], function(idx, t) {
		WS.Register([ 'ScoreBoard.Team(' + t + ').Name' ]);
		WS.Register([ 'ScoreBoard.Team(' + t + ').AlternateName' ]);
		WS.Register([ 'ScoreBoard.Team(' + t + ').Color' ], function(k, v) { 
			$('.Team' + t + 'custColor').css('color', WS.state['ScoreBoard.Team(' + t + ').Color(overlay_fg)']); 
			$('.Team' + t + 'custColor').css('background-color', WS.state['ScoreBoard.Team(' + t + ').Color(overlay_bg)']); 
			$('#head' + t).css('background-color', WS.state['ScoreBoard.Team(' + t + ').Color(overlay_bg)']); } );
	});
        WS.Register( [ 'ScoreBoard.Team(1).Logo' ], function(k, v) { $('.Logo1').attr('src', v); } );
        WS.Register( [ 'ScoreBoard.Team(2).Logo' ], function(k, v) { $('.Logo2').attr('src', v); } );

	WS.Register( [ 'ScoreBoard.Team(1).Skater' ], function(k, v) { skaterUpdate(1, k, v); } ); 
	WS.Register( [ 'ScoreBoard.Team(2).Skater' ], function(k, v) { skaterUpdate(2, k, v); } ); 

}


var skaterIdRegex = /Skater\(([^\)]+)\)/;
var penaltyRegex = /Penalty\(([^\)]+)\)/;
function skaterUpdate(t, k, v) { 
	match = k.match(skaterIdRegex); 
	if (match == null || match.length == 0)
		return;
	var id = match[1]; // id = skater id
	var prefix = 'ScoreBoard.Team(' + t + ').Skater(' + id + ')';  //Example: prefix = ScoreBoard.Team('1').Skater('id')
	if (k == prefix + '.Number') {
		$('.Teamd' + t + ' .Skater[id=' + id + ']').remove();
		if (v == null) {
			return;
		}

		// New skater, or number has been updated.
		makeSkaterRows(t, id, v);
		// For penalty numbers one to nine, display the penalties for this skater
		for (var i = 1; i <= 9; i++) {
			displayPenalty(t, id, i); }
		displayPenalty(t, id, 'FO_EXP'); 
	} else {  
		// if this is NOT a new skater, see if it's a penalty 
		match = k.match(penaltyRegex);
		if (match == null || match.length == 0)
			return;
		// If this is a penalty, display it.
		var p = match[1];
		displayPenalty(t, id, p);
	}
}

function displayPenalty(t, s, p) { 
	// Given a team, a skater, and a penalty ID, display that penalty
	var penaltyBoxd = $('.Teamd' + t + ' .Skater.Penalty[id=' + s + '] .Box' + p);
	var totalBoxd = $('.Teamd' + t + ' .Skater.Penalty[id=' + s + '] .Total');

	var prefix = 'ScoreBoard.Team(' + t + ').Skater(' + s + ').Penalty(' + p + ')';
	var nprefix = 'ScoreBoard.Team(' + t + ').Skater(' + s + ')';
	code = WS.state[prefix + ".Code"];
	if (code != null) {
		penaltyBoxd.data("id", WS.state[prefix + ".Id"]);
		if (code == "EXP") { code = "EX"; }
		penaltyBoxd.text(code);
	} else {
		penaltyBoxd.data("id", null);
		penaltyBoxd.html("&nbsp;");
	}

	// Change row colors for skaters on 5 or more penalties, or who have been expelled.
	var cnt = 0; 
	var fo_exp = ($($('.Team' + t + ' .Skater.Penalty[id=' + s + '] .BoxFO_EXP')[0]).data("id") != null);
	$('.Team' + t + ' .Skater.Penalty[id=' + s + '] .Box').each(function(idx, elem) { cnt += ($(elem).data("id") != null ? 1 : 0); });
	totalBox.text(cnt);
	$('.Team' + t + ' .Skater[id=' + s + ']').toggleClass("Warn1", cnt == 5 && !fo_exp);
	$('.Team' + t + ' .Skater[id=' + s + ']').toggleClass("Warn2", cnt == 6 && !fo_exp);
	$('.Team' + t + ' .Skater[id=' + s + ']').toggleClass("Warn3", cnt > 6 || fo_exp);
}

function makeSkaterRows(t, id, number) { //team, id, number

	var team = $('.Team' + t + ' tbody'); 
	var teamd = $('.Teamd' + t); 
	var pd = $('<div class="tdivr">').addClass('Skater Penalty').attr('id', id).data('number', number);
	var head = document.getElementById('head' + t);
	var teamName = WS.state['ScoreBoard.Team(' + t + ').Name'];
	var teamFColor = WS.state['ScoreBoard.Team(' + t + ').Color(overlay_fg)'];
	var teamBColor = WS.state['ScoreBoard.Team(' + t + ').Color(overlay_bg)'];
	
	if (WS.state['ScoreBoard.Team(' + t + ').AlternateName(whiteboard)'] != null) {
		teamName = WS.state['ScoreBoard.Team(' + t + ').AlternateName(whiteboard)']
	}
//      Put this somewhere else
//	head.innerHTML = '<span class="Team' + t + 'custColor"; style="font-size: 200%;">' + teamName + '</span>';
//

    $('.Team' + t + 'custColor').css('color', teamFColor);
    $('.Team' + t + 'custColor').css('background-color', teamBColor);
	
	pd.append($('<div width=10%>').addClass('Number').text(number));
	pd.append($('<div width=50%>').addClass('Name').text(WS.state['ScoreBoard.Team(' + t + ').Skater(' + id + ').Name']));
	$.each([1, 2, 3, 4, 5, 6, 7, 8, 9], function(idx, c) {
		pd.append($('<div width=10%>').addClass('Box Box' + c).html('&nbsp;').css('color',teamFColor));
	});
	pd.append($('<div>').addClass('BoxFO_EXP').html('&nbsp;').css('color',teamFColor));
	pd.append($('<div>').addClass('Total').text('0').css('color',teamFColor));

	var inserted = false;
	teamd.find('div.Penalty').each(function (idx, row) {
		row = $(row);
		if (row.data('number') > number) {
			row.before(pd);
			inserted = true;
			return false;
		}
	});
	if (!inserted)
		teamd.append(pd);
}


