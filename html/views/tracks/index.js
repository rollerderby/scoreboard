(function () {
  // tiles keep this shape; the grid picks the number of columns that makes them biggest
  const tileAspect = 16 / 9;

  function trvLayout() {
    const tiles = $('#Tracks>.Tile');
    const n = tiles.length;
    $('#NoTracks').toggleClass('sbHide', n > 0);
    if (n === 0) {
      return;
    }
    const w = $('#Tracks').width();
    const h = $('#Tracks').height();
    var best = { cols: 1, rows: n, width: 0 };
    for (var cols = 1; cols <= n; cols++) {
      const rows = Math.ceil(n / cols);
      const width = Math.min(w / cols, (h / rows) * tileAspect);
      if (width > best.width) {
        best = { cols: cols, rows: rows, width: width };
      }
    }
    $('#Tracks').css({
      'grid-template-columns': 'repeat(' + best.cols + ', 1fr)',
      'grid-template-rows': 'repeat(' + best.rows + ', 1fr)',
    });
    const gap = Math.min(w, h) * 0.01;
    const width = Math.min((w - gap * best.cols) / best.cols, ((h - gap * best.rows) / best.rows) * tileAspect);
    tiles.children('.Inner').css({ width: width + 'px', height: width / tileAspect + 'px' });
    tiles.find('.AutoFit').each(function () {
      const fit = $(this).children('div').data('AutoFit');
      if (fit) {
        setTimeout(fit, 50);
      }
    });
  }

  WS.Register('ScoreBoard.Tracks.Track(*).Id', function () {
    setTimeout(trvLayout, 0); // after the tile has been added or removed
  });
  $(window).on('resize', trvLayout);
  $(trvLayout);
})();

function trvTrackOrder(k, v) {
  return ('0000' + v).slice(-4);
}

function trvTrackName(k, v) {
  return /^[0-9]+$/.test(v) ? 'Track ' + v : v;
}

function trvTime(k, v) {
  const ms = Number(v);
  if (ms < 3600000) {
    return _timeConversions.msToMinSec(ms);
  }
  // a long time before the game starts
  return Math.floor(ms / 3600000) + ':' + ('0' + _timeConversions.msToMinSec(ms % 3600000)).slice(-5);
}

function trvIsLineup(k, v) {
  return v === 'Lineup';
}

// the dot of a running team timeout or official review blinks, as on the main scoreboard
function trvIsCurrent(k, v, elem) {
  const prefix = k.upTo('Track') + '.';
  const team = elem.attr('team');
  return (
    WS.state[prefix + 'TimeoutOwner'] === team &&
    !isTrue(WS.state[prefix + 'OfficialReview']) &&
    Number(WS.state[prefix + 'Timeouts' + team]) + 1 === Number(elem.attr('dot'))
  );
}

function trvIsCurrentReview(k, v, elem) {
  const prefix = k.upTo('Track') + '.';
  return WS.state[prefix + 'TimeoutOwner'] === elem.attr('team') && isTrue(WS.state[prefix + 'OfficialReview']);
}

function trvStatus(k) {
  const prefix = k.upTo('Track') + '.';
  if (!isTrue(WS.state[prefix + 'Connected'])) {
    const seen = Number(WS.state[prefix + 'LastSeen']);
    return seen ? 'No connection since ' + new Date(seen).toTimeString().slice(0, 5) : 'No connection';
  }
  switch (WS.state[prefix + 'GameState']) {
    case 'Running':
      return 'P' + WS.state[prefix + 'Period'] + ' · Jam ' + WS.state[prefix + 'Jam'];
    case 'Finished':
      return 'Final';
    case 'Prepared':
      return 'Not started';
    default:
      return 'No game';
  }
}
