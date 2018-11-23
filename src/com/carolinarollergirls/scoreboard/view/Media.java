package com.carolinarollergirls.scoreboard.view;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Set;
import java.util.Map;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Media extends ScoreBoardEventProvider {
    public Set<String> getFormats();
    public Set<String> getTypes(String format);
    public Map<String, MediaFile> getMediaFiles(String format, String type);

    public static final String EVENT_REMOVE_FILE = "RemoveFile";

    public static interface MediaFile extends ScoreBoardEventProvider {
        public String getFormat();
        public String getType();
        public String getId();
        public String getName();
        public String getSrc();
        public static final String EVENT_FILE = "File";
    }

}
