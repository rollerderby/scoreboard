
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 * Penalty Timing (C) 2013 Rob Thomas (The G33k) <xrobau@gmail.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

$(function() {
  setupJamControlPage();
  setupPeriodTimePage();

  WS.AutoRegister();
  WS.Connect();
});

function isTrue(value) {
  if (typeof value === 'boolean') {
    return value;
  } else {
    return (String(value).toLowerCase() === 'true');
  }
}

function setupJamControlPage() {
  $('#JamControlPage button.StartJam').on('click', function() { WS.Set('ScoreBoard.StartJam', true); });
  $('#JamControlPage button.StopJam').on('click', function() { WS.Set('ScoreBoard.StopJam', true); });
  $('#JamControlPage button.Timeout').on('click', function() { WS.Set('ScoreBoard.Timeout', true); });
  $('#JamControlPage button.Undo').on('click', function() { WS.Set('ScoreBoard.ClockUndo', true); });
  $('#JamControlPage div.Timeout button.Official').on('click', function() { WS.Set('ScoreBoard.OfficialTimeout', true); });
  $('#JamControlPage div.Timeout button.Team1').on('click', function() { WS.Set('ScoreBoard.Team(1).Timeout', true); });
  $('#JamControlPage div.OfficialReview button.Team1').on('click', function() { WS.Set('ScoreBoard.Team(1).OfficialReview', true); });
  $('#JamControlPage div.Timeout button.Team2').on('click', function() { WS.Set('ScoreBoard.Team(2).Timeout', true); });
  $('#JamControlPage div.OfficialReview button.Team2').on('click', function() { WS.Set('ScoreBoard.Team(2).OfficialReview', true); });

  WS.Register(['ScoreBoard.Team(*).Name', 'ScoreBoard.Team(*).AlternateName(operator)'], function(k, v) {
    var name = WS.state['ScoreBoard.Team('+k.Team+').AlternateName(operator)'];
    name = name || WS.state['ScoreBoard.Team('+k.Team+').Name'];
    $('.Name.Team'+k.Team).text(name);
  });

  // Setup clocks
  var showJamControlClock = function(clock) {
    $('#JamControlPage div.Time').not('.'+clock+'Time').hide().end()
      .filter('.'+clock+'Time').show();
  };
  // In case no clocks are running now, default to showing only Jam
  showJamControlClock('Jam');

  WS.Register('ScoreBoard.Clock(*).Running', function(k, v) {
    $('#JamControlPage span.ClockBubble.'+k.Clock).toggleClass('Running', isTrue(v));
  });
  $.each( [ 'Start', 'Stop', 'Timeout', 'Undo' ], function(i, button) {
    WS.Register('ScoreBoard.Settings.Setting(ScoreBoard.Button.'+button+'Label)', function(k, v) {
      $('#JamControlPage span.'+button+'Label').text(v);
    });
  });
  WS.Register('ScoreBoard.Clock(*).Running', function(k, v) {
    if (isTrue(v)) {
      showJamControlClock(k.Clock);
    }
  });
  WS.Register('ScoreBoard.Clock(*).Direction'); // for rounding
}

function setupPeriodTimePage() {
  $('#PeriodTimePage button.TimeDown').on('click', function() {
    WS.Set('ScoreBoard.Clock(Period).Time', -1000, 'change');
  });
  $('#PeriodTimePage button.TimeUp').on('click', function() {
    WS.Set('ScoreBoard.Clock(Period).Time', 1000, 'change');
  });
  $('#PeriodTimePage button.SetTime').on('click', function() {
    var t = $('#PeriodTimePage input:text.SetTime');
    WS.Set('ScoreBoard.Clock(Period).Time', _timeConversions.minSecToMs(t.val()));
  });
}


function toTime(k, v) {
  k = WS._enrichProp(k);
  var isCountDown = isTrue(WS.state['ScoreBoard.Clock(' + k.Clock + ').Direction']);
  return _timeConversions.msToMinSecNoZero(v, isCountDown);
}
//# sourceURL=nso\jt\index.js
