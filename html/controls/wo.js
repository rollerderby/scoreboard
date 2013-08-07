/**
 * Copyright (C) 2008-2013 Mr Temper <MrTemper@CarolinaRollergirls.com>, Rob Thomas, and WrathOfJon <crgscorespam@sacredregion.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

XML_ELEMENT_SELECTOR = "ScoreBoard";

$sb(function() {
	$.each([ 1, 2 ], function(i, t) {
		$sb("ScoreBoard.Team("+t+").Name").$sbElement("#Team"+t+"Name");
		$sb("ScoreBoard.Team("+t+").AlternateName(overlay).Name").$sbControl("#Team"+t+"OverlayName");
	});
});
