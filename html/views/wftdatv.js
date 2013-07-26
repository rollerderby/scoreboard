
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 * Copyright (C) 2013 Rob Thomas <xrobau@gmail.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

XML_ELEMENT_SELECTOR = "ScoreBoard";

function setupMainDiv(div) {
	div.css({ position: "fixed" });

	_crgUtils.bindAndRun($(window), "resize", function() {
	  var aspect16x9 = _windowFunctions.get16x9Dimensions();
	  div.css(aspect16x9).css("fontSize", aspect16x9.height/30); // Font scaling. 
	  });
} 

$sb(function() {
	  setupMainDiv($("#mainDiv"));

	  console.log("Derp");
	  // Team Names
	  $.each( [ "1", "2" ], function(i, team) {
		  $sb("ScoreBoard.Team("+team+").Name").$sbElement("#Team"+team+">.TeamName>a", {
		      sbelement: { autoFitText: true }
		    });
	  });
});

	
