
XML_ELEMENT_SELECTOR = "ScoreBoard";

$sb(function() {
  setupJamControlPage();
  setupPeriodTimePage();
  setupTeamScorePage();
});

function setupJamControlPage() {
  $sb("ScoreBoard.StartJam").$sbControl("#JamControlPage button.StartJam").val(true);
  $sb("ScoreBoard.StopJam").$sbControl("#JamControlPage button.StopJam").val(true);
  $sb("ScoreBoard.Timeout").$sbControl("#JamControlPage button.Timeout").val(true);

  $.each( [ "Period", "Jam", "Timeout" ], function(i, clock) {
    $sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("content", function(event, value) {
      $("#JamControlPage span.ClockBubble."+clock).toggleClass("Running", isTrue(value));
    });
  });
}

function setupPeriodTimePage() {
  var time = $sb("ScoreBoard.Clock(Period).Time");

  time.$sbElement("#PeriodTimePage a.Time", { sbelement: {
    convert: _timeConversions.msToMinSec
  }});

  time.$sbControl("#PeriodTimePage button.TimeDown", { sbcontrol: {
    sbSetAttrs: { change: true }
  }});
  time.$sbControl("#PeriodTimePage button.TimeUp", { sbcontrol: {
    sbSetAttrs: { change: true }
  }});

  time.$sbControl("#PeriodTimePage input:text.SetTime,#PeriodTimePage button.SetTime", {
    sbelement: { convert: _timeConversions.msToMinSec },
    sbcontrol: {
      convert: _timeConversions.minSecToMs,
      delayupdate: true
    }
  });
}

function setupTeamScorePage() {
  $.each([ "1", "2" ], function(i, n) {
    var team = $sb("ScoreBoard.Team("+n+")");
    var score = team.$sb("Score");

    team.$sb("Name").$sbElement("#TeamScorePage a.Team"+n+".Name");
    score.$sbElement("#TeamScorePage a.Team"+n+".Score");
    score.$sbControl("#TeamScorePage button.Team"+n+".ScoreDown", { sbcontrol: {
      sbSetAttrs: { change: true }
    }});
    score.$sbControl("#TeamScorePage button.Team"+n+".ScoreUp", { sbcontrol: {
      sbSetAttrs: { change: true }
    }});

    score.$sbControl("#TeamScorePage input:text.Team"+n+".SetScore,#TeamScorePage button.Team"+n+".SetScore", {
      sbcontrol: { delayupdate: true }
    });
  });
}
