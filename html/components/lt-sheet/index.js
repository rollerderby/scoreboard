WS.Register('ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').Period(*).Jam(*).StarPass', {
  triggerBatchFunc: function () {
    const selector = 'tr.Jam, tr.SP:not(.sbHide)';
    $('.LT.Period').each(function (i, e) {
      const dark = $(e).closest('[sbSheetStyle="sheet"]').length ? 1 : $(e).find(selector).length % 2;
      $(e)
        .find(selector)
        .each(function (idx) {
          $(this).toggleClass('Darker', idx % 2 === dark);
        });
    });
  },
});

$('#FieldingEditor').find('#skaterList').controlgroup();

function editFielding(k, v, elem) {
  'use strict';
  _openFieldingEditor(k.Game, k.Period, k.Jam, k.TeamJam, k.Fielding, false);
}

function editUpcomingFielding(k, v, elem) {
  'use strict';
  _openFieldingEditor(k.Game, undefined, WS.state['ScoreBoard.Game(' + k.Game + ').UpcomingJamNumber'], k.Team, k.Position, true);
}

function emptyIfInjAst(k, v) {
  'use strict';
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

function _openFieldingEditor(g, p, j, t, pos, upcoming) {
  'use strict';
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
    width: '700px',
  });
}

function isUpcoming(k) {
  'use strict';
  return k.Period == null;
}

function toBtStartText(k) {
  'use strict';
  const prefix = 'ScoreBoard.Game(' + k.Game + ').Team(' + k.Team + ').BoxTrip(' + k.BoxTrip + ').';
  const between = isTrue(WS.state[prefix + 'StartBetweenJams']);
  const afterSP = isTrue(WS.state[prefix + 'StartAfterSP']);
  return (between ? 'Before ' : '') + 'Jam ' + WS.state[prefix + 'StartJamNumber'] + (afterSP ? ' after SP' : '');
}

function toBtEndText(k) {
  'use strict';
  const prefix = 'ScoreBoard.Game(' + k.Game + ').Team(' + k.Team + ').BoxTrip(' + k.BoxTrip + ').';
  const between = isTrue(WS.state[prefix + 'EndBetweenJams']);
  const afterSP = isTrue(WS.state[prefix + 'EndAfterSP']);
  const jam = WS.state[prefix + 'EndJamNumber'];
  return (between ? ' After ' : ' ') + (jam === 0 ? 'ongoing' : 'Jam ' + jam) + (afterSP && !between ? ' after SP ' : ' ');
}
