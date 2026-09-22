function sbNewUuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = (Math.random() * 16) | 0,
      v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

function sbCloseDialog(k, v, elem) {
  elem.closest('.ui-dialog-content').dialog('close');
}

function sbCloseDialogIfNull(k, v, elem) {
  if (v == null) {
    elem.closest('.ui-dialog-content').dialog('close');
  }
}

function _sbClockSelect(game, setting) {
  var jam = isTrue(WS.state[game + '.InJam']);
  var timeout = isTrue(WS.state[game + '.Clock(Timeout).Running']);
  var lineup = isTrue(WS.state[game + '.Clock(Lineup).Running']);
  var postTimeout = lineup && timeout;
  var postTimeoutClock = WS.state['ScoreBoard.Settings.Setting(' + setting + ')'];
  var intermission = isTrue(WS.state[game + '.Clock(Intermission).Running']);
  var final = isTrue(WS.state[game + '.OfficialScore']);
  var clockDuringFinal = isTrue(WS.state[game + '.ClockDuringFinalScore']);

  var clock = 'NoClock';
  var betweenJams = false;
  if (jam) {
    clock = 'Jam';
  } else if (postTimeout) {
    clock = "PostTimeout" + postTimeoutClock;
    betweenJams = true;
  } else if (lineup) {
    clock = 'Lineup';
    betweenJams = true;
  } else if (timeout) {
    clock = 'Timeout';
    betweenJams = true;
  } else if (intermission) {
    if (final && !clockDuringFinal) {
      clock = 'Final';
    } else {
      clock = 'Intermission';
    }
  }

  $('.Clock,.SlideDown').removeClass('Show');
  $('.ShowIn' + clock).addClass('Show');
  if (betweenJams) { $('.ShowInBetweenJams').addClass('Show'); }
}

function sbSetActiveTimeout(k) {
  var to = WS.state[k.upTo('Game') + '.TimeoutOwner'].slice(-1);
  var or = WS.state[k.upTo('Game') + '.OfficialReview'];

  $('.Team .Dot').removeClass('Current');

  if (to && to !== 'O') {
    var dotSel;
    if (or) {
      dotSel = '[Team=' + to + '] .OfficialReview1';
    } else {
      dotSel = '[Team=' + to + '] .Timeout' + (WS.state[k.upTo('Game') + '.Team(' + to + ').Timeouts'] + 1);
    }
    $(dotSel).addClass('Current');
  }
}

function sbReverseOnNonSheet(k, v, elem) {
  if (elem.closest('[sbSheetStyle]').attr('sbSheetStyle') !== 'sheet') {
    elem.append(elem.children('tr').get().reverse());
  }
}

function _sbUpdateUrl(key, val) {
  window.history.replaceState(null, '', _urlWithParam(key, val));
}
