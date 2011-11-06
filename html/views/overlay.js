
function setupMainDiv(div) {
  div.css({ position: "fixed" });

  _crgUtils.bindAndRun($(window), "resize", function() {
    var aspect4x3 = _windowFunctions.get4x3Dimensions();
    $("#mainDiv").css(aspect4x3).css("fontSize", aspect4x3.height);
  });
}

$(function() {
  $("a.ClockMinSec").data("sbelement", { convert: _timeConversions.msToMinSec });
});

$sb(function() {
  setupMainDiv($("#mainDiv"));

  var showClockJLT = function() {
    if ($sb("ScoreBoard.Clock(Jam).Running").$sbIsTrue()) {
      $("a.ClockJLT").switchClass("ShowLineup ShowTimeout", "ShowJam");
    } else if ($sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue()) {
      $("a.ClockJLT").switchClass("ShowLineup ShowJam", "ShowTimeout");
    } else if ($sb("ScoreBoard.Clock(Lineup).Running").$sbIsTrue()) {
      $("a.ClockJLT").switchClass("ShowJam ShowTimeout", "ShowLineup");
    } else {
      $("a.ClockJLT").switchClass("ShowLineup ShowTimeout", "ShowJam");
    }
  };
  var showClockPI = function() {
    if ($sb("ScoreBoard.Clock(Period).Running").$sbIsTrue()) {
      $("a.ClockPI").switchClass("ShowIntermission", "ShowPeriod");
    } else if ($sb("ScoreBoard.Clock(Intermission).Running").$sbIsTrue()) {
      $("a.ClockPI").switchClass("ShowPeriod", "ShowIntermission");
    } else {
      $("a.ClockPI").switchClass("ShowIntermission", "ShowPeriod");
    }
  };

  $sb("ScoreBoard.Clock(Jam).Running").$sbBindAndRun("content", showClockJLT);
  $sb("ScoreBoard.Clock(Lineup).Running").$sbBindAndRun("content", showClockJLT);
  $sb("ScoreBoard.Clock(Timeout).Running").$sbBindAndRun("content", showClockJLT);

  $sb("ScoreBoard.Clock(Period).Running").$sbBindAndRun("content", showClockPI);
  $sb("ScoreBoard.Clock(Intermission).Running").$sbBindAndRun("content", showClockPI);

  $sb("ScoreBoard.Team(1).Name").$sbElement("#Team1Name>a", { sbelement: { autoFitText: true } });
  $sb("ScoreBoard.Team(2).Name").$sbElement("#Team2Name>a", { sbelement: { autoFitText: true } });
});
