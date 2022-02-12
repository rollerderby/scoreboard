$(function () {
  'use strict';
  setupJamControlPage();
  setupPeriodTimePage();

  WS.AutoRegister();
  WS.Connect();
});

function isTrue(value) {
  'use strict';
  if (typeof value === 'boolean') {
    return value;
  } else {
    return String(value).toLowerCase() === 'true';
  }
}

function setupJamControlPage() {
  'use strict';
  $('#JamControlPage button.StartJam').on('click', function () {
    WS.Set('ScoreBoard.CurrentGame.StartJam', true);
  });
  $('#JamControlPage button.StopJam').on('click', function () {
    WS.Set('ScoreBoard.CurrentGame.StopJam', true);
  });
  $('#JamControlPage button.Timeout').on('click', function () {
    WS.Set('ScoreBoard.CurrentGame.Timeout', true);
  });
  $('#JamControlPage button.Undo').on('click', function () {
    WS.Set('ScoreBoard.CurrentGame.ClockUndo', true);
  });
  $('#JamControlPage div.Timeout button.Official').on('click', function () {
    WS.Set('ScoreBoard.CurrentGame.OfficialTimeout', true);
  });
  $('#JamControlPage div.Timeout button.Team1').on('click', function () {
    WS.Set('ScoreBoard.CurrentGame.Team(1).Timeout', true);
  });
  $('#JamControlPage div.OfficialReview button.Team1').on('click', function () {
    WS.Set('ScoreBoard.CurrentGame.Team(1).OfficialReview', true);
  });
  $('#JamControlPage div.Timeout button.Team2').on('click', function () {
    WS.Set('ScoreBoard.CurrentGame.Team(2).Timeout', true);
  });
  $('#JamControlPage div.OfficialReview button.Team2').on('click', function () {
    WS.Set('ScoreBoard.CurrentGame.Team(2).OfficialReview', true);
  });

  WS.Register(
    [
      'ScoreBoard.CurrentGame.Team(*).Name',
      'ScoreBoard.CurrentGame.Team(*).UniformColor',
      'ScoreBoard.CurrentGame.Team(*).AlternateName(operator)',
    ],
    function (k, v) {
      var name = WS.state['ScoreBoard.CurrentGame.Team(' + k.Team + ').AlternateName(operator)'];
      name = name || WS.state['ScoreBoard.CurrentGame.Team(' + k.Team + ').UnifromColor'];
      if (name == null || name === '') {
        name = WS.state['ScoreBoard.CurrentGame.Team(' + k.Team + ').Name'];
      }
      $('.Name.Team' + k.Team).text(name);
    }
  );

  // Setup clocks
  var showJamControlClock = function (clock) {
    $('#JamControlPage div.Time')
      .not('.' + clock + 'Time')
      .hide()
      .end()
      .filter('.' + clock + 'Time')
      .show();
  };
  // In case no clocks are running now, default to showing only Jam
  showJamControlClock('Jam');

  WS.Register('ScoreBoard.CurrentGame.Clock(*).Running', function (k, v) {
    $('#JamControlPage span.ClockBubble.' + k.Clock).toggleClass('Running', isTrue(v));
  });
  $.each(['Start', 'Stop', 'Timeout', 'Undo'], function (i, button) {
    WS.Register('ScoreBoard.CurrentGame.Label(' + button + ')', function (k, v) {
      $('#JamControlPage span.' + button + 'Label').text(v);
    });
  });
  WS.Register('ScoreBoard.CurrentGame.Clock(*).Running', function (k, v) {
    if (isTrue(v)) {
      showJamControlClock(k.Clock);
    }
  });
  WS.Register('ScoreBoard.CurrentGame.Clock(*).Direction'); // for rounding
}

function setupPeriodTimePage() {
  'use strict';
  $('#PeriodTimePage button.TimeDown').on('click', function () {
    WS.Set('ScoreBoard.CurrentGame.Clock(Period).Time', -1000, 'change');
  });
  $('#PeriodTimePage button.TimeUp').on('click', function () {
    WS.Set('ScoreBoard.CurrentGame.Clock(Period).Time', 1000, 'change');
  });
  $('#PeriodTimePage button.SetTime').on('click', function () {
    var t = $('#PeriodTimePage input:text.SetTime');
    WS.Set('ScoreBoard.CurrentGame.Clock(Period).Time', _timeConversions.minSecToMs(t.val()));
  });
}

function toTime(k, v) {
  'use strict';
  k = WS._enrichProp(k);
  var isCountDown = isTrue(WS.state['ScoreBoard.CurrentGame.Clock(' + k.Clock + ').Direction']);
  return _timeConversions.msToMinSecNoZero(v, isCountDown);
}
