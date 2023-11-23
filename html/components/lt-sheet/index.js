function initLtSheet() {
  WS.Register('ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').Period(*).Jam(*).StarPass', {
    triggerBatchFunc: function () {
      $('.LT.Period').each(function (i, e) {
        const dark = $('[context="Sheet"]').length ? 1 : $(e).find('tr:visible').length % 2;
        $(e)
          .find('tr:visible')
          .each(function (idx) {
            $(this).toggleClass('Darker', idx % 2 === dark);
          });
      });
    },
  });
}

function editFielding(elem) {
  'use strict';
  const k = WS._enrichProp(WS._getContext(elem));
  openFieldingEditor(k.Game, k.Period, k.Jam, k.TeamJam, k.Fielding, false);
}

function editUpcomingFielding(elem) {
  'use strict';
  const k = WS._enrichProp(WS._getContext(elem));
  openFieldingEditor(k.Game, undefined, WS.state['ScoreBoard.Game(' + k.Game + ').UpcomingJamNumber'], k.Team, k.Position, true);
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
