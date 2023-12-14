(function () {
  const prefix = 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').Period(*).Jam(*).TeamJam(*).ScoringTrip(*).';
  WS.Register([prefix + 'AfterSP', prefix + 'Score', prefix + 'Current'], function (k) {
    const selectorPrefix = '[Team="' + k.TeamJam + '"] [Period="' + k.Period + '"] [Jam="' + k.Jam + '"]';
    if (k.ScoringTrip === 1) {
      $(selectorPrefix + '.Jam:not(.SP) [ScoringTrip="2"]').text(toTripPoints(k));
      $(selectorPrefix + '.SP [ScoringTrip="2"]').text(toTripSpPoints(k));
    } else if (k.ScoringTrip > 10) {
      $(selectorPrefix + '.Jam:not(.SP) [ScoringTrip="10"]').text(toTripPoints(k));
      $(selectorPrefix + '.SP [ScoringTrip="10"]').text(toTripSpPoints(k));
    }
  });

  WS.Register('ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').Period(*).Jam(*).StarPass', {
    triggerBatchFunc: function () {
      const selector = 'tr.Jam:not(.Hide)';
      $('.SK.Period').each(function (i, e) {
        const dark = $(e).closest('[context="sheet"]').length ? 1 : $(e).find(selector).length % 2;
        $(e)
          .find(selector)
          .each(function (idx) {
            $(this).toggleClass('Darker', idx % 2 === dark);
          });
      });
    },
  });
})();

function toggleEdit(k, v, elem) {
  'use strict';
  elem
    .closest('tbody')
    .children('tr[Jam="' + elem.closest('[Jam]').attr('Jam') + '"]')
    .toggleClass('Edit');
  return true;
}

function notRemovable(k) {
  'use strict';
  const prefix = k.upTo('Jam');
  return (
    WS.state[prefix + '.TeamJam(1).JamScore'] + WS.state[prefix + '.TeamJam(1).OsOffset'] !== 0 ||
    WS.state[prefix + '.TeamJam(2).JamScore'] + WS.state[prefix + '.TeamJam(2).OsOffset'] !== 0 ||
    (WS.state[prefix + '.WalltimeStart'] > 0 && WS.state[prefix + '.WalltimeEnd'] === 0)
  );
}

function noPoints(k) {
  'use strict';
  const prefix = k.upTo('Jam');
  return (
    WS.state[prefix + '.TeamJam(1).JamScore'] + WS.state[prefix + '.TeamJam(1).OsOffset'] === 0 &&
    WS.state[prefix + '.TeamJam(2).JamScore'] + WS.state[prefix + '.TeamJam(2).OsOffset'] === 0
  );
}

function notRunning(k) {
  'use strict';
  const prefix = k.upTo('Jam');
  return WS.state[prefix + '.WalltimeStart'] === 0 || WS.state[prefix + '.WalltimeEnd'] > 0;
}

function toXnoSP(k, v) {
  'use strict';
  return isTrue(v) && !isTrue(WS.state[k.upTo('TeamJam') + '.StarPass']) ? 'X' : '';
}

function toXifSP(k, v) {
  'use strict';
  return isTrue(v) && isTrue(WS.state[k.upTo('TeamJam') + '.StarPass']) ? 'X' : '';
}

function toNiPreSp(k, v) {
  'use strict';
  return isTrue(v) || isTrue(WS.state[k.upTo('TeamJam') + '.ScoringTrip(1).AfterSP']) ? 'X' : '';
}

function toTripPoints(k) {
  'use strict';
  const prefix = k.upTo('ScoringTrip') + '.';
  if (k.ScoringTrip === '2' || k.ScoringTrip === '1') {
    const prefix1 = k.upTo('TeamJam') + '.ScoringTrip(1).';
    const t1Score = WS.state[prefix1 + 'Score'];
    const t1Text = t1Score && !isTrue(WS.state[prefix1 + 'AfterSP']) ? t1Score + ' + ' : '';
    const score = WS.state[prefix + 'Score'];
    if (score == null) {
      return t1Text ? t1Text + 'NI' : '';
    } else if (isTrue(WS.state[prefix + 'AfterSP'])) {
      return t1Text ? t1Text + 'SP' : '';
    } else if (score > 0) {
      return t1Text + score;
    } else {
      return t1Text + (isTrue(WS.state[prefix + 'Current']) ? '.' : '0');
    }
  } else if (Number(k.ScoringTrip) >= 10) {
    if (isTrue(WS.state[prefix + 'AfterSP']) || WS.state[prefix + 'Score'] == null) {
      return '';
    } else {
      let trip = 11;
      let val = WS.state[prefix + 'Score'];
      const prefixNoNumber = k.upTo('TeamJam') + '.ScoringTrip(';
      while (WS.state[prefixNoNumber + trip + ').Score'] != null && !isTrue(WS.state[prefixNoNumber + trip + ').AfterSP'])) {
        val = val + ' + ' + WS.state[prefixNoNumber + trip + ').Score'];
        trip++;
      }
      return val;
    }
  } else {
    const score = WS.state[prefix + 'Score'];
    if (score == null || isTrue(WS.state[prefix + 'AfterSP'])) {
      return '';
    } else if (score > 0) {
      return score;
    } else {
      return isTrue(WS.state[prefix + 'Current']) ? '.' : '0';
    }
  }
}

