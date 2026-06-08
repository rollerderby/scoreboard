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
          .find('tr.Skater:not([role="NotInGame"]), tr.NonSkater:not(.sbHide)')
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
    prefix + 'Team(*).Position(*)',
    prefix + 'Team(*).AllBlockersSet',
  ]);
})();

function _pltGetFieldingPrefix(gameId, teamId, skaterId) {
  var position = WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Skater(' + skaterId + ').Position'];
  position = position.slice(position.lastIndexOf('_') + 1);
  var fieldingPrefix = ').TeamJam(' + teamId + ').Fielding(' + position + ')';
  if (
    isTrue(WS.state['ScoreBoard.Game(' + gameId + ').InJam']) ||
    isTrue(WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').FieldingAdvancePending'])
  ) {
    fieldingPrefix =
      'ScoreBoard.Game(' +
      gameId +
      ').Period(' +
      WS.state['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber'] +
      ').Jam(' +
      WS.state['ScoreBoard.Game(' + gameId + ').Period(' + WS.state['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber'] + ').CurrentJamNumber'] +
      fieldingPrefix;
  } else {
    fieldingPrefix = 'ScoreBoard.Game(' + gameId + ').Jam(' + WS.state['ScoreBoard.Game(' + gameId + ').UpcomingJamNumber'] + fieldingPrefix;
  }
  return fieldingPrefix;
}

function pltMaySub(k) {
  return isTrue(WS.state[k.upTo('Skater') + '.PenaltyBox']) || isTrue(WS.state[k.upTo('Skater') + '.HasUnserved']);
}

function _pltSetRole(k, role, ignoreIneligible, ignoreRoleChange) {
  const oldRole = WS.state[k + '.Role'];
  if (oldRole === 'Ineligible' && !ignoreIneligible) {
    _pltOpenIneligibleDialog(k, role);
  } else if (pltMaySub(k) && !ignoreRoleChange) {
    _pltOpenNoRoleChangeDialog(k, role);
  } else if (
    (role === 'Jammer' || role === 'Pivot') &&
    (isTrue(WS.state[k.upTo('Team') + '.Position(' + role + ').PenaltyBox']) ||
      isTrue(WS.state[k.upTo('Team') + '.Position(' + role + ').HasUnserved']))
  ) {
    _pltOpenSubstituteDialog(k, role);
  } else if (
    ((role === 'Blocker' && oldRole !== 'Pivot') || (role === 'Pivot' && isTrue(WS.state[k.upTo('Team') + '.NoPivot']))) &&
    isTrue(WS.state[k.upTo('Team') + '.AllBlockersSet']) &&
    oldRole !== 'Blocker'
  ) {
    _pltOpenReplaceDialog(k, role);
  } else {
    WS.Set(k + '.Role', role);
  }
}

function pltSetJammer(k) {
  _pltSetRole(k, 'Jammer', false, false);
}

function pltSetPivot(k) {
  _pltSetRole(k, 'Pivot', false, false);
}

function pltSetBlocker(k) {
  _pltSetRole(k, 'Blocker', false, false);
}

function pltToBoxString(k, v) {
  return isTrue(v) ? '' : 'Box';
}

function pltToggleBox(k, v, elem) {
  const fieldingPath = _pltGetFieldingPrefix(k.Game, k.Team, k.Skater);
  if (elem.hasClass('Serving')) {
    WS.Set(k + '.PenaltyBox', false);
  } else if (WS.state[fieldingPath + '.CurrentBoxTrip'] == null || isTrue(WS.state[k + '.HasUnserved'])) {
    WS.Set(k + '.PenaltyBox', true);
  } else {
    pltOpenUnendDialog(fieldingPath);
  }
}

function pltAdvance(k, v, elem) {
  if (elem.hasClass('Show')) {
    WS.Set(k.upTo('Team') + '.AdvanceFieldings', true);
  }
}

