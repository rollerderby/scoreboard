function createDataTab(tab) {
  'use strict';
  var games;
  var teams;
  var rulesets;
  // Upload table
  var sbUploadTable = $('<table>').addClass('UpDown Hide').appendTo(tab);
  $('<tr>')
    .addClass('Name')
    .appendTo($('<thead>').appendTo(sbUploadTable))
    .append(
      $('<th>')
        .append(
          $('<button>')
            .addClass('Show Left')
            .text('Show')
            .button()
            .on('click', function () {
              $(this).closest('table').removeClass('Hide');
            })
        )
        .append(
          $('<button>')
            .addClass('Hide Left')
            .text('Hide')
            .button()
            .on('click', function () {
              $(this).closest('table').addClass('Hide');
            })
        )
        .append($('<span>').text('Upload ScoreBoard JSON or Statsbook'))
    );
  var contentTd = $('<td>').appendTo($('<tr>').addClass('Content').appendTo($('<tbody>').appendTo(sbUploadTable)));

  var iframeId = 'SaveLoadUploadHiddenIframe';
  var uploadForm = $('<form method="post" enctype="multipart/form-data" target="' + iframeId + '"/>')
    .append('<iframe id="' + iframeId + '" name="' + iframeId + '" style="display: none"/>')
    .append('<input type="file" name="file"/>')
    .appendTo(contentTd);
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
  _crgUtils.bindAndRun(uploadForm.children('input:file').button(), 'change', function () {
    uploadForm.children('button').button(this.value ? 'enable' : 'disable');
  });

  // Download table
  var sbDownloadTable = $('<table>').addClass('UpDown Hide').appendTo(tab);
  $('<tr>')
    .addClass('Name')
    .appendTo($('<thead>').appendTo(sbDownloadTable))
    .append(
      $('<th>')
        .attr('colspan', '5')
        .append(
          $('<button>')
            .addClass('Show Left')
            .text('Show')
            .button()
            .on('click', function () {
              $(this).closest('table').removeClass('Hide');
            })
        )
        .append(
          $('<button>')
            .addClass('Hide Left')
            .text('Hide')
            .button()
            .on('click', function () {
              $(this).closest('table').addClass('Hide');
            })
        )
        .append($('<span>').text('Download ScoreBoard JSON'))
    );
  var downloadRow = $('<tr>').addClass('Content').appendTo($('<tbody>').appendTo(sbDownloadTable));

  var links = [
    { name: 'Selected Elements', url: '' },
    { name: 'All Games', url: 'games.json?path=ScoreBoard.Game' },
    { name: 'All Teams', url: 'teams.json?path=ScoreBoard.PreparedTeam' },
    { name: 'All Rulesets', url: 'rulesets.json?path=ScoreBoard.Rulesets.Ruleset' },
    { name: 'All Data', url: '' },
  ];
  $.each(links, function () {
    $('<td><a download/></td>')
      .appendTo(downloadRow)
      .children('a')
      .text(this.name)
      .button()
      .attr('href', '/SaveJSON/' + this.url);
  });
  var allDataA = downloadRow.find('>td:eq(4)>a');
  var updateAllUrl = function () {
    var d = new Date();
    var name = $.datepicker.formatDate('yy-mm-dd_', d);
    name += _timeConversions.twoDigit(d.getHours());
    name += _timeConversions.twoDigit(d.getMinutes());
    name += _timeConversions.twoDigit(d.getSeconds());
    allDataA.attr('href', '/SaveJSON/scoreboard-' + name + '.json');
  };
  setInterval(updateAllUrl, 1000);

  var selectedA = downloadRow.find('>td:eq(0)>a');
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
    var d = new Date();
    var name = $.datepicker.formatDate('yy-mm-dd_', d);
    name += _timeConversions.twoDigit(d.getHours());
    name += _timeConversions.twoDigit(d.getMinutes());
    name += _timeConversions.twoDigit(d.getSeconds());
    selectedA.attr('href', '/SaveJSON/crg-dataset-' + name + '.json?path=' + paths);
  };

  // Delete table
  var sbDeleteTable = $('<table>').addClass('UpDown Hide').appendTo(tab);
  $('<tr>')
    .addClass('Name')
    .appendTo($('<thead>').appendTo(sbDeleteTable))
    .append(
      $('<th>')
        .attr('colspan', '4')
        .append(
          $('<button>')
            .addClass('Show Left')
            .text('Show')
            .button()
            .on('click', function () {
              $(this).closest('table').removeClass('Hide');
            })
        )
        .append(
          $('<button>')
            .addClass('Hide Left')
            .text('Hide')
            .button()
            .on('click', function () {
              $(this).closest('table').addClass('Hide');
            })
        )
        .append($('<span>').text('Delete ScoreBoard Data'))
    );
  var deleteButtonsRow = $('<tr>').addClass('Content').appendTo($('<tbody>').appendTo(sbDeleteTable));
  $('<td><button></td>')
    .appendTo(deleteButtonsRow)
    .children('button')
    .text('Delete Selected Elements')
    .button()
    .on('click', function () {
      createRemoveDialog('selected');
    });
  $('<td><button></td>')
    .appendTo(deleteButtonsRow)
    .children('button')
    .text('Delete All Games')
    .button()
    .on('click', function () {
      createRemoveDialog('games');
    });
  $('<td><button></td>')
    .appendTo(deleteButtonsRow)
    .children('button')
    .text('Delete All Teams')
    .button()
    .on('click', function () {
      createRemoveDialog('teams');
    });
  $('<td><button></td>')
    .appendTo(deleteButtonsRow)
    .children('button')
    .text('Delete All Rulesets')
    .button()
    .on('click', function () {
      createRemoveDialog('rulesets');
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
    var gamesSelector = 'tr.Content.None'; // class None is not used, so this will match nothing
    var teamsSelector = 'tr.Content.None';
    var rulesetsSelector = 'tr.Content.None';
    if (type === 'selected') {
      gamesSelector = teamsSelector = rulesetsSelector = 'tr.Content.Selected';
    } else if (type === 'games') {
      gamesSelector = 'tr.Content';
    } else if (type === 'teams') {
      teamsSelector = 'tr.Content';
    } else if (type === 'rulesets') {
      rulesetsSelector = 'tr.Content';
    }
    var count = games.find(gamesSelector).length + teams.find(teamsSelector).length + rulesets.find(rulesetsSelector).length;
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
          games.find(gamesSelector).each(function () {
            WS.Set('ScoreBoard.Game(' + $(this).attr('id') + ')', null);
          });
          teams.find(teamsSelector).each(function () {
            WS.Set('ScoreBoard.PreparedTeam(' + $(this).attr('id') + ')', null);
          });
          rulesets.find(rulesetsSelector).each(function () {
            WS.Set('ScoreBoard.Rulesets.Ruleset(' + $(this).attr('id') + ')', null);
          });
          div.dialog('close');
        },
        No: function () {
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
                  .addClass('Show Left')
                  .text('Show')
                  .button()
                  .on('click', function () {
                    $(this).closest('table').removeClass('Hide');
                  })
              )
              .append(
                $('<button>')
                  .addClass('Hide Left')
                  .text('Hide')
                  .button()
                  .on('click', function () {
                    $(this).closest('table').addClass('Hide');
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
        .append($('<span>'))
        .append($('<a edit>').addClass('Edit Left'))
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

  WS.Register('ScoreBoard.Game(*).Name', function (k, v) {
    games.find('tr.Content[id="' + k.Game + '"]').remove();
    if (v == null) {
      return;
    }
    var row = itemTemplate.clone(true);
    row.attr('name', v).attr('id', k.Game);
    row.find('td.Name>span').text(v);
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

  updateSelectedUrl();
}
