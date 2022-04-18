function str_sort(a, b) {
  'use strict';
  return $(b).text() < $(a).text() ? 1 : -1;
}
jQuery.fn.sortOptions = function sortOptions() {
  'use strict';
  $('> option', this[0]).sort(str_sort).appendTo(this[0]);
};

var Skaters = new DataSet();
Skaters.AddTrigger('UPDATE', '*', {}, function (n, o, k) {
  'use strict';
  if (this.Name && this.Team && this.Id) {
    var att = { 'data-name': this.Name, 'data-team': this.Team, value: this.Skater };
    var $s = $('#Skaters option[value="' + this.Skater + '"]');
    if ($s.length === 0) {
      $s = $('<option>').attr(att).appendTo('#Skaters');
    }
    $s.attr(att).text(this.Name);
    $('#Skaters').sortOptions();
  }
});
Skaters.AddTrigger('DELETE', '*', {}, function (n, o, k) {
  'use strict';
  $('#Skaters option[value="' + this.Skater + '"]').remove();
});

$(initialize);

function initialize() {
  'use strict';
  WS.Register(
    [
      'ScoreBoard.Settings.Setting(Overlay.Interactive.Clock)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.Score)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowLineups)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowAllNames)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.BackgroundColor)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)',
    ],
    function (k, v) {
      $('[data-setting="' + k + '"]').each(function (i) {
        var $t = $(this);
        if ($t.hasClass('ToggleSwitch')) {
          $t.val(v).toggleClass('current', v != null && v !== '');
        } else {
          if ($t.prop('tagName') === 'SELECT') {
            $('option[value="' + v + '"]', $t).attr('selected', 'selected');
          } else {
            if (!$t.hasClass('NoToggle')) {
              $t.toggleClass('current', $t.val() === v);
            }
          }
        }
      });
    }
  );

  WS.Register(
    [
      'ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowLineups)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowAllNames)',
    ],
    function (k, v) {
      var useLineups = isTrue(WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)']);
      var showLineups = WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowLineups)'] === 'On';
      var showJammerNames = WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)'] === 'On';
      var showAllNames = WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowAllNames)'] === 'On';
      $('[data-setting="ScoreBoard.Settings.Setting(Overlay.Interactive.ShowLineups)').toggleClass('disabled', !useLineups);
      $('[data-setting="ScoreBoard.Settings.Setting(Overlay.Interactive.ShowAllNames)').toggleClass(
        'disabled',
        !useLineups || !showLineups
      );
      $('[data-setting="ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)').toggleClass(
        'disabled',
        useLineups && showLineups && showAllNames
      );
      $('#Preview')
        .toggleClass('Wide', useLineups && showLineups && showJammerNames && !showAllNames)
        .toggleClass('XWide', useLineups && showLineups && showAllNames);
    }
  );

  var skaterRegEx = /^ScoreBoard\.CurrentGame\.Team\((.+)\)\.Skater\((.+?)\)\.(.+)$/;
  WS.Register('ScoreBoard.CurrentGame.Team', function (k, v) {
    var m = k.match(skaterRegEx);
    if (m) {
      var key = m[3];
      if (!(key === 'Id' || key === 'Name' || key === 'RosterNumber' || key === 'Flags')) {
        return;
      }

      var d = {};
      d[key] = v;
      d.Team = m[1];
      if (key === 'Id' && v == null) {
        Skaters.Delete({ Skater: m[2] });
      } else {
        Skaters.Upsert(d, { Skater: m[2] });
      }
    }
  });

  WS.Register(
    [
      'ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line1)',
      'ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line2)',
      'ScoreBoard.CurrentGame.Team(1).AlternateName(overlay)',
      'ScoreBoard.CurrentGame.Team(2).AlternateName(overlay)',
    ],
    function (k, v) {
      $('input[data-setting="' + k + '"]').val(v);
    }
  );

  WS.Register(
    [
      'ScoreBoard.CurrentGame.Team(1).Color(overlay_fg)',
      'ScoreBoard.CurrentGame.Team(1).Color(overlay_bg)',
      'ScoreBoard.CurrentGame.Team(2).Color(overlay_fg)',
      'ScoreBoard.CurrentGame.Team(2).Color(overlay_bg)',
    ],
    function (k, v) {
      if (v == null || v === '') {
        $('input[data-setting="' + k + '"]').attr('cleared', 'true');
        $('input[data-setting="' + k + '"]').val(k.Color === 'overlay_fg' ? '#FFFFFF' : '#333333');
      } else {
        $('input[data-setting="' + k + '"]').attr('cleared', 'false');
        $('input[data-setting="' + k + '"]').val(v);
      }
    }
  );

  WS.Register('ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Style)', function (k, v) {
    $('#LowerThirdStyle option[value="' + v + '"]').attr('selected', 'selected');
  });

  WS.Connect();
  WS.AutoRegister();
}

