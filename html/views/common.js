function toTimeoutType(k, v) {
  'use strict';
  var to = WS.state['ScoreBoard.CurrentGame.TimeoutOwner'];
  var or = isTrue(WS.state['ScoreBoard.CurrentGame.OfficialReview']);

  if (!to) {
    return 'Timeout';
  } else if (WS.state['ScoreBoard.CurrentGame.TimeoutOwner'] === 'O') {
    return 'Official Timeout';
  } else if (or) {
    return 'Official Review';
  } else {
    return 'Team Timeout';
  }
}

function intermissionDisplay() {
  'use strict';
  var num = WS.state['ScoreBoard.CurrentGame.Clock(Intermission).Number'];
  var max = WS.state['ScoreBoard.CurrentGame.Rule(Period.Number)'];
  var isOfficial = WS.state['ScoreBoard.CurrentGame.OfficialScore'];
  var showDuringOfficial = WS.state['ScoreBoard.CurrentGame.ClockDuringFinalScore'];
  var ret = '';

  if (isOfficial) {
    if (showDuringOfficial) {
      ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.OfficialWithClock)'];
    } else {
      ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Official)'];
    }
  } else if (num === 0) {
    ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.PreGame)'];
  } else if (num != max) {
    ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Intermission)'];
  } else {
    ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Unofficial)'];
  }

  $('.Clock.Intermission .Time, .Clock.Intermission.Time').toggleClass(
    'Hidden',
    (num == max || isOfficial) && !(isOfficial && showDuringOfficial)
  );
  return ret;
}

function toClockInitialNumber(k, v) {
  'use strict';
  var ret = '';
  var name = WS.state['ScoreBoard.CurrentGame.Clock(' + k.Clock + ').Name'];
  var number = WS.state['ScoreBoard.CurrentGame.Clock(' + k.Clock + ').Number'];

  if (name != null && number != null) {
    ret = name.substring(0, 1) + number;
  }

  if (name === 'Period' && WS.state['ScoreBoard.CurrentGame.Rule(Period.Number)'] == 1) {
    ret = 'Game';
  }
  return ret;
}

function toSP(k, v) {
  'use strict';
  return isTrue(v) ? 'SP' : '';
}

function clockSelect(k, v) {
  'use strict';
  var jam = isTrue(WS.state['ScoreBoard.CurrentGame.InJam']);
  var timeout = isTrue(WS.state['ScoreBoard.CurrentGame.Clock(Timeout).Running']);
  var lineup = isTrue(WS.state['ScoreBoard.CurrentGame.Clock(Lineup).Running']);
  var intermission = isTrue(WS.state['ScoreBoard.CurrentGame.Clock(Intermission).Running']);

  var clock = 'NoClock';
  if (jam) {
    clock = 'Jam';
  } else if (timeout) {
    clock = 'Timeout';
  } else if (lineup) {
    clock = 'Lineup';
  } else if (intermission) {
    clock = 'Intermission';
  }

  $('.Clock,.SlideDown').removeClass('Show');
  $('.ShowIn' + clock).addClass('Show');
}

function setActiveTimeout(k, v) {
  var to = WS.state['ScoreBoard.CurrentGame.TimeoutOwner'].slice(-1);
  var or = WS.state['ScoreBoard.CurrentGame.OfficialReview'];

  $('.Team .Dot').removeClass('Current');

  if (to && to !== 'O') {
    var dotSel;
    if (or) {
      dotSel = '[Team=' + to + '] .OfficialReview1';
    } else {
      dotSel = '[Team=' + to + '] .Timeout' + (WS.state['ScoreBoard.CurrentGame.Team(' + to + ').Timeouts'] + 1);
    }
    $(dotSel).addClass('Current');
  }
}

WS.Register(
  [
    'ScoreBoard.CurrentGame.Clock(Timeout).Running',
    'ScoreBoard.CurrentGame.TimeoutOwner',
    'ScoreBoard.CurrentGame.OfficialReview',
    'ScoreBoard.CurrentGame.Team(*).Timeouts',
  ],
  setActiveTimeout
);

// Show Clocks
WS.Register(['ScoreBoard.CurrentGame.Clock(*).Running', 'ScoreBoard.CurrentGame.InJam'], clockSelect);
