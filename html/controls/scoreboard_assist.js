
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */


$sb(function() {
  createTeamTables();
});

function createTeamTables() {
  $.each( [ "1", "2" ], function(i,e) {
    createTeamTable(e);
  });
}

var BLOCKER_REGEX = /^(Blocker[0-9])$/;
var BLOCKER_REPLACE = "Blocker";
var POSITIONS = "Bench Jammer Blocker Pivot";
var POSITIONS_ARRAY = [ "Bench", "Jammer", "Blocker", "Pivot" ];
var POSITIONS_REGEX = /^(Bench|Jammer|Blocker|Pivot)$/;

function createTeamTable(t) {
  var team = $sb("ScoreBoard.Team("+t+")");
  var table = $("#tableDiv table.Template").clone().removeClass("Template").addClass("Team"+t)
    .appendTo("#tableDiv");
  table.sortedtable({ header: table.find("thead tr:eq(1)") });

  var nameDiv = table.find("th.Team a.Team");
  var namePicker = function(event, value) {
    var n = team.$sb("Name").$sbGet();
    var an = team.$sb("AlternateName(operator).Name").$sbGet();
    if (an != null && an != "")
      n = an;
    nameDiv.text(n);
  };
  team.$sb("Name").$sbBindAndRun("sbchange", namePicker);
  team.$sb("AlternateName(operator).Name").$sbBindAndRun("sbchange", namePicker);

  team.$sbBindAddRemoveEach("Skater", function(event, skater) {
    var row = table.find("tr.Template").clone().removeClass("Template").attr("data-id", skater.$sbId);
    skater.$sb("Name").$sbElement(row.find("td>a.Name"));
    skater.$sb("Number").$sbElement(row.find("td>a.Number"));
    skater.$sb("Position").$sbBindAndRun("sbchange", function(event, value) {
      value = value.replace(BLOCKER_REGEX, BLOCKER_REPLACE);
      if (POSITIONS_REGEX.test(value))
        row.removeClass(POSITIONS).addClass(value);
    });
    skater.$sb("PenaltyBox").$sbBindAndRun("sbchange", function(event, value) {
      row.find("td.Box").toggleClass("InBox", isTrue(value));
    });

    row.find("td.Position").click(function() {
      var td = $(this);
      $.each( POSITIONS_ARRAY, function(i, e) {
        if (td.hasClass(e)) {
          if (td.parent().hasClass(e)) // already set to this position
            return false;
          if (e == "Blocker") {
            $.each( [ "1", "2", "3" ], function(i,n) {
              if ($sb(skater.parent()).$sb("Position("+e+n+").Id").$sbIs("")) {
                e += n;
                return false;
              }
            });
          }
          skater.$sb("Position").$sbSet(e);
          return false;
        }
      });
    });

    row.find("td.Box").click(function() {
      var inBox = $(this).hasClass("InBox");
      skater.$sb("PenaltyBox").$sbSet(String(!inBox));
    });

    table.sortedtable("insert", row);
  }, function(event, skater) {
    table.find("tr[data-id='"+skater.$sbId+"']").remove();
  });

}
