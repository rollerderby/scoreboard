function str_sort(a, b) {
  'use strict';
  return $(a).attr('data-sort') == null || $(b).attr('data-sort') < $(a).attr('data-sort') ? 1 : -1;
}

jQuery.fn.sortDivs = function sortDivsStr() {
  'use strict';
  $('> div', this[0]).sort(str_sort).appendTo(this[0]);
};

$(initialize);

function initialize() {
  'use strict';
  WS.Register([
    'ScoreBoard.CurrentGame.Clock(Intermission).Number',
    'ScoreBoard.CurrentGame.Rulesets.CurrentRule(Period.Number)',
    'ScoreBoard.CurrentGame.Settings.Setting(ScoreBoard.Intermission.PreGame)',
    'ScoreBoard.CurrentGame.Settings.Setting(ScoreBoard.Intermission.Unofficial)',
    'ScoreBoard.CurrentGame.Settings.Setting(ScoreBoard.Intermission.Official)',
    'ScoreBoard.CurrentGame.Settings.Setting(ScoreBoard.Intermission.Intermission)',
    'ScoreBoard.CurrentGame.Team(*).StarPass',
  ]);

  WS.Register(
    [
      'ScoreBoard.CurrentGame.Clock(Timeout).Running',
      'ScoreBoard.CurrentGame.TimeoutOwner',
      'ScoreBoard.CurrentGame.OfficialReview',
      'ScoreBoard.CurrentGame.Team(*).Timeouts',
      'ScoreBoard.CurrentGame.Team(*).OfficialReviews',
      'ScoreBoard.CurrentGame.Team(*).RetainedOfficialReview',
    ],
    function (k, v) {
      smallDescriptionUpdate(k, v);
    }
  );

  WS.Register(
    [
      'ScoreBoard.CurrentGame.Period(*).Jam(*).TeamJam(*).JamScore',
      'ScoreBoard.CurrentGame.Period(*).Jam(*).TeamJam(*).Lead',
      'ScoreBoard.CurrentGame.Period(*).Jam(*).TeamJam(*).Lost',
    ],
    function (k, v) {
      jamData(k, v);
    }
  );

  WS.Register(
    [
      'ScoreBoard.CurrentGame.Team(*).Skater(*).Name',
      'ScoreBoard.CurrentGame.Team(*).Skater(*).RosterNumber',
      'ScoreBoard.CurrentGame.Team(*).Skater(*).Flags',
      'ScoreBoard.CurrentGame.Team(*).Skater(*).Role',
    ],
    function (k, v) {
      var me = '.RosterTeam' + k.Team + ' .Team' + k.Team + ' .Skater[data-skaterId=' + k.Skater + ']';
      var mb = '.PenaltyTeam' + k.Team + ' .Team' + k.Team + ' .Skater[data-skaterId=' + k.Skater + ']';
      if (v == null) {
        $(me).remove();
        $(mb).remove();
        return;
      }
      ensureSkaterExists(k.Skater, k.Team);

      if (k.field === 'Flags') {
        $('.' + k.field, me).attr('data-flag', v);
        $(mb).attr('data-flag', v);
      } else if (k.field === 'Role') {
        // Hide skater row in penalties panel only
        if (v === 'NotInGame') {
          $(mb).addClass('NoShow');
        } else {
          $(mb).removeClass('NoShow');
        }
        updateSort(mb);
      } else if (k.field === 'RosterNumber') {
        $('.Number', me).text(v === '' ? '\xa0' : v);
        $('.Number', mb).text(v === '' ? '\xa0' : v);
        updateSort(me);
        updateSort(mb);
      } else {
        // Name, replace empty string with nbsp
        $('.' + k.field, me).text(v === '' ? '\xa0' : v);
        $('.' + k.field, mb).text(v === '' ? '\xa0' : v);
      }
    }
  );

  WS.Register('ScoreBoard.CurrentGame.Team(*).Skater(*).Penalty(*).Code', function (k, v) {
    if (k.Penalty === 0) {
      // Foulout/Expulsion.
      return;
    }
    var sel = '.PenaltyTeam' + k.Team + ' .Team' + k.Team + ' .Skater[data-skaterId=' + k.Skater + ']';
    if (v == null) {
      $('.Number-' + k.Penalty, sel).remove();
      $(sel).attr('data-count', $('.Penalty', sel).length);
      return;
    }
    ensureSkaterExists(k.Skater, k.Team);
    createPenalty(sel, k.Penalty, v);
  });

  WS.Register('ScoreBoard.CurrentGame.Team(*).Position(*).PenaltyBox', function (k, v) {
    $('.Team' + k.Team + ' .' + k.Position).toggleClass('InBox', isTrue(v));
  });

  WS.Register('ScoreBoard.CurrentGame.Team(*).Position(*).CurrentPenalties', function (k, v) {
    $('.Team' + k.Team + ' .' + k.Position).toggleClass('Penalized', v !== '');
  });

  WS.Register('ScoreBoard.CurrentGame.Team(*).StarPass', function (k, v) {
    $('.Team' + k.Team + ' .Jammer').toggleClass('Jamming', !isTrue(v));
    $('.Team' + k.Team + ' .Pivot').toggleClass('Jamming', isTrue(v));
  });

  WS.Register('ScoreBoard.CurrentGame.Team(*).Lead', function (k, v) {
    $('.Team' + k.Team).toggleClass('Lead', isTrue(v));
  });

  WS.Register(['ScoreBoard.CurrentGame.Team(*).Color'], function (k, v) {
    $(document)
      .find('.ColourTeam' + k.Team)
      .css('color', WS.state['ScoreBoard.CurrentGame.Team(' + k.Team + ').Color(overlay_fg)'] || '');
    $(document)
      .find('.ColourTeam' + k.Team)
      .css('background', WS.state['ScoreBoard.CurrentGame.Team(' + k.Team + ').Color(overlay_bg)'] || '');
  });

  WS.Register('ScoreBoard.CurrentGame.Team(*).Logo', function (k, v) {
    if (v && v !== '') {
      $('img.TeamLogo' + k.Team)
        .attr('src', v)
        .css('display', 'block');
      $('img.TeamLogo' + k.Team)
        .parent()
        .removeClass('NoLogo');
    } else {
      $('img.TeamLogo' + k.Team).css('display', 'none');
      $('img.TeamLogo' + k.Team)
        .parent()
        .addClass('NoLogo');
    }
  });

  WS.Register('ScoreBoard.CurrentGame.Clock(Period).Number', function (k, v) {
    if (v === 2) {
      $('.PPJBox .Team .Period2').show();
    } else {
      $('.PPJBox .Team .Period2').hide();
    }
  });

  WS.Register(['ScoreBoard.Settings.Setting(Overlay.Interactive.BackgroundColor)'], function (k, v) {
    $('body').css('backgroundColor', v || 'transparent');
  });

  WS.Register(
    ['ScoreBoard.Settings.Setting(Overlay.Interactive.Clock)', 'ScoreBoard.Settings.Setting(Overlay.Interactive.Score)'],
    function (k, v) {
      $('div[data-setting="' + k + '"]').toggleClass('Show', v === 'On');
    }
  );

  WS.Register(
    [
      'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowLineups)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowAllNames)',
      'ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)',
    ],
    function (k, v) {
      var jammers = WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)'] === 'On';
      var lineups =
        WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowLineups)'] === 'On' &&
        isTrue(WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)']);
      var allNames = WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowAllNames)'] === 'On';
      $('.TeamBox .JammerBox')
        .toggleClass('JammerNames', jammers || allNames)
        .toggleClass('AllNames', allNames);
      $('.TeamBox')
        .toggleClass('Lineups', lineups)
        .toggleClass('Wide', lineups && jammers && !allNames)
        .toggleClass('XWide', lineups && allNames);
      $('div[data-setting="ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)"]').toggleClass('ShowJammers', jammers || lineups);
    }
  );

  WS.Register('ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)', function (k, v) {
    $('.OverlayPanel').removeClass('Show');
    // sort divs in the panel before we show, just in case it's changed
    if (v === 'PenaltyTeam1' || v === 'PenaltyTeam2') {
      var c = $('.PenaltyTeam [data-flag="BC"]');
      c.empty().remove();
    }
    $('.OverlayPanel.' + v + ' .SortBox').sortDivs();
    $('.OverlayPanel.' + v).addClass('Show');
  });

  WS.Register(
    [
      'ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line1)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line2)',
    ],
    function (k, v) {
      var sp = '.' + k.split('.').slice(4, 6).join(' .').slice(0, -1);
      $(sp).text(v);
    }
  );

  WS.Register(['ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Style)'], function (k, v) {
    $('.LowerThird .Line2').removeClass('ColourTeam1 ColourTeam2 ColourDefault').addClass(v);
  });

  $(document).keyup(function (e) {
    if (e.which === 74) {
      WS.Set(
        'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)',
        WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)'] === 'On' ? 'Off' : 'On'
      );
    }
    if (e.which === 76) {
      WS.Set(
        'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowLineups)',
        WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowLineups)'] === 'On' ? 'Off' : 'On'
      );
    }
    if (e.which === 78) {
      WS.Set(
        'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowAllNames)',
        WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowAllNames)'] === 'On' ? 'Off' : 'On'
      );
    }
    if (e.which === 67) {
      WS.Set(
        'ScoreBoard.Settings.Setting(Overlay.Interactive.Clock)',
        WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Clock)'] === 'On' ? 'Off' : 'On'
      );
    }
    if (e.which === 83) {
      WS.Set(
        'ScoreBoard.Settings.Setting(Overlay.Interactive.Score)',
        WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Score)'] === 'On' ? 'Off' : 'On'
      );
    }
    if (e.which === 48) {
      WS.Set(
        'ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)',
        WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)'] === 'PPJBox' ? '' : 'PPJBox'
      );
    }
    if (e.which === 49) {
      WS.Set(
        'ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)',
        WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)'] === 'RosterTeam1' ? '' : 'RosterTeam1'
      );
    }
    if (e.which === 50) {
      WS.Set(
        'ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)',
        WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)'] === 'RosterTeam2' ? '' : 'RosterTeam2'
      );
    }
    if (e.which === 51) {
      WS.Set(
        'ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)',
        WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)'] === 'PenaltyTeam1' ? '' : 'PenaltyTeam1'
      );
    }
    if (e.which === 52) {
      WS.Set(
        'ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)',
        WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)'] === 'PenaltyTeam2' ? '' : 'PenaltyTeam2'
      );
    }
    if (e.which === 57) {
      WS.Set(
        'ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)',
        WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)'] === 'LowerThird' ? '' : 'LowerThird'
      );
    }
    if (e.which === 85) {
      WS.Set(
        'ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)',
        WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)'] === 'Upcoming' ? '' : 'Upcoming'
      );
    }
    if (e.which === 32) {
      WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)', '');
    }
  });

  WS.AutoRegister();
  WS.Connect();

  setTimeout(function () {
    $('body').removeClass('preload');
  }, 1000);
}

