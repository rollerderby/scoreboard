package com.carolinarollergirls.scoreboard.json;

import java.util.LinkedList;
import java.util.List;

import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.Logger;

/**
 * Converts a ScoreBoardEvent into a representative JSON Update
 */
public class ScoreBoardJSONListener implements ScoreBoardListener {
    public ScoreBoardJSONListener(ScoreBoard sb, JSONStateManager jsm) {
        this.jsm = jsm;
        process(sb, false);
        updateState();
        sb.addScoreBoardListener(this);
    }

    @Override
    public void scoreBoardChange(ScoreBoardEvent<?> event) {
        synchronized (this) {
            try {
                ScoreBoardEventProvider p = event.getProvider();
                String provider = p.getProviderName();
                Property<?> prop = event.getProperty();
                Object v = event.getValue();
                boolean rem = event.isRemove();
                if (prop == ScoreBoardEventProviderImpl.BATCH_START) {
                    batch++;
                } else if (prop == ScoreBoardEventProviderImpl.BATCH_END) {
                    if (batch > 0) { batch--; }
                } else if (prop instanceof Value) {
                    update(getPath(p), prop, v);
                } else if (prop instanceof Child) {
                    if (v instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider) v).getParent() == p) {
                        process((ScoreBoardEventProvider) v, rem);
                    } else if (rem) {
                        remove(getPath(p), prop, ((ValueWithId) v).getId());
                    } else {
                        update(getPath(p), prop, v);
                    }
                } else {
                    Logger.printMessage(provider + " update of unknown kind.	prop: " + prop.getJsonName() +
                                        ", v: " + v);
                }
            } catch (Exception e) { Logger.printStackTrace(e); } finally {
                if (batch == 0) { updateState(); }
            }
        }
    }

    private void updateState() {
        synchronized (this) {
            if (updates.isEmpty()) { return; }
            jsm.updateState(updates);
            updates.clear();
        }
    }

    private void update(String prefix, Property<?> prop, Object v) {
        String path = prefix + "." + prop.getJsonName();
        if (prop instanceof Child) {
            updates.add(new WSUpdate(path + "(" + ((ValueWithId) v).getId() + ")", ((ValueWithId) v).getValue()));
        } else if (v instanceof ScoreBoardEventProvider) {
            updates.add(new WSUpdate(path, ((ScoreBoardEventProvider) v).getId()));
        } else if (v == null || v instanceof Boolean || v instanceof Integer || v instanceof Long) {
            updates.add(new WSUpdate(path, v));
        } else {
            updates.add(new WSUpdate(path, v.toString()));
        }
    }

    private void remove(String prefix, Property<?> prop, String id) {
        String path = prefix + "." + prop.getJsonName() + "(" + id + ")";
        updates.add(new WSUpdate(path, null));
    }

    private void process(ScoreBoardEventProvider p, boolean remove) {
        String path = getPath(p);
        updates.add(new WSUpdate(path, null));
        if (remove) { return; }

        for (Property<?> prop : p.getProperties()) {
            if (prop instanceof Value) {
                Object v = p.get((Value<?>) prop);
                if (v == null) { v = ""; }
                update(path, prop, v);
            } else if (prop instanceof Child) {
                for (ValueWithId c : p.getAll((Child<?>) prop)) {
                    if (c instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider) c).getParent() == p) {
                        process((ScoreBoardEventProvider) c, false);
                    } else {
                        update(getPath(p), prop, c);
                    }
                }
            }
        }
    }

    String getPath(ScoreBoardEventProvider p) {
        String path = "";
        if (p.getParent() != null) { path = getPath(p.getParent()) + "."; }
        path = path + p.getProviderName();
        if (!"".equals(p.getProviderId()) && p.getProviderId() != null) { path = path + "(" + p.getProviderId() + ")"; }
        return path;
    }

    private JSONStateManager jsm;
    private List<WSUpdate> updates = new LinkedList<>();
    private long batch = 0;
}
