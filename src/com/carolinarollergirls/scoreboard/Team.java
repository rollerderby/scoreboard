package com.carolinarollergirls.scoreboard;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import com.carolinarollergirls.scoreboard.event.*;

public interface Team extends ScoreBoardEventProvider
{
  public ScoreBoard getScoreBoard();

  public String getId();

  public String getName();

  public List<AlternateName> getAlternateNames();
  public AlternateName getAlternateName(String id);

  public String getLogo();

  public int getScore();

  public int getTimeouts();
  public int getOfficialReviews();

  public List<Skater> getSkaters();
  public Skater getSkater(String id) throws SkaterNotFoundException;

  public List<Position> getPositions();
  public Position getPosition(String id) throws PositionNotFoundException;

  public boolean isLeadJammer();

  public int getPass();

  public static final String ID_1 = "1";
  public static final String ID_2 = "2";

  public static final String EVENT_NAME = "Name";
  public static final String EVENT_LOGO = "Logo";
  public static final String EVENT_SCORE = "Score";
  public static final String EVENT_TIMEOUTS = "Timeouts";
  public static final String EVENT_OFFICIAL_REVIEWS = "OfficialReviews";
  public static final String EVENT_ADD_SKATER = "AddSkater";
  public static final String EVENT_REMOVE_SKATER = "RemoveSkater";
  public static final String EVENT_LEAD_JAMMER = "LeadJammer";
  public static final String EVENT_PASS = "Pass";
  public static final String EVENT_ADD_ALTERNATE_NAME = "AddAlternateName";
  public static final String EVENT_REMOVE_ALTERNATE_NAME = "RemoveAlternateName";

  public static interface AlternateName extends ScoreBoardEventProvider {
    public String getId();
    public String getName();

    public Team getTeam();

    public static final String EVENT_NAME = "Name";

    public static final String ID_OVERLAY = "overlay";
    public static final String ID_TWITTER = "twitter";
  };
}
