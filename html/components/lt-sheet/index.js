'use strict';

WS.Register('ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').Period(*).Jam(*).StarPass', {
  triggerBatchFunc: function () {
    const selector = 'tr.Jam, tr.SP:not(.sbHide)';
    $('.LtSheet>.Period').each(function (i, e) {
      const dark = $(e).closest('[sbSheetStyle="sheet"]').length ? 1 : $(e).find(selector).length % 2;
      $(e)
        .find(selector)
        .each(function (idx) {
          $(this).toggleClass('Darker', idx % 2 === dark);
        });
    });
  },
});
WS.Register(['ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').Jam(*).TeamJam(*).Fielding(*).BoxTrip(*)']);

function ltsEditFielding(k) {
  _ltsOpenFieldingEditor(k.Game, k.Period, k.Jam, k.TeamJam, k.Fielding, false);
}

function ltsEditUpcomingFielding(k) {
  _ltsOpenFieldingEditor(k.Game, undefined, WS.state['ScoreBoard.Game(' + k.Game + ').UpcomingJamNumber'], k.Team, k.Position, true);
}

function ltsEmptyIfInjAst(k, v) {
  if (
    k.field === 'InjuryContinuation' ||
    k.field === 'Lead' ||
    (isTrue(WS.state['ScoreBoard.Game(' + k.Game + ').Period(' + k.Period + ').Jam(' + k.Jam + ').InjuryContinuation']) &&
      isTrue(WS.state['ScoreBoard.Game(' + k.Game + ').Period(' + k.Period + ').Jam(' + k.Jam + ').TeamJam(' + k.TeamJam + ').Lead)']))
  ) {
    return '';
  } else {
    return v;
  }
}

function _ltsOpenFieldingEditor(g, p, j, t, pos, upcoming) {
  const prefix =
    'ScoreBoard.Game(' +
    g +
    ').' +
    (isTrue(upcoming) ? '' : 'Period(' + p + ').') +
    'Jam(' +
    j +
    ').TeamJam(' +
    t +
    ').Fielding(' +
    pos +
    ')';

  const posName = pos === 'Pivot' ? 'Pivot/Blocker4' : pos;

  WS.SetupDialog($('#FieldingEditor'), prefix, {
    modal: true,
    title: (upcoming ? 'Upcoming Jam' : 'Period ' + p + ' Jam ' + j) + ' ' + posName,
    width: '800px',
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function ltsIsUnknownSkater(k) {
  return !WS.state[k.upTo('Fielding') + '.Skater'] && !isTrue(WS.state[k.upTo('Fielding') + '.NotFielded']);
}

function ltsToBtStartText(k) {
  const prefix = 'ScoreBoard.Game(' + k.Game + ').Team(' + k.Team + ').BoxTrip(' + k.BoxTrip + ').';
  const between = isTrue(WS.state[prefix + 'StartBetweenJams']);
  const afterSP = isTrue(WS.state[prefix + 'StartAfterSP']);
  return (between ? 'Before ' : '') + 'Jam ' + WS.state[prefix + 'StartJamNumber'] + (afterSP ? ' after SP' : '');
}

function ltsToBtEndText(k) {
  const prefix = 'ScoreBoard.Game(' + k.Game + ').Team(' + k.Team + ').BoxTrip(' + k.BoxTrip + ').';
  const between = isTrue(WS.state[prefix + 'EndBetweenJams']);
  const afterSP = isTrue(WS.state[prefix + 'EndAfterSP']);
  const jam = WS.state[prefix + 'EndJamNumber'];
  return (between ? ' After ' : ' ') + (jam === 0 ? 'ongoing' : 'Jam ' + jam) + (afterSP && !between ? ' after SP ' : ' ');
}
