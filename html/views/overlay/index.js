'use strict';

WS.Register(
  [
    'ScoreBoard.CurrentGame.Clock(Timeout).Running',
    'ScoreBoard.CurrentGame.TimeoutOwner',
    'ScoreBoard.CurrentGame.OfficialReview',
    'ScoreBoard.CurrentGame.Team(*).Timeouts',
  ],
  sbSetActiveTimeout
);

WS.Register(['ScoreBoard.CurrentGame.Clock(*).Running', 'ScoreBoard.CurrentGame.InJam'], sbClockSelect);

WS.Register('ScoreBoard.CurrentGame.Rule(Penalties.NumberToFoulout)');

WS.AfterLoad(function () {
  $('body').removeClass('preload');
});

function _ovlToggleSetting(s) {
  WS.Set(
    'ScoreBoard.Settings.Setting(Overlay.Interactive.' + s + ')',
    !isTrue(WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.' + s + ')'])
  );
}

function _ovlTogglePanel(p) {
  WS.Set(
    'ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)',
    WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)'] === p ? '' : p
  );
}

function ovlHandleKey(k, v, elem, e) {
  switch (e.which) {
    case 74: // j
      _ovlToggleSetting('ShowJammers');
      break;
    case 76: // l
      _ovlToggleSetting('ShowLineups');
      break;
    case 78: // n
      _ovlToggleSetting('ShowAllNames');
      break;
    case 80: // p
      _ovlToggleSetting('ShowPenaltyClocks');
      break;
    case 67: // c
      _ovlToggleSetting('Clock');
      break;
    case 83: // s
      _ovlToggleSetting('Score');
      break;
    case 48: // 0
      _ovlTogglePanel('PPJBox');
      break;
    case 49: // 1
      _ovlTogglePanel('RosterTeam1');
      break;
    case 50: // 2
      _ovlTogglePanel('RosterTeam2');
      break;
    case 51: // 3
      _ovlTogglePanel('PenaltyTeam1');
      break;
    case 52: // 4
      _ovlTogglePanel('PenaltyTeam2');
      break;
    case 57: // 9
      _ovlTogglePanel('LowerThird');
      break;
    case 85: // u
      _ovlTogglePanel('Upcoming');
      break;
    case 32: // space
      WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)', '');
      break;
  }
}

function ovlToBackground(k, v) {
  return v || 'transparent';
}

function ovlToIndicator(k, v) {
  var prefix = k.substring(0, k.lastIndexOf('.'));
  return isTrue(WS.state[prefix + '.StarPass'])
    ? 'SP'
    : isTrue(WS.state[prefix + '.Lost'])
    ? ''
    : isTrue(WS.state[prefix + '.Lead'])
    ? '★'
    : '';
}

function ovlIsJamming(k, v, elem) {
  return (isTrue(v) && elem.attr('Position') === 'Pivot') || (!isTrue(v) && elem.attr('Position') === 'Jammer');
}

function ovlToPpjColumnWidth(k, v, elem) {
  let ne1 = $('.PPJBox [Team="1"] .GraphBlock').length;
  const ne2 = $('.PPJBox [Team="2"] .GraphBlock').length;
  if (ne2 > ne1) {
    ne1 = ne2;
  }
  const wid = parseInt(elem.parent().parent().innerWidth());
  const newWidth = parseInt(wid / ne1) - 4;
  $('.ColumnWidth').css('width', newWidth);

  return newWidth;
}

function ovlToPpjMargin(k, v, elem) {
  if (k.TeamJam === '2') {
    return 0;
  }
  return parseInt(elem.parent().innerHeight()) - v * 4;
}

function ovlToLowerThirdColorFg() {
  return _ovlToLowerThirdColor('overlay.fg');
}

function ovlToLowerThirdColorBg() {
  return _ovlToLowerThirdColor('overlay.bg');
}

function _ovlToLowerThirdColor(type) {
  switch (WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Style)']) {
    case 'ColourTeam1':
      return WS.state['ScoreBoard.CurrentGame.Team(1).Color(' + type + ')'];
    case 'ColourTeam2':
      return WS.state['ScoreBoard.CurrentGame.Team(2).Color(' + type + ')'];
    default:
      return '';
  }
}

function ovlToClockType() {
  let ret;
  const to = WS.state['ScoreBoard.CurrentGame.TimeoutOwner'];
  const or = WS.state['ScoreBoard.CurrentGame.OfficialReview'];
  const tc = WS.state['ScoreBoard.CurrentGame.Clock(Timeout).Running'];
  const lc = WS.state['ScoreBoard.CurrentGame.Clock(Lineup).Running'];
  const ic = WS.state['ScoreBoard.CurrentGame.Clock(Intermission).Running'];
  const jc = WS.state['ScoreBoard.CurrentGame.InJam'];

  if (tc) {
    ret = WS.state['ScoreBoard.CurrentGame.Clock(Timeout).Name'];
    if (to !== '' && to !== 'O' && or) {
      ret = 'Official Review';
    }
    if (to !== '' && to !== 'O' && !or) {
      ret = 'Team Timeout';
    }
    if (to === 'O') {
      ret = 'Official Timeout';
    }
    $('.ClockDescription').css('backgroundColor', 'red');
  } else if (lc) {
    ret = WS.state['ScoreBoard.CurrentGame.Clock(Lineup).Name'];
    $('.ClockDescription').css('backgroundColor', '#888');
  } else if (ic) {
    const num = WS.state['ScoreBoard.CurrentGame.Clock(Intermission).Number'];
    const max = WS.state['ScoreBoard.CurrentGame.Rule(Period.Number)'];
    const isOfficial = WS.state['ScoreBoard.CurrentGame.OfficialScore'];
    const showDuringOfficial = WS.state['ScoreBoard.CurrentGame.ClockDuringFinalScore'];
    if (isOfficial) {
      if (showDuringOfficial) {
        ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.OfficialWithClock)'];
      } else {
        ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Official)'];
      }
    } else if (num === 0) {
      ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.PreGame)'];
    } else if (num != max) {
      ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Intermission)'];
    } else if (!isOfficial) {
      ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Unofficial)'];
    }

    $('.ClockDescription').css('backgroundColor', 'blue');
  } else if (jc) {
    ret = 'Jam';
    $('.ClockDescription').css('backgroundColor', '#888');
  } else {
    ret = 'Coming Up';
    $('.ClockDescription').css('backgroundColor', 'blue');
  }

  return ret;
}