function toIndicator(k, v) {
  'use strict';
  var prefix = k.substring(0, k.lastIndexOf('.'));
  return isTrue(WS.state[prefix + '.StarPass'])
    ? 'SP'
    : isTrue(WS.state[prefix + '.Lost'])
    ? ''
    : isTrue(WS.state[prefix + '.Lead'])
    ? '★'
    : '';
}

function ensureSkaterExists(skaterId, team) {
  'use strict';
  if ($('.PenaltyTeam' + team + ' .Team' + team + ' .Skater[data-skaterId=' + skaterId + ']').length === 0) {
    // create the roster entry for this skater
    var xv = $('<div class="Skater"></div>');
    xv.attr('data-skaterId', skaterId);
    $('<div class="Number ColourTeam' + team + '"></div>').appendTo(xv);
    $('<div class="Name">&nbsp;</div>').appendTo(xv);
    $('<div class="Flags">&nbsp;</div>').appendTo(xv);
    $('.RosterTeam' + team + ' .Team' + team).append(xv);

    // create the penalty tracking entry for this skater
    var xz = $('<div class="Skater"></div>');
    xz.attr('data-skaterId', skaterId);
    xz.attr('data-count', 0);
    $('<div class="Number ColourTeam' + team + '">&nbsp;</div>').appendTo(xz);
    $('<div class="Name">&nbsp;</div>').appendTo(xz);
    $('<div class="Penalties">&nbsp;</div>').appendTo(xz);
    $('.PenaltyTeam' + team + ' .Team' + team).append(xz);
  }
}

