'use strict';

(function () {
  const gameId = _windowFunctions.getParam('game');
  const prefix = gameId ? 'ScoreBoard.Game(' + gameId + ').' : 'ScoreBoard.CurrentGame.';

  WS.Register([prefix + 'Rule(Penalties.NumberToFoulout)']);
  WS.Register([prefix + 'Clock(Period).Number'], _pltUpdateCurrentPeriodStyle);
  WS.Register([prefix + 'Clock(Jam).Number'], _pltUpdateCurrentJamStyle);
  WS.Register(prefix + 'Team(*).Skater(*).Role', {
    triggerBatchFunc: function () {
      $('.PltInput>.Team').each(function (i, e) {
        $(e)
          .find('tr.Skater:not([role="NotInGame])')
          .each(function (idx) {
            $(this).toggleClass('Darker', idx % 4 === 2 || idx % 4 === 3);
          });
      });
    },
  });

  WS.Register([
    prefix + 'Team(*).Skater(*).Name',
    prefix + 'Period(*).CurrentJam',
    prefix + 'CurrentPeriodNumber',
    prefix + 'Period(*).CurrentJamNumber',
    prefix + 'Team(*).Skater(*).Position',
    prefix + 'Team(*).Position(*).',
    prefix + 'Team(*).AllBlockersSet',
  ]);
})();

function pltSetBlocker(k) {
  if (isTrue(WS.state[k.upTo('Team') + '.AllBlockersSet'])) {
    _pltOpenReplaceDialog(k);
  } else {
    WS.Set(k + '.Role', 'Blocker');
  }
}

function pltAdvanceOrAnnotation(k, v, elem) {
  if (elem.hasClass('Advance')) {
    WS.Set(k.upTo('Team') + '.AdvanceFieldings', true);
  } else if (elem.hasClass('Show')) {
    _pltOpenAnnotationEditor(k.Game, k.Team, k.Skater);
  }
}

function pltAddPenalty(k) {
  _pltOpenPenaltyEditor(k.Game, k.Team, k.Skater, 99);
}

function pltEditPenalty(k) {
  _pltOpenPenaltyEditor(k.Game, k.Team, k.Skater, k.Penalty);
}

function pltToPeriodJam(k) {
  const prefix = k.upTo('Penalty');
  const pn = WS.state[prefix + '.PeriodNumber'];
  const jn = WS.state[prefix + '.JamNumber'];
  return pn && jn ? pn + '-' + jn : '\xa0';
}

function _pltUpdateCurrentPeriodStyle() {
  const prefix = _windowFunctions.getParam('game')
    ? 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').'
    : 'ScoreBoard.CurrentGame.';
  const periodNumber = WS.state[prefix + 'Clock(Period).Number'];
  if (periodNumber == null) {
    return;
  }

  $('#current-period-style').remove();
  $('<style> .Penalty[period="' + periodNumber + '"] { font-weight: bold; color: #000; }</style>')
    .attr('id', 'current-period-style')
    .appendTo('head');

  _pltUpdateCurrentJamStyle();
}

function _pltUpdateCurrentJamStyle() {
  const prefix = _windowFunctions.getParam('game')
    ? 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').'
    : 'ScoreBoard.CurrentGame.';
  const periodNumber = WS.state[prefix + 'Clock(Period).Number'];
  const jamNumber = WS.state[prefix + 'Clock(Jam).Number'];
  if (jamNumber == null || periodNumber == null) {
    return;
  }

  $('#current-jam-style').remove();
  $('<style> .Penalty[period="' + periodNumber + '"][jam="' + jamNumber + '"] { text-decoration: underline; } </style>')
    .attr('id', 'current-jam-style')
    .appendTo('head');
}

//###################################################################
//
//  Replace Dialog
//
//###################################################################

function _pltOpenReplaceDialog(k) {
  WS.SetupDialog($('#BlockerReplaceDialog'), k, { modal: true, title: 'Replace Blocker', width: '400px' });
}

//###################################################################
//
//  Penalty Editor
//
//###################################################################

function _pltOpenPenaltyEditor(g, t, s, p) {
  if (!g) {
    return; // Whiteboard
  }
  let prefix = 'ScoreBoard.Game(' + g + ').Team(' + t + ')';
  const teamName = WS.state[prefix + '.AlternateName(plt)'] || WS.state[prefix + '.UniformColor'] || WS.state[prefix + '.Name'];
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

function pltCurrentIfNull(k, v) {
  return v || WS.state['ScoreBoard.Game(' + k.Game + ').CurrentPeriodNumber'];
}

function pltCurrentIfInvalid(k, v, elem) {
  return elem.children('[value="' + v + '"]').length ? v : WS.state[k.upTo('Period') + '.CurrentJam'];
}

function pltAdjust(k, v, elem) {
  const dir = elem.attr('dir');
  elem.siblings('select').find(':selected')[dir]().prop('selected', true).parent().trigger('change');
}

function pltUpdateJam(k, v) {
  console.log(k, WS.state['ScoreBoard.Game(' + k.Game + ').Period(' + v + ').CurrentJam']);
  WS.Set(k.upTo('Penalty') + '.Jam', WS.state['ScoreBoard.Game(' + k.Game + ').Period(' + v + ').CurrentJam']);
  return v;
}

function pltToPenaltyCodeDisplay(k, v, elem, suffix) {
  suffix = suffix || '';
  let output = '<div class="Code">' + k.PenaltyCode + suffix + '</div><div class="Description">';
  v.split(',').forEach(function (d) {
    output = output + '<div>' + d + '</div>';
  });
  output = output + '</div>';
  return output;
}

function pltToExpCodeDisplay(k, v, elem) {
  return pltToPenaltyCodeDisplay(k, v, elem, '(EXP)');
}

//###################################################################
//
//  LT Annotation Editor
//
//###################################################################

function _pltOpenAnnotationEditor(gameId, teamId, skaterId) {
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
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function pltNoUnend(k, v) {
  return isTrue(v) || WS.state[k.upTo('Fielding') + '.CurrentBoxTrip'] == null;
}
