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

function setupMainDiv(div) {
  div.css({ position: "fixed" });
//  div.bind("click", function() { _windowFunctions.fullscreenRequest(true); });
//  div.bind("dblclick", function() { _windowFunctions.fullscreenRequest(false); });


  _crgUtils.bindAndRun($(window), "resize", function() {
    var aspect16x9 = _windowFunctions.get16x9Dimensions();
    div.css(aspect16x9).css("fontSize", aspect16x9.height);
  });
//  av.initialize();
}


function initialize() {
	WS.Connect();
	WS.AutoRegister();
	setupMainDiv($("#mainDiv"));

	$.each([1, 2], function(idx, t) {
		WS.Register([ 'ScoreBoard.Team(' + t + ').Name' ]); //called when team name changes
		WS.Register([ 'ScoreBoard.Team(' + t + ').AlternateName' ]);
		WS.Register([ 'ScoreBoard.Team(' + t + ').Color' ], function(k, v) { $('.Team' + t + 'custColor').css('color', WS.state['ScoreBoard.Team(' + t + ').Color(overlay_fg).Color']); $('.Team' + t + 'custColor').css('background-color', WS.state['ScoreBoard.Team(' + t + ').Color(overlay_bg).Color']); $('#head' + t).css('background-color', WS.state['ScoreBoard.Team(' + t + ').Color(overlay_bg).Color']); } );
	});

        WS.Register( [ 'ScoreBoard.Team(1).Logo' ], function(k, v) { $('.Logo1').attr('src', v); } );
        WS.Register( [ 'ScoreBoard.Team(2).Logo' ], function(k, v) { $('.Logo2').attr('src', v); } );

	WS.Register( [ 'ScoreBoard.Team(1).Skater' ], function(k, v) { skaterUpdate(1, k, v); } ); //called when skater info changes
	WS.Register( [ 'ScoreBoard.Team(2).Skater' ], function(k, v) { skaterUpdate(2, k, v); } ); //arguments: team number, skater id, jam number

}


var skaterIdRegex = /Skater\(([^\)]+)\)/;
var penaltyRegex = /Penalty\(([^\)]+)\)/;
function skaterUpdate(t, k, v) { //arguments: team number, skater id, jam number 
	match = k.match(skaterIdRegex); //match = skater id cleaned up
	if (match == null || match.length == 0)
		return;
	var id = match[1]; // id = skater id
	var prefix = 'ScoreBoard.Team(' + t + ').Skater(' + id + ')';  //Example: prefix = ScoreBoard.Team('1').Skater('id')
	if (k == prefix + '.Number') { //Example if skater id == ScoreBoard.Team('team').Skater('id').Number
		var rowd = $('.Teamd' + t + ' .Skater.Penalty[id=' + id + ']');
		if (v == null) { // if jam number is null
			$('.Teamd' + t + ' .Skater[id=' + id + ']').remove();
			return;
		}

		if (rowd.length == 0) { //if the rows haven't been drawn yet?
			rowd = makeSkaterRows(t, id, v); //create skater rows
		}
		for (var i = 1; i <= 9; i++) { // for penalty numbers one to nine..
			displayPenalty(t, id, i); } // display penalties (team, skater id, penalty #)
		displayPenalty(t, id, 'FO_EXP'); // display foulout status
	} else {  // if skater id does NOT match ScoreBoard.Team('team').Skater('id').Number
		// Look for penalty
		match = k.match(penaltyRegex);
		if (match == null || match.length == 0)
			return;
		var p = match[1];
		displayPenalty(t, id, p);
	}
}

function displayPenalty(t, s, p) { // team skater penalty#
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

	var cntd = 0; // Change row colors for skaters on 5 or more penalties
	$('.Teamd' + t + ' .Skater.Penalty[id=' + s + '] .Box').each(function(idx, elem) { cntd += ($(elem).data("id") != null ? 1 : 0); });
	totalBoxd.text(cntd);
	$('.Teamd' + t + ' .Skater[id=' + s + ']').toggleClass("Warn1", cntd == 5);
	$('.Teamd' + t + ' .Skater[id=' + s + ']').toggleClass("Warn2", cntd == 6);
	$('.Teamd' + t + ' .Skater[id=' + s + ']').toggleClass("Warn3", cntd > 6);
}

function makeSkaterRows(t, id, number) { //team, id, number
//        console.log("in makeSkaterRows, t is " + t + " , id is " + id + " , number is " + number);
	var team = $('.Team' + t + ' tbody'); //for example team = $('.Team1 tbody)
	var teamd = $('.Teamd' + t); //for example team = $('.Team1 tbody)
	var pd = $('<div class="tdivr">').addClass('Skater Penalty').attr('id', id).data('number', number);
	var head = document.getElementById('head' + t);
	var teamName = WS.state['ScoreBoard.Team(' + t + ').Name'];
	var teamFColor = WS.state['ScoreBoard.Team(' + t + ').Color(overlay_fg)'];
	var teamBColor = WS.state['ScoreBoard.Team(' + t + ').Color(overlay_bg)'];
	
	if (WS.state['ScoreBoard.Team(' + t + ').AlternateName(whiteboard).Name'] != null) {
		teamName = WS.state['ScoreBoard.Team(' + t + ').AlternateName(whiteboard).Name']
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


// Audio Video Source
var av = {
        videoElement: null,
        audioSource: '',
        videoSource: '',

        gotSources: function (sourceInfos) {
                                console.log('Did we make it here? 2');
                for (var i = 0; i !== sourceInfos.length; ++i) {
                        var sourceInfo = sourceInfos[i];
                        console.log(sourceInfo);
                        if (sourceInfo.kind === 'audio' && av.audioSource == "") {
                                av.audioSource = sourceInfo.id;
                        } else if (sourceInfo.kind === 'video' && ( i == 2 ) ) {
                                av.videoSource = sourceInfo.id;
                                console.log('Selecting 2nd camera');
                        } else {
                                console.log('Some other kind of source: ', sourceInfo);
                        }
                }
                av.start();
        },

        initialize: function() {
                // if (typeof MediaStreamTrack === 'undefined'){
                if (typeof navigator.mediaDevices === 'undefined'){
                        alert('This browser does not support navigator.mediaDevices.\n\nTry Chrome Canary.');
                } else {
                        //MediaStreamTrack.getSources(av.gotSources);
                        navigator.mediaDevices.enumerateDevices()
                        .then(av.gotSources);
                        // av.start();
                }
        },

        successCallback: function(stream) {
                window.stream = stream; // make stream available to console

                if (av.videoElement == null) {
                        av.videoElement = document.createElement("video");
                        av.videoElement.className = 'video_underlay';
                        document.body.appendChild(av.videoElement);
                }

                av.videoElement.src = window.URL.createObjectURL(stream);
                av.videoElement.play();
                $(document.body).addClass("HasUnderlay");
        },

        errorCallback: function(error) {
                console.log('navigator.getUserMedia error: ', error);
        },

        start: function() {
                if (!!window.stream) {
                        videoElement.src = null;
                        window.stream.stop();
                }
                var constraints = {
                        audio: {
                                optional: [{sourceId: av.audioSource}]
                        },
                        video: {
                                optional: [{sourceId: av.videoSource}]
                        },
                        width: {min: 640, ideal: window.innerWidth},
                        height: {min: 480, ideal: window.innerHeight},
                        aspectRatio: 1.5,
                };
                console.log(constraints);
                console.log(window);

                var getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia || navigator.mozGetUserMedia;
                navigator.webkitGetUserMedia(constraints, av.successCallback, av.errorCallback);
        }
}


