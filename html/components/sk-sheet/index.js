'use strict';

(function () {
  const prefix = 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').Period(*).Jam(*).TeamJam(*).ScoringTrip(*).';
  WS.Register([prefix + 'AfterSP', prefix + 'Score', prefix + 'Current'], function (k) {
    const selectorPrefix = '[Team="' + k.TeamJam + '"] [Period="' + k.Period + '"] [Jam="' + k.Jam + '"]';
    if (k.ScoringTrip === 1) {
      $(selectorPrefix + '>.Jam [ScoringTrip="2"]').text(sksToTripPoints(k));
      $(selectorPrefix + '>.SP [ScoringTrip="2"]').text(sksToTripSpPoints(k));
    } else if (k.ScoringTrip > 10) {
      $(selectorPrefix + '>.Jam [ScoringTrip="10"]').text(sksToTripPoints(k));
      $(selectorPrefix + '>.SP [ScoringTrip="10"]').text(sksToTripSpPoints(k));
    }
  });

  WS.Register('ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').Period(*).Jam(*).StarPass', {
    triggerBatchFunc: function () {
      const selector = 'tr.Jam:not(.sbHide)';
      $('.SkSheet>.Period').each(function (i, e) {
        const dark = $(e).closest('[sbSheetStyle="sheet"]').length ? 1 : $(e).find(selector).length % 2;
        $(e)
          .find(selector)
          .each(function (idx) {
            $(this).toggleClass('Darker', idx % 2 === dark);
          });
      });
    },
  });
})();

function sksToggleEdit(k, v, elem) {
  elem.closest('tbody').toggleClass('Edit');
  return true;
}

function sksNotRemovable(k) {
  const prefix = k.upTo('Jam');
  return (
    WS.state[prefix + '.TeamJam(1).JamScore'] + WS.state[prefix + '.TeamJam(1).OsOffset'] !== 0 ||
    WS.state[prefix + '.TeamJam(2).JamScore'] + WS.state[prefix + '.TeamJam(2).OsOffset'] !== 0 ||
    (WS.state[prefix + '.WalltimeStart'] > 0 && WS.state[prefix + '.WalltimeEnd'] === 0)
  );
}

function sksNoPoints(k) {
  const prefix = k.upTo('Jam');
  return (
    WS.state[prefix + '.TeamJam(1).JamScore'] + WS.state[prefix + '.TeamJam(1).OsOffset'] === 0 &&
    WS.state[prefix + '.TeamJam(2).JamScore'] + WS.state[prefix + '.TeamJam(2).OsOffset'] === 0
  );
}

function sksNotRunning(k) {
  const prefix = k.upTo('Jam');
  return WS.state[prefix + '.WalltimeStart'] === 0 || WS.state[prefix + '.WalltimeEnd'] > 0;
}

function sksToXnoSP(k, v) {
  return isTrue(v) && !isTrue(WS.state[k.upTo('TeamJam') + '.StarPass']) ? 'X' : '';
}

function sksToXifSP(k, v) {
  return isTrue(v) && isTrue(WS.state[k.upTo('TeamJam') + '.StarPass']) ? 'X' : '';
}

function sksToNiPreSp(k, v) {
  return isTrue(v) || isTrue(WS.state[k.upTo('TeamJam') + '.ScoringTrip(1).AfterSP']) ? 'X' : '';
}

function sksToTripPoints(k) {
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

function sksToTripSpPoints(k) {
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

function sksOpenTripEditor(k, v, elem, event) {
  if (event.target === elem[0] || event.target === elem.children('span')[0]) {
    _sksSetupTripEditor(k.Game, k.Period, k.Jam, k.TeamJam, Number(k.ScoringTrip));
  }
}

function sksToPreSpScore(k, v) {
  return v - WS.state[k.upTo('TeamJam') + '.AfterSPScore'];
}

function sksToArrow(k, v, elem) {
  return elem.closest('tr').hasClass('SP') === (elem.closest('[sbSheetStyle]').attr('sbSheetStyle') === 'sheet') ? '↓' : '↑';
}

function sksHasOffset(k, v) {
  return v !== 0 || WS.state[k.upTo('TeamJam') + '.OsOffsetReason'] !== '';
}

function sksHasUnexplainedOffset(k, v) {
  return v !== 0 && WS.state[k.upTo('TeamJam') + '.OsOffsetReason'] === '';
}

function sksShowOffsetEditor(k) {
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

function sksToToJamNumber(k, v) {
  return v + '.' + WS.state[k.upTo('Timeout') + '.WalltimeStart'];
}

function sksIsOrThisTeam(k, v, elem) {
  return isTrue(v) && WS.state[k.upTo('Timeout') + '.Owner'] === k.Game + '_' + elem.closest('[Team]').attr('Team');
}

function sksToToTypeName(k, v) {
  if (!v) {
    return 'Untyped Timeout';
  } else if (v === 'O') {
    return 'Official Timeout';
  } else {
    const prefix = k.upTo('Game') + '.Team(' + v.slice(-1) + ').';
    const teamName = WS.state[prefix + 'UniformColor'] || WS.state[prefix + 'Name'];
    return (WS.state[k.upTo('Timeout') + '.Review'] ? 'Official Review ' : 'Team Timeout ') + teamName;
  }
}

function sksToToDuration(k, v) {
  return isTrue(v) ? 'Running' : _timeConversions.msToMinSec(WS.state[k.upTo('Timeout') + '.Duration'], true);
}

function _sksSetupTripEditor(gameId, p, j, teamId, t) {
  $(':not(.sbTemplates)>#TripEditor').dialog('close');

  const prefix = 'ScoreBoard.Game(' + gameId + ').Period(' + p + ').Jam(' + j + ').TeamJam(' + teamId + ').ScoringTrip(';
  while (t > 1 && WS.state[prefix + (t - 1) + ').Score'] === undefined) {
    t--;
  }
  if (t < 1) {
    t = 1;
  }

  WS.SetupDialog($('#TripEditor'), prefix + t + ')', {
    title: 'Period ' + p + ' Jam ' + j + ' Trip ' + (t === 1 ? 'Initial' : t),
    width: '330px',
    buttons: {
      '⬅ Prev': function () {
        _sksSetupTripEditor(gameId, p, j, teamId, t - 1);
      },
      'Next ➡': function () {
        _sksSetupTripEditor(gameId, p, j, teamId, t + 1);
      },
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function sksRemoveTrip(k, v, elem, event) {
  if (event.which === 13 && v === '') {
    WS.Set(k + '.Remove', null);
  }
}
