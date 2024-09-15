var nextPanel = '';
var currrentPanel = '';

(function () {
  $('#PanelSelect').val('');
})();

function ovaKeyHandler(k, v, elem, e) {
  var tag = e.target.tagName.toLowerCase();
  var c = String.fromCharCode(e.keyCode || e.charCode).toUpperCase();
  if (e.keyCode === 27) {
    $('body').focus();
    e.preventDefault();
    return false;
  }
  if (tag !== 'input' && tag !== 'textarea') {
    $('[data-key="' + c + '"]').each(function () {
      var $t = $(this);
      if ($t.prop('tagName') === 'OPTION') {
        $t.attr('selected', 'selected').parent().trigger('change');
      }
      if ($t.prop('tagName') === 'BUTTON') {
        $t.trigger('click');
      }
    });
    e.preventDefault();
  }
}

function ovaLineups() {
  return (
    isTrue(WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)']) &&
    isTrue(WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowLineups)'])
  );
}

function ovaNoLineups() {
  return !ovaLineups;
}

function ovaNobody() {
  return ovaNoLineups && !isTrue(WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)']);
}

function ovaUpdatePanel(k, v, elem) {
  currrentPanel = v;
  elem.toggleClass('changed', currrentPanel !== nextPanel);
  return currrentPanel !== '';
}

function ovaSelectPanel(k, v) {
  if (v !== nextPanel) {
    nextPanel = v;
    $('#PanelSet').toggleClass('changed', nextPanel !== currrentPanel);
    $('#LowerThirdControls').toggleClass('sbHide', nextPanel !== 'LowerThird');
  }
}

function ovaSelectLowerThird(k, v, elem) {
  const option = elem.children('option[value="' + v + '"]');
  WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line1)', option.attr('data-line1'));
  WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line2)', option.attr('data-line2'));
  WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Style)', option.attr('data-style'));
}

function ovaAddKeeper() {
  const line1 = WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line1)'];
  const line2 = WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line2)'];
  const style = WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Style)'];

  $('<option>')
    .attr('data-line1', line1)
    .attr('data-line2', line2)
    .attr('data-style', style)
    .attr('value', '_' + Math.random().toString(36).substring(2, 11))
    .text(line1 + '/' + line2 + ' (' + style + ')')
    .appendTo('#Keepers');
}

function ovaGetNextPanel() {
  return nextPanel === currrentPanel ? '' : nextPanel;
}

function ovaDefaultFgIfNull(k, v) {
  return v || '#FFFFFF';
}

function ovaDefaultBgIfNull(k, v) {
  return v || '#333333';
}

function ovaSetPreview(k, v, elem) {
  $('#Preview>iframe').css(elem.attr('dim'), v);
}
