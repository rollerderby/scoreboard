WS.AfterLoad(function () {
  _windowFunctions.configureZoom();
  $('body')
    .attr('showTeam', _windowFunctions.getParam('team') || 'both')
    .attr('sbSheetStyle', _windowFunctions.getParam('pos') || 'plt')
    .attr('nextButton', _windowFunctions.getParam('next') || 'both')
    .attr('showNonSkaters', _windowFunctions.checkParam('nonskaters', '1') || null)
    .attr('noCalledBy', _windowFunctions.checkParam('nocallers', '1') || null)
    .attr('hideCopy', _windowFunctions.checkParam('hideCopy', '1') || null)
    .attr('swapTeams', _windowFunctions.checkParam('swapTeams', '1') || null);
  updateColspan();

  $('#OptionsDialog #OptionZoomable').toggleClass('sbActive', _windowFunctions.checkParam('zoomable', '1')).button();
  $('#OptionsDialog #OptionNonSkaters').toggleClass('sbActive', _windowFunctions.checkParam('nonskaters', '1')).button();
  $('#OptionsDialog #OptionNoCallers').toggleClass('sbActive', _windowFunctions.checkParam('nocallers', '1')).button();
  $('#OptionsDialog #OptionHideCopy').toggleClass('sbActive', _windowFunctions.checkParam('hideCopy', '1')).button();
  $('#OptionsDialog #OptionSwapTeams').toggleClass('sbActive', _windowFunctions.checkParam('swapTeams', '1')).button();
  $('#OptionsDialog [team="' + _windowFunctions.getParam('team') + '"]').addClass('sbActive');
  $('#OptionsDialog [pos="' + $('body').attr('sbSheetStyle') + '"]').addClass('sbActive');
  $('#OptionsDialog [next="' + $('body').attr('nextButton') + '"]').addClass('sbActive');
  $('#OptionsDialog').dialog({
    modal: true,
    closeOnEscape: true,
    title: 'Settings Editor',
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
    width: '500px',
    maxHeight: 0.9 * window.innerHeight,
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
    $('#UseLTDialog').dialog(!isTrue(v) && $('body[sbSheetStyle*="lt"]').length ? 'open' : 'close');
  });
});

function toTitle() {
  const pos = _windowFunctions.getParam('pos').toUpperCase();
  const team = _windowFunctions.getParam('team') || 'both';
  const prefix = 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').Team(' + team + ').';
  return (
    pos +
    ' ' +
    (team === 'both'
      ? 'both'
      : WS.state[prefix + 'AlternateName(plt)'] || WS.state[prefix + 'UniformColor'] || WS.state[prefix + 'Name'] || '') +
    ' | CRG ScoreBoard'
  );
}

function updateTitle() {
  $('title').text(toTitle());
}

function openOptionsDialog() {
  $('#OptionsDialog').dialog('open');
}

function updateColspan() {
  const pos = $('body').attr('sbSheetStyle');
  const nextAdj = $('body[nextButton="row"').length;
  const cols = pos === 'pt' ? 10 : ((pos === 'lt' ? 4 : 14) - nextAdj)
  $('.Teamname').attr('colspan', cols);
}

function setTeam(k, v, elem) {
  $('#OptionsDialog [team]').removeClass('sbActive');
  elem.addClass('sbActive');
  $('body').attr('showTeam', elem.attr('team'));
  _sbUpdateUrl('team', elem.attr('team'));
  updateTitle();
}

function setPos(k, v, elem) {
  $('#OptionsDialog [pos]').removeClass('sbActive');
  elem.addClass('sbActive');
  $('body').attr('sbSheetStyle', elem.attr('pos'));
  _sbUpdateUrl('pos', elem.attr('pos'));
  updateColspan();
  updateTitle();
}

function setNextJamBtn(k, v, elem) {
  $('#OptionsDialog [next]').removeClass('sbActive');
  elem.addClass('sbActive');
  $('body').attr('nextButton', elem.attr('next'));
  _sbUpdateUrl('next', elem.attr('next'));
  updateColspan();
}

function setNonSkaters(k, v, elem) {
  elem.toggleClass('sbActive');
  _sbUpdateUrl('nonskaters', elem.filter('.sbActive').length);
  $('body').attr('showNonSkaters', elem.hasClass('sbActive') || null);
}

function setNoCallers(k, v, elem) {
  elem.toggleClass('sbActive');
  _sbUpdateUrl('nocallers', elem.filter('.sbActive').length);
  $('body').attr('noCalledBy', elem.hasClass('sbActive') || null);
}

function setHideCopy(k, v, elem) {
  elem.toggleClass('sbActive');
  _sbUpdateUrl('hideCopy', elem.filter('.sbActive').length);
  $('body').attr('hideCopy', elem.hasClass('sbActive') || null);
}

function setSwapTeams(k, v, elem) {
  elem.toggleClass('sbActive');
  _sbUpdateUrl('swapTeams', elem.filter('.sbActive').length);
  $('body').attr('swapTeams', elem.hasClass('sbActive') || null);
}

function setZoom(k, v, elem) {
  elem.toggleClass('sbActive');
  _sbUpdateUrl('zoomable', elem.filter('.sbActive').length);
  _windowFunctions.configureZoom();
}

function advanceFieldings(k) {
  const team = $('body').attr('showTeam');
  if (team === 'both') {
    WS.Set(k + '.Team(1).AdvanceFieldings', true);
    WS.Set(k + '.Team(2).AdvanceFieldings', true);
  } else {
    WS.Set(k + '.Team(' + team + ').AdvanceFieldings', true);
  }
}
