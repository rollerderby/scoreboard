(function () {
  'use strict';
  _windowFunctions.configureZoom();
  $('body')
    .attr('showTeam', _windowFunctions.getParam('team'))
    .attr('context', _windowFunctions.getParam('pos') || 'plt');

  $('#OptionsDialog #OptionZoomable').prop('checked', _windowFunctions.checkParam('zoomable', 1)).button();
  $('#OptionsDialog [team="' + _windowFunctions.getParam('team') + '"]').addClass('Active');
  $('#OptionsDialog [pos="' + $('body').attr('context') + '"]').addClass('Active');
  $('#OptionsDialog').dialog({
    modal: true,
    closeOnEscape: true,
    title: 'Option Editor',
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
    width: '500px',
    autoOpen: !_windowFunctions.hasParam('team'),
  });

  $('#UseLTDialog').dialog({
    modal: true,
    closeOnEscape: false,
    title: 'Use Lineup Tracking',
    buttons: {
      Enable: function () {
        WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)', true);
      },
    },
    width: '300px',
    autoOpen: false,
  });
  WS.Register(['ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)'], function (k, v) {
    $('#UseLTDialog').dialog(!isTrue(v) && $('body[context*="lt"]').length ? 'open' : 'close');
  });
})();

function toTitle() {
  const pos = $('body').attr('context').toUpperCase();
  const team = $('body').attr('showTeam');
  const prefix = 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').Team(' + team + ').';
  return (
    pos +
    ' ' +
    (team === 'both'
      ? 'both'
      : WS.state[prefix + 'AlternateName(operator)'] || WS.state[prefix + 'UniformColor'] || WS.state[prefix + 'Name']) +
    ' CRG ScoreBoard'
  );
}

function openOptionsDialog() {
  'use strict';
  $('#OptionsDialog').dialog('open');
}

function setTeam(k, v, elem) {
  $('#OptionsDialog [team]').removeClass('Active');
  elem.addClass('Active');
  $('body').attr('showTeam', elem.attr('team'));
  window.history.replaceState(null, '', new URL(window.location).searchParams.set('team', elem.attr('team')));
}

function setPos(k, v, elem) {
  $('#OptionsDialog [pos]').removeClass('Active');
  elem.addClass('Active');
  $('body').attr('context', elem.attr('pos'));
  window.history.replaceState(null, '', new URL(window.location).searchParams.set('pos', elem.attr('pos')));
}

function setZoom(k, v, elem) {
  'use strict';
  elem.toggleClass('Active');
  window.history.replaceState(null, '', new URL(window.location).searchParams.set('zoomable', elem.filter('.Active').length));
  _windowFunctions.configureZoom();
}
