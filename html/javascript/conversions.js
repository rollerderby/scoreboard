function _compareAttrThenSubId(attr, a, b) {
  'use strict';
  if ($(a).attr(attr) == null) {
    return true;
  } else if ($(a).attr(attr) === $(b).attr(attr)) {
    return _windowFunctions.numCompareByAttr('sbSubId', a, b);
  } else {
    return _windowFunctions.alphaCompareByAttr(attr, a, b);
  }
}

function _numCompareAttrThenSubId(attr, a, b) {
  'use strict';
  if ($(a).attr(attr) == null) {
    return true;
  } else if ($(a).attr(attr) === $(b).attr(attr)) {
    return _windowFunctions.numCompareByAttr('sbSubId', a, b);
  } else {
    return _windowFunctions.numCompareByAttr(attr, a, b);
  }
}

function compareRosterNumber(a, b) {
  'use strict';
  return _compareAttrThenSubId('rosterNumber', a, b);
}

function compareName(a, b) {
  'use strict';
  return _compareAttrThenSubId('name', a, b);
}

function compareValue(a, b) {
  'use strict';
  return _compareAttrThenSubId('value', a, b);
}

function compareRole(a, b) {
  'use strict';
  return _compareAttrThenSubId('role', a, b);
}

function comparePeriod(a, b) {
  'use strict';
  const comp = _numCompareAttrThenSubId('Period', a, b);
  return $(a).closest('[context]').attr('context') === 'sheet' ? comp : !comp;
}

function compareJam(a, b) {
  'use strict';
  const comp = _numCompareAttrThenSubId('Jam', a, b);
  return $(a).closest('[context]').attr('context') === 'sheet' ? comp : !comp;
}

function orderRsAsTree(a, b) {
  'use strict';
  const rsA = $(a).attr('value');
  const rsB = $(b).attr('value');
  const setA = _getParents(rsA);
  const setB = _getParents(rsB);

  for (let i = 0; i < Math.min(setA.length, setB.length); i++) {
    if (setA[i] !== setB[i]) {
      return WS.state['ScoreBoard.Rulesets.Ruleset(' + setA[i] + ').Name'] > WS.state['ScoreBoard.Rulesets.Ruleset(' + setB[i] + ').Name'];
    }
  }
  return setA.length > setB.length;
}

function toCssUrl(k, v) {
  'use strict';
  if (v) {
    return 'url("' + v + '")';
  } else {
    return '';
  }
}

function toShadow(k, v) {
  'use strict';
  if (v == null || v === '') {
    return '';
  }
  const shadow = '0px 0px 0.2em ' + v;
  return shadow + ', ' + shadow + ', ' + shadow;
}

function empty(k, v) {
  'use strict';
  return !v;
}

function notEmpty(k, v) {
  'use strict';
  return !!v;
}

function setAndFalse(k, v) {
  'use strict';
  return v != null && !isTrue(v);
}

function toNbspIfEmpty(k, v) {
  'use strict';
  return v || '\xa0';
}

function toNullIfEmpty(k, v) {
  'use strict';
  return v === '' ? null : v;
}

function toX(k, v) {
  'use strict';
  return isTrue(v) ? 'X' : '';
}

function fromX(k, v, elem) {
  'use strict';
  return elem.text() === '';
}

function toTime(k, v) {
  'use strict';
  const isCountDown = isTrue(WS.state[k.upTo('Clock') + '.Direction']);
  return _timeConversions.msToMinSecNoZero(v, isCountDown);
}

function toSP(k, v) {
  'use strict';
  return isTrue(v) ? 'SP' : '';
}
function toSpJamNo(k, v) {
  'use strict';
  return isTrue(v) ? 'SP' : 'SP*';
}

function emptyIfNoSp(k, v) {
  'use strict';
  return k.field !== 'StarPass' && isTrue(WS.state[k.upTo('TeamJam') + '.StarPass']) ? v : '';
}

function checkInj(k, v) {
  'use strict';
  if (isTrue(WS.state[k.upTo('Jam') + '.InjuryContinuation'])) {
    return isTrue(v) ? 'INJ*' : 'INJ';
  } else {
    return WS.state[k.upTo('Jam') + '.Number'];
  }
}

function isCurrentPeriod(k, v) {
  'use strict';
  return k.field === 'Number' && v === WS.state[k.upTo('Game') + '.CurrentPeriodNumber'];
}

function _getParents(rs) {
  'use strict';
  const parent = WS.state['ScoreBoard.Rulesets.Ruleset(' + rs + ').Parent'];
  if (!parent) {
    return [rs];
  } else {
    return _getParents(parent).concat([rs]);
  }
}

function indentByParents(k, v) {
  'use strict';
  return '&nbsp;'.repeat(3 * (_getParents(k.Ruleset).length - 1)) + v;
}

function toMediaPath(k) {
  'use strict';
  return '/' + k.Format + '/' + k.Type + '/' + k.File;
}

function appendText(k, v, elem) {
  'use strict';
  const old = WS.state[k];
  closeDialog(k, v, elem);
  return (old ? old + '; ' : '') + elem.text();
}

function toTimeoutType(k) {
  'use strict';
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

function intermissionDisplay(k) {
  'use strict';
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

function toClockInitialNumber(k) {
  'use strict';
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

function clockSelect(k) {
  'use strict';
  var jam = isTrue(WS.state[k.upTo('Game') + '.InJam']);
  var timeout = isTrue(WS.state[k.upTo('Game') + '.Clock(Timeout).Running']);
  var lineup = isTrue(WS.state[k.upTo('Game') + '.Clock(Lineup).Running']);
  var intermission = isTrue(WS.state[k.upTo('Game') + '.Clock(Intermission).Running']);

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

function setActiveTimeout(k) {
  var to = WS.state[k.upTo('Game') + '.TimeoutOwner'].slice(-1);
  var or = WS.state[k.upTo('Game') + '.OfficialReview'];

  $('.Team .Dot').removeClass('Current');

  if (to && to !== 'O') {
    var dotSel;
    if (or) {
      dotSel = '[Team=' + to + '] .OfficialReview1';
    } else {
      dotSel = '[Team=' + to + '] .Timeout' + (WS.state[k.upTo('Game') + '.Team(' + to + ').Timeouts'] + 1);
    }
    $(dotSel).addClass('Current');
  }
}

function closeDialog(k, v, elem) {
  'use strict';
  elem.closest('.ui-dialog-content').dialog('close');
}

function closeIfNull(k, v, elem) {
  'use strict';
  if (v == null) {
    elem.closest('.ui-dialog-content').dialog('close');
  }
}
