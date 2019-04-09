package com.carolinarollergirls.scoreboard.json;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.LinkedList;
import java.util.List;
import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.core.Media;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl.BatchEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;

/**
 * Converts a ScoreBoardEvent into a representative JSON Update
 */
public class ScoreBoardJSONListener implements ScoreBoardListener {
    public ScoreBoardJSONListener(ScoreBoard sb, JSONStateManager jsm) {
        this.jsm = jsm;
        initialize(sb);
        sb.addScoreBoardListener(this);
    }

    public void scoreBoardChange(ScoreBoardEvent event) {
        synchronized (this) {
            try {
                ScoreBoardEventProvider p = event.getProvider();
                String provider = p.getProviderName();
                Property prop = event.getProperty();
                Object v = event.getValue();
                boolean rem = event.isRemove();
                if (prop == BatchEvent.START) {
                    batch++;
                } else if (prop == BatchEvent.END) {
                    if (batch > 0) { batch--; }
                } else if (prop instanceof PermanentProperty) {
                    update(getPath(p), prop, v);
                } else if (prop instanceof AddRemoveProperty) {
                    if (v instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider)v).getParent() == p) {
                        process((ScoreBoardEventProvider)v, rem);
                    } else if (rem) {
                        remove(getPath(p), prop, ((ValueWithId)v).getId());
                    } else {
                        update(getPath(p), prop, v);
                    }
                } else {
                    ScoreBoardManager.printMessage(provider + " update of unknown kind.	prop: " + PropertyConversion.toFrontend(prop) + ", v: " + v);
                }
            } catch (Exception e) {
                ScoreBoardManager.printMessage("Error!  " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (batch == 0) {
                    updateState();
                }
            }
        }
    }

    private void updateState() {
        synchronized (this) {
            if (updates.isEmpty()) {
                return;
            }
            jsm.updateState(updates);
            updates.clear();
        }
    }

    private void update(String prefix, Property prop, Object v) {
        String path = prefix + "." + PropertyConversion.toFrontend(prop);
        if (prop instanceof AddRemoveProperty) {
            updates.add(new WSUpdate(path + "(" + ((ValueWithId)v).getId() + ")", ((ValueWithId)v).getValue()));
        } else if (v instanceof ScoreBoardEventProvider) {
            updates.add(new WSUpdate(path, ((ScoreBoardEventProvider) v).getId()));
        } else if (v == null || v instanceof Boolean || v instanceof Integer
                || v instanceof Long){
            updates.add(new WSUpdate(path, v));
        } else {
            updates.add(new WSUpdate(path, v.toString()));
        }
    }

    private void remove(String prefix, Property prop, String id) {
        String path = prefix + "." + PropertyConversion.toFrontend(prop) + "(" + id + ")";
        updates.add(new WSUpdate(path, null));
    }

    private void process(ScoreBoardEventProvider p, boolean remove) {
        String path = getPath(p);
        updates.add(new WSUpdate(path, null));
        if (remove) { return; }

        for (Class<? extends Property> type : p.getProperties()) {
            for (Property prop : type.getEnumConstants()) {
                if (prop instanceof PermanentProperty) {
                    Object v = p.get((PermanentProperty)prop);
                    if (v == null) { v = ""; }
                    update(path, prop, v);
                } else if (prop instanceof AddRemoveProperty) {
                    for (ValueWithId c : p.getAll((AddRemoveProperty)prop)) {
                        if (c instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider)c).getParent() == p) {
                            process((ScoreBoardEventProvider)c, false);
                        } else {
                            update(getPath(p), prop, c);
                        }
                    }
                }
            }
        }
    }

    private void initialize(ScoreBoard sb) {
        process(sb, false);

        //announce empty directories to the frontend
        for (ValueWithId mf : sb.getMedia().getAll(Media.Child.FORMAT)) {
            for (ValueWithId mt : ((Media.MediaFormat)mf).getAll(Media.MediaFormat.Child.TYPE)) {
                updates.add(new WSUpdate(getPath((ScoreBoardEventProvider)mt), ""));
            }
        }

        updateState();
    }

    String getPath(ScoreBoardEventProvider p) {
        String path = "";
        if (p.getParent() != null) {
            path = getPath(p.getParent()) + ".";
        }
        path = path + p.getProviderName();
        if (!"".equals(p.getProviderId()) && p.getProviderId() != null) {
            path = path + "(" + p.getProviderId() + ")";
        }
        return path;
    }

    private JSONStateManager jsm;
    private List<WSUpdate> updates = new LinkedList<WSUpdate>();
    private long batch = 0;
}
