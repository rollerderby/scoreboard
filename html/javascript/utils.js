'use strict';

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

function sbClockSelect(k) {
  var jam = isTrue(WS.state[k.upTo('Game') + '.InJam']);
  var timeout = isTrue(WS.state[k.upTo('Game') + '.Clock(Timeout).Running']);
  var lineup = isTrue(WS.state[k.upTo('Game') + '.Clock(Lineup).Running']);
  var intermission = isTrue(WS.state[k.upTo('Game') + '.Clock(Intermission).Running']);

  var clock = 'NoClock';
  if (jam) {
    clock = 'Jam';
  } else if (timeout) {
    clock = 'Timeout';
  } else if (lineup) {
    clock = 'Lineup';
  } else if (intermission) {
    clock = 'Intermission';
  }

  $('.Clock,.SlideDown').removeClass('Show');
  $('.ShowIn' + clock).addClass('Show');
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
  if (elem.closest('[sbSheetType]').attr('sbSheetType') !== 'sheet') {
    elem.append(elem.children('tr').get().reverse());
  }
}

function _sbUpdateUrl(key, val) {
  let url = new URL(window.location);
  url.searchParams.set(key, val);
  window.history.replaceState(null, '', url);
}
