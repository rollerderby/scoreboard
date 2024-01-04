'use strict';

function sbToNullIfEmpty(k, v) {
  return v === '' ? null : v;
}

function sbToNbspIfEmpty(k, v) {
  return v || '\xa0';
}

function sbAppendText(k, v, elem) {
  const old = WS.state[k];
  sbCloseDialog(k, v, elem);
  return (old ? old + '; ' : '') + elem.text();
}

function sbSubAnn(k, v, elem) {
  return 'Substitute for #' + elem.attr('rosterNumber');
}

function sbToTime(k, v) {
  const isCountDown = isTrue(WS.state[k.upTo('Clock') + '.Direction']);
  return _timeConversions.msToMinSecNoZero(v, isCountDown);
}

function sbToLongTime(k, v) {
  const isCountDown = isTrue(WS.state[k.upTo('Clock') + '.Direction']);
  return _timeConversions.msToMinSec(v, isCountDown);
}

function sbToSeconds(k, v) {
  const isCountDown = isTrue(WS.state[k.upTo('Clock') + '.Direction']);
  return _timeConversions.msToSeconds(v, isCountDown);
}

function sbFromLongTime(k, v) {
  return _timeConversions.minSecToMs(v);
}
function sbToClockInitialNumber(k) {
  let ret = '';
  const name = WS.state[k.upTo('Clock') + '.Name'];
  const number = WS.state[k.upTo('Clock') + '.Number'];

  if (name && number) {
    ret = name.substring(0, 1) + number;
  }

  if (name === 'Period' && WS.state[k.upTo('Game') + '.Rule(Period.Number)'] == 1) {
    ret = 'Game';
  }
  return ret;
}

function sbToTimeoutType(k) {
  const to = WS.state[k.upTo('Game') + '.TimeoutOwner'];
  const or = isTrue(WS.state[k.upTo('Game') + '.OfficialReview']);

  if (!to) {
    return 'Timeout';
  } else if (to === 'O') {
    return 'Official Timeout';
  } else if (or) {
    return 'Official Review';
  } else {
    return 'Team Timeout';
  }
}

function sbToToTypeVal(k, v) {
  return WS.state[k.upTo('Timeout') + '.Owner'] + '.' + v;
}

function sbFromToTypeVal(k, v) {
  const parts = v.split('.');
  WS.Set(k.upTo('Timeout') + '.Owner', parts[0]);
  return isTrue(parts[1]);
}

function sbToIntermissionDisplay(k) {
  const num = WS.state[k.upTo('Game') + '.Clock(Intermission).Number'];
  const max = WS.state[k.upTo('Game') + '.Rule(Period.Number)'];
  const isOfficial = WS.state[k.upTo('Game') + '.OfficialScore'];
  const showDuringOfficial = WS.state[k.upTo('Game') + '.ClockDuringFinalScore'];
  let ret = '';

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

function sbToJamNoDisplay(k, v) {
  if (isTrue(WS.state[k.upTo('Jam') + '.InjuryContinuation'])) {
    return isTrue(v) ? 'INJ*' : 'INJ';
  } else {
    return WS.state[k.upTo('Jam') + '.Number'];
  }
}

function sbToEmptyIfNoSp(k, v) {
  return k.field !== 'StarPass' && isTrue(WS.state[k.upTo('TeamJam') + '.StarPass']) ? v : '';
}

function _sbRsGetParents(rs) {
  const parent = WS.state['ScoreBoard.Rulesets.Ruleset(' + rs + ').Parent'];
  if (!parent) {
    return [rs];
  } else {
    return _sbRsGetParents(parent).concat([rs]);
  }
}

function sbIndentByParents(k, v) {
  return '&nbsp;'.repeat(3 * (_sbRsGetParents(k.Ruleset).length - 1)) + v;
}

function sbToMediaPath(k) {
  return '/' + k.Format + '/' + k.Type + '/' + k.File;
}
