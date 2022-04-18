function createRulesetsTab(tab, id, isGame) {
  'use strict';
  var rulesets = {};
  var activeRuleset = null;
  var definitions = {};
  initialize();

  function loadRulesets() {
    tab.find('#parent').empty();
    if (isGame) {
      tab.find('#select').empty().append($('<option>').prop('value', '').append('Custom'));
    }
    addRulesetOptions('', '');
    if (isGame) {
      tab.find('#select').val(WS.state['ScoreBoard.Game(' + id + ').Ruleset'] || '');
    }
  }

  function addRulesetOptions(parentId, prefix) {
    $.each(rulesets, function (idx, val) {
      if (val.Parent === parentId && val.Id !== '') {
        $('<option>')
          .prop('value', val.Id)
          .html(prefix + val.Name)
          .appendTo(tab.find('#parent'))
          .clone()
          .appendTo(tab.find('#select'));
        addRulesetOptions(val.Id, prefix + '&nbsp;');
      }
    });
  }

  function loadDefinitions() {
    var d = tab.find('>.definitions>.rules');
    d.empty();
    function newSection(def) {
      var name = def.Group;
      var section = $('<div>').addClass('section folded').attr('group', def.Group);

      section.append(
        $('<div>')
          .addClass('header')
          .on('click', function (e) {
            section.toggleClass('folded');
          })
          .append(name)
      );

      return section;
    }
    function findSection(def) {
      var section = null;

      var children = d.find('.section');
      $.each(children, function (idx, s) {
        if (section != null) {
          return;
        }
        s = $(s);
        if (s.attr('group') === def.Group) {
          section = s;
        }
      });
      if (section == null) {
        section = newSection(def);
        d.append(section);
      }

      var div = $('<div>').addClass('definition').attr('fullname', def.Fullname);
      section.append(div);
      return div;
    }
    // Keep them in the same order they are in the Java code.
    var sortedValues = $.map(definitions, function (v) {
      return v;
    }).sort(function (a, b) {
      return a.Index - b.Index;
    });
    $.each(sortedValues, function (idx, def) {
      var div = findSection(def);
      var tooltiptext = null;
      if (def.Description !== '') {
        tooltiptext = $('<span>').addClass('tooltiptext').append(def.Description);
      }
      $('<div>')
        .addClass('name')
        .appendTo(div)
        .append(
          $('<label>')
            .append($('<input>').addClass('Selector').attr('type', 'checkbox').prop('checked', true).on('click', definitionOverride))
            .append(def.Name)
            .append(tooltiptext)
        );

      var value = $('<div>').addClass('value').appendTo(div);
      value.append($('<span>').addClass('inherit'));
      if (def.Type === 'Boolean') {
        var select = $('<select>').addClass('override');
        select.append($('<option>').attr('value', true).append(def.TrueValue));
        select.append($('<option>').attr('value', false).append(def.FalseValue));
        select.on('change', function () {
          setRule(def.Fullname, $(this).val());
        });
        select.appendTo(value);
      } else {
        // Treat as string
        value.append(
          $('<input>')
            .addClass('override')
            .attr('type', 'text')
            .on('change', function () {
              setRule(def.Fullname, $(this).val());
            })
        );
      }
    });
  }

  function initialize() {
    tab.append(
      $('<div>')
        .addClass('definitions')
        .append(
          $('<div>')
            .addClass('selection')
            .toggleClass('Hide', !isGame)
            .append($('<span>').text('Current Ruleset: '))
            .append(
              $('<select>')
                .attr('id', 'select')
                .on('change', function () {
                  WS.Set('ScoreBoard.Game(' + id + ').Ruleset', $(this).val());
                })
            )
        )
        .append($('<div>').addClass('buttons top'))
        .append(
          $('<div>')
            .addClass('metadata')
            .append($('<span>').text('Name: '))
            .append(
              $('<input type="text">')
                .attr('id', 'name')
                .attr('size', '20')
                .on('change', function () {
                  WS.Set('ScoreBoard.Rulesets.Ruleset(' + activeRuleset.Id + ').Name', $(this).val());
                })
            )
            .append($('<span>').text('  Parent: '))
            .append(
              $('<select>')
                .attr('id', 'parent')
                .on('change', function () {
                  var newParent = $(this).val();
                  if (!activeRuleset.Readonly && activeRuleset.Id !== '' && !isAncestor(activeRuleset.Id, newParent)) {
                    WS.Set('ScoreBoard.Rulesets.Ruleset(' + activeRuleset.Id + ').Parent', newParent);
                  }
                })
            )
        )
        .append($('<div>').addClass('rules'))
        .append($('<div>').addClass('buttons bottom'))
    );

    WS.Register(['ScoreBoard.Rulesets.RuleDefinition'], {
      triggerBatchFunc: function () {
        definitions = {};
        for (var prop in WS.state) {
          var k = WS._enrichProp(prop);
          if (k.RuleDefinition) {
            definitions[k.RuleDefinition] = definitions[k.RuleDefinition] || {};
            definitions[k.RuleDefinition][k.field] = WS.state[prop];
            definitions[k.RuleDefinition].Fullname = k.RuleDefinition;
            definitions[k.RuleDefinition].Group = k.RuleDefinition.split('.')[0];
            definitions[k.RuleDefinition].Name = k.RuleDefinition.split('.')[1];
          }
        }
        loadDefinitions();
      },
    });

    // If the definitions change, we'll have to redraw the rulesets too.
    var rereadProperties = ['ScoreBoard.Rulesets.RuleDefinition', 'ScoreBoard.Rulesets.Ruleset'];
    if (isGame) {
      rereadProperties = rereadProperties.concat('ScoreBoard.Game(' + id + ').Rule(*)');
    }
    WS.Register(rereadProperties, {
      triggerBatchFunc: function () {
        rulesets = {};
        if (isGame) {
          rulesets[''] = { Id: '', Rules: {}, Parent: '', Name: 'Custom Ruleset', Readonly: false };
        }
        for (var prop in WS.state) {
          var k = WS._enrichProp(prop);
          if (k.Rulesets != null && k.Ruleset != null) {
            rulesets[k.Ruleset] = rulesets[k.Ruleset] || { Rules: {} };
            if (k.field === 'Rule') {
              rulesets[k.Ruleset].Rules[k.Rule] = WS.state[prop];
            } else {
              rulesets[k.Ruleset][k.field] = WS.state[prop];
            }
          } else if (isGame && k.Game === id && k.field === 'Rule') {
            rulesets[''].Rules[k.Rule] = WS.state[prop];
          }
        }

        // Populate inherited values from parents.
        $.each(rulesets, function (idx, rs) {
          rs.Inherited = {};
          var pid = rs.Parent;
          while (pid !== '' && rulesets[pid]) {
            /* jshint -W083 */
            $.each(rulesets[pid].Rules, function (name, value) {
              if (rs.Inherited[name] === undefined) {
                rs.Inherited[name] = value;
              }
            });
            /* jshint +W083 */
            pid = rulesets[pid].Parent;
          }
        });
        loadRulesets();
        displayRuleset();
      },
    });

    if (isGame) {
      WS.Register(['ScoreBoard.Game(' + id + ').Ruleset'], function (k, v) {
        tab.find('#select').val(v);
        displayRuleset();
      });
    }

    WS.Register(['ScoreBoard.CurrentGame.Ruleset'], function (k, v) {
      if (activeRuleset == null || activeRuleset.Readonly) {
        return;
      }
      var definitionsDiv = tab.children('.definitions');
      definitionsDiv.find('.Update').show();
    });

    if (isGame) {
      WS.Register('ScoreBoard.Game(' + id + ').State', function (k, v) {
        tab.find('#select').prop('disabled', v !== 'Prepared');
      });
    }
  }

  function setRule(rule, value) {
    if (activeRuleset.Id === '') {
      WS.Set('ScoreBoard.Game(' + id + ').Rule(' + rule + ')', value);
    } else {
      WS.Set('ScoreBoard.Rulesets.Ruleset(' + activeRuleset.Id + ').Rule(' + rule + ')', value);
    }
  }

  function definitionOverride(e) {
    var elem = $(e.target);
    var value = elem.parents('.definition').find('.value');
    var rule = elem.parents('.definition').attr('fullname');
    if (!elem.prop('checked')) {
      value.children('.inherit').show();
      value.children('.override').hide();
      setRule(rule, null);
    } else {
      value.children('.inherit').hide();
      value.children('.override').show();
    }
  }

  function displayRuleset() {
    var activeId = isGame ? WS.state['ScoreBoard.Game(' + id + ').Ruleset'] || '' : id;
    activeRuleset = rulesets[activeId] || rulesets[''];
    if (!activeRuleset) {
      return;
    }
    var definitionsDiv = tab.children('.definitions');

    definitionsDiv.find('#name').val(activeRuleset.Name);
    definitionsDiv.find('#parent').val(activeRuleset.Parent);

    if (!isTrue(activeRuleset.Readonly)) {
      definitionsDiv.find('.definition *').prop('disabled', false);
    }

    $.each(activeRuleset.Inherited, function (key, val) {
      setDefinition(key, val, true);
    });
    $.each(activeRuleset.Rules, function (key, val) {
      setDefinition(key, val, false);
    });

    if (isTrue(activeRuleset.Readonly)) {
      tab.find('#name, #parent').prop('disabled', true);
      definitionsDiv.find('.definition *').prop('disabled', true);
      definitionsDiv.find('.Update').hide();
    } else if (activeRuleset.Id === '') {
      tab.find('#name, #parent').prop('disabled', true);
      definitionsDiv.find('.definition .Selector').prop('disabled', true);
      definitionsDiv.find('.Update').show();
    } else {
      tab.find('#name, #parent').prop('disabled', false);
      definitionsDiv.find('.Update').show();
    }
  }

  function setDefinition(key, value, inherited) {
    var def = tab.find('.definition[fullname="' + key + '"]');
    def.find('.value>input').prop('value', value);
    def.find('.value>select').val(value);
    value = def.find('.value>select>option:selected').text() || value;
    if (inherited) {
      def.find('.name input').prop('checked', false);
      def.find('.value .inherit').show().empty().append(value);
      def.find('.value .override').hide();
    } else {
      def.find('.name input').prop('checked', true);
      def.find('.value .inherit').hide();
      def.find('.value .override').show();
    }
  }

  function isAncestor(id1, id2) {
    var curId = id2;
    while (curId !== '') {
      if (curId === id1) {
        return true;
      }
      curId = rulesets[curId].Parent;
    }
    return false;
  }
}
