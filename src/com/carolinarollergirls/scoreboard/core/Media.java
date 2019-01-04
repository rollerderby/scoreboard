package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Media extends ScoreBoardEventProvider {
    public MediaFormat getFormat(String format);

    // Deletes a file off disk. True if successful.
    public boolean removeMediaFile(String format, String type, String id);

    public boolean validFileName(String fn);

    public enum Child implements AddRemoveProperty {
	FORMAT;
    }

    public static interface MediaFormat extends ScoreBoardEventProvider {
        public String getFormat();
	public MediaType getType(String type);
	
	public enum Child implements AddRemoveProperty {
	    TYPE;
	}
    }
    
    public static interface MediaType extends ScoreBoardEventProvider {
        public String getFormat();
        public String getType();
	
	public MediaFile getFile(String id);
	public void addFile(MediaFile file);
	public void removeFile(MediaFile file);
	
	public enum Child implements AddRemoveProperty {
	    FILE;
	}
    }
    
    public static interface MediaFile extends ScoreBoardEventProvider {
        public String getFormat();
        public String getType();
        public String getId();
        public String getName();
        public void setName(String s);
        public String getSrc();
        
        public enum Value implements PermanentProperty {
            ID,
            SRC,
            NAME;
        }
    }
}