function pltAnnotation(k, v, elem) {
  if (elem.hasClass('Show')) {
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
  const prefix = _windowFunctions.getParam('game') ? 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').' : 'ScoreBoard.CurrentGame.';
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
  const prefix = _windowFunctions.getParam('game') ? 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').' : 'ScoreBoard.CurrentGame.';
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

let pltReplacePath = '';
let pltReplaceTarget = '';

function _pltOpenReplaceDialog(k, pos) {
  pltReplacePath = k + '.Role';
  pltReplaceTarget = pos;
  WS.SetupDialog($('#BlockerReplaceDialog'), k, { modal: true, title: 'Replace Blocker', width: '400px' });
}

function pltFinishReplace(k, v, elem, event) {
  sbCloseDialog(k, v, elem, event);
  WS.Set(pltReplacePath, pltReplaceTarget);
}

//###################################################################
//
//  Ineligible Dialog
//
//###################################################################

var pltIneligiblePath = '';
var pltIneligibleTarget = '';

function _pltOpenIneligibleDialog(k, role) {
  pltIneligiblePath = k;
  pltIneligibleTarget = role;
  WS.SetupDialog($('#IneligibleDialog'), k, { modal: true, title: 'Ineligible Skater', width: '400px' });
}

function pltToIneligibleReason(k, v) {
  if (v == null) {
    return 'an injury calloff (or similar)';
  } else if (v === 'FO') {
    return 'a foulout';
  } else if (v === 'RE') {
    return 'removal for safety concerns';
  } else {
    return 'an expulsion';
  }
}

function pltIgnoreIneligible(k, v, elem, event) {
  sbCloseDialog(k, v, elem, event);
  _pltSetRole(pltIneligiblePath, pltIneligibleTarget, true, false);
}

//###################################################################
//
//  No Role Change Dialog
//
//###################################################################

var pltRoleChangePath = '';
var pltRoleChangeTarget = '';

function _pltOpenNoRoleChangeDialog(k, role) {
  pltRoleChangePath = k;
  pltRoleChangeTarget = role;
  WS.SetupDialog($('#NoRoleChangeDialog'), k, { modal: true, title: 'Role Change not Allowed', width: '400px' });
}

function pltIgnoreRoleChange(k, v, elem, event) {
  sbCloseDialog(k, v, elem, event);
  _pltSetRole(pltRoleChangePath, pltRoleChangeTarget, true, true);
}

//###################################################################
//
//  Substitute Dialog
//
//###################################################################

var pltSubstituteSkater = '';
var pltSubstituteTarget = '';

function _pltOpenSubstituteDialog(k, role) {
  pltSubstituteSkater = k.Skater;
  pltSubstituteTarget = ').TeamJam(' + k.Team + ').Fielding(' + role + ')';
  if (isTrue(WS.state['ScoreBoard.Game(' + k.Game + ').InJam']) || isTrue(WS.state[k.upTo('Team') + '.FieldingAdvancePending'])) {
    pltSubstituteTarget =
      k.upTo('Game') +
      '.Period(' +
      WS.state[k.upTo('Game') + '.CurrentPeriodNumber'] +
      ').Jam(' +
      WS.state[k.upTo('Game') + '.Period(' + WS.state[k.upTo('Game') + '.CurrentPeriodNumber'] + ').CurrentJamNumber'] +
      pltSubstituteTarget;
  } else {
    pltSubstituteTarget = k.upTo('Game') + '.Jam(' + WS.state[k.upTo('Game') + '.UpcomingJamNumber'] + pltSubstituteTarget;
  }
  WS.SetupDialog($('#SubstituteDialog'), pltSubstituteTarget, { modal: true, title: 'Substitute Skater?', width: '400px' });
}

function pltToPosition(k) {
  return k.Fielding;
}

function pltDoSubstitute(k, v, elem, event) {
  sbCloseDialog(k, v, elem, event);
  console.log(pltSubstituteTarget, pltSubstituteSkater);
  WS.Set(pltSubstituteTarget + '.Skater', pltSubstituteSkater, 'change');
}

//###################################################################
//
//  Unend Dialog
//
//###################################################################

function pltOpenUnendDialog(k) {
  WS.SetupDialog($('#UnendDialog'), k, {
    modal: true,
    title: 'Continue Box Trip?',
    width: '400px',
    buttons: {
      Cancel: function () {
        $(this).dialog('close');
      },
    },
  });
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
  var prefix = 'ScoreBoard.Game(' + g + ').Team(' + t + ')';
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
    width: Math.min(1200, 0.9 * window.innerWidth),
  });
}

function pltCurrentIfNull(k, v) {
  return v || WS.state['ScoreBoard.Game(' + k.Game + ').CurrentPeriodNumber'];
}

function pltCurrentIfInvalid(k, v, elem) {
  return elem.children('[value="' + v + '"]').length
    ? v
    : WS.state[k.upTo('Period') + '.CurrentJam'] ||
        WS.state['ScoreBoard.Game(' + k.Game + ').Period(' + WS.state['ScoreBoard.Game(' + k.Game + ').CurrentPeriodNumber'] + ').CurrentJam'];
}

function pltIsThisPeriod(k, v, elem) {
  return (
    (v != null && v != elem.attr('Period')) || (v == null && WS.state['ScoreBoard.Game(' + k.Game + ').CurrentPeriodNumber'] != elem.attr('Period'))
  );
}

function pltAdjust(k, v, elem) {
  const dir = elem.attr('dir');
  elem.siblings('select').find(':selected')[dir]().prop('selected', true).parent().trigger('change');
}

function pltUpdateJam(k, v) {
  WS.Set(k.upTo('Penalty') + '.Jam', WS.state['ScoreBoard.Game(' + k.Game + ').Period(' + v + ').CurrentJam']);
  return v;
}

function pltIsFoExp(k) {
  return k.Penalty == 0;
}

function pltIsNotFoExp(k) {
  return k.Penalty != 0;
}

function pltToPenaltyCodeDisplay(k, v, elem, suffix) {
  suffix = suffix || '';
  var output = '<div class="Code">' + k.PenaltyCode + suffix + '</div><div class="Description">';
  v.split(',').forEach(function (d) {
    output = output + '<div>' + d + '</div>';
  });
  output = output + '</div>';
  return output;
}

function pltToExpCodeDisplay(k, v, elem) {
  return pltToPenaltyCodeDisplay(k, v, elem, ' (EXP)');
}

function pltToggleReassign(k, v, elem) {
  elem.closest('.CalledBy').toggleClass('Reassign');
  elem.toggleClass('sbActive', elem.closest('.CalledBy').hasClass('Reassign'));
}

function pltFilterOfficial(k, v, elem) {
  const targetPosition = elem.parent().parent().attr('OfficialPosition');
  const officialsPosition = v.match(/\p{Lu}/gu).join();
  return targetPosition.startsWith(officialsPosition);
}

//###################################################################
//
//  LT Annotation Editor
//
//###################################################################

function _pltOpenAnnotationEditor(gameId, teamId, skaterId) {
  WS.SetupDialog($('#AnnotationEditor'), _pltGetFieldingPrefix(gameId, teamId, skaterId), {
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

function pltSubable(k) {
  return !isTrue(WS.state[k.upTo('Fielding') + '.PenaltyBox']) && !isTrue(WS.state[k.upTo('Fielding') + '.HasUnserved']);
}

function pltNoUnend(k, v) {
  return isTrue(v) || WS.state[k.upTo('Fielding') + '.CurrentBoxTrip'] == null;
}
