function appendScores(k, v) {
  'use strict';
  return v + ' Scores';
}

function appendPenalties(k, v) {
  'use strict';
  return v + ' Penalties';
}

function appendLineups(k, v) {
  'use strict';
  return v + ' Lineups';
}

function appendScoreAndLineups(k, v) {
  'use strict';
  return v + ' Score + Lineups';
}

function selectSheets(k, v) {
  'use strict';
  $('#SheetsTab').find('.Sheet').addClass('Hide');
  $('#SheetsTab')
    .find(v)
    .removeClass('Hide')
    .toggleClass('Only', v.length === 15);
}
