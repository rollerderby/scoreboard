function setupGameAdvance(element, gameId, auto) {
  'use strict';
  element
    .addClass('Hide GameAdvance clickMe')
    .text('Go To Current Game')
    .button()
    .click(function () {
      window.location.href = window.location.href.replace(/game=[^&]*(&|$)|$/, 'game=' + WS.state['ScoreBoard.CurrentGame.Game']);
    });

  WS.Register('ScoreBoard.CurrentGame.Game', function (k, v) {
    var change = !(v === gameId || v == null);
    element.toggleClass('Hide', !change);
    if (auto && change) {
      window.location.href = window.location.href.replace(/game=[^&]*(&|$)|$/, 'game=' + v);
    }
  });
}

function createTeamTimeTab(tab, gameId) {
  'use strict';
  var table = $('<table>').attr('id', 'TeamTime').appendTo(tab);

  $('<tr><td/></tr>').appendTo(table).children('td').append(createMetaControlTable(gameId));
  $('<tr><td/></tr>').appendTo(table).children('td').append(createJamControlTable(gameId));
  $('<tr><td/></tr>').appendTo(table).children('td').append(createTeamTable(gameId));
  $('<tr><td/></tr>').appendTo(table).children('td').append(createTimeTable(gameId));
  table.children('tr').children('td').children('table').addClass('TabTable');

  var sk1 = $('<div>').addClass('SKSheet').appendTo(tab);
  var sk2 = $('<div>').addClass('SKSheet').appendTo(tab);
  $('<div>').attr('id', 'TripEditor').appendTo(tab);
  $('<div>').attr('id', 'skaterSelector').appendTo(tab);
  $('<div>').attr('id', 'osOffsetEditor').appendTo(tab);
  prepareSkSheetTable(sk1, gameId, 1, 'operator');
  prepareSkSheetTable(sk2, gameId, 2, 'operator');
  prepareTripEditor();
  prepareSkaterSelector(gameId);
  prepareOsOffsetEditor();

  initialLogin();
}

function setClockControls(value) {
  'use strict';
  $('#ShowClockControlsButton').prop('checked', value);
  $('label.ShowClockControlsButton').toggleClass('ui-state-active', value);
  $('#TeamTime').find('tr.Control').toggleClass('Show', value);
}

function setTabBar(value) {
  'use strict';
  $('#ShowTabBarButton').prop('checked', value);
  $('label.ShowTabBarButton').toggleClass('ui-state-active', value);
  $('#tabBar').toggle(value);
}

function setReplaceButton(value) {
  'use strict';
  $('#EnableReplaceButton').prop('checked', value);
  $('label.EnableReplaceButton').toggleClass('ui-state-active', value);
  $('#ClockUndo').toggleClass('Hidden KeyInactive', value);
  $('#ClockReplace').toggleClass('Hidden KeyInactive', !value);
}

function createMetaControlTable(gameId) {
  'use strict';
  var table = $('<table><tr><td/></tr><tr><td/></tr><tr><td/></tr></table>').addClass('MetaControl');
  var buttonsTd = _crgUtils.createRowTable(1).appendTo(table.find('>tbody>tr:eq(0)').addClass('Buttons').children('td')).find('tr>td');
  var helpTd = _crgUtils.createRowTable(1).appendTo(table.find('>tbody>tr:eq(1)').addClass('Help Hidden').children('td')).find('tr>td');
  var periodEndTd = _crgUtils
    .createRowTable(1)
    .appendTo(table.find('>tbody>tr:eq(2)').addClass('PeriodEnd Hidden').children('td'))
    .find('tr>td');

  $('<label>').text('Edit Key Control').attr('for', 'EditKeyControlButton').appendTo(buttonsTd);
  $('<input type="checkbox">')
    .attr('id', 'EditKeyControlButton')
    .appendTo(buttonsTd)
    .button()
    .on('click', function () {
      _crgKeyControls.editKeys(this.checked);
      table.find('tr.Help').toggleClass('Hidden', !this.checked);
    });
  $('<a>')
    .text(
      'Key Control Edit mode enabled.  Buttons do not operate in this mode.  Move the mouse over a button, then press a normal key (not ESC, Enter, F1, etc.) to assign.  Backspace/Delete to remove.'
    )
    .appendTo(helpTd);

  WSActiveButton('ScoreBoard.Settings.Setting(ScoreBoard.AutoEndJam)', $('<button>').text('Auto End Jams').button()).appendTo(buttonsTd);

  $('<label>').addClass('EnableReplaceButton').text('Enable Replace on Undo').attr('for', 'EnableReplaceButton').appendTo(buttonsTd);
  $('<input type="checkbox">')
    .attr('id', 'EnableReplaceButton')
    .appendTo(buttonsTd)
    .button()
    .on('click', function () {
      var value = this.checked;
      setReplaceButton(value);
      var operator = $('#operatorId').text();
      if (operator) {
        WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.Operator__' + operator + '.ReplaceButton)', value);
      }
    });

  $('<label>')
    .addClass('ShowClockControlsButton')
    .text('Show Start/Stop Buttons')
    .attr('for', 'ShowClockControlsButton')
    .appendTo(buttonsTd);
  $('<input type="checkbox">')
    .attr('id', 'ShowClockControlsButton')
    .appendTo(buttonsTd)
    .button()
    .on('click', function () {
      var value = this.checked;
      setClockControls(value);
      var operator = $('#operatorId').text();
      if (operator) {
        WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.Operator__' + operator + '.StartStopButtons)', value);
      }
    });

  $('<label>').addClass('ShowTabBarButton').text('Show Tab Bar').attr('for', 'ShowTabBarButton').appendTo(buttonsTd);
  $('<input type="checkbox">')
    .attr('id', 'ShowTabBarButton')
    .appendTo(buttonsTd)
    .button()
    .on('click', function () {
      var value = this.checked;
      setTabBar(value);
      var operator = $('#operatorId').text();
      if (operator) {
        WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.Operator__' + operator + '.TabBar)', value);
      }
    });

  var startButton = $('<button>')
    .attr('id', 'GameControl')
    .text('Start New Game')
    .addClass('clickMe')
    .appendTo(buttonsTd)
    .button()
    .on('click', function () {
      createGameControlDialog(gameId);
    });

  var periodEndControlsLabel = $('<label>')
    .attr('for', 'PeriodEndControlsCheckbox')
    .text('End of Period Controls')
    .addClass('PeriodEndControls')
    .appendTo(buttonsTd);
  $('<input type="checkbox">')
    .attr('id', 'PeriodEndControlsCheckbox')
    .appendTo(buttonsTd)
    .button()
    .on('click', function () {
      table.find('tr.PeriodEnd').toggleClass('Hidden', !this.checked);
      updateHighlights();
    });

  var confirmedButton = toggleButton('ScoreBoard.Game(' + gameId + ').OfficialScore', 'Official Score', 'Unofficial Score');
  confirmedButton.appendTo(periodEndTd);
  var periodEndTimeoutDialog = createPeriodEndTimeoutDialog(periodEndTd, gameId);
  $('<button>')
    .addClass('PeriodEndTimeout')
    .text('Timeout before Period End')
    .appendTo(periodEndTd)
    .button()
    .on('click', function () {
      periodEndTimeoutDialog.dialog('open');
    });
  var otButton = $('<button>')
    .text('Overtime')
    .appendTo(periodEndTd)
    .button()
    .on('click', function () {
      createOvertimeDialog(WS.state['ScoreBoard.Game(' + gameId + ').Rule(Period.Number)'], gameId);
    });

  function updateHighlights() {
    var noPeriod = !isTrue(WS.state['ScoreBoard.Game(' + gameId + ').InPeriod']);
    var last =
      WS.state['ScoreBoard.Game(' + gameId + ').Rule(Period.Number)'] == WS.state['ScoreBoard.Game(' + gameId + ').Clock(Period).Number'];
    var tie = WS.state['ScoreBoard.Game(' + gameId + ').Team(1).Score'] === WS.state['ScoreBoard.Game(' + gameId + ').Team(2).Score'];
    var official = isTrue(WS.state['ScoreBoard.Game(' + gameId + ').OfficialScore']);

    startButton.toggleClass('clickMe', official);
    periodEndControlsLabel.toggleClass('clickMe', noPeriod && last && !official && table.find('tr.PeriodEnd').hasClass('Hidden'));
    confirmedButton.toggleClass('clickMe', noPeriod && last && !tie && !official);
    otButton.toggleClass('clickMe', noPeriod && last && tie && !official);
  }
  WS.Register(
    [
      'ScoreBoard.Game(' + gameId + ').InPeriod',
      'ScoreBoard.Game(' + gameId + ').Clock(Period).Number',
      'ScoreBoard.Game(' + gameId + ').Rule(Period.Number)',
      'ScoreBoard.Game(' + gameId + ').Team(*).Score',
      'ScoreBoard.Game(' + gameId + ').OfficialScore',
    ],
    updateHighlights
  );

  return table;
}

function hideEndOfPeriodControls() {
  'use strict';
  $('#PeriodEndControlsCheckbox').removeAttr('checked');
  $('#PeriodEndControlsCheckbox').button('refresh');
  $('tr.PeriodEnd').addClass('Hidden');
}

