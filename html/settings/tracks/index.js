WS.Register([
  'ScoreBoard.Game(*).Name',
  'ScoreBoard.Game(*).EventInfo',
  'ScoreBoard.CurrentGame.Game',
  'ScoreBoard.CurrentGame.State',
  'ScoreBoard.Tracks.ServerGame(*)',
  'ScoreBoard.Settings.Setting(ScoreBoard.Tracks.Track)',
]);

$(function () {
  $.get('/urls', function (data) {
    $('#Urls').text(
      data
        .trim()
        .split('\n')
        .map(function (url) {
          return url.replace(/^https?:\/\//, '').replace(/\/$/, '');
        })
        .filter(function (address) {
          // IPv6 and loopback addresses are no use to the tracks
          return /^\d+\.\d+\.\d+\.\d+:/.test(address) && address.indexOf('127.') !== 0;
        })
        .join(', '),
    );
  });
});

function _trkSortKey(date, time, gameNo, name) {
  // games without a date are listed last
  return (date || '9999') + ' ' + (time || '99:99') + ' ' + ('0000' + (gameNo || '')).slice(-4) + ' ' + name;
}

function trkGameOrder(k, v) {
  const prefix = 'ScoreBoard.Game(' + k.Game + ').EventInfo(';
  return _trkSortKey(WS.state[prefix + 'Date)'], WS.state[prefix + 'StartTime)'], WS.state[prefix + 'GameNo)'], v);
}

function trkServerGameOrder(k, v) {
  const prefix = k.upTo('ServerGame') + '.';
  return _trkSortKey(WS.state[prefix + 'Date'], WS.state[prefix + 'StartTime'], WS.state[prefix + 'GameNo'], v);
}

function trkTime(k, v) {
  if (!v || v === '0') {
    return '';
  }
  const d = new Date(Number(v));
  return (
    _timeConversions.twoDigit(d.getHours()) +
    ':' +
    _timeConversions.twoDigit(d.getMinutes()) +
    ':' +
    _timeConversions.twoDigit(d.getSeconds())
  );
}

function trkContact(k, v) {
  return v && v !== '0' ? 'Last contact: ' + trkTime(k, v) : '';
}

function trkIsOn(k, v) {
  return isTrue(v);
}

function trkTrackOrder(k, v) {
  return ('0000' + v).slice(-4);
}

function trkConnection(k) {
  const prefix = k.upTo('Track') + '.';
  if (isTrue(WS.state[prefix + 'Connected'])) {
    return 'Connected (' + WS.state[prefix + 'RemoteAddr'] + ')';
  }
  return 'Not connected since ' + trkTime(k, WS.state[prefix + 'LastSeen']);
}

function trkPeriod(k) {
  const prefix = k.upTo('Track') + '.';
  const period = WS.state[prefix + 'Period'];
  return period ? period + ' (' + _timeConversions.msToMinSec(WS.state[prefix + 'PeriodClock']) + ')' : '';
}

function _trkClockTime(ms) {
  ms = Number(ms);
  if (ms < 3600000) {
    return _timeConversions.msToMinSec(ms);
  }
  // a long time before the game starts
  return Math.floor(ms / 3600000) + ':' + ('0' + _timeConversions.msToMinSec(ms % 3600000)).slice(-5);
}

function trkClock(k) {
  const prefix = k.upTo('Track') + '.';
  const clock = WS.state[prefix + 'Clock'];
  return clock ? clock + ' ' + _trkClockTime(WS.state[prefix + 'ClockTime']) : '';
}

function trkScore(k) {
  const prefix = k.upTo('Track') + '.';
  return (
    WS.state[prefix + 'Team1'] + ' ' + WS.state[prefix + 'Score1'] + ' – ' + WS.state[prefix + 'Score2'] + ' ' + WS.state[prefix + 'Team2']
  );
}

function trkServerConnected(k, v) {
  return isTrue(v) ? 'Connected' : 'Not connected';
}

function trkEditLink(k) {
  return '/nso/hnso?game=' + k.TrackGame;
}

function trkTrackGameStatus(k, v) {
  return v && v !== '0' ? 'Handed over for review at ' + trkTime(k, v) : 'Running on the track (read-only here)';
}

function trkIsMine(k, v) {
  return v !== '' && v === WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Tracks.Track)'];
}

function trkIsElsewhere(k, v) {
  return v !== '' && v !== WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Tracks.Track)'];
}

function trkServerGameStatus(k, v) {
  const prefix = k.upTo('ServerGame') + '.';
  if (isTrue(WS.state[prefix + 'HandedOver'])) {
    return 'Handed over for review (track ' + v + ')';
  } else if (v) {
    return 'On track ' + v;
  }
  return WS.state[prefix + 'State'];
}

function trkPushStatus(k) {
  const prefix = k.upTo('PulledGame') + '.';
  const error = WS.state[prefix + 'Error'];
  const handedOver = WS.state[prefix + 'HandedOver'];
  const pushed = WS.state[prefix + 'Pushed'];
  if (handedOver && handedOver !== '0') {
    return 'Handed over for review at ' + trkTime(k, handedOver) + ' (read-only here)';
  } else if (error) {
    return error;
  }
  return pushed && pushed !== '0' ? 'Sent at ' + trkTime(k, pushed) : 'Not sent yet';
}

function _trkConfirm(title, text, label, onYes) {
  const buttons = {};
  buttons[label] = function () {
    onYes();
    $(this).dialog('close');
  };
  buttons.Cancel = function () {
    $(this).dialog('close');
  };
  $('<div>')
    .append($('<p>').text(text))
    .dialog({
      title: title,
      modal: true,
      width: 500,
      close: function () {
        $(this).dialog('destroy').remove();
      },
      buttons: buttons,
    });
}

function trkPull(k) {
  const pull = function () {
    WS.Set(k.upTo('ServerGame') + '.Pull', true);
  };
  if (WS.state['ScoreBoard.CurrentGame.State'] === 'Running') {
    _trkConfirm('Pull Game', 'The game on this scoreboard is still running. Replace it with this game on the scoreboard?', 'Pull', pull);
  } else {
    pull();
  }
}

function trkHandOver(k) {
  _trkConfirm(
    'Hand Over',
    'Send this game to the tournament server for review? It can only be changed there after that, unless you take it back.',
    'Hand Over',
    function () {
      WS.Set(k.upTo('PulledGame') + '.HandOver', true);
    },
  );
}

function trkTakeBack(k) {
  _trkConfirm(
    'Take Back',
    'Pull this game back from the tournament server, with the changes made there? It can then only be changed here again.',
    'Take Back',
    function () {
      WS.Set(k.upTo('PulledGame') + '.TakeBack', true);
    },
  );
}

function trkForget(k) {
  _trkConfirm(
    'Forget',
    'Stop sending this game to the tournament server? The game stays on this scoreboard. On the tournament server it shows as running on this track until it is released there.',
    'Forget',
    function () {
      WS.Set(k.upTo('PulledGame') + '.Forget', true);
    },
  );
}

function trkRelease(k) {
  _trkConfirm(
    'Release',
    "Let another track pull this game? Only do this when the track can't carry on with it, e.g. its laptop broke down. Changes the track hasn't sent yet are lost.",
    'Release',
    function () {
      WS.Set(k.upTo('TrackGame') + '.Release', true);
    },
  );
}
