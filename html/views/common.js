function jammer(k, v) {
  'use strict';
  var id = getTeamId(k);
  var prefix = 'ScoreBoard.CurrentGame.Team(' + id + ').';
  var jammerName = WS.state[prefix + 'Position(Jammer).Name'];
  var pivotName = WS.state[prefix + 'Position(Pivot).Name'];
  var leadJammer = isTrue(WS.state[prefix + 'DisplayLead']);
  var starPass = isTrue(WS.state[prefix + 'StarPass']);
  var inJam = isTrue(WS.state['ScoreBoard.CurrentGame.InJam']);

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
  $('.Team' + id + ' .Lead').toggleClass('HasLead', leadJammer && !starPass);
  $('.Team' + id).toggleClass('HasJammerName', jn !== '');
  $('.Team' + id + ' .Lead').toggleClass('HasStarPass', starPass);
  return jn;
}

function getTeamId(k) {
  'use strict';
  if (k.indexOf('Team(1)') > 0) {
    return '1';
  }
  if (k.indexOf('Team(2)') > 0) {
    return '2';
  }
  return null;
}

function nameUpdate(k, v) {
  'use strict';
  $('.Team' + getTeamId(k)).toggleClass('HasName', v !== '');
  return v;
}

function logoUpdate(k, v) {
  'use strict';
  var id = getTeamId(k);
  var prefix = 'ScoreBoard.CurrentGame.Team(' + id + ').';
  var logo = WS.state[prefix + 'Logo'];
  if (logo == null) {
    logo = '';
  }
  if (logo !== '') {
    logo = 'url("' + logo + '")';
  }

  $('.Team' + id + '>.Logo').css('background-image', logo);
  $('.Team' + id).toggleClass('HasLogo', logo !== '');
  var nameAutoFit = $('.Team' + id + '>.Name>div').data('AutoFit');
  if (nameAutoFit) {
    nameAutoFit();
  }
}

function smallDescriptionUpdate(k, v) {
  'use strict';
  var lc = WS.state['ScoreBoard.CurrentGame.Clock(Lineup).Running'];
  var tc = WS.state['ScoreBoard.CurrentGame.Clock(Timeout).Running'];
  var to = WS.state['ScoreBoard.CurrentGame.TimeoutOwner'].slice(-1);
  var or = WS.state['ScoreBoard.CurrentGame.OfficialReview'];
  var lcn = WS.state['ScoreBoard.CurrentGame.Clock(Lineup).Name'];
  var tcn = WS.state['ScoreBoard.CurrentGame.Clock(Timeout).Name'];
  var ret = '';

  $.each(['1', '2'], function (idx, id) {
    var tto = WS.state['ScoreBoard.CurrentGame.Team(' + id + ').Timeouts'];
    var tor = WS.state['ScoreBoard.CurrentGame.Team(' + id + ').OfficialReviews'];
    var tror = WS.state['ScoreBoard.CurrentGame.Team(' + id + ').RetainedOfficialReview'];
    $('.Team' + id + ' .Timeout1').toggleClass('Used', tto < 1);
    $('.Team' + id + ' .Timeout2').toggleClass('Used', tto < 2);
    $('.Team' + id + ' .Timeout3').toggleClass('Used', tto < 3);
    $('.Team' + id + ' .OfficialReview1').toggleClass('Used', tor < 1);
    $('.Team' + id + ' .OfficialReview1').toggleClass('Retained', tror);
  });

  $('.Team .Dot').removeClass('Active');
  $('.Clock.Description,.Team>.Timeouts,.Team>.OfficialReviews').removeClass('Red');
  if (lc) {
    ret = lcn;
  } else if (tc) {
    $('.Clock.Description').addClass('Red');

    ret = tcn;
    if (to !== '') {
      if (to === 'O') {
        ret = 'Official Timeout';
      } else {
        var dotSel;
        if (or) {
          ret = 'Official Review';
          $('.Team' + to + '>.OfficialReviews:not(.Header)').addClass('Red');
          dotSel = '.Team' + to + ' .OfficialReview1';
        } else {
          ret = 'Team Timeout';
          $('.Team' + to + '>.Timeouts').addClass('Red');
          dotSel = '.Team' + to + ' .Timeout' + (WS.state['ScoreBoard.CurrentGame.Team(' + to + ').Timeouts'] + 1);
        }
        console.log(dotSel, $(dotSel).length);
        $(dotSel).addClass('Active');
      }
    }
  }
  return ret;
}

function intermissionDisplay() {
  'use strict';
  var num = WS.state['ScoreBoard.CurrentGame.Clock(Intermission).Number'];
  var max = WS.state['ScoreBoard.CurrentGame.Rule(Period.Number)'];
  var isOfficial = WS.state['ScoreBoard.CurrentGame.OfficialScore'];
  var ret = '';

  if (isOfficial) {
    ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Official)'];
  } else if (num === 0) {
    ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.PreGame)'];
  } else if (num != max) {
    ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Intermission)'];
  } else {
    ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Unofficial)'];
  }

  $('.Clock.Intermission .Time').toggleClass('Hide', num == max || isOfficial);
  return ret;
}

function toClockInitialNumber(k, v) {
  'use strict';
  var ret = '';
  $.each(['Period', 'Jam'], function (i, c) {
    if (k.indexOf('Clock(' + c + ')') > -1) {
      var name = WS.state['ScoreBoard.CurrentGame.Clock(' + c + ').Name'];
      var number = WS.state['ScoreBoard.CurrentGame.Clock(' + c + ').Number'];

      if (name != null && number != null) {
        ret = name.substring(0, 1) + number;
      }

      if (name === 'Period' && WS.state['ScoreBoard.CurrentGame.Rule(Period.Number)'] == 1) {
        ret = 'Game';
      }
    }
  });
  return ret;
}

function toTime(k, v) {
  'use strict';
  k = WS._enrichProp(k);
  var isCountDown = isTrue(WS.state['ScoreBoard.CurrentGame.Clock(' + k.Clock + ').Direction']);
  return _timeConversions.msToMinSecNoZero(v, isCountDown);
}

function toSP(k, v) {
  'use strict';
  return isTrue(v) ? 'SP' : '';
}

function clockRunner(k, v) {
  'use strict';
  var lc = WS.state['ScoreBoard.CurrentGame.Clock(Lineup).Running'];
  var tc = WS.state['ScoreBoard.CurrentGame.Clock(Timeout).Running'];
  var ic = WS.state['ScoreBoard.CurrentGame.Clock(Intermission).Running'];

  var clock = 'Jam';
  if (isTrue(tc)) {
    clock = 'Timeout';
  } else if (isTrue(lc)) {
    clock = 'Lineup';
  } else if (isTrue(ic)) {
    clock = 'Intermission';
  }

  $('.Clock,.SlideDown').removeClass('Show');
  $('.SlideDown.ShowIn' + clock + ',.Clock.ShowIn' + clock).addClass('Show');
}

// Show Clocks
WS.Register('ScoreBoard.CurrentGame.Clock(*).Running', function (k, v) {
  'use strict';
  clockRunner(k, v);
});
WS.Register('ScoreBoard.CurrentGame.Clock(*).Direction');

WS.Register('ScoreBoard.CurrentGame.Rule(Period.Number)');
WS.Register('ScoreBoard.CurrentGame.InJam');
