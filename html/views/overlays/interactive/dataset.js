/* 
   DataSet Manager
   (C) Copyright 2015, Jared Quinn
*/

var DataSet = (function() { 

	/* Utility Functions */

	var _arrayContains = function(haystack, needle) {
		var i = haystack.length;
		while (i--) { if (haystack[i] === needle) { return true; } }
		return false;
	}



	/************************************************************************************************/
	/* Data Trigger Handler
	/************************************************************************************************/
	function DataTriggers() { }

	DataTriggers.prototype = new Array();

	DataTriggers.prototype.get = function(typ, key) {
		var found = [];
		for(var i = 0; i < this.length ; i++ ) {
			var trigger = this[i];
			var ut = (trigger.Type == typ && (key == '*' || _arrayContains(trigger.Key, key) ));
			if(ut) found.push(trigger);
		}
		return found;
	}

	DataTriggers.prototype.add = function(typ, key, match, func) {
		var sk;
		sk = typeof(key) == 'array' || typeof(key) == 'object' ? key : [ key ];
		e = { Type: typ, Func: func, Key: sk, Match: match };
		return this.push(e);
	}

	DataTriggers.prototype.action = function(typ, key, newval, oldval, fullRecord) {
		trig = this.get(typ, key);
		for(var i = 0; i < trig.length; i++) {
			var trigger = trig[i];
			if(fullRecord.isMatch(trigger.Match)) {
				trigger.Func.call(fullRecord, newval, oldval, key);
			}
		}
	}

	/************************************************************************************************/
	/* Individual Data Records 
	/************************************************************************************************/

	function DataRecord(data, ds) { 
		this.DataSet = ds;
		this.Keys = {};
		this.set(data);
	}

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

	DataRecord.prototype.keys = function() {
		var ret = [];
		for(k in this) ret.push(k);
		return ret;
	}

	DataRecord.prototype.set = function(data) {
		var up = [];
		for(k in data) {
			var dst = this[k], src = data[k];
			if(src === dst) continue;
			this[k] = src;
			this.Keys[k] = src;
			up.push({'Key': k, 'Original': dst, 'New': src});
		}
		for(var c = 0; c < up.length; c++ ) 
			this.DataSet.Triggers.action( 'VALUE', up[c].Key, up[c].New, up[c].Original, this );

		return this;
	}

	DataRecord.prototype.values = function() {
		return this.Keys;
	}

	/************************************************************************************************/
	/* Data Select Object 
	/************************************************************************************************/

	function DataSelect(owner, match) {
		this.DataSet = owner;
		this.Selector = match;

		var items = this.DataSet._filter(match);
		for(var i = 0; i < items.length; i++) 
			this.push(items[i]);
	}


	DataSelect.prototype = new Array();

	DataSelect.prototype.Each = function(callback) {
		for(var i = 0; i < this.length; i++) 
			callback.call(this[i]);
		return this;
	}

	/************************************************************************************************/
	/* Data Set Object 
	/************************************************************************************************/

	function DataSet() {
		this.Triggers = new DataTriggers();
	}	
	
	DataSet.prototype = new Array();

	DataSet.prototype._filter = function(match) {
		var found = [];
		for(var i = 0; i < this.length; i ++) {
			if( this[i].isMatch(match)) { found.push(this[i]); }
		}
		return found;
	}

	/* Magic UpSert & Transform Function */
	DataSet.prototype.Upsert = function(data, match) {
		var found = match ? this._filter(match) : [];
		if(found.length == 0) {
			for(var k in match) { data[k] = match[k]; }
			return this.Insert( data );
		} else {
			this.Update( found, data );
		}
		return found;
	}
	
	DataSet.prototype.Delete = function(matches) {
		for(var i = 0; i < this.length; i ++) {
			var c = this[i];
			var o = c.values();
			this.Triggers.action('DELETE', '*', {}, o, c );
			this.splice(i,1);
		}
	}
	
	DataSet.prototype.Filter = function(match) {
		var items = this._filter(match);
		return new DataSelect(this, match);
	}

	DataSet.prototype.All = function(match) {
		return new DataSelect(this, {});
	}

	DataSet.prototype.Update = function(records, newdata) {
		var upd = [];
		for(var i = 0; i < records.length; i ++) {
			var record = records[i];
			var orig = record.values();
			record.set(newdata);
			upd.push(record);
			this.Triggers.action( 'UPDATE', '*', record, orig, record );
		};
		return upd;
	}

	DataSet.prototype.AddTrigger = function(typ, key, match, func) {
		return this.Triggers.add(typ, key, match, func);
	}

	
	DataSet.prototype.Insert = function(data) {
		var newRecord = new DataRecord( data, this );
		this.push( newRecord );

		this.Triggers.action( 'INSERT', '*', newRecord, {}, newRecord );
		return [ newRecord ];
	}

	return DataSet;

})();

o = new DataSet();

