(function () {
  const view = _windowFunctions.checkParam('preview', 'true') ? 'Preview' : 'View';
  $('body').attr('sbPrefix', '&: ScoreBoard.Settings.Setting(ScoreBoard.' + view + ' : )');

  WS.Register('ScoreBoard.Settings.Setting(ScoreBoard.' + view + '_CurrentView)', function (k, v) {
    $('div#video>video').each(function () {
      this.pause();
    });
    $('.DisplayPane').removeClass('Show');
    $('div#' + v + '.DisplayPane').addClass('Show');
    $('div#' + v + '.DisplayPane>video').each(function () {
      this.currentTime = 0;
      this.play();
    });
  });

  // the display's theme is set by the SBO, unless the URL has one
  if (!_windowFunctions.getParam('theme')) {
    WS.Register('ScoreBoard.Settings.Setting(ScoreBoard.' + view + '_Theme)', function (k, v) {
      $('head link.DisplayTheme').remove();
      if (v) {
        // a theme folder has its stylesheet in theme.css
        $('<link>')
          .addClass('DisplayTheme')
          .attr({ href: /\/$/.test(v) ? v + 'theme.css' : v, type: 'text/css', rel: 'stylesheet' })
          .appendTo('head');
      }
      $(window).trigger('resize');
    });
  }

  WS.Register(
    ['ScoreBoard.Settings.Setting(ScoreBoard.' + view + '_BoxStyle)', 'ScoreBoard.Settings.Setting(ScoreBoard.' + view + '_SidePadding)'],
    {
      triggerBatchFunc: function () {
        $(window).trigger('resize');
      },
    }
  );

  WS.Register(
    [
      'ScoreBoard.CurrentGame.Clock(Timeout).Running',
      'ScoreBoard.CurrentGame.TimeoutOwner',
      'ScoreBoard.CurrentGame.OfficialReview',
      'ScoreBoard.CurrentGame.Team(*).Timeouts',
    ],
    sbSetActiveTimeout
  );

  // Show Clocks
  WS.Register(['ScoreBoard.Settings.Setting(ScoreBoard.' + view + '_ClockAfterTimeout)', 'ScoreBoard.CurrentGame.Clock(*).Running', 'ScoreBoard.CurrentGame.InJam'],
    function (k) { _sbClockSelect('ScoreBoard.CurrentGame', 'ScoreBoard.' + view + '_ClockAfterTimeout') });
})();

WS.AfterLoad(function () {
  const switchTimeMs = 5000;
  const div = $('#SponsorBox');
  var lastSwitch;
  function setNextSrc() {
    const banners = $('#Banners [File]');
    if (banners.length === 0) {
      div.find('img').attr('src', '');
    } else {
      // Use time so different scoreboards will be using the same images.
      const index = Math.round((new Date().getTime() / switchTimeMs) % banners.length);
      div.find('.NextImg>img').attr('src', $(banners[index]).attr('src'));
    }
  }
  function nextImgFunction() {
    if (new Date().getTime() - lastSwitch > 1020) {
      // last switch completed, can do new one
      const cur = $(div.find('.CurrentImg')[0]);
      const nex = $(div.find('.NextImg')[0]);
      const fin = $(div.find('.FinishedImg')[0]);
      cur.removeClass('CurrentImg').addClass('FinishedImg');
      nex.removeClass('NextImg').addClass('CurrentImg');
      fin.removeClass('FinishedImg').addClass('NextImg');
      setNextSrc();
    }
    // Align to clock, so different scoreboards will be synced.
    lastSwitch = new Date().getTime();
    setTimeout(nextImgFunction, switchTimeMs - (lastSwitch % switchTimeMs));
  }

  setNextSrc();
  nextImgFunction();
});

function dspIsFlat(k, v) {
  return v && v.includes('box_flat');
}

function dspIsBright(k, v) {
  return v && v.includes('bright');
}

function dspToLeftMargin(k, v) {
  return v ? v + '%' : '0%';
}

function dspToWidth(k, v) {
  return v ? 100 - 2 * v + '%' : '100%';
}

function dspIsLeft(k, v, elem) {
  return (elem.attr('Team') === '1') !== isTrue(v);
}

function dspIsRight(k, v, elem) {
  return (elem.attr('Team') !== '1') !== isTrue(v);
}

function dspIsJamming(k, v, elem) {
  return (!isTrue(v) && elem.attr('Position') === 'Jammer') || (isTrue(v) && elem.attr('Position') === 'Pivot');
}

function dspIsNotJamming(k, v, elem) {
  return !dspIsJamming(k, v, elem);
}

function dspToJammerName(k, v) {
  return v || (isTrue(WS.state[k.upTo('Team') + '.DisplayLead']) ? 'Lead' : '');
}
