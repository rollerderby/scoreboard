WS.Register('ScoreBoard.Rulesets.Ruleset(*).Parent');

WS.AfterLoad(function () {
  'use strict';

  setInterval(updateAllUrl, 1000);
  updateSelectedUrl();
});

function updateAllUrl() {
  const d = new Date();
  let name = $.datepicker.formatDate('yy-mm-dd_', d);
  name += _timeConversions.twoDigit(d.getHours());
  name += _timeConversions.twoDigit(d.getMinutes());
  name += _timeConversions.twoDigit(d.getSeconds());
  $('#downloadAll').attr('href', '/SaveJSON/scoreboard-' + name + '.json');
}

function updateSelectedUrl() {
  const paths = $('#games tr.Content.Selected')
    .map((elem) => 'ScoreBoard.Game(' + elem.attr('game') + ')')
    .get()
    .concat(
      $('#teams tr.Content.Selected')
        .map((elem) => 'ScoreBoard.PreparedTeam(' + elem.attr('preparedTeam') + ')')
        .get(),
      $('#rulesets tr.Content.Selected')
        .map((elem) => 'ScoreBoard.Rulesets.Ruleset(' + elem.attr('Ruleset') + ')')
        .get(),
      $('#operators tr.Content.Selected')
        .map((elem) => 'ScoreBoard.Settings.Setting(' + elem.attr('Setting') + ')')
        .get()
    )
    .join();
  const d = new Date();
  let name = $.datepicker.formatDate('yy-mm-dd_', d);
  name += _timeConversions.twoDigit(d.getHours());
  name += _timeConversions.twoDigit(d.getMinutes());
  name += _timeConversions.twoDigit(d.getSeconds());
  $('#downloadSelected').attr('href', '/SaveJSON/crg-dataset-' + name + '.json?path=' + paths);
}

function createRemoveSelectedDialog() {
  'use strict';
  createRemoveDialog('selected');
}

function uploadJson(k, v, elem) {
  'use strict';
  elem.closest('form').attr('action', 'Load/JSON').submit();
}

function uploadXlsx(k, v, elem) {
  'use strict';
  elem.closest('form').attr('action', 'Load/xlsx').submit();
}

function uploadBlankXlsx(k, v, elem) {
  'use strict';
  elem.closest('form').attr('action', 'Load/blank_xlsx').submit();
}

function updateUploadButtons(k, v, elem) {
  'use strict';
  $('UploadButton').button(elem.val() ? 'enable' : 'disable');
}

function createRemoveDialog(type) {
  let div = $('#Templates .RemoveDataDialog').clone(true);
  const selector = type === 'selected' ? ' tr.Content.Selected' : type === 'singleElement' ? ' tr.Content.ToDelete' : ' tr.Content.None'; // class None is not used, so this will match nothing
  div.find('a.Elements').text($(selector).length);
  div.dialog({
    title: 'Remove Data',
    modal: true,
    width: 700,
    close: function () {
      $(this).dialog('destroy').remove();
    },
    buttons: {
      'Yes, Remove': function () {
        $('#games' + selector).each(function () {
          WS.Set('ScoreBoard.Game(' + $(this).attr('Game') + ')', null);
        });
        $('#teams' + selector).each(function () {
          WS.Set('ScoreBoard.PreparedTeam(' + $(this).attr('PreparedTeam') + ')', null);
        });
        $('#rulesets' + selector).each(function () {
          WS.Set('ScoreBoard.Rulesets.Ruleset(' + $(this).attr('Ruleset') + ')', null);
        });
        $('#operators' + selector).each(function () {
          $.each($(this).data(), function (k, v) {
            WS.Set('ScoreBoard.Settings.Setting(' + v + ')', null);
          });
        });
        $('.ToDelete').removeClass('ToDelete');
        div.dialog('close');
      },
      No: function () {
        $('.ToDelete').removeClass('ToDelete');
        div.dialog('close');
      },
    },
  });
}

function selectAll(k, v, elem) {
  'use strict';
  const turnOn = elem.closest('table').find('tr.Content:not(.Selected)').length > 0;
  elem.closest('table').find('tr.Content').toggleClass('Selected', turnOn);
}

function newGame() {
  'use strict';
  const gameid = newUUID();
  WS.Set('ScoreBoard.Game(' + gameid + ').Id', gameid);
  window.open('/nso/hnso?game=' + gameid, '_blank');
}

function newTeam() {
  'use strict';
  const teamid = newUUID();
  WS.Set('ScoreBoard.PreparedTeam(' + teamid + ').Id', teamid);
  window.open('/settings/teams?team=' + teamid, '_blank');
}

function newRuleset() {
  'use strict';
  const rulesetid = newUUID();
  WS.Set('ScoreBoard.Rulesets.Ruleset(' + rulesetid + ').Id', rulesetid);
  window.open('/settings/rulesets?ruleset=' + rulesetid, '_blank');
}

function select(k, v, elem) {
  'use strict';
  elem.closest('tr.Content').toggleClass('Selected');
  updateSelectedUrl();
}

function deleteElem(k, v, elem) {
  'use strict';
  elem.closest('tr.Content').addClass('ToDelete');
  createRemoveDialog('singleElement');
}

function gameDlLink(k, v) {
  'use strict';
  return v ? '/SaveJSON/crg-game-' + v.replace(/[\/|\\:*?"<>\ ]/g, '_') + '.json?path=ScoreBoard.Game(' + k.Game + ')' : null;
}

function teamDlLink(k, v) {
  'use strict';
  return v
    ? '/SaveJSON/crg-team-' + v.replace(/[\/|\\:*?"<>\ ]/g, '_') + '.json?path=ScoreBoard.PreparedTeam(' + k.PreparedTeam + ')'
    : null;
}

function rulesetDlLink(k, v) {
  'use strict';
  return v
    ? '/SaveJSON/crg-ruleset-' + v.replace(/[\/|\\:*?"<>\ ]/g, '_') + '.json?path=ScoreBoard.Rulesets.Ruleset(' + k.Ruleset + ')'
    : null;
}

function operatorDlLink(k, v) {
  'use strict';
  return '/SaveJSON/crg-operator-' + k.Setting.split('.')[2].replace(/[\/|\\:*?"<>\ ]/g, '_') + '.json?path=' + k;
}

function gameEditLink(k) {
  'use strict';
  return '/nso/hnso?game=' + k.Game;
}

function teamEditLink(k) {
  'use strict';
  return '/settings/teams?team=' + k.PreparedTeam;
}

function rulesetEditLink(k) {
  'use strict';
  return '/settings/rulesets?ruleset=' + k.Ruleset;
}

function toEditButtonLabel(k, v) {
  'use strict';
  return isTrue(v) ? 'View' : 'Edit';
}

function toOperatorName(k) {
  'use strict';
  return k.Setting.split('.')[2];
}