function updateSort(sel) {
  'use strict';
  var skaterRow = $(sel);
  var sortValue;
  // First, sort invisible rows to the end, so they don't interfere with alternating row color
  if (skaterRow.hasClass('NoShow')) {
    sortValue = '1';
  } else {
    sortValue = '0';
  }

  // Second, sort by number with missing numbers at the end
  var n = $('.Number', sel).text();
  if (n === '' || n === '-' || n == null) {
    sortValue += 'ZZZZZZ';
  } else {
    sortValue += n;
  }

  skaterRow.attr('data-sort', sortValue);
  skaterRow.parent().sortDivs();
}

function createPenalty(mb, pnum, v) {
  'use strict';
  $('.Number-' + pnum, mb).remove();
  var penalty = $('<div class="Penalty Number-' + pnum + ' Penalty-' + v + '">' + v + '</div>');
  $(mb).attr('data-count', $('.Penalty', mb).length + 1);
  penalty.attr('data-sort', pnum);
  $(mb).children('.Penalties').append(penalty);
  $(mb).children('.Penalties').sortDivs();
}

function jamData(k, v) {
  'use strict';
  var period = k.Period;
  var jam = k.Jam;
  var team = k.TeamJam;
  var key = k.field;

  var pa = '.PPJBox .Team' + team + ' .Period' + period;
  var me = pa + ' .Jam' + jam;
  var $pId = $(pa);
  var $mId = $(me);

  if (v == null) {
    $(me).remove();
    $pId.sortDivs();
    return;
  }

  if ($(me).length === 0) {
    pointsPerJamColumnWidths();
    var xv = $('<div data-sort="' + jam + '" class="ColumnWidth GraphBlock Jam' + jam + '"></div>');
    $('<div class="JammerStar ColumnWidth"></div>').appendTo(xv);
    $('<div class="Points ColumnWidth"></div>').appendTo(xv);
    $pId.append(xv);
    $pId.sortDivs();
  }

  switch (key) {
    case 'Lead':
      $(me).attr('lead', v);
      break;
    case 'Lost':
      $(me).attr('lost', v);
      break;
    case 'JamScore':
      var setHeight = v * 4 + 'px';
      $(me).css('height', setHeight);

      if (team === 1) {
        var hid = $('.PPJBox .Team1 .Period').innerHeight();
        var marg = parseInt(hid) - parseInt(setHeight);
        $(me).css('marginTop', marg);
      }
      if (v !== 0) {
        $('.Points', me).text(v);
      }
      break;
  }

  pointsPerJamColumnWidths();
}

