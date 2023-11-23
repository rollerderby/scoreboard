let thisGame = null;
let currentGame = null;

$(function () {
  'use strict';
  thisGame = _windowFunctions.getParam('game');
});

function isThisGame(k, v) {
  'use strict';
  currentGame = v;
  return checkAutoAdvance();
}

function checkAutoAdvance() {
  'use strict';
  if (thisGame != null && currentGame != null && thisGame !== currentGame) {
    if ($('#GameAdvance').parent().hasClass('Auto')) {
      advanceGame();
      return true;
    }
    return false;
  } else {
    return true;
  }
}

function advanceGame() {
  'use strict';
  let url = new URL(window.location);
  url.searchParams.set('game', currentGame);
  window.location.replace(url);
}
