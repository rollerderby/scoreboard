let activeRsPrefix = _windowFunctions.hasParam('ruleset')
  ? 'ScoreBoard.Rulesets.Ruleset(' + _windowFunctions.getParam('ruleset') + ')'
  : 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ')';

function compareIndex(a, b) {
  'use strict';
  return _compareAttrThenSubId('index', a, b);
}

function compareChildIndex(a, b) {
  'use strict';
  return _compareAttrThenSubId('index', $(a).children('[index]'), $(b).children('[index]'));
}

function updateActiveRs(k, v) {
  'use strict';
  if (k.Game) {
    activeRsPrefix = v ? 'ScoreBoard.Rulesets.Ruleset(' + v + ')' : 'ScoreBoard.Game(' + k.Game + ')';
  }
  return v;
}

function noGame(k) {
  'use strict';
  return !k.Game;
}

function isGame(k) {
  'use strict';
  return k.Game != null;
}

function triggerFold(k, v, elem) {
  'use strict';
  elem.parent().toggleClass('folded');
}

function sectionName(k, v, elem) {
  'use strict';
  return elem.parent().attr('RuleDefinition').slice(0, -2);
}

function part2(k, v) {
  'use strict';
  return v ? v.split('.', 2)[1] : '';
}

function disableToggle() {
  'use strict';
  return isTrue(WS.state[activeRsPrefix + '.Readonly']) || WS.state[activeRsPrefix + '.Ruleset'] == null;
}

function disableSetter() {
  'use strict';
  return isTrue(WS.state[activeRsPrefix + '.Readonly']);
}

function definitionOverride(k, v, elem) {
  'use strict';
  return elem.prop('checked') ? _getEffectiveValue(k.Ruleset, k.Rule) : null;
}

function _getEffectiveValue(rs, rule) {
  'use strict';
  let value = null;
  while (value == null && rs != null) {
    value = WS.state['ScoreBoard.Rulesets.Ruleset(' + rs + ').Rule(' + rule + ')'];
    rs = WS.state['ScoreBoard.Rulesets.Ruleset(' + rs + ').Parent'];
  }
  return value;
}

function getDisplayValue(k, v, elem) {
  'use strict';
  const value = WS.state[activeRsPrefix + '.Rule(' + k.Rule + ')'] || _getEffectiveValue(activeRsPrefix.split('(')[1].slice(0, -1), k.Rule);
  return (
    elem
      .parent()
      .find('select>option[value="' + value + '"]')
      .text() || value
  );
}
