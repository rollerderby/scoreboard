$(initialize);
/*
 Inside whiteboard screen for use with CRG Penalty Tracking funcitonality.
 Basically just the original PT javascript with a few tweaks.
 Modified by Adam Smasher (Dan Alt)
 Last modified 7/3/16
*/
var penaltyEditor = null;
var period = null;
var jam = null;
var teamId = null;
var skaterId = null;
var penaltyId = null;
var fo_exp = null;

function initialize() {
	WS.Connect();
	WS.AutoRegister();

	$.each([1, 2], function(idx, t) {
		WS.Register([ 'ScoreBoard.Team(' + t + ').Name' ]); //called when team name changes?
		WS.Register([ 'ScoreBoard.Team(' + t + ').AlternateName' ]);
		WS.Register([ 'ScoreBoard.Team(' + t + ').Color' ], function(k, v) { $('.Team' + t + 'custColor').css('color', WS.state['ScoreBoard.Team(' + t + ').Color(overlay_fg)']); $('.Team' + t + 'custColor').css('background-color', WS.state['ScoreBoard.Team(' + t + ').Color(overlay_bg)']); $('#head' + t).css('background-color', WS.state['ScoreBoard.Team(' + t + ').Color(overlay_bg)']); } );
//		WS.Register([ 'ScoreBoard.Team(' + t + ')' ]);
	});
	WS.Register( [ 'ScoreBoard.Clock(Period).Number' ], function(k, v) { period = v; });
	WS.Register( [ 'ScoreBoard.Clock(Jam).Number' ], function(k, v) { jam = v; });

	WS.Register( [ 'Game.Team(1).Skater' ], function(k, v) { skaterUpdate(1, k, v); } ); //called when skater info changes?
	WS.Register( [ 'Game.Team(2).Skater' ], function(k, v) { skaterUpdate(2, k, v); } ); //arguments: team number, skater id, jam number
	WS.Register( [ 'PenaltyCode' ], function(k, v) { penaltyCode(k, v); } );
	WS.Register( [ 'ScoreBoard.Clock(Period).MinimumNumber', 'ScoreBoard.Clock(Period).MaximumNumber' ], function(k, v) { setupSelect('Period'); } );
	WS.Register( [ 'ScoreBoard.Clock(Jam).MinimumNumber', 'ScoreBoard.Clock(Jam).MaximumNumber' ], function(k, v) { setupSelect('Jam'); } );

}

function adjust(which, inc) {
	var elem = $(".PenaltyEditor ." + which);
	console.log(elem, elem.val(), inc);
	elem.val(parseInt(elem.val()) + inc);
}

function clear() {
	console.log(penaltyId, skaterId);
	if (penaltyId == null || skaterId == null) {
		penaltyEditor.dialog("close");
	} else {
		WS.Command("Penalty", { teamId: teamId, skaterId: skaterId, penaltyId: penaltyId, fo_exp: fo_exp, jam: 0, period: 0 });
		penaltyEditor.dialog('close');
	}
}

var skaterIdRegex = /Skater\(([^\)]+)\)/;
var penaltyRegex = /Penalty\(([^\)]+)\)/;
function skaterUpdate(t, k, v) { //arguments: team number, skater id, jam number 
	match = k.match(skaterIdRegex); //match = skater id cleaned up
	if (match == null || match.length == 0)
		return;
	var id = match[1]; // id = skater id
	var prefix = 'Game.Team(' + t + ').Skater(' + id + ')';  //Example: prefix = Game.Team('1').Skater('id')
	if (k == prefix + '.Number') { //Example if skater id == Game.Team('team').Skater('id').Number
		var row = $('.Team' + t + ' .Skater.Penalty[id=' + id + ']');
		if (v == null) { // if jam number is null
			$('.Team' + t + ' .Skater[id=' + id + ']').remove();
			return;
		}

		if (row.length == 0) { //if the rows haven't been drawn yet?
			row = makeSkaterRows(t, id, v); //create skater rows
		}
		for (var i = 1; i <= 9; i++) // for penalty numbers one to nine..
			displayPenalty(t, id, i); // display penalties (team, skater id, penalty #)
		displayPenalty(t, id, 'FO_EXP'); // display foulout status
	} else {  // if skater id does NOT match Game.Team('team').Skater('id').Number
		// Look for penalty
		match = k.match(penaltyRegex);
		if (match == null || match.length == 0)
			return;
		var p = match[1];
		displayPenalty(t, id, p);
	}
}

