'use strict';

WS.Register('ScoreBoard.Rulesets.Ruleset(*).Parent');

WS.AfterLoad(function () {
  setInterval(_datUpdateAllUrl, 1000);
  _datUpdateSelectedUrl();
});

function _datUpdateAllUrl() {
  const d = new Date();
  let name = $.datepicker.formatDate('yy-mm-dd_', d);
  name += _timeConversions.twoDigit(d.getHours());
  name += _timeConversions.twoDigit(d.getMinutes());
  name += _timeConversions.twoDigit(d.getSeconds());
  $('#downloadAll').attr('href', '/SaveJSON/scoreboard-' + name + '.json');
}

function _datUpdateSelectedUrl() {
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

function datCreateRemoveSelectedDialog() {
  _datCreateRemoveDialog('selected');
}

function datUploadJson(k, v, elem) {
  elem.closest('form').attr('action', 'Load/JSON').submit();
}

function datUloadXlsx(k, v, elem) {
  elem.closest('form').attr('action', 'Load/xlsx').submit();
}

function datUploadBlankXlsx(k, v, elem) {
  elem.closest('form').attr('action', 'Load/blank_xlsx').submit();
}

function datUpdateUploadButtons(k, v, elem) {
  $('UploadButton').button(elem.val() ? 'enable' : 'disable');
}

function _datCreateRemoveDialog(type) {
  let div = $('#sbTemplates .RemoveDataDialog').clone(true);
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

function datSelectAll(k, v, elem) {
  const turnOn = elem.closest('table').find('tr.Content:not(.Selected)').length > 0;
  elem.closest('table').find('tr.Content').toggleClass('Selected', turnOn);
}

function datNewGame() {
  const gameid = sbNewUuid();
  WS.Set('ScoreBoard.Game(' + gameid + ').Id', gameid);
  window.open('/nso/hnso?game=' + gameid, '_blank');
}

function datNewTeam() {
  const teamid = sbNewUuid();
  WS.Set('ScoreBoard.PreparedTeam(' + teamid + ').Id', teamid);
  window.open('/settings/teams?team=' + teamid, '_blank');
}

function datNewRuleset() {
  const rulesetid = sbNewUuid();
  WS.Set('ScoreBoard.Rulesets.Ruleset(' + rulesetid + ').Id', rulesetid);
  window.open('/settings/rulesets?ruleset=' + rulesetid, '_blank');
}

function datSelect(k, v, elem) {
  elem.closest('tr.Content').toggleClass('Selected');
  _datUpdateSelectedUrl();
}

function datDeleteElem(k, v, elem) {
  elem.closest('tr.Content').addClass('ToDelete');
  _datCreateRemoveDialog('singleElement');
}

function datGameDlLink(k, v) {
  return v ? '/SaveJSON/crg-game-' + v.replace(/[\/|\\:*?"<>\ ]/g, '_') + '.json?path=ScoreBoard.Game(' + k.Game + ')' : null;
}

function datTeamDlLink(k, v) {
  return v
    ? '/SaveJSON/crg-team-' + v.replace(/[\/|\\:*?"<>\ ]/g, '_') + '.json?path=ScoreBoard.PreparedTeam(' + k.PreparedTeam + ')'
    : null;
}

function datRulesetDlLink(k, v) {
  return v
    ? '/SaveJSON/crg-ruleset-' + v.replace(/[\/|\\:*?"<>\ ]/g, '_') + '.json?path=ScoreBoard.Rulesets.Ruleset(' + k.Ruleset + ')'
    : null;
}

function datOperatorDlLink(k) {
  return '/SaveJSON/crg-operator-' + k.Setting.split('.')[2].replace(/[\/|\\:*?"<>\ ]/g, '_') + '.json?path=' + k;
}

function datToEditButtonLabel(k, v) {
  return isTrue(v) ? 'View' : 'Edit';
}

function datToOperatorName(k) {
  return k.Setting.split('.')[2];
}
