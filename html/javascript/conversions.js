function compareAttrThenSubId(attr, a, b) {
  'use strict';
  if ($(a).attr(attr) == null) {
    return true;
  } else if ($(a).attr(attr) === $(b).attr(attr)) {
    return _windowFunctions.numCompareByAttr('subId', a, b);
  } else {
    return _windowFunctions.alphaCompareByAttr(attr, a, b);
  }
}

function numCompareAttrThenSubId(attr, a, b) {
  'use strict';
  if ($(a).attr(attr) == null) {
    return true;
  } else if ($(a).attr(attr) === $(b).attr(attr)) {
    return _windowFunctions.numCompareByAttr('subId', a, b);
  } else {
    return _windowFunctions.numCompareByAttr(attr, a, b);
  }
}

function compareRosterNumber(a, b) {
  'use strict';
  return compareAttrThenSubId('rosterNumber', a, b);
}

function compareName(a, b) {
  'use strict';
  return compareAttrThenSubId('name', a, b);
}

function comparePeriod(a, b) {
  'use strict';
  const comp = numCompareAttrThenSubId('Period', a, b);
  return $('[context="Sheet"]').length ? comp : !comp;
}

function compareJam(a, b) {
  'use strict';
  const comp = numCompareAttrThenSubId('Jam', a, b);
  return $('[context="Sheet"]').length ? comp : !comp;
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

function toNullIfEmpty(v) {
  'use strict';
  return v === '' ? null : v;
}

function toNullIfEmptyVal(k, elem) {
  'use strict';
  return elem.val() === '' ? null : elem.val();
}

function toX(k, v) {
  'use strict';
  return isTrue(v) ? 'X' : '';
}

function toTime(k, v) {
  'use strict';
  const isCountDown = isTrue(WS.state['ScoreBoard.CurrentGame.Clock(' + k.Clock + ').Direction']);
  return _timeConversions.msToMinSecNoZero(v, isCountDown);
}

function toSpJamNo(k, v) {
  'use strict';
  return isTrue(v) ? 'SP' : 'SP*';
}

function emptyIfNoSp(k, v) {
  'use strict';
  return k.field !== 'SP' &&
    isTrue(WS.state['ScoreBoard.Game(' + k.Game + ').Period(' + k.Period + ').Jam(' + k.Jam + ').TeamJam(' + k.TeamJam + ').StarPass'])
    ? v
    : '';
}

function checkInj(k, v) {
  'use strict';
  if (isTrue(WS.state['ScoreBoard.Game(' + k.Game + ').Period(' + k.Period + ').Jam(' + k.Jam + ').InjuryContinuation'])) {
    if (isTrue(WS.state['ScoreBoard.Game(' + k.Game + ').Period(' + k.Period + ').Jam(' + k.Jam + ').TeamJam(' + k.TeamJam + ').Lead'])) {
      return 'INJ*';
    } else {
      return 'INJ';
    }
  } else {
    return WS.state['ScoreBoard.Game(' + k.Game + ').Period(' + k.Period + ').Jam(' + k.Jam + ').Number'];
  }
}

function isCurrentPeriod(k, v) {
  'use strict';
  return k.field === 'Number' && v === WS.state['ScoreBoard.Game(' + k.Game + ').CurrentPeriodNumber'];
}

function getParents(rs) {
  'use strict';
  const parent = WS.state['ScoreBoard.Rulesets.Ruleset(' + rs + ').Parent'];
  if (!parent) {
    return [rs];
  } else {
    return getParents(parent).concat([rs]);
  }
}

function indentByParents(k, v) {
  'use strict';
  return '&nbsp;'.repeat(3 * (getParents(k.Ruleset).length - 1)) + v;
}

function orderRsAsTree(a, b) {
  'use strict';
  const rsA = $(a).attr('value');
  const rsB = $(b).attr('value');
  const setA = getParents(rsA);
  const setB = getParents(rsB);

  for (let i = 0; i < Math.min(setA.length, setB.length); i++) {
    if (setA[i] !== setB[i]) {
      return WS.state['ScoreBoard.Rulesets.Ruleset(' + setA[i] + ').Name'] > WS.state['ScoreBoard.Rulesets.Ruleset(' + setB[i] + ').Name'];
    }
  }
  return setA.length > setB.length;
}

function toMediaPath(k, v) {
  'use strict';
  return '/' + k.Format + '/' + k.Type + '/' + k.File;
}