function displayPenalty(t, s, p) { // team skater penalty#
	var penaltyBox = $('.Team' + t + ' .Skater.Penalty[id=' + s + '] .Box' + p);
	var jamBox = $('.Team' + t + ' .Skater.Jam[id=' + s + '] .Box' + p);
	var totalBox = $('.Team' + t + ' .Skater.Penalty[id=' + s + '] .Total');

	var prefix = 'Game.Team(' + t + ').Skater(' + s + ').Penalty(' + p + ')';
	code = WS.state[prefix + ".Code"];

	if (code != null) {
		penaltyBox.data("id", WS.state[prefix + ".Id"]);
		jamBox.data("id", WS.state[prefix + ".Id"]);
		penaltyBox.text(WS.state[prefix + ".Code"]);
		jamBox.text(WS.state[prefix + ".Period"] + '-' + WS.state[prefix + ".Jam"]);
	} else {
		penaltyBox.data("id", null);
		jamBox.data("id", null);
		penaltyBox.html("&nbsp;");
		jamBox.html("&nbsp;");
	}

	var cnt = 0; // Change row colors for skaters on 5 or more penalties
	$('.Team' + t + ' .Skater.Penalty[id=' + s + '] .Box').each(function(idx, elem) { cnt += ($(elem).data("id") != null ? 1 : 0); });
	totalBox.text(cnt);
	$('.Team' + t + ' .Skater[id=' + s + ']').toggleClass("Warn1", cnt == 5);
	$('.Team' + t + ' .Skater[id=' + s + ']').toggleClass("Warn2", cnt == 6);
	$('.Team' + t + ' .Skater[id=' + s + ']').toggleClass("Warn3", cnt > 6);
}

function makeSkaterRows(t, id, number) { //team, id, number
	var team = $('.Team' + t + ' tbody'); //for example team = $('.Team1 tbody)
	var p = $('<tr>').addClass('Skater Penalty').attr('id', id).data('number', number);
	var head = document.getElementById('head' + t);
	var teamName = WS.state['ScoreBoard.Team(' + t + ').Name'];
	var teamFColor = WS.state['ScoreBoard.Team(' + t + ').Color(overlay_fg)'];
	var teamBColor = WS.state['ScoreBoard.Team(' + t + ').Color(overlay_bg)'];
//	console.log("Smasher FTeam color is: " + teamFColor);
//	console.log("Smasher BTeam color is: " + teamBColor);
	
	if (WS.state['ScoreBoard.Team(' + t + ').AlternateName(whiteboard)'] != null) {
		teamName = WS.state['ScoreBoard.Team(' + t + ').AlternateName(whiteboard)']
	}

	head.innerHTML = '<span class="Team' + t + 'custColor"; style="font-size: 200%;">' + teamName + '</span>';
//	head.innerHTML = '<span class="Team' + t + 'custColor" style="background-color:' + WS.state['Game.Team(1).Color(overlay_bg)'] + '; font-size: 200%;">' + teamName + '</span>';
//	console.log("Team color " + t + " is: " + WS.state['Game.Team(1).Color(overlay_bg)']);
//        $('.Team' + t + 'custColor').css('backgroundColor', "#00ff00");
//        $('.Team' + t + 'custColor').css('backgroundColor', WS.state['Game.Team(' + t + ').Color(overlay_bg)']);
//        $('.Team' + t + 'custColor').css('backgroundColor', teamBColor);
        $('.Team' + t + 'custColor').css('color', teamFColor);
        $('.Team' + t + 'custColor').css('background-color', teamBColor);
//        $('.Team2custColor').css('background-color', WS.state['Game.Team(2).Color(overlay_bg)']);
	
	//head.innerHTML = $sb("ScoreBoard.Team("+team+")")
	//_crgUtils.bindColors($sb("ScoreBoard.Team("+team+")"), "scoreboard", head.style.backgroundColor, null, { 'fg': 'background-color' })
	//head.style.backgroundColor = WS.state['Scoreboard.Team('+t+').overlay_bg'];
	//_crgUtils.bindColors("Scoreboard.Team("+t+")", "overlay", head.style.color, head.style.backgroundColor)
	
	//_crgUtils.bindColors("ScoreBoard.Team("+team+")", "overlay", $("#Team" + team + "Name"));
	//bindColors: function(team, colorName, fg_elem, bg_elem, map)
	
	p.append($('<td>').attr('rowspan', 1).text(number));
	$.each([1, 2, 3, 4, 5, 6, 7, 8, 9], function(idx, c) {
		p.append($('<td>').addClass('Box Box' + c).html('&nbsp;'));
	});
	p.append($('<td>').addClass('BoxFO_EXP').html('&nbsp;'));
	p.append($('<td>').attr('rowspan', 1).addClass('Total').text('0'));

	var inserted = false;
	team.find('tr.Penalty').each(function (idx, row) {
		row = $(row);
		if (row.data('number') > number) {
			row.before(p).before(j);
			inserted = true;
			return false;
		}
	});
	if (!inserted)
		team.append(p).append(j);
}


