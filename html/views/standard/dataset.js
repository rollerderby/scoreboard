/* 
   DataSet Manager
   (C) Copyright 2015, Jared Quinn
*/

var DataSet = function() {

	var _Records = [ ];
	var ACTIONS = { Insert: 'INSERT', Update: 'UPDATE', Value: 'VALUE' }

	function DataRecord() { }

	DataRecord.prototype.isMatch = function(required) {
		var va = 0, vb = 0;
		//if(typeof(required) == 'function') { return required.call(this); }
		for(k in required) { 	
			va++; 
			if(this[k] === required[k]) { vb++; } 
		}
		if(va == vb) return true;
		return false;
	}

	DataRecord.prototype.set = function(data) {
		var up = [];
		for(k in data) {
			var dst = this[k], src = data[k];
			if(src === dst) continue;
			this[k] = src;
			up.push({'Key': k, 'Original': dst, 'New': src});
		}
		for(var c = 0; c < up.length; c++ ) triggers.action( ACTIONS.Value, up[c].Key, up[c].New, up[c].Original, this);
		return this;
	}

	DataRecord.prototype.values = function() {
		var res = {};
		for(k in this) { res[k] = this[k]; }
		return res;
	}


	/* We encapsulate Trigger Handling In this Object */
	var triggers = {
		_triggers: [],
		all: function() {
			return triggers._triggers;
		},
		get: function(typ, key) {
			var found = [];
			for(var i = 0; i < triggers._triggers.length ; i++ ) {
				var trigger = triggers._triggers[i];
				var ut = (trigger.Type == typ && (key == '*' || _arrayContains(trigger.Key, key) ));
				if(ut) found.push(trigger);
			}
			return found;
		},
		action: function(typ, key, newval, oldval, fullRecord) {
			trig = triggers.get(typ, key);
			for(var i = 0; i < trig.length; i++) {
				var trigger = triggers._triggers[i];
				if(fullRecord.isMatch(trigger.Match)) {
					trigger.Func.call(fullRecord, newval, oldval, key);
				}
			}
		},
		add: function(typ, key, match, func) {
			var sk;
			sk = typeof(key) == 'array' ? key : [ key ];
			e = { Type: typ, Func: func, Key: sk, Match: match };
			return triggers._triggers.push(e);
		},
		remove: function(id) {
			delete triggers._triggers[id];
		}

	}

	var _arrayContains = function(a,b) {
		var i = b.length;
		while (i--) { if (a[i] === b[i]) { return true; } }
		return false;
	}

	var _filter = function(match) {
		var found = [];
		for(var i = 0; i < _Records.length; i ++) {
			if( _Records[i].isMatch(match)) { found.push(_Records[i]); }
		}
		return found;
	}

	var _insert = function(data) {
		var rec = new DataRecord();
		rec.set( data );
		_Records.push( rec );

		triggers.action( ACTIONS.Insert, null, rec, {}, rec );
		return [ rec ];
	}

	/* Update a Record */
	var _update = function(records, newdata) {
		var upd = [];
		for(var i = 0; i < records.length; i ++) {
			var record = _Records[i];
			var orig = record.values();

			record.set(newdata);
			upd.push(record);
			triggers.action( ACTIONS.Update, null, record, orig, record );
		};
		return upd;
	}

	/* Magic UpSert & Transform Function */
	var _upsert = function(data, match) {
		var found = match ? _filter(match) : [];
		if(found.length == 0) {
			for(var k in match) { data[k] = match[k]; }
			return _insert( data );
		} else {
			_update( found, data );
		}
	    return found;
	}
	
	return {
		ACTIONS: ACTIONS,
		Upsert: _upsert,
		Filter: _filter,
		AddTrigger: triggers.add,
		GetTriggers: triggers.all,
		_data: _Records,
	}
}

o = new DataSet();

