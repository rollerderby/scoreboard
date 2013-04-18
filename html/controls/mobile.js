
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

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
    $sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("sbchange", function(event, value) {
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
    sbcontrol: {
      convert: _timeConversions.minSecToMs,
      delayupdate: true,
      noSetControlValue: true
    }
  });
}

function setupTeamScorePage() {
  $.each([ "1", "2" ], function(i, n) {
    var team = $sb("ScoreBoard.Team("+n+")");
    var score = team.$sb("Score");

    $.each([ "#Team"+n+"ScorePage", "#TeamBothScorePage" ], function(ii, e) {
      team.$sb("Name").$sbElement(e+" a.Team"+n+".Name");
      score.$sbElement(e+" a.Team"+n+".Score");
      score.$sbControl(e+" button.Team"+n+".ScoreDown", { sbcontrol: {
        sbSetAttrs: { change: true }
      }});
      score.$sbControl(e+" button.Team"+n+".ScoreUp", { sbcontrol: {
        sbSetAttrs: { change: true }
      }});

    });

    score.$sbControl("#TeamBothScorePage input[type='number'].Team"+n+".SetScore,#TeamBothScorePage button.Team"+n+".SetScore", {
      sbcontrol: {
        delayupdate: true,
        noSetControlValue: true
      }
    });
  });
}
