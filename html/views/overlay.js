
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

XML_ELEMENT_SELECTOR = "ScoreBoard";

function setupMainDiv(div) {
  div.css({ position: "fixed" });

  _crgUtils.bindAndRun($(window), "resize", function() {
    var aspect4x3 = _windowFunctions.get4x3Dimensions();
    div.css(aspect4x3).css("fontSize", aspect4x3.height);
  });
}

$sb(function() {
  setupMainDiv($("#mainDiv"));

  var showClockJLT = function() {
    if ($sb("ScoreBoard.Clock(Jam).Running").$sbIsTrue()) {
      $("a.ClockJLT").removeClass("ShowLineup ShowTimeout").addClass("ShowJam");
    } else if ($sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue()) {
      $("a.ClockJLT").removeClass("ShowLineup ShowJam").addClass("ShowTimeout");
    } else if ($sb("ScoreBoard.Clock(Lineup).Running").$sbIsTrue()) {
      $("a.ClockJLT").removeClass("ShowJam ShowTimeout").addClass("ShowLineup");
    } else {
      $("a.ClockJLT").removeClass("ShowLineup ShowTimeout").addClass("ShowJam");
    }
  };
  var showClockPI = function() {
    if ($sb("ScoreBoard.Clock(Period).Running").$sbIsTrue()) {
      $("a.ClockPI").removeClass("ShowIntermission").addClass("ShowPeriod");
    } else if ($sb("ScoreBoard.Clock(Intermission).Running").$sbIsTrue()) {
      $("a.ClockPI").removeClass("ShowPeriod").addClass("ShowIntermission");
    } else {
      $("a.ClockPI").removeClass("ShowIntermission").addClass("ShowPeriod");
    }
  };

  $.each( [ "1", "2" ], function(i, team) {
    if($sb("ScoreBoard.Team("+team+").AlternateName(overlay).Name").$sbGet()) {
      $sb("ScoreBoard.Team("+team+").AlternateName(overlay).Name").$sbElement("#Team"+team+"Name>a", {
    	sbelement: { autoFitText: true }
      }); 
	} else {
      $sb("ScoreBoard.Team("+team+").Name").$sbElement("#Team"+team+"Name>a", {
        sbelement: { autoFitText: true }
      });
	}
    $sb("ScoreBoard.Team("+team+").Score").$sbElement("#Team"+team+"Score>a", {
      sbelement: { autoFitText: true }
    });
  });

  $sb("ScoreBoard.Clock(Period).Number").$sbElement("#ClockPeriodNumber>a>span.Number", {
    sbelement: { autoFitText: true, autoFitTextContainer: "div" }
  });

  var setupClock = function(clock) {
    $sb("ScoreBoard.Clock("+clock+").Time").$sbElement("#Clock"+clock+"Time>a", {
      sbelement: {
        autoFitText: true,
        convert: _timeConversions.msToMinSec
      } });
  };
  $.each( [ "Jam", "Lineup", "Timeout" ], function(i, clock) {
    setupClock(clock);
    $sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("sbchange", showClockJLT);
  });
  $.each( [ "Period", "Intermission" ], function(i, clock) {
    setupClock(clock);
    $sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("sbchange", showClockPI);
  });
});
