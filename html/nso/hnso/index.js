(function () {
  $('#tabsDiv').tabs();
})();

function toTitle(k, v) {
  if (v == null && $('#sbConnectionStatus').attr('status') === 'ready') {
    window.close();
  } else {
    return v + ' | Edit Game | CRG ScoreBoard';
  }
}
