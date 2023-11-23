function initPltInput() {
  'use strict';
  const gameId = _windowFunctions.getParam('game');
  WS.Register(['ScoreBoard.Game(' + gameId + ').Rule(Penalties.NumberToFoulout)']);

  WS.Register(['ScoreBoard.Game(' + gameId + ').Clock(Period).Number'], updateCurrentPeriodStyle());
  WS.Register(['ScoreBoard.Game(' + gameId + ').Clock(Jam).Number'], updateCurrentJamStyle());
  WS.Register('ScoreBoard.Game(' + gameId + ').Team(*).Skater(*).Role', {
    triggerBatchFunc: function () {
      $('.PLT.Team').each(function (i, e) {
        $(e)
          .find('tr:visible')
          .each(function (idx) {
            $(this).toggleClass('Darker', idx % 4 === 0 || idx % 4 === 3);
          });
      });
    },
  });
}
initPltInput();

function updateRowColor(k, v) {
  const dark = $('[context="Sheet"]').length ? $('.LT.Period tr:visible').length % 2 : 1;
  $('.LT.Period tr:visible').each(function (idx) {
    $(this).toggleClass('Darker', idx % 2 === dark);
  });
  return !isTrue(v);
}

function isOnTrackRole(k, v) {
  'use strict';
  return v === 'Jammer' || v === 'Pivot' || v === 'Blocker';
}

function prefixSigma(k, v) {
  'use strict';
  return 'Σ ' + v;
}

function toWarnLevel(k, v) {
  'use strict';
  const limit = WS.state['ScoreBoard.Game(' + k.Game + ').Rule(Penalties.NumberToFoulout)'];
  if (WS.state['ScoreBoard.Game(' + k.Game + ').Team(' + k.Team + ').Skater(' + k.Skater + ').Penalty(0).Code'] || v >= limit) {
    return 3;
  } else if (v == limit - 1) {
    return 2;
  } else if (v == limit - 2) {
    return 1;
  } else {
    return 0;
  }
}

function updateSkaterUnserved(k, v) {
  'use strict';
  $('[Skater="' + k.Skater + '"] .Sitting').toggleClass(
    'Unserved',
    (v != null && !isTrue(v)) ||
      $('[Skater="' + k.Skater + '"] .Box:not([Penalty="0"]):not([Penalty="' + k.Penalty + '"]).Unserved').length > 0
  );
  return v != null && !isTrue(v);
}

function advanceOrAnnotation(elem) {
  'use strict';
  const context = WS._getContext(elem);
  if (elem.hasClass('Active')) {
    WS.Set(context.substring(0, context.lastIndexOf('.')) + '.AdvanceFieldings', true);
  } else if (elem.hasClass('OnTrack')) {
    const k = WS._enrichProp(context);
    openAnnotationEditor(k.Game, k.Team, k.Skater);
  }
}

function addPenalty(elem) {
  'use strict';
  const k = WS._enrichProp(WS._getContext(elem));
  openPenaltyEditor(k.Game, k.Team, k.Skater, 99);
}

function editPenalty(elem) {
  'use strict';
  const k = WS._enrichProp(WS._getContext(elem));
  openPenaltyEditor(k.Game, k.Team, k.Skater, k.Penalty);
}

function toPeriodJam(k, v) {
  'use strict';
  const prefix = 'ScoreBoard.Game(' + k.Game + ').Team(' + k.Team + ').Skater(' + k.Skater + ').Penalty(' + k.Penalty + ').';
  const pn = WS.state[prefix + 'PeriodNumber'];
  const jn = WS.state[prefix + 'JamNumber'];
  return pn && jn ? pn + '-' + jn : '\xa0';
}

function updateCurrentPeriodStyle() {
  'use strict';
  const gameId = _windowFunctions.getParam('game');
  const periodNumber = WS.state['ScoreBoard.Game(' + gameId + ').Clock(Period).Number'];
  if (periodNumber == null) {
    return;
  }

  $('#current-period-style').remove();
  $('<style> .Box[period="' + periodNumber + '"] { font-weight: bold; color: #000; }</style>')
    .attr('id', 'current-period-style')
    .appendTo('head');

  updateCurrentJamStyle();
}

function updateCurrentJamStyle() {
  'use strict';
  const gameId = _windowFunctions.getParam('game');
  const periodNumber = WS.state['ScoreBoard.Game(' + gameId + ').Clock(Period).Number'];
  const jamNumber = WS.state['ScoreBoard.Game(' + gameId + ').Clock(Jam).Number'];
  if (jamNumber == null || periodNumber == null) {
    return;
  }

  $('#current-jam-style').remove();
  $('<style> .Box[period="' + periodNumber + '"][jam="' + jamNumber + '"] { text-decoration: underline; } </style>')
    .attr('id', 'current-jam-style')
    .appendTo('head');
}
