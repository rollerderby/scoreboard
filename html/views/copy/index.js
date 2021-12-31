(function () {
  'use strict';
  var gameId = _windowFunctions.getParam('game');

  prepareRosterSheetTable($('#roster1'), gameId, 1, 'copyToStatsbook');
  prepareRosterSheetTable($('#roster2'), gameId, 2, 'copyToStatsbook');

  prepareSkSheetTable($('#sk1'), gameId, 1, 'copyToStatsbook');
  prepareSkSheetTable($('#sk2'), gameId, 2, 'copyToStatsbook');

  preparePltInputTable($('#plt11'), gameId, 1, 'copyToStatsbook', 1);
  preparePltInputTable($('#plt12'), gameId, 2, 'copyToStatsbook', 1);
  preparePltInputTable($('#plt21'), gameId, 1, 'copyToStatsbook', 2);
  preparePltInputTable($('#plt22'), gameId, 2, 'copyToStatsbook', 2);

  prepareLtSheetTable($('#lt1'), gameId, 1, 'copyToStatsbook');
  prepareLtSheetTable($('#lt2'), gameId, 2, 'copyToStatsbook');

  WS.AutoRegister();
  WS.Connect();
})();