function toTripSpPoints(k) {
  'use strict';
  const prefix = k.upTo('ScoringTrip') + '.';
  if (k.ScoringTrip === '2' || k.ScoringTrip === '1') {
    const prefix1 = k.upTo('TeamJam') + '.ScoringTrip(1).';
    const t1Score = WS.state[prefix1 + 'Score'];
    const t1Text = t1Score && isTrue(WS.state[prefix1 + 'AfterSP']) ? t1Score + ' + ' : '';
    const score = WS.state[prefix + 'Score'];
    if (score == null) {
      return t1Text ? t1Text + 'NI' : '';
    } else if (!isTrue(WS.state[prefix + 'AfterSP'])) {
      return '';
    } else if (score > 0) {
      return t1Text + score;
    } else {
      return t1Text + (isTrue(WS.state[prefix + 'Current']) ? '.' : '0');
    }
  } else if (Number(k.ScoringTrip) >= 10) {
    if (WS.state[prefix + 'Score'] == null) {
      return '';
    } else {
      let trip = 10;
      const prefixNoNumber = k.upTo('TeamJam') + '.ScoringTrip(';
      while (WS.state[prefixNoNumber + trip + ').Score'] != null && !isTrue(WS.state[prefixNoNumber + trip + ').AfterSP'])) {
        trip++;
      }
      let val = WS.state[prefixNoNumber + trip + ').Score'] || '';
      trip++;
      while (WS.state[prefixNoNumber + trip + ').Score'] != null) {
        val = val + ' + ' + WS.state[prefixNoNumber + trip + ').Score'];
        trip++;
      }
      return val;
    }
  } else {
    const score = WS.state[prefix + 'Score'];
    if (!isTrue(WS.state[prefix + 'AfterSP'])) {
      return '';
    } else if (score) {
      return score;
    } else {
      return isTrue(WS.state[prefix + 'Current']) ? '.' : '0';
    }
  }
}

function openTripEditor(k, v, elem, event) {
  'use strict';
  if (event.target === elem[0] || event.target === elem.children('span')[0]) {
    _setupTripEditor(k.Game, k.Period, k.Jam, k.TeamJam, Number(k.ScoringTrip));
  }
}

function toPreSpScore(k, v) {
  'use strict';
  return v - WS.state[k.upTo('TeamJam') + '.AfterSPScore'];
}

function toArrow(k, v, elem) {
  'use strict';
  return elem.closest('tr.Jam').hasClass('SP') === (elem.closest('[context]').attr('context') === 'sheet') ? '↓' : '↑';
}

function hasOffset(k, v) {
  'use strict';
  return v !== 0 || WS.state[k.upTo('TeamJam') + '.OsOffsetReason'] !== '';
}

function hasUnexplainedOffset(k, v) {
  'use strict';
  return v !== 0 && WS.state[k.upTo('TeamJam') + '.OsOffsetReason'] === '';
}

function showOffsetEditor(k) {
  'use strict';
  WS.SetupDialog($('#osOffsetEditor'), k, {
    title: 'OS Offset',
    width: '600px',
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function toToJamNumber(k, v) {
  'use strict';
  return v + '.' + WS.state[k.upTo('Timeout') + '.WalltimeStart'];
}

function isOrThisTeam(k, v, elem) {
  'use strict';
  return isTrue(v) && WS.state[k.upTo('Timeout') + '.Owner'] === k.Game + '_' + elem.closest('[Team]').attr('Team');
}

function toToTypeName(k, v, elem) {
  'use strict';
  if (v === '.false') {
    return 'Untyped Timeout';
  } else if (v === 'O.false') {
    return 'Official Timeout';
  } else {
    const prefix = k.upTo('Game') + '.Team(' + v.slice(-1) + ').';
    const teamName = WS.state[prefix + 'UniformColor'] || WS.state[prefix + 'Name'];
    return (WS.state[k.upTo('Timeout') + '.Review'] ? 'Official Review ' : 'Team Timeout ') + teamName;
  }
}

function toToTypeVal(k, v) {
  'use strict';
  return WS.state[k.upTo('Timeout') + '.Owner'] + '.' + v;
}

function fromToTypeVal(k, v) {
  'use strict';
  const parts = v.split('.');
  WS.Set(k.upTo('Timeout') + '.Owner', parts[0]);
  return isTrue(parts[1]);
}

function toDuration(k, v) {
  'use strict';
  return isTrue(v) ? 'Running' : _timeConversions.msToMinSec(WS.state[k.upTo('Timeout') + '.Duration'], true);
}

function _setupTripEditor(gameId, p, j, teamId, t) {
  'use strict';
  $(':not(#Templates)>#TripEditor').dialog('close');

  const prefix = 'ScoreBoard.Game(' + gameId + ').Period(' + p + ').Jam(' + j + ').TeamJam(' + teamId + ').ScoringTrip(';
  while (t > 1 && WS.state[prefix + (t - 1) + ').Score'] === undefined) {
    t--;
  }
  if (t < 1) {
    t = 1;
  }

  WS.SetupDialog($('#TripEditor'), prefix + t + ')', {
    title: 'Period ' + p + ' Jam ' + j + ' Trip ' + (t === 1 ? 'Initial' : t),
    width: '300px',
  });
}

function remove(k, v, elem, event) {
  'use strict';
  if (event.which === 13 && v === '') {
    WS.Set(k + '.Remove', null);
  }
}

function openPrevTrip(k, v, elem, event) {
  'use strict';
  _setupTripEditor(k.Game, k.Period, k.Jam, k.TeamJam, Number(k.ScoringTrip) - 1);
}

function openNextTrip(k, v, elem, event) {
  'use strict';
  _setupTripEditor(k.Game, k.Period, k.Jam, k.TeamJam, Number(k.ScoringTrip) + 1);
}
