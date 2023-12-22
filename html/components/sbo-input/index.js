'use strict';

WS.Register(['ScoreBoeard.Game(' + _windowFunctions.getParam('game') + ').CurrentJam', 'ScoreBoard.Rulesets.Ruleset(*).Parent']);

function opToggleKeyEdit(k, v, elem) {
  elem.toggleClass('sbActive');
  _crgKeyControls.editKeys(elem.hasClass('sbActive'));
  $('#KeyEditHelp').toggleClass('sbHide', !elem.hasClass('sbActive'));
  elem.parent().siblings().addBack().removeClass('LastGroup').filter(':visible').last().addClass('LastGroup');
}

function opSuddenScoringDisabled(k) {
  return !isTrue(WS.state[k.upTo('Game') + '.RuleJam.SuddenScoring']) || WS.state[k.upTo('Game') + '.CurrentPeriodNumber'] < 2;
}

function _opSetOperatorSetting(name, value) {
  $('#' + name + '_Setting').toggleClass('sbActive', value);
  WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.Operator.' + _windowFunctions.getParam('operator') + '.' + name + ')', value);
  switch (name) {
    case 'ScoreAdjustments':
      $('#ScoreAdjustments').toggleClass('sbHide', !value);
      break;
    case 'TabBar':
      $('#tabBar').toggleClass('sbHide', !value);
      break;
  }
}

function opToggleOperatorSetting(k, v, elem) {
  _opSetOperatorSetting(elem.attr('id').split('_')[0], !elem.hasClass('sbActive'));
}

function opOpenNewGameDialog() {
  WS.SetupDialog($('#NewGameDialog'), 'ScoreBoard', {
    title: 'Start New Game',
    width: '80%',
    modal: true,
    buttons: {
      Cancel: function () {
        $(this).dialog('close');
      },
    },
  });
}

function opEnableStartButton(k, v, elem) {
  elem
    .siblings('button')
    .prop('disabled', v === '')
    .toggleClass('ui-button-disabled ui-state-disabled', v === '');
}

function opIsStartableGame(k, v) {
  return v === 'Finished' || k.Game === WS.state['ScoreBoard.CurrentGame.Game'];
}

function opFetchGame(k, v, elem) {
  return elem.siblings('select').val();
}

function opStartMidGame() {
  $('#NewGameDialog .MidGame').toggleClass('sbHide');
}

function opStartAdHoc(k, v, elem) {
  let startTime = $('#newStartTime').val();
  let intermissionClock = null;
  if (startTime !== '') {
    var now = new Date();
    var parts = startTime.split(':');
    startTime = new Date(now.getFullYear(), now.getMonth(), now.getDate(), parts[0], parts[1]);
    if (startTime < now) {
      startTime.setDate(startTime.getDate() + 1);
    }
    intermissionClock = '' + (startTime - now);
  }
  WS.Command('StartNewGame', {
    Team1: $('#newTeam1').val(),
    Team2: $('#newTeam2').val(),
    Ruleset: $('#newRuleset').val(),
    IntermissionClock: intermissionClock,
    Advance: $('#startMidGame').hasClass('sbHide'),
    Points1: Number($('#newPoints1').val()),
    Points2: Number($('#newPoints2').val()),
    TO1: Number($('#newTo1').val()),
    TO2: Number($('#newTo2').val()),
    OR1: Number($('#newOr1').val()),
    OR2: Number($('#newOr2').val()),
    Period: Number($('#newPeriod').val()),
    Jam: Number($('#newJam').val()),
    PeriodClock: sbFromLongTime($('#newPeriodClock').val()),
  });
  sbCloseDialog(k, v, elem);
}

function opClickOfficial(k) {
  const noPeriod = !isTrue(WS.state[k.upTo('Game') + '.InPeriod']);
  const last = WS.state[k.upTo('Game') + '.Rule(Period.Number)'] == WS.state[k.upTo('Game') + '.Clock(Period).Number'];
  const tie = WS.state[k.upTo('Game') + '.Team(1).Score'] === WS.state[k.upTo('Game') + '.Team(2).Score'];
  const official = isTrue(WS.state[k.upTo('Game') + '.OfficialScore']);
  return noPeriod && last && !tie && !official;
}

function opToggleEndOfPeriod(k, v, elem) {
  elem.toggleClass('sbActive');
  $('#EndOfPeriod').toggleClass('sbHide', !elem.hasClass('sbActive'));
  elem.parent().siblings().addBack().removeClass('LastGroup').filter(':visible').last().addClass('LastGroup');
}

function opClickEndOfPeriod(k, v, elem) {
  const noPeriod = !isTrue(WS.state[k.upTo('Game') + '.InPeriod']);
  const last = WS.state[k.upTo('Game') + '.Rule(Period.Number)'] == WS.state[k.upTo('Game') + '.Clock(Period).Number'];
  const official = isTrue(WS.state[k.upTo('Game') + '.OfficialScore']);
  return !elem.hasClass('sbActive') && noPeriod && last && !official;
}

function opClickOvertime(k) {
  const noPeriod = !isTrue(WS.state[k.upTo('Game') + '.InPeriod']);
  const last = WS.state[k.upTo('Game') + '.Rule(Period.Number)'] == WS.state[k.upTo('Game') + '.Clock(Period).Number'];
  const tie = WS.state[k.upTo('Game') + '.Team(1).Score'] === WS.state[k.upTo('Game') + '.Team(2).Score'];
  const official = isTrue(WS.state[k.upTo('Game') + '.OfficialScore']);
  return noPeriod && last && tie && !official;
}

