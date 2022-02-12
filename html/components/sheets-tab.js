function createSheetsTab(tab, gameId) {
  'use strict';
  WS.Register(['ScoreBoard.Game(' + gameId + ').Team(1).Name'], function (k, v) {
    tab.find('.O1').each(function () {
      $(this).text(v + $(this).data('suffix'));
    });
  });
  WS.Register(['ScoreBoard.Game(' + gameId + ').Team(2).Name'], function (k, v) {
    tab.find('.O2').each(function () {
      $(this).text(v + $(this).data('suffix'));
    });
  });

  var selectorDiv = $('<div>').addClass('SheetSelector').appendTo(tab);
  var selector = $('<select>').appendTo(selectorDiv);
  $('<option value="">No Sheet Selected</option>').appendTo(selector);
  $('<option value=".SKS.T1">').addClass('O1').data('suffix', ' Scores').text('Team 1 Scores').appendTo(selector);
  $('<option value=".SKS.T2">').addClass('O2').data('suffix', ' Scores').text('Team 2 Scores').appendTo(selector);
  $('<option value=".SKS">').text('Both Scores').appendTo(selector);
  $('<option value=".PTS.T1">').addClass('O1').data('suffix', ' Penalties').text('Team 1 Penalties').appendTo(selector);
  $('<option value=".PTS.T2">').addClass('O2').data('suffix', ' Penalties').text('Team 2 Penalties').appendTo(selector);
  $('<option value=".PTS">').text('Both Penalties').appendTo(selector);
  $('<option value=".LTS.T1">').addClass('O1').data('suffix', ' Lineups').text('Team 1 Lineups').appendTo(selector);
  $('<option value=".LTS.T2">').addClass('O2').data('suffix', ' Lineups').text('Team 2 Lineups').appendTo(selector);
  $('<option value=".LTS">').text('Both Lineups').appendTo(selector);
  $('<option value=".LTS.T1, .SKS.T1">')
    .addClass('O1')
    .data('suffix', ' Score + Lineups')
    .text('Team 1 Score + Lineups')
    .appendTo(selector);
  $('<option value=".LTS.T2, .SKS.T2">')
    .addClass('O2')
    .data('suffix', ' Score + Lineups')
    .text('Team 2 Score + Lineups')
    .appendTo(selector);
  selector.on('change', function (event) {
    tab.find('.Sheet').addClass('Hide');
    tab
      .find(selector.val())
      .removeClass('Hide')
      .toggleClass('Only', selector.val().length === 7);
  });

  var sk1 = $('<div>').addClass('T1 SKS Sheet Hide').appendTo(tab);
  var sk2 = $('<div>').addClass('T2 SKS Sheet Hide').appendTo(tab);
  var lt1 = $('<div>').addClass('T1 LTS Sheet Hide').appendTo(tab);
  var lt2 = $('<div>').addClass('T2 LTS Sheet Hide').appendTo(tab);
  var pt1 = $('<div>').addClass('T1 PTS Sheet Hide').appendTo(tab);
  var pt2 = $('<div>').addClass('T2 PTS Sheet Hide').appendTo(tab);
  $('<div>').attr('id', 'TripEditor').appendTo(tab);
  $('<div>').attr('id', 'skaterSelector').appendTo(tab);
  $('<div>').attr('id', 'osOffsetEditor').appendTo(tab);
  $('<div>').attr('id', 'PenaltyEditor').appendTo(tab);
  var ae1 = $('<div>').attr('id', 'AnnotationEditor1').appendTo(tab);
  var ae2 = $('<div>').attr('id', 'AnnotationEditor2').appendTo(tab);
  var fe1 = $('<div>').attr('id', 'FieldingEditor').appendTo(tab);
  var fe2 = $('<div>').attr('id', 'FieldingEditor').appendTo(tab);
  prepareSkSheetTable(sk1, gameId, '1', 'edit');
  prepareSkSheetTable(sk2, gameId, '2', 'edit');
  preparePltInputTable(pt1, gameId, '1', 'edit');
  preparePltInputTable(pt2, gameId, '2', 'edit');
  prepareLtSheetTable(lt1, gameId, '1', 'edit');
  prepareLtSheetTable(lt2, gameId, '2', 'edit');
  prepareTripEditor();
  prepareSkaterSelector(gameId);
  prepareOsOffsetEditor();
  preparePenaltyEditor(gameId);
  prepareAnnotationEditor(ae1, gameId, '1');
  prepareAnnotationEditor(ae2, gameId, '2');
  prepareFieldingEditor(fe1, gameId, '1');
  prepareFieldingEditor(fe2, gameId, '2');
}
