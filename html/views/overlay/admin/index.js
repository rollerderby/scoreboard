'use strict';

function _ovaSktrSort(a, b) {
  return $(b).attr('data-team') === $(a).attr('data-team')
    ? $(b).text() < $(a).text()
      ? 1
      : -1
    : $(b).attr('data-team') < $(a).attr('data-team')
    ? 1
    : -1;
}
jQuery.fn.sortOptions = function sortOptions() {
  $('> option', this[0]).sort(_ovaSktrSort).appendTo(this[0]);
};

let Skaters = new DataSet();
Skaters.AddTrigger('UPDATE', '*', {}, function (n, o, k) {
  'use strict';
  if (this.Name && this.Team && this.Id) {
    const att = { 'data-name': this.Name, 'data-team': this.Team, value: this.Skater };
    let option = $('#Skaters option[value="' + this.Skater + '"]');
    if (option.length === 0) {
      option = $('<option>').attr(att).appendTo('#Skaters');
    }
    option.attr(att).text(this.Name);
    $('#Skaters').sortOptions();
  }
});
Skaters.AddTrigger('DELETE', '*', {}, function (n, o, k) {
  'use strict';
  $('#Skaters option[value="' + this.Skater + '"]').remove();
});

let nextPanel = '';
let currrentPanel = '';

(function () {
  WS.Register(
    [
      'ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowLineups)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowAllNames)',
    ],
    function (k, v) {
      const useLineups = isTrue(WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)']);
      const showLineups = WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowLineups)'] === 'On';
      const showJammerNames = WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)'] === 'On';
      const showAllNames = WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowAllNames)'] === 'On';
      $('[data-setting="ScoreBoard.Settings.Setting(Overlay.Interactive.ShowLineups)').toggleClass('disabled', !useLineups);
      $('[data-setting="ScoreBoard.Settings.Setting(Overlay.Interactive.ShowAllNames)').toggleClass(
        'disabled',
        !useLineups || !showLineups
      );
      $('[data-setting="ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)').toggleClass(
        'disabled',
        useLineups && showLineups && showAllNames
      );
      $('#Preview')
        .toggleClass('Wide', useLineups && showLineups && showJammerNames && !showAllNames)
        .toggleClass('XWide', useLineups && showLineups && showAllNames);
    }
  );

  const skaterPrefix = 'ScoreBoard.CurrentGame.Team(*).Skater(*).';
  WS.Register([skaterPrefix + 'Id', skaterPrefix + 'Name', skaterPrefix + 'RosterNumber', skaterPrefix + 'Flags'], function (k, v) {
    let d = {};
    d[k.field] = v;
    d.Team = k.Team;
    if (k.field === 'Id' && v == null) {
      Skaters.Delete({ Skater: k.Skater });
    } else {
      Skaters.Upsert(d, { Skater: k.Skater });
    }
  });

  WS.Register('ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)', function (k, v) {
    currrentPanel = v;
    $('#PanelSet').toggleClass('changed', currrentPanel !== nextPanel);
    $('#PanelSet').toggleClass('current', currrentPanel !== '');
  });
  $('#PanelSelect').val('');

  WS.Register('ScoreBoard.CurrentGame.Team(*).Name');
})();

WS.AfterLoad(function () {
  $(document).on('keyup', function (e) {
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
  });
});

function ovaSelectPanel(k, v) {
  'use strict';
  if (v !== nextPanel) {
    nextPanel = v;
    $('#PanelSet').toggleClass('changed', nextPanel !== currrentPanel);
    $('#LowerThirdControls').toggleClass('sbHide', nextPanel !== 'LowerThird');
  }
}

function ovaSelectSkater(k, v) {
  const option = $('option[value=' + v + ']', $('select#Skaters'));
  const team = option.attr('data-team');
  const name = option.attr('data-name');
  const tnam = WS.state['ScoreBoard.CurrentGame.Team(' + team + ').Name'];

  WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line1)', name);
  WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line2)', tnam);
  WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Style)', 'ColourTeam' + team);
}

function ovaSelectKeeper(k, v) {
  const option = $('option[value="' + v + '"]', $('select#Keepers'));
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
    .attr('value', '_' + Math.random().toString(36).substr(2, 9))
    .text(line1 + '/' + line2 + ' (' + style + ')')
    .appendTo('#Keepers');
}

function ovaInvert(k) {
  return WS.state[k] === 'On' ? 'Off' : 'On';
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
