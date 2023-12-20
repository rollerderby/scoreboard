'use strict';

(function () {
  const view = _windowFunctions.checkParam('preview', 'true') ? 'Preview' : 'View';
  $('body').attr('sbPrefix', '&: ScoreBoard.Settings.Setting(ScoreBoard.' + view + ' : )');

  WS.Register('ScoreBoard.Settings.Setting(ScoreBoard.' + view + '_CurrentView)', function (k, v) {
    $('div#video>video').each(function () {
      this.pause();
    });
    $('.DisplayPane.Show').addClass('SlideOut');
    $('.DisplayPane').removeClass('Show');
    $('div#' + v + '.DisplayPane').addClass('Show');
    $('div#' + v + '.DisplayPane>video').each(function () {
      this.currentTime = 0;
      this.play();
    });
  });

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
  WS.Register(['ScoreBoard.CurrentGame.Clock(*).Running', 'ScoreBoard.CurrentGame.InJam'], sbClockSelect);
})();

WS.AfterLoad(function () {
  const switchTimeMs = 5000;
  const div = $('#SponsorBox');
  function setNextSrc() {
    const banners = $('#Banners [File]');
    if (banners.length === 0) {
      div.find('.NextImg>img').attr('src', '').toggle(false);
    } else {
      // Use time so different scoreboards will be using the same images.
      const index = Math.round((new Date().getTime() / switchTimeMs) % banners.length);
      div.find('.NextImg>img').attr('src', $(banners[index]).attr('src')).toggle(true);

      // Also set the current image. This gets a banner up when the page is loaded,
      // and is otherwise a noop.
      div.find('.CurrentImg>img').attr('src', $(banners[index]).attr('src')).toggle(true);
    }
  }
  function nextImgFunction() {
    var cur = $(div.find('.CurrentImg')[0]);
    var nex = $(div.find('.NextImg')[0]);
    var fin = $(div.find('.FinishedImg')[0]);
    cur.removeClass('CurrentImg').addClass('FinishedImg');
    nex.removeClass('NextImg').addClass('CurrentImg');
    fin.removeClass('FinishedImg').addClass('NextImg');
    setNextSrc();
    // Align to clock, so different scoreboards will be synced.
    setTimeout(nextImgFunction, switchTimeMs - (new Date().getTime() % switchTimeMs));
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

function dspToJammerName(k, v) {
  const id = k.Team;
  const prefix = 'ScoreBoard.CurrentGame.Team(' + id + ').';
  let jammerName = WS.state[prefix + 'Position(Jammer).Name'];
  let pivotName = WS.state[prefix + 'Position(Pivot).Name'];
  const leadJammer = isTrue(WS.state[prefix + 'DisplayLead']);
  const starPass = isTrue(WS.state[prefix + 'StarPass']);
  const inJam = isTrue(WS.state['ScoreBoard.CurrentGame.InJam']);

  if (jammerName == null || jammerName === '') {
    jammerName = leadJammer ? 'Lead' : '';
  }
  if (pivotName == null) {
    pivotName = '';
  }

  var jn = !starPass ? jammerName : pivotName;
  if (!inJam) {
    jn = ''; // When no clocks are running, do not show jammer names.
  }
  $('[Team=' + id + '] .Lead').toggleClass('HasLead', leadJammer && !starPass);
  $('[Team=' + id + ']').toggleClass('HasJammerName', jn !== '');
  $('[Team=' + id + '] .Lead').toggleClass('HasStarPass', starPass);
  return jn;
}
