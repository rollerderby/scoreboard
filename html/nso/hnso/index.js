WS.AfterLoad(function () {
  'use strict';

  $('#tabsDiv').tabs();
});

function toTitle(k, v) {
  'use strict';
  if (v == null && $('#ConnectionStatus').attr('status') === 'ready') {
    window.close();
  } else {
    return v + ' | Edit Game | CRG ScoreBoard';
  }
}
