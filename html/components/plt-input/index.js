(function () {
  'use strict';
  const gameId = _windowFunctions.getParam('game');
  const prefix = gameId ? 'ScoreBoard.Game(' + gameId + ').' : 'ScoreBoard.CurrentGame.';

  WS.Register([prefix + 'Rule(Penalties.NumberToFoulout)']);
  WS.Register([prefix + 'Clock(Period).Number'], updateCurrentPeriodStyle());
  WS.Register([prefix + 'Clock(Jam).Number'], updateCurrentJamStyle());
  WS.Register(prefix + 'Team(*).Skater(*).Role', {
    triggerBatchFunc: updatePtRowColor,
  });

  WS.Register([
    prefix + 'Team(*).Skater(*).Name',
    prefix + 'Period(*).CurrentJam',
    prefix + 'CurrentPeriodNumber',
    prefix + 'Period(*).CurrentJamNumber',
  ]);

  $('#AnnotationEditor #skaterList').controlgroup();
})();

function updatePtRowColor() {
  $('.PLT.Team').each(function (i, e) {
    $(e)
      .find('tr.Skater:not([role="NotInGame])')
      .each(function (idx) {
        $(this).toggleClass('Darker', idx % 4 === 2 || idx % 4 === 3);
      });
  });
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
  const limit = WS.state['ScoreBoard.' + (k.Game ? 'Game(' + k.Game + ')' : 'CurrentGame') + '.Rule(Penalties.NumberToFoulout)'];
  if (
    WS.state[
      'ScoreBoard.' + (k.Game ? 'Game(' + k.Game + ')' : 'CurrentGame') + '.Team(' + k.Team + ').Skater(' + k.Skater + ').Penalty(0).Code'
    ] ||
    v >= limit
  ) {
    return 3;
  } else if (v == limit - 1) {
    return 2;
  } else if (v == limit - 2) {
    return 1;
  } else {
    return 0;
  }
}

function updateSkaterUnserved(k, v, elem) {
  'use strict';
  elem
    .siblings('.Sitting')
    .toggleClass('Unserved', (v != null && !isTrue(v)) || elem.siblings('.Box:not([Penalty="0"]).Unserved').length > 0);
  return v != null && !isTrue(v);
}

function advanceOrAnnotation(k, v, elem) {
  'use strict';
  if (elem.hasClass('Active')) {
    WS.Set(k.substring(0, k.lastIndexOf('.')) + '.AdvanceFieldings', true);
  } else if (elem.hasClass('OnTrack')) {
    _openAnnotationEditor(k.Game, k.Team, k.Skater);
  }
}

function addPenalty(k, v, elem) {
  'use strict';
  _openPenaltyEditor(k.Game, k.Team, k.Skater, 99);
}

function editPenalty(k, v, elem) {
  'use strict';
  _openPenaltyEditor(k.Game, k.Team, k.Skater, k.Penalty);
}

function toPeriodJam(k, v) {
  'use strict';
  const prefix =
    'ScoreBoard.' +
    (k.Game ? 'Game(' + k.Game + ')' : 'CurrentGame') +
    '.Team(' +
    k.Team +
    ').Skater(' +
    k.Skater +
    ').Penalty(' +
    k.Penalty +
    ').';
  const pn = WS.state[prefix + 'PeriodNumber'];
  const jn = WS.state[prefix + 'JamNumber'];
  return pn && jn ? pn + '-' + jn : '\xa0';
}

function updateCurrentPeriodStyle() {
  'use strict';
  const prefix = _windowFunctions.getParam('game')
    ? 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').'
    : 'ScoreBoard.CurrentGame.';
  const periodNumber = WS.state[prefix + 'Clock(Period).Number'];
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
  const prefix = _windowFunctions.getParam('game')
    ? 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').'
    : 'ScoreBoard.CurrentGame.';
  const periodNumber = WS.state[prefix + 'Clock(Period).Number'];
  const jamNumber = WS.state[prefix + 'Clock(Jam).Number'];
  if (jamNumber == null || periodNumber == null) {
    return;
  }

  $('#current-jam-style').remove();
  $('<style> .Box[period="' + periodNumber + '"][jam="' + jamNumber + '"] { text-decoration: underline; } </style>')
    .attr('id', 'current-jam-style')
    .appendTo('head');
}