$('#Controls input, #Controls .Selector').on('change', function () {
  'use strict';
  var t = $(this).attr('data-setting');
  var v = $(this).val();
  if ($(this).attr('type') === 'color') {
    $(this).attr('cleared', 'false');
  }
  if (v === '' && t.endsWith('AlternateName(overlay)')) {
    // Delete the AlternateName
    WS.Set(t, null);
  } else if (t) {
    WS.Set(t, v);
  }
});

$('.SelectUpdator').on('change', function () {
  'use strict';
  var $t = $(this);
  var v = $t.val();

  // if we have an element target, update it
  var target = $t.attr('data-target');
  var field = $t.attr('data-field');

  if (target) {
    // we have a target element to write to not data
    var ov = $(target).attr(field);
    if (field) {
      $(target).attr(field, v);
    } else {
      $(target).val(v).trigger('change');
    }

    // flag it as changed
    if (ov !== v) {
      $(target).addClass('changed');
    }
  }

  $($t.attr('data-subforms')).hide();
  var forms = $('option[value=' + v + ']', $t).attr('data-form');
  if (forms) {
    $(forms).show();
    $('input[type=text],textarea', $(forms)).eq(0).select().focus();
  }
});

$('select#Skaters').on('change', function (e) {
  'use strict';
  var $t = $(this);
  var v = $t.val();
  var team = $('option[value=' + v + ']', $t).attr('data-team');
  var name = $('option[value=' + v + ']', $t).attr('data-name');
  var tnam = WS.state['ScoreBoard.CurrentGame.Team(' + team + ').AlternateName(overlay)'];
  tnam = tnam ? tnam : WS.state['ScoreBoard.CurrentGame.Team(' + team + ').Name'];
  $('#LowerThirdStyle option[value=ColourTeam' + team + ']')
    .attr('selected', 'selected')
    .trigger('change');
  $('input[data-setting="ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line1)"]').val(name).trigger('change');
  $('input[data-setting="ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line2)"]').val(tnam).trigger('change');
});

$('select#Keepers').on('change', function (e) {
  'use strict';
  var $t = $(this);
  var v = $t.val();

  var $d = $('option[value="' + v + '"]', this);
  var line1 = $d.attr('data-line1');
  var line2 = $d.attr('data-line2');
  var style = $d.attr('data-style');

  $('#LowerThirdStyle option[value=' + style + ']')
    .attr('selected', 'selected')
    .trigger('change');
  $('input[data-setting="ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line1)"]').val(line1).trigger('change');
  $('input[data-setting="ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line2)"]').val(line2).trigger('change');
});

$('#KeeperAdd').on('click', function () {
  'use strict';
  $('#LowerThirdStyle').trigger('change');
  var line1 = $('input[data-setting="ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line1)"]').val();
  var line2 = $('input[data-setting="ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line2)"]').val();
  var style = $('#LowerStyle').val();

  $('<option>')
    .attr('data-line1', line1)
    .attr('data-line2', line2)
    .attr('data-style', style)
    .attr('value', '_' + Math.random().toString(36).substr(2, 9))
    .text(line1 + '/' + line2 + ' (' + style + ')')
    .appendTo('#Keepers');
});

$('#Controls button').on('click', function () {
  'use strict';
  var $t = $(this);
  var v = $t.val();
  $t.removeClass('changed');
  if ($t.hasClass('ClearPrev')) {
    $t = $t.prev();
    $t.attr('cleared', true);
  } else if ($t.hasClass('ToggleSwitch')) {
    if ($t.hasClass('NoAuto')) {
      var nv = $t.attr('data-next');
      if (nv === v) {
        nv = null;
      }
      v = nv ? nv : null;
      if (v) {
        $t.val(v).attr('data-next', v);
      }
    } else {
      v = v === 'On' ? null : 'On';
    }
  }
  WS.Set($t.attr('data-setting'), v);
});

$(function () {
  'use strict';
  $(document).on('keyup', function (e) {
    var tag = e.target.tagName.toLowerCase();
    var c = String.fromCharCode(e.keyCode || e.charCode).toUpperCase();
    if (e.keyCode === 27) {
      $('body').focus();
      e.preventDefault();
      return false;
    }
    if (tag !== 'input' && tag !== 'textarea') {
      $('[data-key="' + c + '"]').each(function () {
        var $t = $(this);
        if ($t.prop('tagName') === 'OPTION') {
          $t.attr('selected', 'selected').parent().trigger('change');
        }
        if ($t.prop('tagName') === 'BUTTON') {
          $t.trigger('click');
        }
      });
      e.preventDefault();
    }
  });
});
