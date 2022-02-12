function prepareRosterSheetTable(element, gameId, teamId, mode, statsbookPeriod) {
  /* Values supported for mode:
   * copyToStatsbook: Only roster cells for IGRF.
   */

  'use strict';
  $(initialize);

  var alternateName = 'operator';
  var tbody;

  function initialize() {
    var table = $('<table cellpadding="0" cellspacing="0" border="1">')
      .addClass('Roster Team')
      .addClass('AlternateName_' + alternateName)
      .appendTo(element);
    var thead = $('<thead>').appendTo(table);
    $('<tr>').appendTo(thead).append($('<td colspan=2 id=league>'));
    $('<tr>').appendTo(thead).append($('<td colspan=2 id=team>'));
    $('<tr>').appendTo(thead).append($('<td colspan=2 id=color>'));
    $('<tr>').appendTo(thead).append($('<td>').text('Skater #')).append($('<td>').text('Skater Name'));
    tbody = $('<tbody>').appendTo(table);

    WS.Register(['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').LeagueName'], function (k, v) {
      element.find('#league').text(v);
    });
    WS.Register(['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').TeamName'], function (k, v) {
      element.find('#team').text(v);
    });
    WS.Register(['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').UniformColor'], function (k, v) {
      element.find('#color').text(v);
    });

    WS.Register(['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Color'], function (k, v) {
      element
        .find('#head')
        .css('background-color', WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Color(' + alternateName + '_bg)'] || '');
      element
        .find('#head')
        .css('color', WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Color(' + alternateName + '_fg)'] || '');
    });
    WS.Register(['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Skater'], function (k, v) {
      skaterUpdate(teamId, k, v);
    });
  }

  function skaterUpdate(t, k, v) {
    if (k.Skater == null) {
      return;
    }

    var prefix = 'ScoreBoard.Game(' + gameId + ').Team(' + t + ').Skater(' + k.Skater + ')';
    var row = tbody.children('tr.Skater[id=' + k.Skater + ']');
    if (k.field === 'RosterNumber') {
      // New skater, or number has been updated.
      if (v == null) {
        row.remove();
        return;
      }
      if (row.length === 0) {
        row = $('<tr>')
          .appendTo(tbody)
          .addClass('Skater')
          .attr('id', k.Skater)
          .append($('<td>').addClass('Number'))
          .append($('<td>').addClass('Name'));
      }
      row.attr('number', String(v));
      tbody
        .children()
        .sort(function (a, b) {
          return $(a).attr('number') > $(b).attr('number') ? 1 : -1;
        })
        .appendTo(tbody);
    }
    var flags = WS.state[prefix + '.Flags'];
    row.attr('flags', flags);
    row.children('.Name').text(WS.state[prefix + '.Name']);
    row
      .children('.Number')
      .text(String(WS.state[prefix + '.RosterNumber']) + (flags === 'ALT' || flags === 'BC' || flags === 'B' ? '*' : ''));
  }
}
