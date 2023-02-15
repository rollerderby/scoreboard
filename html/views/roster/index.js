$(function () {
  'use strict';
  createRosterTab($('#RosterTab'));

  WS.AutoRegister();
  WS.Connect();
});
