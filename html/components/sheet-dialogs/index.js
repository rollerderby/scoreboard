function initDialogs() {
  'use strict';
  const gameId = _windowFunctions.getParam('game');

  if ($('#OptionsDialog').length) {
    $('#OptionsDialog #OptionZoomable').prop('checked', _windowFunctions.checkParam('zoomable', 1)).button();
    $('#OptionsDialog [team="' + _windowFunctions.getParam('team') + '"]').addClass('Active');
    $('#OptionsDialog').dialog({
      modal: true,
      closeOnEscape: true,
      title: 'Option Editor',
      buttons: [{ text: 'Save', click: setURL }],
      width: '500px',
      autoOpen: !_windowFunctions.hasParam('team') && $('.OneTeam').length,
    });
  }

  const useLTDialog = $('#UseLTDialog');
  if (useLTDialog.length) {
    useLTDialog.dialog({
      modal: true,
      closeOnEscape: false,
      title: 'Use Lineup Tracking',
      buttons: [
        {
          text: 'Enable',
          click: function () {
            WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)', true);
            useLTDialog.dialog('close');
          },
        },
      ],
      width: '300px',
      autoOpen: false,
    });
    WS.Register(['ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)'], function (k, v) {
      useLTDialog.dialog(isTrue(v) ? 'close' : 'open');
    });
  }

  const penaltyEditor = $('#PenaltyEditor');
  if (penaltyEditor.length) {
    WS.Register([
      'ScoreBoard.Game(' + gameId + ').Team(*).Skater(*).Name',
      'ScoreBoard.Game(' + gameId + ').Team(*).Skater(*).Penalty(*).Jam',
      'ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)',
      'ScoreBoard.Game(' + gameId + ').Period(*).CurrentJamNumber',
      'ScoreBoard.Game(' + gameId + ').Period(*).Jam(*).Id',
      'ScoreBoard.Game(' + gameId + ').UpcomingJam',
    ]);
    $('#PenaltyEditor .Period').on('change', function () {
      setupJamSelect();
    });

    WS.Register('ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber', function (k, v) {
      setupPeriodSelect(v);
    });

    penaltyEditor.dialog({
      modal: true,
      closeOnEscape: false,
      title: 'Penalty Editor',
      autoOpen: false,
      width: '80%',
    });
  }

  const annotationEditor = $('#AnnotationEditor');
  if (annotationEditor.length) {
    annotationEditor.find('#skaterList').controlgroup();
    annotationEditor.dialog({
      modal: true,
      closeOnEscape: false,
      title: 'Annotation & Box Trip Editor',
      autoOpen: false,
      width: '700px',
    });

    const teamId = _windowFunctions.getParam('team') || '*';
    WS.Register([
      'ScoreBoard.Game(' + gameId + ').Team(*).Skater(*).Position',
      'ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber',
      'ScoreBoard.Game(' + gameId + ').UpcomingJamNumber',
      'ScoreBoard.Game(' + gameId + ').Period(*).CurrentJamNumber',
      'ScoreBoard.Game(' + gameId + ').InJam',
      'ScoreBoard.Game(' + gameId + ').Jam(*).TeamJam(' + teamId + ').NoPivot',
      'ScoreBoard.Game(' + gameId + ').Jam(*).TeamJam(' + teamId + ').StarPass',
      'ScoreBoard.Game(' + gameId + ').Jam(*).TeamJam(' + teamId + ').Fielding(*)',
    ]);
  }

  const fieldingEditor = $('#FieldingEditor');
  if (fieldingEditor.length) {
    const teamId = _windowFunctions.getParam('team') || '*';
    WS.Register([
      'ScoreBoard.Game(' + gameId + ').Period(*).Jam(*).TeamJam(' + teamId + ').Fielding(*).Skater',
      'ScoreBoard.Game(' + gameId + ').Period(*).Jam(*).TeamJam(' + teamId + ').Fielding(*).SitFor3',
      'ScoreBoard.Game(' + gameId + ').Period(*).Jam(*).TeamJam(' + teamId + ').Fielding(*).NotFielded',
      'ScoreBoard.Game(' + gameId + ').Period(*).Jam(*).TeamJam(' + teamId + ').Fielding(*).CurrentBoxTrip',
      'ScoreBoard.Game(' + gameId + ').Period(*).Jam(*).TeamJam(' + teamId + ').Fielding(*).Annotation',
      'ScoreBoard.Game(' + gameId + ').Period(*).Jam(*).TeamJam(' + teamId + ').Fielding(*).Id',
    ]);
    fieldingEditor.find('#skaterList').controlgroup();
    fieldingEditor.dialog({ modal: true, closeOnEscape: false, title: 'Fielding Editor', autoOpen: false, width: '700px' });
  }
}

//###################################################################
//
//  Options Dialog
//
//###################################################################

function openOptionsDialog() {
  'use strict';
  $('#OptionsDialog').dialog('open');
}

