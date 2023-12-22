'use strict';

function skiToggleEdit(k, v, elem) {
  console.log('toggle', elem);
  elem.siblings().addBack().toggleClass('sbHide').filter('input:visible, select:visible').trigger('focus');
}

function skiIsToThisTeam(k, v) {
  const prefix = k.upTo('Game');
  return v === WS.state[prefix + '.TimeoutOwner'] && !isTrue(WS.state[prefix + 'OfficialReview']);
}

function skiIsOrThisTeam(k, v) {
  const prefix = k.upTo('Game');
  return v === WS.state[prefix + '.TimeoutOwner'] && isTrue(WS.state[prefix + 'OfficialReview']);
}

function skiIsTeam2(k) {
  return k.Team === '2';
}

function skiToJammerId(k, v, elem) {
  if (typeof _crgKeyControls === 'Object') {
    _crgKeyControls.setupKeyControl(elem, _crgKeyControls.operator);
  }
  return 'Team' + k.Team + 'Jammer' + k.Skater;
}

function skiToPivotId(k, v, elem) {
  if (typeof _crgKeyControls === 'Object') {
    _crgKeyControls.setupKeyControl(elem, _crgKeyControls.operator);
  }
  return 'Team' + k.Team + 'Pivot' + k.Skater;
}
