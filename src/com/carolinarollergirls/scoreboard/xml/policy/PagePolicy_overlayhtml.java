package com.carolinarollergirls.scoreboard.xml.policy;

/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 * Copyright (C) 2013 Rob Thomas <xrobau@gmail.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.defaults.DefaultPolicyModel;

public class PagePolicy_overlayhtml extends DefaultPolicyModel {
  public PagePolicy_overlayhtml() {  
	super(ID, NAME, DESCRIPTION);    
	addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, BLACK_BACKGROUND, "Boolean", String.valueOf(true)));
  }

  public static final String ID = "PagePolicy_overlay.html";
  public static final String NAME = "Page: overlay.html";
  public static final String DESCRIPTION = "When Enabled, the overlay page will display team logos next to their Time-Outs-Remaining area.";
  public static final String BLACK_BACKGROUND = "Black Background";

}