function setURL() {
  'use strict';
  let url = new URL(window.location);
  url.searchParams.set('zoomable', $('#OptionsDialog #OptionZoomable.Active').length);
  const selectedTeamButton = $('[team].Active');
  if (selectedTeamButton.length) {
    url.searchParams.set('team', selectedTeamButton.attr('team'));
  }
  window.location.replace(url);
  optionsDialog.dialog('close');
}

function setTeam(elem) {
  $('[team]').removeClass('Active');
  elem.addClass('Active');
  setURL();
}

//###################################################################
//
//  Penalty Editor
//
//###################################################################

function openPenaltyEditor(g, t, s, p) {
  'use strict';
  let prefix = 'ScoreBoard.Game(' + g + ').Team(' + t + ')';
  const teamName = WS.state[prefix + '.AlternateName(operator)'] || WS.state[prefix + '.UniformColor'] || WS.state[prefix + '.Name'];

  prefix = 'ScoreBoard.Game(' + g + ').Team(' + t + ').Skater(' + s + ')';
  const skaterName = WS.state[prefix + '.Name'];
  const skaterNumber = WS.state[prefix + '.RosterNumber'];

  $('#PenaltyEditor .Codes>div').removeClass('Selected');

  $('#PenaltyEditor').dialog('option', 'title', teamName + ' ' + skaterNumber + ' (' + skaterName + ')');
  const periodNumber = WS.state['ScoreBoard.Game(' + g + ').CurrentPeriodNumber'];
  $('#PenaltyEditor .Period').val(periodNumber).trigger('change');
  $('#PenaltyEditor .Jam').val(WS.state['ScoreBoard.Game(' + g + ').Period(' + periodNumber + ').CurrentJam']);

  $('#PenaltyEditor .Codes>.Penalty').toggle(p !== '0');
  $('#PenaltyEditor .Codes>.FO_EXP').toggle(p === '0');

  const c = WS.state[prefix + '.Penalty(' + p + ').Code'];
  const per = WS.state[prefix + '.Penalty(' + p + ').PeriodNumber'];
  const j = WS.state[prefix + '.Penalty(' + p + ').Jam'];
  let penaltyNumber = p;
  let wasServed = !isTrue(WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)']);
  let isNew = true;

  if (c != null && j != null && per != null) {
    wasServed = isTrue(WS.state[prefix + '.Penalty(' + p + ').Served']);
    $('#PenaltyEditor .Codes>div.' + (p === '0' ? 'FO_EXP' : 'Penalty') + '[PenaltyCode="' + c + '"]').addClass('Selected');
    $('#PenaltyEditor .Period').val(per).trigger('change');
    $('#PenaltyEditor .Jam').val(j);
    isNew = false;
  }
  $('#PenaltyEditor #served').toggleClass('Active', wasServed);
  $('#PenaltyEditor #setJam').toggleClass('Hide', isNew);
  while (!isNaN(penaltyNumber) && penaltyNumber > 1 && WS.state[prefix + '.Penalty(' + (penaltyNumber - 1) + ').Code'] == null) {
    penaltyNumber--;
  }

  $('#PenaltyEditor').attr('sbPrefix', 'd:' + prefix + '.Penalty(' + penaltyNumber + ')');
  $('#PenaltyEditor').dialog('open');
}

function adjust(elem) {
  'use strict';
  const which = elem.attr('sel');
  const inc = elem.attr('inc');
  $('#PenaltyEditor .' + which + ' :selected')
    [inc]()
    .prop('selected', true);
  if (which === 'Period') {
    setupJamSelect();
  }
}

function setupPeriodSelect(num) {
  'use strict';
  const select = $('#PenaltyEditor .Period');
  select.empty();
  for (var i = 1; i <= num; i++) {
    $('<option>').attr('value', i).text(i).appendTo(select);
  }
}

function setupJamSelect() {
  'use strict';
  const gameId = _windowFunctions.getParam('game');
  var p = $('#PenaltyEditor .Period').val();
  var prefix = 'ScoreBoard.Game(' + gameId + ').Period(' + p + ').';
  var min = WS.state[prefix + 'CurrentJamNumber'];
  if (min == null) {
    min = 1;
  }
  while (WS.state[prefix + 'Jam(' + (min - 1) + ').Id'] != null) {
    min--;
  }
  var max = WS.state[prefix + 'CurrentJamNumber'];
  if (max == null) {
    max = 0;
  }
  while (WS.state[prefix + 'Jam(' + (max + 1) + ').Id'] != null) {
    max++;
  }
  var select = $('#PenaltyEditor .Jam');
  select.empty();
  for (var i = min; i <= max; i++) {
    $('<option>')
      .attr('value', WS.state[prefix + 'Jam(' + i + ').Id'])
      .text(i)
      .appendTo(select);
  }
  if (p >= WS.state['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber']) {
    $('<option>')
      .attr('value', WS.state['ScoreBoard.Game(' + gameId + ').UpcomingJam'])
      .text(max + 1)
      .appendTo(select);
  }
}

function closePenEd() {
  'use strict';
  $('#PenaltyEditor').dialog('close');
}

function toPenaltyCodeDisplay(k, v, suffix) {
  'use strict';
  suffix = suffix || '';
  let output = '<div class="Code">' + k.PenaltyCode + suffix + '</div><div class="Description">';
  v.split(',').forEach(function (d) {
    output = output + '<div>' + d + '</div>';
  });
  output = output + '</div>';
  return output;
}

