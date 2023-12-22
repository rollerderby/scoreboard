'use strict';

function skiToggleEdit(k, v, elem) {
  console.log('toggle', elem);
  elem.siblings().addBack().toggleClass('sbHide').filter('input:visible, select:visible').trigger('focus');
}

function skiIsTeam2(k) {
  return k.Team === '2';
}

function skiSetupKey(k, v, elem) {
  if (typeof _crgKeyControls === 'Object') {
    _crgKeyControls.setupKeyControl(elem, _crgKeyControls.operator);
  }
}