function opOpenPeriodEndTimeoutDialog(k) {
  WS.SetupDialog($('#PeriodEndTimeoutDialog'), k, {
    title: 'Timeout before End of Period',
    width: '600px',
    modal: true,
    buttons: {
      'Start Timeout': function () {
        WS.Set(k + '.Timeout', true);
        WS.Set(k + '.Clock(Period).Time', sbFromTime($(this).find('input').val()));
        $(this).dialog('close');
      },
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function opOpenOvertimeDialog(k) {
  WS.SetupDialog($('#OvertimeDialog'), k, {
    title: 'Overtime',
    width: '600px',
    modal: true,
    buttons: {
      'Start Overtime Lineup clock': function () {
        WS.Set(k + '.StartOvertime', true);
        $(this).dialog('close');
      },
      Cancel: function () {
        $(this).dialog('close');
      },
    },
  });
}

function opPossiblInjCont(k) {
  return !isTrue(k.upTo('Game') + '.Rule(Jam.InjuryContinuation)') || !isTrue(k.upTo('Game') + '.Team(1).Injury');
}

function opUndo(k) {
  if ($('#ReplaceButton_Setting').hasClass('sbActive')) {
    WS.Set(k + '.ClockReplace', true);
  } else {
    WS.Set(k + '.ClockUndo', true);
  }
}

function opToAdjSort(k, v) {
  return (
    v +
    '_' +
    ('000' + WS.state[k.upTo('ScoreAdjustment') + '.JamNumberRecorded']).slice(-3) +
    '_' +
    (isTrue(WS.state[k.upTo('ScoreAdjustment' + '.RecordedDuringJam')]) ? '0' : '1')
  );
}

function opSetAdjSelects(k, v, elem) {
  elem.find('.Jam').val(WS.state[k + '.JamNumberRecorded'] - 1);
}

function opSelectLast(k, v, elem) {
  return elem.children().last().attr('value');
}

function opAfterRecording(k, v, elem) {
  return Number(v) > Number(elem.parent().parent().attr('Jam'));
}

function opUpdateTripSelects(k, v, elem) {
  const newSelect = elem
    .siblings('.Trip')
    .addClass('sbHide')
    .filter('[Jam="' + v + '"]');
  newSelect.val(newSelect.children().last().attr('value')).removeClass('sbHide');
}

function opNotSelectedJam(k, v, elem) {
  return v != elem.parent().parent().attr('Jam') - 1;
}

function opToTripNo(k, v) {
  return v == 1 ? 'In' : v;
}

function opGetAppliedTrip(k, v, elem) {
  return elem.siblings('.Trip:not(.sbHide)').val();
}

function opClockNumberDialog(k) {
  switch (k.Clock) {
    case 'Period':
    case 'Intermission':
      opOpenPeriodDialog(k);
      break;
    case 'Jam':
      opOpenJamDialog(k);
      break;
    case 'Timeout':
      opOpenTimeoutDialog(k);
      break;
  }
}

function opClockDialog(k) {
  WS.SetupDialog($('#ClockDialog'), k, {
    title: k.Clock + ' Clock',
    width: 400,
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function opSetTime(k, v, elem) {
  WS.Set(k + '.Time', sbFromLongTime(elem.siblings('input').val()));
  elem.siblings('input').val('');
}

function opOpenPeriodDialog(k) {
  WS.SetupDialog($('#PeriodDialog'), k.upTo('Game'), {
    title: 'Periods',
    width: 500,
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function opOpenJamDialog(k) {
  WS.SetupDialog($('#JamDialog'), k.upTo('Game') + '.Period(' + WS.state[k.upTo('Game') + '.CurrentPeriodNumber'] + ')', {
    title: 'Jams',
    width: 700,
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function opJamIsRunning(k) {
  return WS.state[k.upTo('Jam') + '.WalltimeEnd'] === 0 && WS.state[k.upTo('Jam') + '.WalltimeStart'] > 0;
}

function opHasPoints(k) {
  return (
    WS.state[k.upTo('Jam') + '.TeamJam(1).JamScore'] + WS.state[k.upTo('Jam') + '.TeamJam(1).OsOffset'] != 0 ||
    WS.state[k.upTo('Jam') + '.TeamJam(2).JamScore'] + WS.state[k.upTo('Jam') + '.TeamJam(2).OsOffset'] != 0
  );
}

function opInsertBeforeUpcoming(k) {
  WS.Set(k.upTo('Game') + '.Jam)' + WS.state[k.upTo('Game') + '.UpcomingJamNumber'] + ').InsertBefore', true);
}

function opOpenTimeoutDialog(k) {
  WS.SetupDialog($('#TimeoutDialog'), k.upTo('Game'), {
    title: 'Timeouts',
    width: 750,
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function opToToSortString(k, v) {
  return k.Period + '_' + ('00' + v).slice(-3) + '_' + WS.state[k.upTo('Timeout') + '.WalltimeStart'];
}

function opToCurentJam(k, v) {
  return WS.state[k.upTo('Game') + '.Period(' + v + ').CurrentJam'];
}

function opAddTimeout(k, v, elem) {
  WS.Set(k.upto('Game') + '.Period(' + elem.closest('tr').children('.Period').children('select').val() + ').InsertTimeout', true);
}