//###################################################################
//
//  Penalty Editor
//
//###################################################################

function _openPenaltyEditor(g, t, s, p) {
  'use strict';
  if (!g) {
    return; // Whiteboard
  }
  let prefix = 'ScoreBoard.Game(' + g + ').Team(' + t + ')';
  const teamName = WS.state[prefix + '.AlternateName(operator)'] || WS.state[prefix + '.UniformColor'] || WS.state[prefix + '.Name'];
  prefix = 'ScoreBoard.Game(' + g + ').Team(' + t + ').Skater(' + s + ')';
  const skaterName = WS.state[prefix + '.Name'];
  const skaterNumber = WS.state[prefix + '.RosterNumber'];

  p = Number(p);
  while (!isNaN(p) && p > 1 && WS.state[prefix + '.Penalty(' + (p - 1) + ').Code'] == null) {
    p--;
  }

  WS.SetupDialog($('#PenaltyEditor'), prefix + '.Penalty(' + p + ')', {
    modal: true,
    title: teamName + ' ' + skaterNumber + ' (' + skaterName + ')',
    width: '80%',
  });
}

function currentIfNull(k, v) {
  'use strict';
  return v || WS.state['ScoreBoard.Game(' + k.Game + ').CurrentPeriodNumber'];
}

function currentIfInvalid(k, v, elem) {
  'use strict';
  return elem.children('[value="' + v + '"]').length ? v : WS.state['ScoreBoard.Game(' + k.Game + ').Period(' + k.Period + ').CurrentJam'];
}

function adjust(k, v, elem) {
  'use strict';
  const dir = elem.attr('dir');
  elem.siblings('select').find(':selected')[dir]().prop('selected', true).parent().toggle('change');
}

function updateJam(k, v) {
  'use strict';
  WS.Set(
    'ScoreBoard.Game(' + k.Game + ').Team(' + k.Team + ').Skater(' + k.Skater + ').Penalty(' + k.Penalty + ').Jam',
    WS.state['ScoreBoard.Game(' + k.Game + ').Period(' + v + ').CurrentJam']
  );
  return v;
}

function toPenaltyCodeDisplay(k, v, elem, suffix) {
  'use strict';
  suffix = suffix || '';
  let output = '<div class="Code">' + k.PenaltyCode + suffix + '</div><div class="Description">';
  v.split(',').forEach(function (d) {
    output = output + '<div>' + d + '</div>';
  });
  output = output + '</div>';
  return output;
}

function toExpCodeDisplay(k, v, elem) {
  'use strict';
  return toPenaltyCodeDisplay(k, v, elem, '(EXP)');
}

//###################################################################
//
//  LT Annotation Editor
//
//###################################################################

function _openAnnotationEditor(gameId, teamId, skaterId) {
  'use strict';
  let position = WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Skater(' + skaterId + ').Position'];
  position = position.slice(position.lastIndexOf('_') + 1);
  let fieldingPrefix = ').TeamJam(' + teamId + ').Fielding(' + position + ')';
  if (isTrue(WS.state['ScoreBoard.Game(' + gameId + ').InJam'])) {
    fieldingPrefix =
      'ScoreBoard.Game(' +
      gameId +
      ').Period(' +
      WS.state['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber'] +
      ').Jam(' +
      WS.state[
        'ScoreBoard.Game(' + gameId + ').Period(' + WS.state['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber'] + ').CurrentJamNumber'
      ] +
      fieldingPrefix;
  } else {
    fieldingPrefix =
      'ScoreBoard.Game(' + gameId + ').Jam(' + WS.state['ScoreBoard.Game(' + gameId + ').UpcomingJamNumber'] + fieldingPrefix;
  }
  WS.SetupDialog($('#AnnotationEditor'), fieldingPrefix, {
    modal: true,
    title: 'Annotation & Box Trip Editor',
    width: '700px',
  });
}

function subAnn(k, v, elem) {
  'use strict';
  return 'Substitute for #' + elem.attr('rosterNumber');
}
