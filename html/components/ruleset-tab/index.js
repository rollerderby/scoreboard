WS.Register('ScoreBoard.Rulesets.Ruleset(*).Parent');

function rsCompareChildIndex(a, b) {
  return _sbCompareAttrThenSubId('index', $(a).children('[index]'), $(b).children('[index]'));
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

function rsIsInherited(k, v) {
  return (k.field === 'Ruleset' && !!v) || (!k.Game && !v);
}

function rsIsNotInherited(k, v) {
  return !rsIsInherited(k, v);
}

function rsDisableToggle(k, v) {
  return k.Game != null || isTrue(v);
}

function rsDefinitionOverride(k, v, elem) {
  return elem.prop('checked') ? _rsGetEffectiveValue(k.Ruleset, k.Rule) : null;
}

function _rsGetEffectiveValue(rs, rule) {
  var value = null;
  while (value == null && rs != null) {
    value = WS.state['ScoreBoard.Rulesets.Ruleset(' + rs + ').Rule(' + rule + ')'];
    rs = WS.state['ScoreBoard.Rulesets.Ruleset(' + rs + ').Parent'];
  }
  return value;
}

function rsGetDisplayValue(k, v, elem) {
  const value = k.Game
    ? v
    : _rsGetEffectiveValue(_windowFunctions.getParam('ruleset'), WS._enrichProp(WS._getContext(elem)[0]).RuleDefinition);
  return (
    elem
      .parent()
      .find('select>option[value="' + value + '"]')
      .text() || value
  );
}
