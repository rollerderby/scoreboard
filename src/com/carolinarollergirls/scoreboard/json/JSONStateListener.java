package com.carolinarollergirls.scoreboard.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public interface JSONStateListener {
    // A snapshot of the current state, and which keys it it have changed.
    // Keys with a value of null are considered deleted, and will not be present
    // in state.
    public void sendUpdates(StateTrie state, StateTrie changed);

    class PathTrie {
        boolean isPath = false;
        Map<String, PathTrie> subtries = new HashMap<>();

        public PathTrie() {}
        public PathTrie(Set<String> content) { addAll(content); }

        public void addAll(Set<String> c) {
            for (String p : c) { add(p); }
        }
        public void add(String path) {
            String[] p = path.split("(?=[.(])");
            PathTrie head = this;
            for (int i = 0; !head.isPath && i < p.length; i++) {
                if (head.subtries.containsKey(p[i])) {
                    head = head.subtries.get(p[i]);
                } else {
                    PathTrie child = new PathTrie();
                    head.subtries.put(p[i], child);
                    head = child;
                }
            }
            head.isPath = true;
        }

        public boolean covers(String p) { return _covers(p.split("(?=[.(])"), 0); }
        private boolean _covers(String[] p, int i) {
            PathTrie head = this;
            for (;; i++) {
                if (head.isPath) { return true; }
                if (i >= p.length) { return false; }
                // Allow Blah(*).
                String catchAllKey = p[i].charAt(0) + "*)";
                if (head.subtries.containsKey(catchAllKey)) {
                    int j;
                    // id captured by * might contain . and thus be split - find the end
                    for (j = i; j < p.length && !p[j].endsWith(")"); j++)
                        ;
                    if (head.subtries.get(catchAllKey)._covers(p, j + 1)) { return true; }
                }
                head = head.subtries.get(p[i]);
                if (head == null) { return false; }
            }
        }

        public void merge(PathTrie other) {
            if (other.isPath) { isPath = true; }
            for (String key : other.subtries.keySet()) {
                if (subtries.containsKey(key)) {
                    subtries.get(key).merge(other.subtries.get(key));
                } else {
                    subtries.put(key, other.subtries.get(key));
                }
            }
        }

        public Map<String, Object> intersect(StateTrie stateTrie, boolean filterSecrets) {
            Map<String, Object> results = new TreeMap<>();
            if (stateTrie != null) { _intersect(stateTrie, results, "", filterSecrets, false); }
            return results;
        }
        private void _intersect(StateTrie stateTrie, Map<String, Object> results, String prefix, boolean filterSecrets,
                                boolean coveringAsterisk) {
            if (coveringAsterisk) {
                for (String otherKey : stateTrie.subtries.keySet()) {
                    _intersect(stateTrie.subtries.get(otherKey), results, prefix + otherKey, filterSecrets,
                               !otherKey.endsWith(")"));
                }
            } else if (isPath) {
                stateTrie.fetchAll(results, prefix, filterSecrets);
            } else {
                for (String key : subtries.keySet()) {
                    if (key.endsWith("*)")) {
                        for (String otherKey : stateTrie.subtries.keySet()) {
                            subtries.get(key)._intersect(stateTrie.subtries.get(otherKey), results, prefix + otherKey,
                                                         filterSecrets, !otherKey.endsWith(")"));
                        }
                    } else if (stateTrie.subtries.containsKey(key)) {
                        subtries.get(key)._intersect(stateTrie.subtries.get(key), results, prefix + key, filterSecrets,
                                                     false);
                    }
                }
            }
        }
    }

    class StateTrie implements Cloneable {
        boolean isPath;
        Object value;
        Map<String, StateTrie> subtries = new HashMap<>();

        public StateTrie() {}
        public StateTrie(Map<String, Object> content) { addAll(content); }

        @Override
        public StateTrie clone() {
            StateTrie clone = new StateTrie();
            clone.isPath = isPath;
            clone.value = value;
            for (String key : subtries.keySet()) { clone.subtries.put(key, subtries.get(key).clone()); }
            return clone;
        }

        public Object get(String key) { return _get(key.split("(?=[.(])"), 0); }
        private Object _get(String[] p, int i) {
            if (i == p.length) {
                return value;
            } else if (subtries.containsKey(p[i])) {
                return subtries.get(p[i])._get(p, i + 1);
            } else {
                return null;
            }
        }
        public Map<String, Object> getAll(boolean filterSecrets) {
            Map<String, Object> results = new TreeMap<>();
            fetchAll(results, "", filterSecrets);
            return results;
        }
        public void fetchAll(Map<String, Object> results, String prefix, boolean filterSecrets) {
            if (isPath && (!filterSecrets || !prefix.endsWith("Secret"))) { results.put(prefix, value); }
            for (String key : subtries.keySet()) { subtries.get(key).fetchAll(results, prefix + key, filterSecrets); }
        }
        public boolean isEmpty() { return !isPath && subtries.isEmpty(); }
        public int size() {
            int size = isPath ? 1 : 0;
            for (StateTrie subtrie : subtries.values()) { size += subtrie.size(); }
            return size;
        }

        public void addAll(Map<String, Object> c) {
            for (String p : c.keySet()) { add(p, c.get(p)); }
        }
        public void add(String path, Object val) {
            String[] p = path.split("(?=[.(])");
            StateTrie head = this;
            for (int i = 0; i < p.length; i++) {
                if (head.subtries.containsKey(p[i])) {
                    head = head.subtries.get(p[i]);
                } else {
                    StateTrie child = new StateTrie();
                    head.subtries.put(p[i], child);
                    head = child;
                }
            }
            head.isPath = true;
            head.value = val;
            if (val == null) { head.subtries.clear(); }
        }

        public void remove(String path) { _remove(path.split("(?=[.(])"), 0); }
        public boolean _remove(String[] p, int i) {
            if (i == p.length) {
                value = null;
                isPath = false;
            } else if (subtries.containsKey(p[i])) {
                if (subtries.get(p[i])._remove(p, i + 1)) { subtries.remove(p[i]); }
            }
            return isEmpty();
        }

        public void mergeChangeTrie(StateTrie changeTrie) { _mergeChangeTrie(changeTrie, false); }
        public boolean _mergeChangeTrie(StateTrie changeTrie, boolean removing) {
            if (changeTrie.isPath) {
                if (isPath && value.equals(changeTrie.value)) {
                    changeTrie.isPath = false;
                } else {
                    changeTrie.isPath = isPath || changeTrie.value != null;
                    value = changeTrie.value;
                    isPath = value != null;
                    removing = removing || value == null;
                }
            } else if (removing && isPath) {
                value = null;
                isPath = false;
            }
            List<String> emptyChange = new ArrayList<>();
            Set<String> trieKeys = new HashSet<>(subtries.keySet());
            for (Entry<String, StateTrie> entry : changeTrie.subtries.entrySet()) {
                String key = entry.getKey();
                if (subtries.containsKey(key)) {
                    if (subtries.get(key)._mergeChangeTrie(entry.getValue(), removing)) { subtries.remove(key); };
                    if (entry.getValue().isEmpty()) { emptyChange.add(key); }
                } else {
                    StateTrie cleaned = changeTrie.subtries.get(key)._cleanAndClone();
                    if (cleaned == null) {
                        emptyChange.add(key);
                    } else {
                        subtries.put(key, cleaned);
                    }
                }
            }
            for (String key : trieKeys) {
                if (removing && !changeTrie.subtries.containsKey(key) && subtries.containsKey(key)) {
                    changeTrie.subtries.put(key, subtries.get(key)._nullValues());
                    subtries.remove(key);
                }
            }
            for (String key : emptyChange) { changeTrie.subtries.remove(key); }
            return isEmpty();
        }
        private StateTrie _nullValues() {
            value = null;
            for (StateTrie subtrie : subtries.values()) { subtrie._nullValues(); }
            return this;
        }
        private StateTrie _cleanAndClone() {
            if (value == null) { isPath = false; }
            StateTrie clone = new StateTrie();
            clone.value = value;
            clone.isPath = isPath;
            Iterator<Entry<String, StateTrie>> it = subtries.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, StateTrie> item = it.next();
                StateTrie subtrie = item.getValue()._cleanAndClone();
                if (subtrie == null) {
                    it.remove();
                } else {
                    clone.subtries.put(item.getKey(), subtrie);
                }
            }
            return isEmpty() ? null : clone;
        }

        public Map<String, Object> filter(PathTrie filter, boolean filterSecrets) {
            return filter.intersect(this, filterSecrets);
        }
    }
}
