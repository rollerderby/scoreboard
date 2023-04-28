function createDataTab(tab) {
  'use strict';
  var games;
  var teams;
  var rulesets;
  var operators;
  var operatorSettings = {};

  // Operations table
  var operationsTable = $('<table>').addClass('UpDown').appendTo(tab);
  $('<tr>')
    .addClass('Name')
    .appendTo($('<thead>').appendTo(operationsTable))
    .append($('<th colspan="4">').append($('<span>').text('Operations')));
  var operationsRow = $('<tr>').addClass('Content').appendTo($('<tbody>').appendTo(operationsTable));

  var downloadAllA = $('<td><a download/></td>').appendTo(operationsRow).children('a').text('Download All Data').button();
  var updateAllUrl = function () {
    var d = new Date();
    var name = $.datepicker.formatDate('yy-mm-dd_', d);
    name += _timeConversions.twoDigit(d.getHours());
    name += _timeConversions.twoDigit(d.getMinutes());
    name += _timeConversions.twoDigit(d.getSeconds());
    downloadAllA.attr('href', '/SaveJSON/scoreboard-' + name + '.json');
  };
  setInterval(updateAllUrl, 1000);

  var downloadSelectedA = $('<td><a download/></td>').appendTo(operationsRow).children('a').text('Download Selected').button();
  var updateSelectedUrl = function () {
    var paths = 'X';
    games.find('tr.Content.Selected').each(function () {
      paths = paths + ',ScoreBoard.Game(' + $(this).attr('id') + ')';
    });
    teams.find('tr.Content.Selected').each(function () {
      paths = paths + ',ScoreBoard.PreparedTeam(' + $(this).attr('id') + ')';
    });
    rulesets.find('tr.Content.Selected').each(function () {
      paths = paths + ',ScoreBoard.Rulesets.Ruleset(' + $(this).attr('id') + ')';
    });
    operators.find('tr.Content.Selected').each(function () {
      paths = paths + ',ScoreBoard.Settings.Setting(Operator__' + $(this).attr('id') + '.';
    });
    var d = new Date();
    var name = $.datepicker.formatDate('yy-mm-dd_', d);
    name += _timeConversions.twoDigit(d.getHours());
    name += _timeConversions.twoDigit(d.getMinutes());
    name += _timeConversions.twoDigit(d.getSeconds());
    downloadSelectedA.attr('href', '/SaveJSON/crg-dataset-' + name + '.json?path=' + paths);
  };

  $('<td><button></td>')
    .appendTo(operationsRow)
    .children('button')
    .text('Delete Selected')
    .button()
    .on('click', function () {
      createRemoveDialog('selected');
    });

  var importTd = $('<td>').appendTo(operationsRow);

  var iframeId = 'SaveLoadUploadHiddenIframe';
  var uploadForm = $('<form method="post" enctype="multipart/form-data" target="' + iframeId + '"/>')
    .append('<iframe id="' + iframeId + '" name="' + iframeId + '" style="display: none"/>')
    .append('<input type="file" name="file"/>')
    .appendTo(importTd);
  $('<button>')
    .html('Import JSON')
    .appendTo(uploadForm)
    .button()
    .on('click', function () {
      uploadForm.attr('action', '/Load/JSON').submit();
    });
  $('<button>')
    .html('Import Statsbook')
    .appendTo(uploadForm)
    .button()
    .on('click', function () {
      uploadForm.attr('action', '/Load/xlsx').submit();
    });
  $('<button>')
    .html('Upload Blank Statsbook')
    .appendTo(uploadForm)
    .button()
    .on('click', function () {
      uploadForm.attr('action', '/Load/blank_xlsx').submit();
    });
  var spinner = $('<div>').addClass('spin').appendTo(uploadForm);
  _crgUtils.bindAndRun(uploadForm.children('input:file').button(), 'change', function () {
    uploadForm.children('button').button(this.value ? 'enable' : 'disable');
  });
  WS.Register('ScoreBoard.ImportsInProgress', function (k, v) {
    spinner.toggle(v != 0);
  });

  var removeDialogTemplate = $('<div>')
    .addClass('RemoveDataDialog')
    .append(
      $('<p>').html(
        'This will delete <a class="Elements"></a> elements. You cannot undo this operation. (But you can reimport this data if you have it stored in a JSON file.)'
      )
    )
    .append($('<p>').text('Are you sure?'));

  function createRemoveDialog(type) {
    var div = removeDialogTemplate.clone(true);
    var selector = 'tr.Content.None'; // class None is not used, so this will match nothing
    if (type === 'selected') {
      selector = 'tr.Content.Selected';
    } else if (type === 'singleElement') {
      selector = 'tr.Content.ToDelete';
    }
    var count = tab.find(selector).length;
    div.find('a.Elements').text(count);
    div.dialog({
      title: 'Remove Data',
      modal: true,
      width: 700,
      close: function () {
        $(this).dialog('destroy').remove();
      },
      buttons: {
        'Yes, Remove': function () {
          games.find(selector).each(function () {
            WS.Set('ScoreBoard.Game(' + $(this).attr('id') + ')', null);
          });
          teams.find(selector).each(function () {
            WS.Set('ScoreBoard.PreparedTeam(' + $(this).attr('id') + ')', null);
          });
          rulesets.find(selector).each(function () {
            WS.Set('ScoreBoard.Rulesets.Ruleset(' + $(this).attr('id') + ')', null);
          });
          operators.find(selector).each(function () {
            var op = $(this).attr('id');
            for (const setting of operatorSettings[op]) {
              WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.Operator__' + op + '.' + setting + ')', null);
            }
          });
          tab.find('.ToDelete').removeClass('ToDelete');
          div.dialog('close');
        },
        No: function () {
          tab.find('.ToDelete').removeClass('ToDelete');
          div.dialog('close');
        },
      },
    });
  }

  // Data Tables
  var typeTemplate = $('<table>')
    .addClass('Type')
    .append(
      $('<thead>').append(
        $('<tr>')
          .addClass('Type')
          .append(
            $('<th>')
              .addClass('Type')
              .append(
                $('<button>')
                  .addClass('SelectAll Left')
                  .text('All')
                  .button()
                  .on('click', function () {
                    var turnOn = $(this).closest('table').find('tr.Content:not(.Selected)').length > 0;
                    $(this).closest('table').find('tr.Content').toggleClass('Selected', turnOn);
                  })
              )
              .append($('<span>').addClass('Type'))
              .append($('<button>').addClass('New Left').text('New').button())
          )
      )
    )
    .append($('<tbody>'));

  var itemTemplate = $('<tr>')
    .addClass('Content')
    .append(
      $('<td>')
        .addClass('Name')
        .append(
          $('<button>')
            .addClass('Select Left')
            .text('Select')
            .button()
            .on('click', function () {
              $(this).parents('tr.Content').toggleClass('Selected');
              updateSelectedUrl();
            })
        )
        .append(
          $('<button>')
            .addClass('Delete Left')
            .text('Delete')
            .button()
            .on('click', function () {
              $(this).parents('tr.Content').addClass('ToDelete');
              createRemoveDialog('singleElement');
            })
        )
        .append($('<a download>').addClass('Download Left').text('Download').button())
        .append($('<a>').addClass('Edit Left'))
        .append($('<span>'))
    );

  games = typeTemplate
    .clone(true)
    .attr('type', 'Games')
    .find('th.Type>button.New')
    .on('click', function () {
      var gameid = newUUID();
      WS.Set('ScoreBoard.Game(' + gameid + ').Id', gameid);
      window.open('/nso/hnso?game=' + gameid, '_blank');
    })
    .end()
    .find('tr.Type>th.Type>span.Type')
    .text('Games')
    .end()
    .appendTo(tab);

  teams = typeTemplate
    .clone(true)
    .attr('type', 'Teams')
    .find('th.Type>button.New')
    .on('click', function () {
      var teamid = newUUID();
      WS.Set('ScoreBoard.PreparedTeam(' + teamid + ').Id', teamid);
      window.open('/settings/teams?team=' + teamid, '_blank');
    })
    .end()
    .find('tr.Type>th.Type>span.Type')
    .text('Teams')
    .end()
    .appendTo(tab);

  rulesets = typeTemplate
    .clone(true)
    .attr('type', 'Rulesets')
    .find('th.Type>button.New')
    .on('click', function () {
      var rulesetid = newUUID();
      WS.Set('ScoreBoard.Rulesets.Ruleset(' + rulesetid + ').Id', rulesetid);
      window.open('/settings/rulesets?ruleset=' + rulesetid, '_blank');
    })
    .end()
    .find('tr.Type>th.Type>span.Type')
    .text('Rulesets')
    .end()
    .appendTo(tab);

  operators = typeTemplate
    .clone(true)
    .attr('type', 'Operators')
    .find('th.Type>button.New')
    .hide()
    .end()
    .find('tr.Type>th.Type>span.Type')
    .text('Operators')
    .end()
    .appendTo(tab);

  WS.Register('ScoreBoard.Game(*).Name', function (k, v) {
    games.find('tr.Content[id="' + k.Game + '"]').remove();
    if (v == null) {
      return;
    }
    var row = itemTemplate.clone(true);
    row.attr('name', v).attr('id', k.Game);
    row.find('td.Name>span').text(v);
    row
      .find('a.Download')
      .attr('href', '/SaveJSON/crg-game-' + v.replace(/[\/|\\:*?"<>\ ]/g, '_') + '.json?path=ScoreBoard.Game(' + k.Game + ')');
    row
      .find('a.Edit')
      .attr('href', '/nso/hnso?game=' + k.Game)
      .text('Edit')
      .button();
    _windowFunctions.appendAlphaSortedByAttr(games.children('tbody'), row, 'name');
  });

  WS.Register('ScoreBoard.PreparedTeam(*).FullName', function (k, v) {
    teams.find('tr.Content[id="' + k.PreparedTeam + '"]').remove();
    if (v == null) {
      return;
    }
    var row = itemTemplate.clone(true);
    row.attr('name', v).attr('id', k.PreparedTeam);
    row.find('td.Name>span').text(v);
    row
      .find('a.Download')
      .attr(
        'href',
        '/SaveJSON/crg-team-' + v.replace(/[\/|\\:*?"<>\ ]/g, '_') + '.json?path=ScoreBoard.PreparedTeam(' + k.PreparedTeam + ')'
      );
    row
      .find('a.Edit')
      .attr('href', '/settings/teams?team=' + k.PreparedTeam)
      .text('Edit')
      .button();
    _windowFunctions.appendAlphaSortedByAttr(teams.children('tbody'), row, 'name');
  });

  var rsData = {};

  function displayRulesetTree(parentId, table, prefix) {
    $.each(rsData, function (idx, val) {
      if (val.Parent === parentId) {
        var row = itemTemplate.clone(true);
        row.attr('id', val.Id);
        row.find('td.Name>span').html(prefix + val.Name);
        row.find('button.Delete').prop('disabled', val.Readonly);
        row
          .find('a.Download')
          .attr(
            'href',
            '/SaveJSON/crg-ruleset-' + val.Name.replace(/[\/|\\:*?"<>\ ]/g, '_') + '.json?path=ScoreBoard.Game(' + val.id + ')'
          );
        row
          .find('a.Edit')
          .attr('href', '/settings/rulesets?ruleset=' + val.Id)
          .text(val.Readonly ? 'View' : 'Edit')
          .button();
        table.append(row);

        displayRulesetTree(val.Id, table, prefix + '&nbsp;&nbsp;&nbsp;');
      }
    });
  }

  WS.Register(
    ['ScoreBoard.Rulesets.Ruleset(*).Name', 'ScoreBoard.Rulesets.Ruleset(*).Parent', 'ScoreBoard.Rulesets.Ruleset(*).Readonly'],
    function (k, v) {
      rsData[k.Ruleset] = rsData[k.Ruleset] || { Id: k.Ruleset };
      rsData[k.Ruleset][k.field] = v;

      rulesets.find('tr.Content').remove();
      displayRulesetTree('', rulesets.children('tbody'), '');
    }
  );

  WS.Register('ScoreBoard.Settings.Setting(*)', function (k, v) {
    if (!k.Setting.startsWith('ScoreBoard.Operator__')) {
      return;
    }
    var dotPos = k.Setting.indexOf('.', 21);
    var op = k.Setting.substring(21, dotPos);
    var setting = k.Setting.substring(dotPos + 1);

    var settings = operatorSettings[op] || new Set();
    if (v == null) {
      settings.delete(setting);
    } else {
      settings.add(setting);
    }
    if (settings.size === 0) {
      delete operatorSettings[op];
      operators.find('tr.Content[id="' + op + '"]').remove();
    } else {
      operatorSettings[op] = settings;
      if (!operators.find('tr.Content[id="' + op + '"]').length) {
        var row = itemTemplate.clone(true);
        row.attr('name', op).attr('id', op);
        row.find('td.Name>span').text(op);
        row
          .find('a.Download')
          .attr(
            'href',
            '/SaveJSON/crg-operator-' +
              op.replace(/[\/|\\:*?"<>\ ]/g, '_') +
              '.json?path=ScoreBoard.Settings.Setting(ScoreBoard.Operator__' +
              op +
              '.'
          );
        row.find('a.Edit').hide();
        _windowFunctions.appendAlphaSortedByAttr(operators.children('tbody'), row, 'name');
      }
    }
  });

  updateSelectedUrl();
}
