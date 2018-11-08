package com.carolinarollergirls.scoreboard.model;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Map;

import com.carolinarollergirls.scoreboard.view.Media;

public interface MediaModel extends Media {
    public Map<String, MediaFileModel> getMediaFileModels(String format, String type);

    // Deletes a file off disk. True if successful.
    public boolean removeMediaFile(String format, String type, String id);

    public boolean validFileName(String fn);

    public static interface MediaFileModel extends MediaFile {
        public void setName(String s);
    }
}