function pointsPerJamColumnWidths() {
  'use strict';
  var ne1 = $('.PPJBox .Team1 .GraphBlock').length;
  var ne2 = $('.PPJBox .Team2 .GraphBlock').length;
  if (ne2 > ne1) {
    ne1 = ne2;
  }
  var nel = ne1 + 3;
  var wid = parseInt($('.PPJBox').innerWidth());
  var newwidth = parseInt(wid / nel) - 3;
  $('.ColumnWidth').innerWidth(newwidth);
  $('.PPJBox .Team1 .GraphBlock').css('backgroundColor', WS.state['ScoreBoard.CurrentGame.Team(1).Color(overlay_bg)']);
  $('.PPJBox .Team2 .GraphBlock').css('backgroundColor', WS.state['ScoreBoard.CurrentGame.Team(2).Color(overlay_bg)']);
}

function clockType(k, v) {
  'use strict';
  var ret;
  var to = WS.state['ScoreBoard.CurrentGame.TimeoutOwner'];
  var or = WS.state['ScoreBoard.CurrentGame.OfficialReview'];
  var tc = WS.state['ScoreBoard.CurrentGame.Clock(Timeout).Running'];
  var lc = WS.state['ScoreBoard.CurrentGame.Clock(Lineup).Running'];
  var ic = WS.state['ScoreBoard.CurrentGame.Clock(Intermission).Running'];

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
    var num = WS.state['ScoreBoard.CurrentGame.Clock(Intermission).Number'];
    var max = WS.state['ScoreBoard.CurrentGame.Rule(Period.Number)'];
    var isOfficial = WS.state['ScoreBoard.CurrentGame.OfficialScore'];
    if (isOfficial) {
      ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Official)'];
    } else if (num === 0) {
      ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.PreGame)'];
    } else if (num != max) {
      ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Intermission)'];
    } else if (!isOfficial) {
      ret = WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Unofficial)'];
    }

    $('.ClockDescription').css('backgroundColor', 'blue');
  } else {
    ret = 'Jam';
  }

  return ret;
}
