function toggleEdit(k, v, elem) {
  'use strict';
  console.log('toggle', elem);
  elem.siblings().addBack().toggleClass('sbHide').filter('input:visible, select:visible').trigger('focus');
}

function isToThisTeam(k, v) {
  'use strict';
  const prefix = k.upTo('Game');
  return v === WS.state[prefix + '.TimeoutOwner'] && !isTrue(WS.state[prefix + 'OfficialReview']);
}

function isOrThisTeam(k, v) {
  'use strict';
  const prefix = k.upTo('Game');
  return v === WS.state[prefix + '.TimeoutOwner'] && isTrue(WS.state[prefix + 'OfficialReview']);
}

function toJammerId(k, v, elem) {
  'use strict';
  if (typeof _crgKeyControls === 'Object') {
    _crgKeyControls.setupKeyControl(elem, _crgKeyControls.operator);
  }
  return 'Team' + k.Team + 'Jammer' + k.Skater;
}

function toPivotId(k, v, elem) {
  'use strict';
  if (typeof _crgKeyControls === 'Object') {
    _crgKeyControls.setupKeyControl(elem, _crgKeyControls.operator);
  }
  return 'Team' + k.Team + 'Pivot' + k.Skater;
}