function toExpCodeDisplay(k, v) {
  'use strict';
  return toPenaltyCodeDisplay(k, v, '(EXP)');
}

//###################################################################
//
//  LT Annotation Editor
//
//###################################################################

function openAnnotationEditor(gameId, teamId, skaterId) {
  'use strict';
  const annotationEditor = $('#AnnotationEditor');
  const prefix = 'ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Skater(' + skaterId + ').';
  const skaterNumber = WS.state[prefix + 'RosterNumber'];
  let position = WS.state[prefix + 'Position'];
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
  annotationEditor.data('skaterNumber', skaterNumber);
  annotationEditor.data('position', position);
  annotationEditor.attr('sbPrefix', 'd:' + fieldingPrefix);
  annotationEditor
    .find('#skaterList')
    .removeClass('Hide')
    .filter(':not([Team="' + teamId + '"])')
    .addClass('Hide')
    .end()
    .children()
    .removeClass('Hide')
    .filter('[skater="' + skaterId + '"]')
    .addClass('Hide');
  annotationEditor.find('#annotation').val(WS.state[fieldingPrefix + '.Annotation']);
  annotationEditor.find('.Box').toggleClass('Hide', WS.state[fieldingPrefix + '.CurrentBoxTrip'] === '');
  annotationEditor.find('.Box .Current').toggleClass('Hide', !isTrue(WS.state[prefix + 'PenaltyBox']));
  annotationEditor.find('.Box .Past').toggleClass('Hide', isTrue(WS.state[prefix + 'PenaltyBox']));
  annotationEditor.dialog('open');
}

function closeAnnEd() {
  'use strict';
  $('#AnnotationEditor').dialog('close');
}

function append(k, elem) {
  'use strict';
  const old = WS.state[k];
  closeAnnEd();
  return (old ? old + '; ' : '') + elem.text();
}

function subAnn() {
  'use strict';
  closeAnnEd();
  return 'Substitute for #' + $('#AnnotationEditor').data('skaterNumber');
}

//###################################################################
//
//  LT Fielding Editor
//
//###################################################################

function openFieldingEditor(g, p, j, t, pos, upcoming) {
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
  const notFielded = isTrue(WS.state[prefix + '.NotFielded']);

  $('#FieldingEditor').dialog('option', 'title', (upcoming ? 'Upcoming Jam' : 'Period ' + p + ' Jam ' + j) + ' ' + posName);
  $('#FieldingEditor [Team]').addClass('Hide');
  $('#FieldingEditor [Team="' + t + '"]').removeClass('Hide');
  $('#FieldingEditor [Skater]').removeClass('Active');
  if (!notFielded) {
    $('#FieldingEditor [Skater="' + (WS.state[prefix + '.Skater'] || '') + '"]').addClass('Active');
  }
  $('#FieldingEditor #notFielded').toggleClass('Active', notFielded);
  $('#FieldingEditor #sitFor3').toggleClass('Active', isTrue(WS.state[prefix + '.SitFor3']));
  $('#FieldingEditor #annotation').val(WS.state[prefix + '.Annotation']);
  $('#FieldingEditor #notFielded').toggleClass('Hide', isTrue(upcoming));
  $('#FieldingEditor .BoxTripComments').toggleClass('Hide', WS.state[prefix + '.CurrentBoxTrip'] === '');
  $('#FieldingEditor [BoxTrip]').addClass('Hide');
  $('#FieldingEditor [BoxTrip]:has([Fielding="' + WS.state[prefix + '.Id'] + '"])').removeClass('Hide');
  $('#FieldingEditor').attr('sbPrefix', 'd:' + prefix);
  $('#FieldingEditor').dialog('open');
}

function closeFieldEd() {
  $('#FieldingEditor').dialog('close');
}

function toggle(k, elem) {
  return elem.toggleClass('Active').hasClass('Active');
}

function toBtStartText(k, v) {
  const prefix = 'ScoreBoard.Game(' + k.Game + ').Team(' + k.Team + ').BoxTrip(' + k.BoxTrip + ').';
  const between = isTrue(WS.state[prefix + 'StartBetweenJams']);
  const afterSP = isTrue(WS.state[prefix + 'StartAfterSP']);
  return (between ? 'Before ' : '') + 'Jam ' + WS.state[prefix + 'StartJamNumber'] + (afterSP ? ' after SP' : '');
}

function toBtEndText(k, v) {
  const prefix = 'ScoreBoard.Game(' + k.Game + ').Team(' + k.Team + ').BoxTrip(' + k.BoxTrip + ').';
  const between = isTrue(WS.state[prefix + 'EndBetweenJams']);
  const afterSP = isTrue(WS.state[prefix + 'EndAfterSP']);
  const jam = WS.state[prefix + 'EndJamNumber'];
  return (between ? ' After ' : ' ') + (jam === 0 ? 'ongoing' : 'Jam ' + jam) + (afterSP && !between ? ' after SP ' : ' ');
}