function addDays(date, days) {
  'use strict';
  var result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

function createGameControlDialog(gameId) {
  'use strict';
  var dialog = $('<div>').addClass('GameControl');
  var title = 'Start New Game';

  var preparedGame = $('<div>').addClass('section').appendTo(dialog);
  $('<span>').addClass('header').append('Start a prepared game').appendTo(preparedGame);
  $('<div>')
    .append($('<span>').append('Game: '))
    .append($('<select>').addClass('Game').append('<option value="">No Game Selected</option>'))
    .appendTo(preparedGame);
  $('<button>')
    .addClass('StartGame')
    .append('Start Game')
    .button({ disabled: true })
    .appendTo(preparedGame)
    .on('click', function () {
      WS.Set('ScoreBoard.CurrentGame.Game', preparedGame.find('select.Game option:selected').val());
      dialog.dialog('close');
    });

  var adhocGame = $('<div>').addClass('section').appendTo(dialog);
  var adhocState = $('<div>').addClass('section').appendTo(dialog);

  var adhocStartGame = function () {
    var StartTime = adhocGame.find('input.StartTime').val();
    var IntermissionClock = null;
    var points1 = Number(adhocState.find('input.Points.Team1').val());
    var points2 = Number(adhocState.find('input.Points.Team2').val());
    var to1 = Number(adhocState.find('input.TO.Team1').val());
    var to2 = Number(adhocState.find('input.TO.Team2').val());
    var or1 = Number(adhocState.find('input.OR.Team1').val());
    var or2 = Number(adhocState.find('input.OR.Team2').val());
    var period = Number(adhocState.find('input.Period').val());
    var jam = Number(adhocState.find('input.Jam').val());
    var periodClock = _timeConversions.minSecToMs(adhocState.find('input.PeriodClock').val());
    var advance = points1 + points2 + to1 + to2 + or1 + or2 + period + jam > 0;
    if (StartTime !== '') {
      var now = new Date();
      var parts = StartTime.split(':');
      StartTime = new Date(now.getFullYear(), now.getMonth(), now.getDate(), parts[0], parts[1]);
      if (StartTime < now) {
        StartTime = addDays(StartTime, 1);
      }
      IntermissionClock = '' + (StartTime - now);
    }
    var game = {
      Team1: adhocGame.find('select.Team1').val(),
      Team2: adhocGame.find('select.Team2').val(),
      Ruleset: adhocGame.find('select.Ruleset').val(),
      IntermissionClock: IntermissionClock,
      Advance: advance,
      Points1: points1,
      Points2: points2,
      TO1: to1,
      TO2: to2,
      OR1: or1,
      OR2: or2,
      Period: period,
      Jam: jam,
      PeriodClock: periodClock,
    };
    WS.Command('StartNewGame', game);
    dialog.dialog('close');
  };

  $('<span>').addClass('header').append('Start an adhoc game').appendTo(adhocGame);
  $('<div>')
    .append($('<span>').append('Team 1: '))
    .append($('<select>').addClass('Team1').append('<option value="">New Team</option>'))
    .appendTo(adhocGame);
  $('<div>')
    .append($('<span>').append('Team 2: '))
    .append($('<select>').addClass('Team2').append('<option value="">New Team</option>'))
    .appendTo(adhocGame);
  $('<div>').append($('<span>').append('Ruleset: ')).append($('<select>').addClass('Ruleset')).appendTo(adhocGame);
  $('<div>').append($('<span>').append('Start Time: ')).append($('<input>').attr('type', 'time').addClass('StartTime')).appendTo(adhocGame);
  $('<button>').addClass('StartGame').append('Start Game').button().appendTo(adhocGame).on('click', adhocStartGame);

  $('<button>')
    .addClass('setPrepState')
    .text('Start Mid-Game')
    .button()
    .appendTo(adhocState)
    .on('click', function () {
      adhocState.children('.prepState').removeClass('Hide');
      adhocState.children('.setPrepState').addClass('Hide');
    });

  $('<table>')
    .addClass('prepState perTeam Hide')
    .append($('<tr>').addClass('colLabel').append($('<td>')).append($('<td>').text('Team1')).append($('<td>').text('Team2')))
    .append(
      $('<tr>')
        .append($('<td>').addClass('rowLabel').text('Points:'))
        .append($('<td>').append($('<input>').attr('type', 'number').attr('min', '0').addClass('Points Team1')))
        .append($('<td>').append($('<input>').attr('type', 'number').attr('min', '0').addClass('Points Team2')))
    )
    .append(
      $('<tr>')
        .append($('<td>').addClass('rowLabel').text('Timeouts Taken:'))
        .append($('<td>').append($('<input>').attr('type', 'number').attr('min', '0').addClass('TO Team1')))
        .append($('<td>').append($('<input>').attr('type', 'number').attr('min', '0').addClass('TO Team2')))
    )
    .append(
      $('<tr>')
        .append($('<td>').addClass('rowLabel').text('Reviews Taken:'))
        .append($('<td>').append($('<input>').attr('type', 'number').attr('min', '0').addClass('OR Team1')))
        .append($('<td>').append($('<input>').attr('type', 'number').attr('min', '0').addClass('OR Team2')))
    )
    .append(
      $('<tr>')
        .append($('<td>').addClass('rowLabel').text('Period:'))
        .append($('<td>').append($('<input>').attr('type', 'number').attr('min', '1').addClass('Period')))
    )
    .append(
      $('<tr>')
        .append($('<td>').addClass('rowLabel').text('Jam:'))
        .append($('<td>').append($('<input>').attr('type', 'number').attr('min', '0').addClass('Jam')))
    )
    .append(
      $('<tr>')
        .append($('<td>').addClass('rowLabel').text('Period Clock:'))
        .append($('<td>').append($('<input>').attr('type', 'text').addClass('PeriodClock')))
    )
    .appendTo(adhocState);

  WS.Register(['ScoreBoard.Game(*).Name', 'ScoreBoard.Game(*).State'], function (k, v) {
    var name = WS.state['ScoreBoard.Game(' + k.Game + ').Name'] || '';
    var state = WS.state['ScoreBoard.Game(' + k.Game + ').State'] || '';
    var include = (state === 'Prepared' || state === 'Running') && name !== '' && k.Game != WS.state['ScoreBoard.CurrentGame.Game'];
    var options = preparedGame.find('option[value="' + k.Game + '"]');
    if (include && options.length === 0) {
      var option = $('<option>').attr('value', k.Game).data('name', name).text(name);
      _windowFunctions.appendAlphaSortedByData(preparedGame.find('select.Game'), option, 'name', 1);
    } else if (include) {
      options.text(name);
    } else {
      options.remove();
    }
  });
  preparedGame.find('select.Game').change(function (e) {
    preparedGame.find('button.StartGame').button('option', 'disabled', $(this).find('option:selected').val() === '');
  });

  WS.Register('ScoreBoard.PreparedTeam(*).Name', function (k, v) {
    if (v == null) {
      adhocGame.find('option[value="' + k.PreparedTeam + '"]').remove();
      return;
    }
    var options = adhocGame.find('option[value="' + k.PreparedTeam + '"]');
    if (options.length === 0) {
      var option = $('<option>').attr('value', k.PreparedTeam).text(v);
      _windowFunctions.appendAlphaSortedByAttr(adhocGame.find('select.Team1'), option, 'value', 1);
      _windowFunctions.appendAlphaSortedByAttr(adhocGame.find('select.Team2'), option.clone(), 'value', 1);
    } else {
      options.text(v);
    }
  });

  WS.Register('ScoreBoard.Game(' + gameId + ').Ruleset', function (k, v) {
    adhocGame.find('select.Ruleset').val(v);
  });
  WS.Register('ScoreBoard.Rulesets.Ruleset(*).Name', function (k, v) {
    var select = adhocGame.find('select.Ruleset');
    select.children('option[value="' + k.Ruleset + '"]').remove();
    if (v == null) {
      return;
    }
    var option = $('<option>').attr('value', k.Ruleset).attr('name', v).text(v);
    _windowFunctions.appendAlphaSortedByAttr(select, option, 'name');
    select.val(WS.state['ScoreBoard.Game(' + gameId + ').Ruleset']);
  });

  dialog.dialog({
    title: title,
    width: '600px',
    modal: true,
    buttons: {
      Cancel: function () {
        $(this).dialog('close');
      },
    },
    close: function () {
      $(this).dialog('destroy').remove();
    },
  });
  return dialog;
}

function createPeriodEndTimeoutDialog(td, gameId) {
  'use strict';
  var dialog = $('<div>');
  var applyDiv = $('<div>').addClass('Apply').appendTo(dialog);
  $('<span>').text('Timeout with ').appendTo(applyDiv);
  var periodSeconds = $('<input type="text" size="3">').val('1').appendTo(applyDiv);
  $('<span>').text(' seconds left on Period clock:').appendTo(applyDiv);
  $('<button>').addClass('Apply').text('Apply').appendTo(applyDiv).button();
  var waitDiv = $('<div>').addClass('Wait').appendTo(dialog).hide();
  $('<span>').text('Starting Timeout when Period clock to reaches ').appendTo(waitDiv);
  $('<span>').addClass('TargetSeconds').appendTo(waitDiv);
  $('<span>').text(' seconds...').appendTo(waitDiv);
  $('<button>').addClass('Cancel').text('Cancel').appendTo(waitDiv).button();
  var applying = false;
  WS.Register('ScoreBoard.Game(' + gameId + ').Clock(Period).Time', function (k, v) {
    checkTimeFunction(v);
  });
  WS.Register('ScoreBoard.Game(' + gameId + ').Clock(Period).Running');
  var checkTimeFunction = function (v) {
    if (!applying) {
      return;
    }
    var currentSecs = Number(_timeConversions.msToSeconds(v, isTrue(WS.state['ScoreBoard.Game(' + gameId + ').Clock(Period).Direction'])));
    var targetSecs = Number(waitDiv.find('span.TargetSeconds').text());
    if (currentSecs > targetSecs) {
      return;
    }
    if (currentSecs < targetSecs) {
      WS.Set('ScoreBoard.Game(' + gameId + ').Clock(Period).Time', _timeConversions.secondsToMs(targetSecs));
    }
    WS.Set('ScoreBoard.Game(' + gameId + ').Timeout', true);
    applying = false;
    td.find('button.PeriodEndTimeout').button('option', 'label', 'Timeout before Period End');
    applyDiv.show();
    waitDiv.hide();
    dialog.dialog('close');
  };
  applyDiv.find('button.Apply').on('click', function () {
    var secs = Number(periodSeconds.val());
    if (isNaN(secs)) {
      return;
    }
    waitDiv.find('span.TargetSeconds').text(secs);
    td.find('button.PeriodEndTimeout').button('option', 'label', 'Timeout at ' + secs + ' Period seconds');
    applyDiv.hide();
    waitDiv.show();
    applying = true;
    checkTimeFunction(WS.state['ScoreBoard.Game(' + gameId + ').Clock(Period).Time']);
  });
  waitDiv.find('button.Cancel').on('click', function () {
    td.find('button.PeriodEndTimeout').button('option', 'label', 'Timeout before Period End');
    applying = false;
    applyDiv.show();
    waitDiv.hide();
  });
  dialog.dialog({
    title: 'Timeout before End of Period',
    width: '600px',
    modal: true,
    autoOpen: false,
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
  return dialog;
}

function createOvertimeDialog(numPeriods, gameId) {
  'use strict';
  var dialog = $('<div>');
  $('<span>').text('Note: Overtime can only be started at the end of Period ').appendTo(dialog);
  $('<span>').text(numPeriods).appendTo(dialog);
  $('<button>')
    .addClass('StartOvertime')
    .text('Start Overtime Lineup clock')
    .appendTo(dialog)
    .on('click', function () {
      WS.Set('ScoreBoard.Game(' + gameId + ').StartOvertime', true);
      dialog.dialog('close');
    });
  dialog.dialog({
    title: 'Overtime',
    width: '600px',
    modal: true,
    buttons: {
      Cancel: function () {
        $(this).dialog('close');
      },
    },
    close: function () {
      $(this).dialog('destroy').remove();
    },
  });
}

function createJamControlTable(gameId) {
  'use strict';
  var table = $('<table><tr><td/></tr></table>').addClass('JamControl');
  var td = table.find('td');
  var replaceInfoTr = _crgUtils.createRowTable(1).addClass('ReplaceInfo Hidden').appendTo(td);
  var controlsTr = _crgUtils.createRowTable(4, 1).appendTo(td).find('tr:eq(0)').addClass('Controls');

  $('<span>').html('Replace &quot;<span id="replacedLabel"></span>&quot; with').appendTo(replaceInfoTr);
  WS.Register('ScoreBoard.Game(' + gameId + ').Label(Replaced)', function (k, v) {
    $('#replacedLabel').text(v);
  });

  var jamStartButton = $('<button>')
    .html('<span><span class="Label">Start Jam</span></span>')
    .attr('id', 'StartJam')
    .addClass('KeyControl')
    .button()
    .on('click', function () {
      WS.Set('ScoreBoard.Game(' + gameId + ').StartJam', true);
    });
  WS.Register('ScoreBoard.Game(' + gameId + ').Label(Start)', function (k, v) {
    jamStartButton.find('span.Label').text(v);
  });
  jamStartButton.appendTo(controlsTr.children('td:eq(0)'));

  var jamStopButton = $('<button>')
    .html('<span><span class="Label">Stop Jam</span></span>')
    .attr('id', 'StopJam')
    .addClass('KeyControl')
    .button()
    .on('click', function () {
      WS.Set('ScoreBoard.Game(' + gameId + ').StopJam', true);
    });
  WS.Register('ScoreBoard.Game(' + gameId + ').Label(Stop)', function (k, v) {
    jamStopButton.find('span.Label').text(v);
  });
  jamStopButton.appendTo(controlsTr.children('td:eq(1)'));

  var timeoutButton = $('<button>')
    .html('<span><span class="Label">Timeout</span></span>')
    .attr('id', 'Timeout')
    .addClass('KeyControl')
    .button()
    .on('click', function () {
      WS.Set('ScoreBoard.Game(' + gameId + ').Timeout', true);
    });
  WS.Register('ScoreBoard.Game(' + gameId + ').Label(Timeout)', function (k, v) {
    timeoutButton.find('span.Label').text(v);
  });
  timeoutButton.appendTo(controlsTr.children('td:eq(2)'));

  var undoButton = $('<button>')
    .html('<span><span class="Label">Undo</span></span>')
    .attr('id', 'ClockUndo')
    .addClass('KeyControl')
    .button()
    .on('click', function () {
      WS.Set('ScoreBoard.Game(' + gameId + ').ClockUndo', true);
    });
  WS.Register('ScoreBoard.Game(' + gameId + ').Label(Undo)', function (k, v) {
    undoButton.find('span.Label').text(v);
  });
  var replaceButton = $('<button>')
    .html('<span><span class="Label">Replace</span></span>')
    .attr('id', 'ClockReplace')
    .addClass('KeyControl Hidden KeyInactive')
    .button()
    .on('click', function () {
      WS.Set('ScoreBoard.Game(' + gameId + ').ClockReplace', true);
    });
  WS.Register('ScoreBoard.Game(' + gameId + ').Label(Undo)', function (k, v) {
    replaceButton.find('span.Label').text(v);
    if (!replaceButton.hasClass('Hidden')) {
      var rep = v === 'No Action';
      $('#TeamTime').find('.TabTable:not(.JamControl)').toggleClass('Faded', rep);
      $('#TeamTime').find('.ReplaceInfo').toggleClass('Hidden', !rep);
    }
  });
  undoButton
    .on('mouseenter mouseleave', function (event) {
      replaceButton.toggleClass('hover', event.type === 'mouseenter');
    })
    .appendTo(controlsTr.children('td:eq(3)'));
  replaceButton
    .on('mouseenter mouseleave', function (event) {
      undoButton.toggleClass('hover', event.type === 'mouseenter');
    })
    .appendTo(controlsTr.children('td:eq(3)'));

  WS.Register(['ScoreBoard.Game(' + gameId + ').InJam', 'ScoreBoard.Game(' + gameId + ').Clock(Jam).Running'], function () {
    var inJam = isTrue(WS.state['ScoreBoard.Game(' + gameId + ').InJam']);
    var timeLeft = isTrue(WS.state['ScoreBoard.Game(' + gameId + ').Clock(Jam).Running']);
    jamStopButton.toggleClass('clickMe', inJam && !timeLeft);
  });

  WS.Register(
    [
      'ScoreBoard.Game(' + gameId + ').Rule(Lineup.Duration)',
      'ScoreBoard.Game(' + gameId + ').Rule(Lineup.OvertimeDuration)',
      'ScoreBoard.Game(' + gameId + ').Clock(Lineup).Running',
      'ScoreBoard.Game(' + gameId + ').Clock(Lineup).Time',
      'ScoreBoard.Game(' + gameId + ').InOvertime',
    ],
    function () {
      var inLineup = isTrue(WS.state['ScoreBoard.Game(' + gameId + ').Clock(Lineup).Running']);
      var overtime = isTrue(WS.state['ScoreBoard.Game(' + gameId + ').InOvertime']);
      var curTime = WS.state['ScoreBoard.Game(' + gameId + ').Clock(Lineup).Time'];
      var maxTime = _timeConversions.minSecToMs(
        overtime
          ? WS.state['ScoreBoard.Game(' + gameId + ').Rule(Lineup.OvertimeDuration)']
          : WS.state['ScoreBoard.Game(' + gameId + ').Rule(Lineup.Duration)']
      );

      jamStartButton.toggleClass('clickMe', inLineup && curTime > maxTime);
      timeoutButton.toggleClass('clickMe', inLineup && curTime > maxTime);
    }
  );

  return table;
}

var timeoutDialog;
function createTeamTable(gameId) {
  'use strict';
  var table = $('<table>').append('<tbody>').addClass('Team');
  var row = $('<tr></tr>');
  var nameRow = row.clone().addClass('Name').appendTo(table);
  var scoreRow = row.clone().addClass('Score').appendTo(table);
  var speedScoreRow = row.clone().addClass('SpeedScore').appendTo(table);
  var timeoutRow = row.clone().addClass('Timeout').appendTo(table);
  var flagsRow = row.clone().appendTo(table);
  var jammerRow = row.clone().addClass('Jammer').appendTo(table);
  var pivotRow = row.clone().addClass('Pivot').appendTo(table);

  $.each(['1', '2'], function () {
    var team = String(this);
    var prefix = 'ScoreBoard.Game(' + gameId + ').Team(' + team + ')';
    var first = team === '1';

    var nameTr = _crgUtils.createRowTable(2).appendTo($('<td>').appendTo(nameRow)).find('tr');
    var scoreTr = _crgUtils.createRowTable(3).appendTo($('<td>').appendTo(scoreRow)).find('tr');
    var speedScoreTr = _crgUtils.createRowTable(6).appendTo($('<td>').appendTo(speedScoreRow)).find('tr');
    var timeoutTr = _crgUtils.createRowTable(6).appendTo($('<td>').appendTo(timeoutRow)).find('tr');
    var flagsTr = _crgUtils.createRowTable(2).appendTo($('<td>').appendTo(flagsRow)).find('tr');
    var jammerTr = _crgUtils.createRowTable(1).appendTo($('<td>').appendTo(jammerRow)).find('tr');
    var pivotTr = _crgUtils.createRowTable(1).appendTo($('<td>').appendTo(pivotRow)).find('tr');

    var nameTd = nameTr.children('td:eq(' + (first ? 1 : 0) + ')').addClass('Name');
    var nameDisplayDiv = $('<div>').appendTo(nameTd);
    var nameA = $('<a>').appendTo(nameDisplayDiv).text('');
    var colorA = $('<a>').appendTo(nameDisplayDiv).text('');
    var altNameA = $('<a>').appendTo(nameDisplayDiv).text('');

    var nameEditTable = $(
      '<table><tr><td>Alternate Name</td></tr>' + '<tr><td><input class="AlternateName" type="text" size="15" /></td></tr></table>'
    ).appendTo(nameTd);
    var altNameInput = $(nameEditTable).find('.AlternateName');

    nameEditTable.hide();
    WS.Register(prefix + '.Name', function (k, v) {
      nameA.text(v);
    });
    var nameInputFocus = function () {
      if (nameDisplayDiv.css('display') !== 'none') {
        nameDisplayDiv.hide();
        nameEditTable.show();
        altNameInput.addClass('Editing').trigger('editstart');
      }
    };
    var nameInputBlur = function (event) {
      if (event.relatedTarget !== altNameInput[0]) {
        nameEditTable.hide();
        nameDisplayDiv.show();
        altNameInput.removeClass('Editing').trigger('editstop');
      }
    };
    var nameInputKeyup = function (event) {
      var c = $(event.target);
      switch (event.which) {
        case 13:
          /* RET */ if (c.is('textarea') && !event.ctrlKey) {
            break;
          }
          c.trigger('blur');
          break;
        case 27:
          /* ESC */ c.trigger('blur');
          break;
      }
    };

    nameDisplayDiv.on('click', function () {
      altNameInput.focus();
    });
    altNameInput.on('focus', nameInputFocus);
    altNameInput.on('blur', nameInputBlur);
    altNameInput.on('keyup', nameInputKeyup);

    altNameInput.on('change', function () {
      var val = $.trim(altNameInput.val());
      if (val === '') {
        WS.Set(prefix + '.AlternateName(operator)', null);
      } else {
        WS.Set(prefix + '.AlternateName(operator)', val);
      }
    });

    WS.Register(prefix + '.UniformColor', function (k, v) {
      colorA.text(v || '');
      nameA.toggleClass('AlternateName', colorA.text() !== '' || altNameA.text() !== '');
    });
    WS.Register(prefix + '.AlternateName(operator)', function (k, v) {
      altNameA.text(v || '');
      altNameInput.val(v || '');
      nameA.toggleClass('AlternateName', v != null || colorA.text() !== '');
      colorA.toggleClass('AlternateName', v != null);
    });

    var names = nameA.add(altNameA);
    WS.Register(prefix + '.Color(*)', function (k, v) {
      v = v || '';
      switch (k.Color) {
        case 'operator_fg':
          names.css('color', v);
          break;
        case 'operator_bg':
          names.css('background-color', v);
          break;
        case 'operator_glow':
          var shadow = '';
          if (v) {
            shadow = '0 0 0.2em ' + v;
            shadow = shadow + ', ' + shadow + ', ' + shadow;
          }
          names.css('text-shadow', shadow);
          break;
      }
    });

    var logoTd = nameTr.children('td:eq(' + (first ? 0 : 1) + ')').addClass('Logo');
    var logoNone = $('<a>').html('No Logo').addClass('NoLogo').appendTo(logoTd);
    var logoSelect = mediaSelect(prefix + '.Logo', 'images', 'teamlogo', 'Logo').appendTo(logoTd);
    var logoImg = $('<img>').appendTo(logoTd);

    var logoShowSelect = function (show) {
      var showImg = !!WS.state[prefix + '.Logo'];
      logoImg.toggle(!show && showImg);
      logoNone.toggle(!show && !showImg);
      logoSelect.toggle(show);
      if (show) {
        logoSelect.focus();
      }
    };
    WS.Register(prefix + '.Logo', function (k, v) {
      logoShowSelect(false);
      logoImg.attr('src', v);
    });
    logoSelect
      .on('blur', function () {
        logoShowSelect(false);
      })
      .on('keyup', function (event) {
        if (event.which === 27 /* ESC */) {
          $(this).trigger('blur');
        }
      });

    logoTd.on('click', function () {
      if (!logoSelect.is(':visible')) {
        logoShowSelect(true);
      }
    });

    function flash(element) {
      element.stop(true).last().css({ color: '#F00' });
      element.last().animate({ color: '#000' }, 1000);
    }

    var scoreSubTr = _crgUtils.createRowTable(2).appendTo(scoreTr.children('td:eq(1)')).find('tr');

    var subScoreTd = scoreSubTr.children('td:eq(' + (first ? '0' : '1') + ')');
    $('<span>').text('Jam Pts: ').appendTo(subScoreTd);
    var jamScore = $('<a>').appendTo(subScoreTd).addClass('JamScore');
    WS.Register(prefix + '.JamScore', function (k, v) {
      jamScore.text(v);
    });
    $('<br>').appendTo(subScoreTd);
    $('<span>').text('Trip Pts: ').appendTo(subScoreTd);
    var tripScore = $('<a>').appendTo(subScoreTd).addClass('TripScore');
    WS.Register(prefix + '.TripScore', function (k, v) {
      tripScore.text(v);
    });

    var score = $('<a/>').appendTo(scoreSubTr.children('td:eq(' + (first ? '1' : '0') + ')').addClass('Score'));
    WS.Register(prefix + '.Score', function (k, v) {
      score.text(v);
    });

    var scoreDownTd = scoreTr.children('td:eq(' + (first ? '0' : '2') + ')').addClass('Down');
    $('<button>')
      .append($('<span>').text('Points -1'))
      .attr('id', 'Team' + team + 'ScoreDown')
      .addClass('KeyControl BigButton')
      .button()
      .on('click', function () {
        WS.Set(prefix + '.TripScore', -1, 'change');
        if (WS.state[prefix + '.Score'] === 0) {
          flash(score);
        }
        if (WS.state[prefix + '.JamScore'] === 0) {
          flash(jamScore);
        }
        if (WS.state[prefix + '.TripScore'] === 0) {
          flash(tripScore);
        }
      })
      .appendTo(scoreDownTd);
    $('<br />').appendTo(scoreDownTd);
    $('<button>')
      .append($('<span>').text('Remove Trip'))
      .val('true')
      .attr('id', 'Team' + team + 'RemoveTrip')
      .addClass('KeyControl TripButton')
      .button()
      .on('click', function () {
        WS.Set(prefix + '.RemoveTrip', true);
      })
      .appendTo(scoreDownTd);

    var scoreUpTd = scoreTr.children('td:eq(' + (first ? '2' : '0') + ')').addClass('Up');

    $('<button>')
      .append($('<span>').text('Points +1'))
      .attr('id', 'Team' + team + 'ScoreUp')
      .addClass('KeyControl BigButton')
      .button()
      .on('click', function () {
        WS.Set(prefix + '.TripScore', +1, 'change');
      })
      .appendTo(scoreUpTd);
    $('<br />').appendTo(scoreUpTd);
    $('<button>')
      .append($('<span>').text('Add Trip'))
      .attr('id', 'Team' + team + 'AddTrip')
      .addClass('KeyControl TripButton')
      .button()
      .on('click', function () {
        WS.Set(prefix + '.AddTrip', true);
      })
      .appendTo(scoreUpTd);

    if (first) {
      $('<span>').text('Set Trip Pts').appendTo(speedScoreTr.find('td:eq(0)'));
    }
    function addSpeedScoreButton(amount, pos) {
      $('<button>')
        .append($('<span>').text(amount))
        .attr('id', 'Team' + team + 'TripScore' + amount)
        .addClass('KeyControl')
        .button()
        .on('click', function () {
          WS.Set(prefix + '.TripScore', amount);
        })
        .appendTo(speedScoreTr.find('td:eq(' + pos + ')'));
    }
    for (var i = 0; i <= 4; i++) {
      addSpeedScoreButton(i, first ? i + 1 : 4 - i);
    }

    var timeoutButton = $('<button>')
      .append($('<span>').text('Team TO'))
      .attr('id', 'Team' + team + 'Timeout')
      .addClass('KeyControl')
      .button()
      .on('click', function () {
        WS.Set(prefix + '.Timeout', true);
      });
    timeoutButton.appendTo(timeoutTr.children('td:eq(' + (first ? '0' : '5') + ')').addClass('Timeout'));
    var timeoutCount = $('<a/>')
      .on('click', function () {
        timeoutDialog.dialog('open');
      })
      .appendTo(timeoutTr.children('td:eq(' + (first ? '1' : '4') + ')').addClass('Timeouts'));
    WS.Register(prefix + '.Timeouts', function (k, v) {
      timeoutCount.text(v);
    });

    var reviewButton = $('<button>')
      .append($('<span>').text('Off Review'))
      .attr('id', 'Team' + team + 'OfficialReview')
      .addClass('KeyControl')
      .button()
      .on('click', function () {
        WS.Set(prefix + '.OfficialReview', true);
      });
    reviewButton.appendTo(timeoutTr.children('td:eq(' + (first ? '2' : '3') + ')').addClass('OfficialReview'));
    var officialReviews = $('<a/>')
      .on('click', function () {
        timeoutDialog.dialog('open');
      })
      .appendTo(timeoutTr.children('td:eq(' + (first ? '3' : '2') + ')').addClass('OfficialReviews'));
    WS.Register(prefix + '.OfficialReviews', function (k, v) {
      officialReviews.text(v);
    });

    WS.Register(['ScoreBoard.Game(' + gameId + ').TimeoutOwner', 'ScoreBoard.Game(' + gameId + ').OfficialReview'], function (k, v) {
      var to = WS.state['ScoreBoard.Game(' + gameId + ').TimeoutOwner'].slice(-1) === team;
      var or = isTrue(WS.state['ScoreBoard.Game(' + gameId + ').OfficialReview']);
      timeoutButton.toggleClass('Active', to && !or);
      reviewButton.toggleClass('Active', to && or);
    });

    var retainedORButton = WSActiveButton(prefix + '.RetainedOfficialReview', $('<button>'))
      .append($('<span>').text('Retained'))
      .attr('id', 'Team' + team + 'RetainedOfficialReview')
      .addClass('KeyControl')
      .button();
    retainedORButton.appendTo(timeoutTr.children('td:eq(' + (first ? '4' : '1') + ')').addClass('RetainedOfficialReview'));

    if (first) {
      var otoButton = $('<button>')
        .append($('<span>').text('Official TO'))
        .attr('id', 'OfficialTimeout')
        .addClass('KeyControl')
        .button()
        .on('click', function () {
          WS.Set('ScoreBoard.Game(' + gameId + ').OfficialTimeout', true);
        });
      WS.Register('ScoreBoard.Game(' + gameId + ').TimeoutOwner', function (k, v) {
        otoButton.toggleClass('Active', v === 'O');
      });
      otoButton.appendTo(timeoutTr.children('td:eq(5)').addClass('OfficialTimeout'));
      otoButton.wrap('<div></div>');
    }

    var leadJammerTd = flagsTr
      .children('td:eq(' + (first ? '1' : '0') + ')')
      .append($('<span>'))
      .children('span');
    WSActiveButton(prefix + '.Lost', $('<button>'))
      .append($('<span>').text('Lost'))
      .attr('id', 'Team' + team + 'Lost')
      .addClass('KeyControl')
      .button()
      .appendTo(leadJammerTd);
    WSActiveButton(prefix + '.Lead', $('<button>'))
      .append($('<span>').text('Lead'))
      .attr('id', 'Team' + team + 'Lead')
      .addClass('KeyControl')
      .button()
      .appendTo(leadJammerTd);
    WSActiveButton(prefix + '.Calloff', $('<button>'))
      .append($('<span>').text('Call'))
      .attr('id', 'Team' + team + 'Call')
      .addClass('KeyControl')
      .button()
      .appendTo(leadJammerTd);
    WSActiveButton(prefix + '.Injury', $('<button>'))
      .append($('<span>').text('Inj'))
      .attr('id', 'Team' + team + 'Inj')
      .addClass('KeyControl')
      .button()
      .appendTo(leadJammerTd);
    WSActiveButton(prefix + '.NoInitial', $('<button>'))
      .append($('<span>').text('NI'))
      .attr('id', 'Team' + team + 'NI')
      .addClass('KeyControl')
      .button()
      .appendTo(leadJammerTd);

    leadJammerTd.controlgroup();

    var starPassTd = flagsTr.children('td:eq(' + (first ? '0' : '1') + ')');
    WSActiveButton(prefix + '.StarPass', $('<button>'))
      .append($('<span>').text('Star Pass'))
      .attr('id', 'Team' + team + 'StarPass')
      .addClass('KeyControl')
      .button()
      .appendTo(starPassTd);
    WSActiveButton(prefix + '.NoPivot', $('<button>'))
      .append($('<span>').text('No Pivot'))
      .attr('id', 'Team' + team + 'NoPivot')
      .addClass('KeyControl')
      .button()
      .appendTo(starPassTd);

    var makeSkaterSelector = function (pos) {
      var container = $('<span class="skaterSelector">');

      var none = $('<button>')
        .append($('<span>').text('?'))
        .attr('skater', '')
        .attr('id', 'Team' + team + pos + 'None')
        .addClass('KeyControl')
        .button();
      container.append(none).controlgroup();
      none.on('click', function () {
        WS.Set(prefix + '.Position(' + pos + ').Skater', '');
      });

      function setValue(v) {
        container.children().removeClass('Active');
        v = v || '';
        container.children('[skater="' + v + '"]').addClass('Active');
      }
      WS.Register([prefix + '.Skater(*).RosterNumber', prefix + '.Skater(*).Role'], function (k, v) {
        container.children('[skater="' + k.Skater + '"]').remove();
        if (v != null && WS.state[prefix + '.Skater(' + k.Skater + ').Role'] !== 'NotInGame') {
          var number = WS.state[prefix + '.Skater(' + k.Skater + ').RosterNumber'];
          var button = $('<button>')
            .attr('number', number)
            .attr('skater', k.Skater)
            .attr('id', 'Team' + team + pos + k.Skater)
            .addClass('KeyControl')
            .append($('<span>').text(number))
            .on('click', function () {
              WS.Set(prefix + '.Position(' + pos + ').Skater', k.Skater);
            })
            .button();
          _crgKeyControls.setupKeyControl(button, _crgKeyControls.operator);
          _windowFunctions.appendAlphaSortedByAttr(container, button, 'number', 1);
        }
        setValue(WS.state[prefix + '.Position(' + pos + ').Skater']);
      });
      WS.Register(prefix + '.Position(' + pos + ').Skater', function (k, v) {
        setValue(v);
      });
      return container;
    };

    var jammerSelectTd = jammerTr.children('td');
    $('<span>').text('Jammer:').appendTo(jammerSelectTd);
    makeSkaterSelector('Jammer').appendTo(jammerSelectTd);

    var jammerBoxButton = WSActiveButton(prefix + '.Position(Jammer).PenaltyBox', $('<button>'))
      .append($('<span>').text('Box'))
      .attr('id', 'Team' + team + 'JammerBox')
      .addClass('KeyControl Box')
      .button()
      .appendTo(jammerSelectTd);

    var pivotSelectTd = pivotTr.children('td');
    $('<span>').text('Piv/4th Bl:').appendTo(pivotSelectTd);
    makeSkaterSelector('Pivot').appendTo(pivotSelectTd);

    var pivotBoxButton = WSActiveButton(prefix + '.Position(Pivot).PenaltyBox', $('<button>'))
      .append($('<span>').text('Box'))
      .attr('id', 'Team' + team + 'PivotBox')
      .addClass('KeyControl Box')
      .button()
      .appendTo(pivotSelectTd);

    WS.Register('ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)', function (k, v) {
      jammerBoxButton.toggleClass('Hide', isTrue(v));
      pivotBoxButton.toggleClass('Hide', isTrue(v));
    });
  });

  return table;
}

function createTimeTable(gameId) {
  'use strict';
  var table = $('<table>').append('<tbody>').addClass('Time');
  var row = $('<tr></tr>');
  var nameRow = row.clone().addClass('Name').appendTo(table);
  var numberRow = row.clone().addClass('Number').appendTo(table);
  var controlRow = row.clone().addClass('Control').appendTo(table);
  var timeRow = row.clone().addClass('Time').appendTo(table);

  $.each(['Period', 'Jam', 'Lineup', 'Timeout', 'Intermission'], function () {
    var clock = String(this);
    var prefix = 'ScoreBoard.Game(' + gameId + ').Clock(' + clock + ')';

    var nameTd = $('<td>').appendTo(nameRow);
    var numberTr = _crgUtils.createRowTable(3).appendTo($('<td>').appendTo(numberRow)).find('tr');
    var controlTr = _crgUtils.createRowTable(2).appendTo($('<td>').appendTo(controlRow)).find('tr');
    var timeTr = _crgUtils.createRowTable(3).appendTo($('<td>').appendTo(timeRow)).find('tr');

    var name = $('<a>').appendTo(nameTd.addClass('Name'));
    WS.Register(prefix + '.Name', function (k, v) {
      name.text(v);
    });
    if (clock === 'Period' || clock === 'Jam') {
      var it = $('<a>').appendTo(nameTd).addClass('InvertedTime');
      WS.Register(prefix + '.InvertedTime', function (k, v) {
        it.text(_timeConversions.msToMinSec(v, !isTrue(WS.state[prefix + '.Direction'])));
      });
      WS.Register(prefix + '.Direction', function (k, v) {
        it.toggleClass('CountDown', isTrue(v));
        it.toggleClass('CountUp', !isTrue(v));
      });
    }
    if (clock === 'Jam') {
      WS.Register('ScoreBoard.Game(' + gameId + ').InJam', function (k, v) {
        nameTd.toggleClass('Running', isTrue(v));
      });
    } else {
      WS.Register(prefix + '.Running', function (k, v) {
        nameTd.toggleClass('Running', isTrue(v));
      });
    }
    WS.Register('ScoreBoard.Game(' + gameId + ').NoMoreJam', function (k, v) {
      nameTd.toggleClass('NoMoreJam', isTrue(v));
    });

    var number = $('<a>').appendTo(numberTr.children('td:eq(1)').addClass('Number').css('width', '20%'));
    WS.Register(prefix + '.Number', function (k, v) {
      number.text(v);
    });
    if (clock === 'Period') {
      var periodDialog = createPeriodDialog(gameId);
      numberTr.children('td:eq(1)').on('click', function () {
        periodDialog.dialog('open');
      });
    } else if (clock === 'Jam') {
      var jamDialog = createJamDialog(gameId);
      numberTr.children('td:eq(1)').on('click', function () {
        jamDialog.dialog('open');
      });
    } else if (clock === 'Timeout') {
      timeoutDialog = createTimeoutDialog(gameId);
      numberTr.children('td:eq(1)').on('click', function () {
        timeoutDialog.dialog('open');
      });
    }

    $('<button>')
      .append($('<span>').text('Start'))
      .val('true')
      .attr('id', 'Clock' + clock + 'Start')
      .addClass('KeyControl')
      .button()
      .appendTo(controlTr.children('td:eq(0)').addClass('Start'))
      .on('click', function () {
        WS.Set(prefix + '.Start', true);
      });
    $('<button>')
      .append($('<span>').text('Stop'))
      .val('true')
      .attr('id', 'Clock' + clock + 'Stop')
      .addClass('KeyControl')
      .button()
      .appendTo(controlTr.children('td:eq(1)').addClass('Stop'))
      .on('click', function () {
        WS.Set(prefix + '.Stop', true);
      });

    $('<button>')
      .append($('<span>').text('-1'))
      .attr('id', 'Clock' + clock + 'TimeDown')
      .addClass('KeyControl')
      .button()
      .on('click', function () {
        WS.Set(prefix + '.Time', -1000, 'change');
      })
      .appendTo(timeTr.children('td:eq(0)').addClass('Button'));
    var time = $('<a>').appendTo(timeTr.children('td:eq(1)').addClass('Time'));
    WS.Register(prefix + '.Time', function (k, v) {
      time.text(_timeConversions.msToMinSec(v, isTrue(WS.state[prefix + '.Direction'])));
    });
    var timeDialog = createTimeDialog(gameId, clock);
    timeTr.children('td:eq(1)').on('click', function () {
      timeDialog.dialog('open');
    });
    $('<button>')
      .append($('<span>').text('+1'))
      .attr('id', 'Clock' + clock + 'TimeUp')
      .addClass('KeyControl')
      .button()
      .on('click', function () {
        WS.Set(prefix + '.Time', +1000, 'change');
      })
      .appendTo(timeTr.children('td:eq(2)').addClass('Button'));
  });

  return table;
}

function createPeriodDialog(gameId) {
  'use strict';
  var dialog = $('<div>').addClass('NumberDialog');
  var table = $('<table>').appendTo(dialog);
  var headers = $('<tr><td/><td/><td/><td/><td/></tr>').appendTo(table);
  $('<a>').text('Nr').addClass('Title').appendTo(headers.children('td:eq(0)').addClass('Title'));
  $('<a>').text('Jams').addClass('Title').appendTo(headers.children('td:eq(1)').addClass('Title'));
  $('<a>').text('Duration').addClass('Title').appendTo(headers.children('td:eq(2)').addClass('Title'));

  WS.Register(
    [
      'ScoreBoard.Game(' + gameId + ').Period(*).CurrentJamNumber',
      'ScoreBoard.Game(' + gameId + ').Period(*).Duration',
      'ScoreBoard.Game(' + gameId + ').Period(*).Number',
      'ScoreBoard.Game(' + gameId + ').Period(*).Running',
    ],
    function (k, v) {
      var nr = k.Period;
      if (nr == null || nr == 0) {
        return;
      }
      var prefix = 'ScoreBoard.Game(' + gameId + ').Period(' + nr + ')';
      var key = k.field;
      if (k.parts.length > 4) {
        return;
      }
      if (!['CurrentJamNumber', 'Duration', 'Number', 'Running'].includes(key)) {
        return;
      }

      var row = table.find('tr.Period[nr=' + nr + ']');
      if (row.length === 0 && v != null) {
        row = $('<tr>')
          .addClass('Period')
          .attr('nr', nr)
          .append($('<td>').addClass('Number').text(nr))
          .append($('<td>').addClass('Jams').text(0))
          .append($('<td>').addClass('Duration'))
          .append(
            $('<td>').append(
              $('<button>')
                .text('Delete')
                .button()
                .on('click', function () {
                  //TODO: confirmation popup
                  WS.Set(prefix + '.Delete', true);
                })
            )
          )
          .append(
            $('<td>').append(
              $('<button>')
                .text('Insert Before')
                .button()
                .on('click', function () {
                  WS.Set(prefix + '.InsertBefore', true);
                })
            )
          );
        var inserted = false;
        table.find('tr.Period').each(function (i, r) {
          r = $(r);
          if (Number(r.attr('nr')) > Number(nr)) {
            r.before(row);
            inserted = true;
            return false;
          }
        });
        if (!inserted) {
          row.appendTo(table);
        }
      } else if (key === 'Number' && v == null && row.length > 0) {
        row.remove();
        return;
      }
      if (v != null) {
        if (key === 'CurrentJamNumber') {
          row.children('td.Jams').text(v);
        }
        if (key === 'Duration' && !isTrue(WS.state[prefix + '.Running'])) {
          row.children('td.Duration').text(_timeConversions.msToMinSec(v, true));
        }
        if (key === 'Running' && isTrue(v)) {
          row.children('td.Duration').text('running');
        }
        if (key === 'Running' && !isTrue(v)) {
          row.children('td.Duration').text(_timeConversions.msToMinSec(WS.state[prefix + '.Duration'], true));
        }
      }
    }
  );

  return dialog.dialog({
    title: 'Periods',
    autoOpen: false,
    modal: true,
    width: 500,
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function createJamDialog(gameId) {
  'use strict';
  var currentPeriod;
  var nextJam;
  var dialog = $('<div>').addClass('NumberDialog');
  var tableTemplate = $('<table>').addClass('Period');
  var headers = $('<tr><td/><td/><td/><td/><td/><td/></tr>').appendTo(tableTemplate);
  $('<a>').text('Nr').addClass('Title').appendTo(headers.children('td:eq(0)').addClass('Title'));
  $('<a>').text('Points').addClass('Title').appendTo(headers.children('td:eq(1)').addClass('Title'));
  $('<a>').text('Duration').addClass('Title').appendTo(headers.children('td:eq(2)').addClass('Title'));
  $('<a>').text('PC at end').addClass('Title').appendTo(headers.children('td:eq(3)').addClass('Title'));
  var footer = $('<tr><td colspan="4"></td><td></td><td></td>').addClass('Jam').attr('nr', 999).appendTo(tableTemplate);
  $('<span>').text('Upcoming').appendTo(footer.children('td:eq(0)'));
  $('<button>')
    .text('Insert Before')
    .button()
    .on('click', function () {
      WS.Set('ScoreBoard.Game(' + gameId + ').Jam(' + nextJam + ').InsertBefore', true);
    })
    .appendTo(footer.children('td:eq(2)'));

  WS.Register(
    [
      'ScoreBoard.Game(' + gameId + ').Period(*).Jam(*).Duration',
      'ScoreBoard.Game(' + gameId + ').Period(*).Jam(*).Number',
      'ScoreBoard.Game(' + gameId + ').Period(*).Jam(*).PeriodClockDisplayEnd',
      'ScoreBoard.Game(' + gameId + ').Period(*).Jam(*).TeamJam(*).JamScore',
    ],
    function (k, v) {
      var per = k.Period;
      if (per == 0) {
        return;
      }
      var nr = k.Jam;
      var prefix = 'ScoreBoard.Game(' + gameId + ').Period(' + per + ').Jam(' + nr + ')';
      var key = k.field;

      var table = dialog.find('table.Period[nr=' + per + ']');
      if (table.length === 0 && v != null) {
        table = tableTemplate.clone(true).attr('nr', per).appendTo(dialog);
        if (per == currentPeriod) {
          table.addClass('Show');
        }
      }
      if (table.length === 0) {
        return;
      }

      var row = table.find('tr.Jam[nr=' + nr + ']');
      if (row.length === 0 && v != null) {
        row = $('<tr>')
          .addClass('Jam')
          .attr('nr', nr)
          .append($('<td>').addClass('Number').text(nr))
          .append(
            $('<td>').addClass('Points').append($('<span>').addClass('1')).append($('<span>').text(' - ')).append($('<span>').addClass('2'))
          )
          .append($('<td>').addClass('Duration'))
          .append($('<td>').addClass('PC'))
          .append(
            $('<td>').append(
              $('<button>')
                .text('Delete')
                .button()
                .on('click', function () {
                  //TODO: confirmation popup
                  WS.Set(prefix + '.Delete', true);
                })
            )
          )
          .append(
            $('<td>').append(
              $('<button>')
                .text('Insert Before')
                .button()
                .on('click', function () {
                  WS.Set(prefix + '.InsertBefore', true);
                })
            )
          );
        var inserted = false;
        table.find('tr.Jam').each(function (i, r) {
          r = $(r);
          if (Number(r.attr('nr')) > Number(nr)) {
            r.before(row);
            inserted = true;
            return false;
          }
        });
        if (!inserted) {
          row.appendTo(table);
        }
      } else if (key === 'Number' && v == null && row.length > 0) {
        row.remove();
        return;
      }
      if (v != null) {
        if (key === 'JamScore') {
          row.find('td.Points .' + k.TeamJam).text(v);
        }
        if (key === 'Duration') {
          if (WS.state[prefix + '.WalltimeEnd'] === 0 && WS.state[prefix + '.WalltimeStart'] > 0) {
            row.children('td.Duration').text('running');
          } else {
            row.children('td.Duration').text(_timeConversions.msToMinSec(v, true));
          }
        }
        if (key === 'PeriodClockDisplayEnd') {
          if (WS.state[prefix + '.WalltimeEnd'] === 0 && WS.state[prefix + '.WalltimeStart'] > 0) {
            row.children('td.PC').text('running');
          } else {
            row
              .children('td.PC')
              .text(_timeConversions.msToMinSec(v, isTrue(WS.state['ScoreBoard.Game(' + gameId + ').Clock(Period).Direction'])));
          }
        }
      }
    }
  );

  WS.Register(['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber'], function (k, v) {
    currentPeriod = v;
    dialog.find('table.Period.Show').removeClass('Show');
    dialog.find('table.Period[nr=' + v + ']').addClass('Show');
  });

  WS.Register(['ScoreBoard.Game(' + gameId + ').Jam(*).Number'], function (k, v) {
    if (v != null) {
      nextJam = v;
    }
  });

  return dialog.dialog({
    title: 'Jams',
    autoOpen: false,
    modal: true,
    width: 550,
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function createTimeoutDialog(gameId) {
  'use strict';
  var firstJamListed = [0];
  var lastJamListed = [0];
  var periodDropdownTemplate = $('<select>').attr('id', 'PeriodDropdown').append($('<option>').attr('value', 0).text('P0'));
  var jamDropdownTemplate = [$('<select>').attr('id', 'JamDropdown').attr('period', 0).append($('<option>').attr('value', 0).text('J0'))];
  var typeDropdownTemplate = $('<select>')
    .attr('id', 'TypeDropdown')
    .append($('<option>').attr('value', '.false').text('No type'))
    .append($('<option>').attr('value', 'O.false').text('Off. Timeout'))
    .append(
      $('<option>')
        .attr('value', gameId + '_1.false')
        .text('Team TO left')
    )
    .append(
      $('<option>')
        .attr('value', gameId + '_2.false')
        .text('Team TO right')
    )
    .append(
      $('<option>')
        .attr('value', gameId + '_1.true')
        .text('Off. Review left')
    )
    .append(
      $('<option>')
        .attr('value', gameId + '_2.true')
        .text('Off. Review right')
    );

  var dialog = $('<div>').addClass('NumberDialog');
  var table = $('<table>').appendTo(dialog);
  var headers = $('<tr><td/><td/><td/><td/><td/><td/><td/></tr>').appendTo(table);
  $('<a>').text('Period').addClass('Title').appendTo(headers.children('td:eq(0)').addClass('Title'));
  $('<a>').text('After Jam').addClass('Title').appendTo(headers.children('td:eq(1)').addClass('Title'));
  $('<a>').text('Duration').addClass('Title').appendTo(headers.children('td:eq(2)').addClass('Title'));
  $('<a>').text('Period Clock').addClass('Title').appendTo(headers.children('td:eq(3)').addClass('Title'));
  $('<a>').text('Type').addClass('Title').appendTo(headers.children('td:eq(4)').addClass('Title'));
  $('<a>').text('Retained').addClass('Title').appendTo(headers.children('td:eq(5)').addClass('Title'));

  var footer = $('<tr><td/><td colspan="3"/><td/><td/><td/></tr>').attr('id', 'toFooter').appendTo(table);
  periodDropdownTemplate.clone().appendTo(footer.find('td:eq(0)'));
  $('<button>')
    .text('Add Timeout')
    .button()
    .on('click', function () {
      WS.Set('ScoreBoard.Game(' + gameId + ').Period(' + footer.find('#PeriodDropdown').val() + ').InsertTimeout', true);
    })
    .appendTo(footer.find('td:eq(1)'));

  WS.Register(['ScoreBoard.Game(' + gameId + ').Clock(Period).Time']);

  WS.Register(['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber'], function (k, v) {
    footer.find('#PeriodDropdown').val(v);
  });
  WS.Register(
    ['ScoreBoard.Game(' + gameId + ').Period(*).CurrentJamNumber', 'ScoreBoard.Game(' + gameId + ').Period(*).FirstJamNumber'],
    processJamNumber
  );

  WS.Register(['ScoreBoard.Game(' + gameId + ').Period(*).CurrentJam', 'ScoreBoard.Game(' + gameId + ').Period(*).Jam(*).Id']);
  WS.Register(['ScoreBoard.Game(' + gameId + ').Period(*).Timeout'], processTimeout);

  function addJam(p, j, append) {
    var option = $('<option>')
      .attr('value', j)
      .text('J' + j);
    if (append) {
      jamDropdownTemplate[p].append(option.clone());
      table.find('#JamDropdown[period=' + p + ']').append(option);
    } else {
      jamDropdownTemplate[p].prepend(option.clone());
      table.find('#JamDropdown[period=' + p + ']').prepend(option);
    }
  }
  function removeJam(p, j) {
    jamDropdownTemplate[p].children('option[value=' + j + ']').remove();
    table.find('#JamDropdown[period=' + p + '] option[value=' + j + ']').remove();
  }
  function clearPeriod(p) {
    table.find('tr.Timeout[period=' + p + ']').remove();
    jamDropdownTemplate[p].children('option').remove();
    table.find('#JamDropdown[period=' + p + '] option').remove();
    firstJamListed[p] = 0;
    lastJamListed[p] = 0;
  }

  function createJamDropdownTemplate(p) {
    firstJamListed[p] = 0;
    lastJamListed[p] = 0;
    jamDropdownTemplate[p] = $('<select>').attr('id', 'JamDropdown').attr('period', p);
    var option = $('<option>')
      .attr('value', p)
      .text('P' + p);
    _windowFunctions.appendAlphaNumSortedByAttr(periodDropdownTemplate, option.clone(), 'value', 0);
    table.find('#PeriodDropdown').each(function (idx, e) {
      _windowFunctions.appendAlphaNumSortedByAttr($(e), option.clone(), 'value', 0);
    });
    footer.find('#PeriodDropdown').val(WS.state['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber']);
  }

  function processJamNumber(k, v) {
    var p = Number(k.Period);
    if (v == null) {
      if (jamDropdownTemplate[p] != null) {
        periodDropdownTemplate.children('option[value=' + p + ']').remove();
        table.find('#PeriodDropdown option[value=' + p + ']').remove();
        clearPeriod(p);
        delete jamDropdownTemplate[p];
      }
      return;
    }
    if (jamDropdownTemplate[p] == null) {
      createJamDropdownTemplate(p);
    }

    var newFirst = WS.state['ScoreBoard.Game(' + gameId + ').Period(' + p + ').FirstJamNumber'];
    var newLast = WS.state['ScoreBoard.Game(' + gameId + ').Period(' + p + ').CurrentJamNumber'];
    var oldFirst = firstJamListed[p];
    var oldLast = lastJamListed[p];
    var j;
    if (newFirst === 0 && oldFirst === 0) {
      return;
    }
    if (newFirst === 0 && oldFirst > 0) {
      clearPeriod(p);
      return;
    }
    if (newFirst > 0 && oldFirst === 0) {
      for (j = newFirst; j <= newLast; j++) {
        addJam(p, j, true);
      }
      firstJamListed[p] = newFirst;
      lastJamListed[p] = newLast;
      return;
    }
    for (j = oldFirst; j < newFirst; j++) {
      removeJam(p, j);
    }
    for (j = oldFirst; j > newFirst; j--) {
      addJam(p, j - 1, false);
    }
    for (j = oldLast; j < newLast; j++) {
      addJam(p, j + 1, true);
    }
    for (j = oldLast; j > newLast; j--) {
      removeJam(p, j);
    }
    firstJamListed[p] = newFirst;
    lastJamListed[p] = newLast;
  }

  function processTimeout(k, v) {
    var id = k.Timeout;
    if (id === 'noTimeout') {
      return;
    }
    var p = Number(k.Period);
    var prefix = 'ScoreBoard.Game(' + gameId + ').Period(' + k.Period + ').Timeout(' + id + ')';
    var row = table.find('tr.Timeout[toId=' + id + ']');
    if (k.field === 'Id' && v == null && row.length > 0) {
      row.remove();
      return;
    }
    if (k.field === 'PrecedingJamNumber') {
      row.remove();
      row = [];
    }
    if (v != null && row.length === 0) {
      var jam = Number(WS.state[prefix + '.PrecedingJamNumber']);
      var dur = isTrue(WS.state[prefix + '.Running']) ? 'Running' : _timeConversions.msToMinSec(WS.state[prefix + '.Duration'], true);
      var pc = _timeConversions.msToMinSec(
        isTrue(WS.state[prefix + '.Running'])
          ? WS.state['ScoreBoard.Game(' + gameId + ').Clock(Period).Time']
          : WS.state[prefix + '.PeriodClockEnd'],
        isTrue(WS.state[prefix + '.Direction'])
      );
      var type = WS.state[prefix + '.Owner'] + '.' + WS.state[prefix + '.Review'];
      var review = isTrue(WS.state[prefix + '.Review']);
      var retained = isTrue(WS.state[prefix + '.RetainedReview']);
      if (jamDropdownTemplate[p] == null) {
        createJamDropdownTemplate(p);
      }
      row = $('<tr>')
        .addClass('Timeout')
        .attr('toId', id)
        .attr('period', k.Period)
        .attr('jam', jam)
        .append(
          $('<td>')
            .addClass('Period')
            .append(
              periodDropdownTemplate
                .clone()
                .val(p)
                .on('change', function () {
                  WS.Set(prefix + '.PrecedingJam', WS.state['ScoreBoard.Game(' + gameId + ').Period(' + $(this).val() + ').CurrentJam']);
                })
            )
        )
        .append(
          $('<td>')
            .addClass('Jam')
            .append(
              jamDropdownTemplate[p]
                .clone()
                .val(jam)
                .on('change', function () {
                  WS.Set(
                    prefix + '.PrecedingJam',
                    WS.state['ScoreBoard.Game(' + gameId + ').Period(' + p + ').Jam(' + $(this).val() + ').Id']
                  );
                })
            )
        )
        .append($('<td>').addClass('Duration').text(dur))
        .append($('<td>').addClass('PerClock').text(pc))
        .append(
          $('<td>')
            .addClass('Type')
            .append(
              typeDropdownTemplate
                .clone()
                .val(type)
                .on('change', function () {
                  var parts = $(this).val().split('.');
                  WS.Set(prefix + '.Owner', parts[0]);
                  WS.Set(prefix + '.Review', isTrue(parts[1]));
                })
            )
        )
        .append(
          $('<td>')
            .addClass('Retained')
            .append(
              $('<button>')
                .toggleClass('Hide', !review)
                .toggleClass('Active', retained)
                .text('Retained')
                .button()
                .on('click', function () {
                  WS.Set(prefix + '.RetainedReview', !isTrue(WS.state[prefix + '.RetainedReview']));
                })
            )
        )
        .append(
          $('<td>').append(
            $('<button>')
              .text('Delete')
              .button()
              .on('click', function () {
                //TODO: confirmation popup
                WS.Set(prefix + '.Delete', true);
              })
          )
        );
      var inserted = false;
      table.find('tr.Timeout').each(function (i, r) {
        r = $(r);
        if (Number(r.attr('period')) > p || (Number(r.attr('period')) === p && Number(r.attr('jam')) > jam)) {
          r.before(row);
          inserted = true;
          return false;
        }
      });
      if (!inserted) {
        table.find('#toFooter').before(row);
      }
    }
    switch (k.field) {
      case 'Running':
      case 'PeriodClockEnd':
        row
          .find('.PerClock')
          .text(
            _timeConversions.msToMinSec(
              isTrue(WS.state[prefix + '.Running'])
                ? WS.state['ScoreBoard.Game(' + gameId + ').Clock(Period).Time']
                : WS.state[prefix + '.PeriodClockEnd'],
              isTrue(WS.state[prefix + '.Direction'])
            )
          );
        break;
      case 'Running':
      case 'Duration':
        row
          .find('.Duration')
          .text(isTrue(WS.state[prefix + '.Running']) ? 'Running' : _timeConversions.msToMinSec(WS.state[prefix + '.Duration'], true));
        break;
      case 'Review':
        row.find('.Retained button').toggleClass('Hide', !isTrue(v));
      /* falls through */
      case 'Owner':
        row.find('#TypeDropdown').val(WS.state[prefix + '.Owner'] + '.' + WS.state[prefix + '.Review']);
        break;
      case 'RetainedReview':
        row.find('.Retained button').toggleClass('Active', isTrue(v));
        break;
    }
  }

  return dialog.dialog({
    title: 'Timeouts',
    autoOpen: false,
    modal: true,
    width: 750,
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function createTimeDialog(gameId, clock) {
  'use strict';
  var prefix = 'ScoreBoard.Game(' + gameId + ').Clock(' + clock + ')';
  var dialog = $('<div>');
  var table = $('<table>').appendTo(dialog).addClass('TimeDialog');
  var row = $('<tr><td/></tr>');
  row.clone().appendTo(table).addClass('Time');
  row.clone().appendTo(table).addClass('MaximumTime');
  row.clone().appendTo(table).addClass('Direction');

  $.each(['Time', 'MaximumTime'], function (_, e) {
    var rowTable = _crgUtils.createRowTable(3).appendTo(table.find('tr.' + this + '>td'));
    rowTable.find('tr:eq(0)').before('<tr><td colspan="3"/></tr>');

    $('<a>')
      .text(this + ': ')
      .addClass('Title')
      .appendTo(rowTable.find('tr:eq(0)>td').addClass('Title'));
    var time = $('<a>').addClass('Time').appendTo(rowTable.find('tr:eq(0)>td'));
    WS.Register(prefix + '.' + e, function (k, v) {
      time.text(_timeConversions.msToMinSec(v, isTrue(WS.state[prefix + '.Direction'])));
    });
    $('<button>')
      .text('-sec')
      .button()
      .on('click', function () {
        WS.Set(prefix + '.' + e, -1000, 'change');
      })
      .appendTo(rowTable.find('tr:eq(1)>td:eq(0)'));
    $('<button>')
      .text('+sec')
      .button()
      .on('click', function () {
        WS.Set(prefix + '.' + e, +1000, 'change');
      })
      .appendTo(rowTable.find('tr:eq(1)>td:eq(2)'));
    var input = $('<input type="text" size="5">').appendTo(rowTable.find('tr:eq(1)>td:eq(1)'));
    $('<button>')
      .text('Set')
      .addClass('Set')
      .button()
      .appendTo(rowTable.find('tr:eq(1)>td:eq(1)'))
      .on('click', function () {
        WS.Set(prefix + '.' + e, _timeConversions.minSecToMs(input.val()));
        input.val('');
      });
  });
  $('<tr><td/><td/><td/></tr>').insertAfter(table.find('tr.Time table tr:eq(0)'));
  $.each(['Start', 'ResetTime', 'Stop'], function (i, t) {
    $('<button>')
      .text(t)
      .button()
      .on('click', function () {
        WS.Set(prefix + '.' + t, 'true');
      })
      .appendTo(table.find('tr.Time table tr:eq(1)>td:eq(' + i + ')'));
  });

  var rowTable = _crgUtils.createRowTable(1, 2).appendTo(table.find('tr.Direction>td'));
  toggleButton(prefix + '.Direction', 'Counting Down', 'Counting Up').appendTo(rowTable.find('tr:eq(1)>td'));

  dialog.dialog({
    autoOpen: false,
    modal: true,
    width: 400,
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });

  WS.Register(prefix + '.Name', function (k, v) {
    dialog.dialog('option', 'title', v + ' Clock');
  });

  return dialog;
}
