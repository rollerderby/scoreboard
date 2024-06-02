'use strict';

let activeRsPrefix = _windowFunctions.hasParam('ruleset')
  ? 'ScoreBoard.Rulesets.Ruleset(' + _windowFunctions.getParam('ruleset') + ')'
  : 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ')';

function rsCompareChildIndex(a, b) {
  return _sbCompareAttrThenSubId('index', $(a).children('[index]'), $(b).children('[index]'));
}

function rsUpdateActive(k, v) {
  if (k.Game) {
    activeRsPrefix = v ? 'ScoreBoard.Rulesets.Ruleset(' + v + ')' : 'ScoreBoard.Game(' + k.Game + ')';
  }
  return v;
}

function rsTriggerFold(k, v, elem) {
  elem.parent().parent().toggleClass('folded');
}

function rsSectionName(k, v, elem) {
  return elem.parent().parent().attr('RuleDefinition').slice(0, -2);
}

function rsPart2(k, v) {
  return v ? v.split('.', 2)[1] : '';
}

function rsDisableToggle() {
  return isTrue(WS.state[activeRsPrefix + '.Readonly']) || WS.state[activeRsPrefix + '.Ruleset'] == null;
}

function rsDisableSetter() {
  return isTrue(WS.state[activeRsPrefix + '.Readonly']);
}

function rsDefinitionOverride(k, v, elem) {
  return elem.prop('checked') ? _rsGetEffectiveValue(k.Ruleset, k.Rule) : null;
}

function _rsGetEffectiveValue(rs, rule) {
  let value = null;
  while (value == null && rs != null) {
    value = WS.state['ScoreBoard.Rulesets.Ruleset(' + rs + ').Rule(' + rule + ')'];
    rs = WS.state['ScoreBoard.Rulesets.Ruleset(' + rs + ').Parent'];
  }
  return value;
}

function rsGetDisplayValue(k, v, elem) {
  const value =
    WS.state[activeRsPrefix + '.Rule(' + k.Rule + ')'] || _rsGetEffectiveValue(activeRsPrefix.split('(')[1].slice(0, -1), k.Rule);
  return (
    elem
      .parent()
      .find('select>option[value="' + value + '"]')
      .text() || value
  );
}
