function createRulesetsTab(tab) {
  var rulesets = {};
  var activeRuleset = null;
  var definitions = {};
  initialize();

  function loadRulesets() {
    var rs = tab.find('>.rulesets>.tree');
    tab.find('#new_parent').empty();
    tab.find('#current_rs').empty();
    rs.empty();
    rs.append(displayRulesetTree(''));
    tab.find('#current_rs').val(WS.state['ScoreBoard.Rulesets.CurrentRulesetId']);
    if (activeRuleset != null) {
      displayRuleset(activeRuleset.Id);
    }
  }

  function displayRulesetTree(parentId) {
    var list = null;
    $.each(rulesets, function(idx, val) {
      if (val.ParentId === parentId) {
        if (list == null) {
          list = $('<ul>');
        }

        $('<option>')
        .prop('value', val.Id)
        .append(val.Name)
        .appendTo(tab.find('#new_parent'))
        .clone().appendTo(tab.find('#current_rs'));
        $('<li>')
        .append(
            $('<a>')
            .attr('href', '#')
            .on('click', function() {
              displayRuleset(val.Id);
            }
            ).append($('<span>').append(val.Name)
            )
        )
        .append(displayRulesetTree(val.Id))
        .appendTo(list);
      }
    });
    return list;
  }

  function loadDefinitions() {
    var d = tab.find('>.definitions>.rules');
    d.empty();
    var findSection = function(def) {
      var newSection = function(def) {
        var name = def.Group;
        var section = $('<div>')
        .addClass('section folded')
        .attr('group', def.Group);

        section.append($('<div>').addClass('header')
            .on('click', function(e) {
              section.toggleClass('folded');
            }).append(name));

        return section;
      };
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

      var div = $('<div>')
      .addClass('definition')
      .attr('fullname', def.Fullname);
      section.append(div);
      return div;
    };
    // Keep them in the same order they are in the Java code.
    var sortedValues = $.map(definitions, function(v) {
      return v;
    }).sort(function(a, b){return a.Index - b.Index;});
    $.each(sortedValues, function(idx, def) {
      var div = findSection(def);
      var tooltiptext = null;
      if (def.Description !== '') {
        tooltiptext = $('<span>').addClass('tooltiptext').append(def.Description);
      }
      $('<div>').addClass('name').appendTo(div)
      .append($('<label>').append($('<input>').attr('type', 'checkbox').prop('checked', true).on('click', definitionOverride))
          .append(def.Name).append(tooltiptext));

      var value = $('<div>').addClass('value').appendTo(div);
      value.append($('<span>').addClass('inherit'));
      if (def.Type === 'Boolean') {
        var select = $('<select>').addClass('override');
        select.append($('<option>').attr('value', true).append(def.TrueValue));
        select.append($('<option>').attr('value', false).append(def.FalseValue));

        select.appendTo(value);
      } else if (def.Type === 'Integer') {
        value.append($('<input>').addClass('override').attr('type', 'text'));
      } else if (def.Type === 'Long') {
        value.append($('<input>').addClass('override').attr('type', 'text'));
      } else if (def.Type === 'Time') {
        value.append($('<input>').addClass('override').attr('type', 'text'));
      } else {
        // Treat as string
        value.append($('<input>').addClass('override').attr('type', 'text'));
      }
    });
  }

  function initialize() {
    tab.append($('<div>').addClass('rulesets')
        .append($('<h1>').text('Rulesets'))
        .append($('<div>').addClass('tree')).append($('<br>'))
        .append($('<table>')
            .append($('<tr>')
                .append($('<th>').attr('colspan', '2').text('New Ruleset')))
            .append($('<tr>')
                .append($('<td>').text('Name:'))
                .append($('<td>').append($('<input type="text">').attr('id', 'new_name'))))
            .append($('<tr>')
                .append($('<td>').text('Parent:'))
                .append($('<td>').append($('<select>').attr('id', 'new_parent'))))
            .append($('<tr>').append($('<td>').addClass('new').attr('colspan', '2')
                .append($('<button>').addClass('New').text('Create').on('click', New).button()))))
        .append($('<br>'))
        .append($('<div>').addClass('current')
            .append($('<h1>').text('Current Ruleset'))
            .append($('<div>').append($('<select>').attr('id', 'current_rs')).on('change', Change))
        ));
    tab.append($('<div>').addClass('definitions')
        .append($('<div>').addClass('buttons top')
            .append($('<button>').addClass('Cancel').text('Cancel').on('click', Cancel).button())
            .append($('<button>').addClass('Update').text('Update').on('click', Update).button())
            .append($('<button>').addClass('Delete').text('Delete').on('click', Delete).button())
            .append($('<span>').addClass('EditNote').text('Note: Changing this ruleset will affect the current game.')))
        .append($('<span>').text('Name: '))
        .append($('<input type="text">').attr('id', 'name').attr('size', '40'))
        .append($('<div>').addClass('rules'))
        .append($('<div>').addClass('buttons bottom')
            .append($('<button>').addClass('Cancel').text('Cancel').on('click', Cancel).button())
            .append($('<button>').addClass('Update').text('Update').on('click', Update).button())
            .append($('<button>').addClass('Delete').text('Delete').on('click', Delete).button())
            .append($('<span>').addClass('EditNote').text('Note: Changing this ruleset will affect the current game.'))));
    tab.children('.definitions').hide();

    WS.Register(['ScoreBoard.Rulesets.RuleDefinition'], {triggerBatchFunc: function() {
      definitions = {};
      for (var prop in WS.state) {
        var k = WS._enrichProp(prop);
        if (k.RuleDefinition) {
          definitions[k.RuleDefinition] = definitions[k.RuleDefinition] || {};
          definitions[k.RuleDefinition][k.field] = WS.state[prop];
          definitions[k.RuleDefinition]['Fullname'] = k.RuleDefinition;
          definitions[k.RuleDefinition]['Group'] = k.RuleDefinition.split('.')[0];
          definitions[k.RuleDefinition]['Name'] = k.RuleDefinition.split('.')[1];
        }
      }
      loadDefinitions();
    }});

    // If the definitions change, we'll have to redraw the rulesets too.
    WS.Register(['ScoreBoard.Rulesets.RuleDefinition', 'ScoreBoard.Rulesets.Ruleset'], {triggerBatchFunc: function() {
      rulesets = {};
      for (var prop in WS.state) {
        var k = WS._enrichProp(prop);
        if (k.Rulesets != null && k.Ruleset != null) {
          rulesets[k.Ruleset] = rulesets[k.Ruleset] || {Rules: {}};
          if (k.field === 'Rule') {
            rulesets[k.Ruleset].Rules[k.Rule] = WS.state[prop];
          } else {
            rulesets[k.Ruleset][k.field] = WS.state[prop];
          }
        }
      }

      // Populate inherited values from parents.
      $.each(rulesets, function(idx, rs) {
        rs.Inherited = {};
        var pid = rs.ParentId;
        while (rulesets[pid]) {
          $.each(rulesets[pid].Rules, function(name, value) {
            if (rs.Inherited[name] === undefined) {
              rs.Inherited[name] = value;
            }
          });
          pid = rulesets[pid].ParentId;
        }
      });
      loadRulesets();
      markEffectiveRulesets();
    }});

    WS.Register(['ScoreBoard.Rulesets.CurrentRulesetId'], function(k, v) {
      tab.find('#current_rs').val(v);
      markEffectiveRulesets();
      var definitions = tab.children('.definitions');
      if (activeRuleset != null && activeRuleset.Effective) {
        definitions.find('.Update, .EditNote').show();
        definitions.find('.Delete').hide();
      } else {
        definitions.find('.Update, .Delete').show();
        definitions.find('.EditNote').hide();
      }
    });
  }

  function markEffectiveRulesets() {
    $.each(rulesets, function(idx, rs) { rs.Effective = false; });
    var id = WS.state['ScoreBoard.Rulesets.CurrentRulesetId'];
    while (rulesets[id]) {
      rulesets[id].Effective = true;
      id = rulesets[id].ParentId;
    }
  }

  function definitionOverride(e) {
    var elem = $(e.target);
    var value = elem.parents('.definition').find('.value');
    if (!elem.prop('checked')) {
      value.children('.inherit').show();
      value.children('.override').hide();
    } else {
      value.children('.inherit').hide();
      value.children('.override').show();
    }
  }

  function displayRuleset(id) {
    var rs = rulesets[id];
    if (!rs) { return; }
    activeRuleset = rs;
    var definitions = tab.children('.definitions');

    definitions.find('#name').val(rs.Name);

    if (!isTrue(rs.Readonly)) {
      definitions.find('.definition *').prop('disabled', false);
    }

    $.each(rs.Inherited, function(key, val) {
      setDefinition(key, val, true);
    });
    $.each(rs.Rules, function(key, val) {
      setDefinition(key, val, false);
    });

    if (isTrue(rs.Readonly)) {
      tab.find('#name').prop('disabled', true);
      definitions.find('.definition *').prop('disabled', true);
      definitions.find('.Update, .Delete, .EditNote').hide();
    } else if (rs.Effective) {
      tab.find('#name').prop('disabled', false);
      definitions.find('.Update, .EditNote').show();
      definitions.find('.Delete').hide();
    } else {
      tab.find('#name').prop('disabled', false);
      definitions.find('.Update, .Delete').show();
      definitions.find('.EditNote').hide();
    }
    definitions.show();
  }

  function New() {
    var newName = tab.find('#new_name').val();
    if (newName.trim() === '') { return; }
    var uuid;
    do {
      uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {var r = Math.random()*16|0,v=c==='x'?r:r&0x3|0x8;return v.toString(16);}).toUpperCase();
    } while (rulesets[uuid]);
    WS.Set('ScoreBoard.Rulesets.Ruleset('+uuid+').Name', newName);
    WS.Set('ScoreBoard.Rulesets.Ruleset('+uuid+').ParentId', tab.find('#new_parent').val());
    $('#new_name').val('');
    activeRuleset = uuid;
  }

  function Update() {
    tab.children('.definitions').hide();
    $.each(definitions, function(idx, val) {
      var def = $('.definition[fullname="' + val.Fullname + '"]');
      var value = null;
      if (def.find('.name input').prop('checked')) {
        var input = def.find('.value>input').prop('value');
        var select = def.find('.value>select').val();
        if (input != null) {
          value = input;
        }
        if (select != null) {
          value = select;
        }
      }
      WS.Set('ScoreBoard.Rulesets.Ruleset(' + activeRuleset.Id + ').Rule(' + val.Fullname + ')', value);
    });
    var newName = tab.find('#name').val();
    if (newName.trim() === '') { newName = WS.state['ScoreBoard.Rulesets.Ruleset(' + activeRuleset.Id + ').Name']; }
    WS.Set('ScoreBoard.Rulesets.Ruleset(' + activeRuleset.Id + ').Name', newName);
  }

  function Delete() {
    tab.children('.definitions').hide();
    WS.Set('ScoreBoard.Rulesets.Ruleset(' + activeRuleset.Id + ')', null);
  }

  function Cancel() {
    tab.children('.definitions').hide();
  }

  function Change() {
    WS.Set('ScoreBoard.Rulesets.CurrentRuleset', tab.find('#current_rs').val());
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
}
