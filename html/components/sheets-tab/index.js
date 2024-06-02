'use strict';

function shtSelectSheet(k, v, elem) {
  elem.toggleClass('sbActive');
  $('#SheetsTab')
    .find('.Sheet.' + elem.text())
    .toggleClass('sbHide', !elem.hasClass('sbActive'));
}

function shtSelectTeamSheet(k, v, elem) {
  elem.toggleClass('sbActive');
  $('#SheetsTab')
    .find('[Team="' + k.Team + '"].Sheet.' + elem.text())
    .toggleClass('sbHide', !elem.hasClass('sbActive'));
}
