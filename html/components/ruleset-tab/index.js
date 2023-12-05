function rulesetPostLoad() {
  $('#RulesetsTab .selection').toggleClass('Hide', _windowFunctions.hasParam('ruleset'));

  if (_windowFunctions.hasParam('ruleset')) {
    const rs = _windowFunctions.getParam('ruleset');
    WS.Register('ScoreBoard.Rulesets.Ruleset(*)', {
      triggerBatchFunc: function () {
        updateRuleset(rs);
      },
    });
  } else if (_windowFunctions.hasParam('game')) {
    const game = _windowFunctions.getParam('game');
    WS.Register(['ScoreBoard.Game(' + game + ').Ruleset', 'ScoreBoard.Rulesets.Ruleset'], {
      triggerBatchFunc: function () {
        updateRuleset(WS.state['ScoreBoard.Game(' + game + ').Ruleset']);
      },
    });
    WS.Register('ScoreBoard.Game(' + game + ').State', function (k, v) {
      $('#RulesetsTab .selection #select').prop('disabled', v !== 'Prepared');
    });
  }
}

function updateRuleset(rs) {
  const prefix = rs ? 'ScoreBoard.Rulesets.Ruleset(' + rs + ')' : 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ')';
  const readOnly = isTrue(WS.state[prefix + '.Readonly']);
  $('#RulesetsTab .definitions').attr('sbPrefix', '§: ' + prefix);
  $('#RulesetsTab #select').val(rs);
  $('#RulesetsTab #name')
    .val(rs ? WS.state[prefix + '.Name'] : 'Custom Ruleset')
    .prop('disabled', !rs);
  $('#RulesetsTab #parent')
    .val(WS.state[prefix + '.Parent'])
    .prop('disabled', readOnly || !rs);
  $('#RulesetsTab > .definitions .section').each(function (idx, elem) {
    elem = $(elem);
    elem.children('.header').text(elem.attr('RuleDefinition'));
  });
  $('#RulesetsTab > .definitions .definition *').prop('disabled', readOnly);
  $('#RulesetsTab > .definitions .definition .Selector').prop('disabled', readOnly || !rs);
  $('#RulesetsTab > .definitions .Update').toggleClass('Hide', readOnly);
  $('#RulesetsTab > .definitions .definition').each(function (idx, elem) {
    elem = $(elem);
    const rule = 'Rule(' + elem.attr('RuleDefinition') + ')';
    let value = WS.state[prefix + '.' + rule];
    let inherited = false;
    let parent = WS.state[prefix + '.Parent'];
    while (value == null && parent != null) {
      value = WS.state['ScoreBoard.Rulesets.Ruleset(' + parent + ').' + rule];
      inherited = true;
      parent = WS.state['ScoreBoard.Rulesets.Ruleset(' + parent + ').Parent'];
    }
    elem.find('.value>input, .value>select').val(value);
    elem.find('.value .inherit').text(elem.find('.value>select>option:selected').text() || value);
    elem.find('.name input').prop('checked', !inherited);
    elem.toggleClass('Inherited', inherited);
  });
}

function compareIndex(a, b) {
  'use strict';
  return compareAttrThenSubId('index', a, b);
}

function compareChildIndex(a, b) {
  'use strict';
  return compareAttrThenSubId('index', $(a).children('[index]'), $(b).children('[index]'));
}

function triggerFold(elem) {
  'use strict';
  elem.parent().toggleClass('folded');
}

function part2(k, v) {
  'use strict';
  return v ? v.split('.', 2)[1] : '';
}

function definitionOverride(v, elem) {
  'use strict';
  elem.parents('.definition').toggleClass('Inherited', !elem.prop('checked'));
  setRule(null, elem);
}

function setRule(v, elem) {
  'use strict';
  WS.Set(WS._getPrefixes(elem)[0]['§'] + '.Rule(' + elem.closest('[RuleDefinition]').attr('RuleDefinition') + ')', v);
}
