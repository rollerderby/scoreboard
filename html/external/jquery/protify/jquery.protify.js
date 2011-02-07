/**
 *  Protify jQuery Plugin
 *  version 0.3
 *  
 * Copyright (c) 2009 Josh Powell
 * Licensed under the MIT license.
 * 
 *  * Date: 2009-02-04 11:45:50 (Wed, 04 Feb 2009)
 *  
 */
(function ($) {
  var $break = { };
  var arrayFunc = {
    _each: function(iterator) {
      for (var i = 0, length = this.length; i < length; i++) {
        iterator(this[i]);
      }
    },
  
    all: function(iterator, context) {
      iterator = iterator || function(x) { return x; };
      var result = true;
      this.each(function(value, index) {
        result = result && !!iterator.call(context, value, index);
        if (!result) { throw $break; }
      });
      return result;
    },
  
    any: function(iterator, context) {
      iterator = iterator || function(x) { return x; };
      var result = false;
      this.each(function(value, index) {
        if (result = !!iterator.call(context, value, index)) {
          throw $break;
        }
      });
      return result;
    },
  
    clear: function() {
      this.length = 0;
      return this;
    },
  
    clone: function() {
      return $.protify([].concat(this));
    },

    collect: function(iterator, context) {
      iterator = iterator || function(x) { return x; };
      var results = $.protify([]);
      this.each(function(value, index) {
        results.push(iterator.call(context, value, index));
      });
      return results;
    },
  
    detect: function(iterator, context) {
      var result;
      this.each(function(value, index) {
        if (iterator.call(context, value, index)) {
          result = value;
          throw $break;
        }
      });
      return result;
    },
    
    compact: function() {
      return $.protify(this.select(function(value) {
        return value !== null;
      }));
    },

    each: function(iterator, context) {
      context = context || this;
      var index = 0;
      try {
        this._each(function(value) {
          iterator.call(context, value, index++);
        });
      } catch (e) {
        if (e != $break) { throw e; }
      }
      return this;
    },
    
    eachSlice: function(number, iterator, context) {
      var index = -number, slices = [], array = this.toArray();
      if (number < 1) { return array; }
      while ((index += number) < array.length) {
        slices.push(array.slice(index, index+number));
      }
      return $.protify($.protify(slices).collect(iterator, context));
    },
    
    extended : function() {
      return true;
    },

    findAll: function(iterator, context) {
      var results = $.protify([]);
      this.each(function(value, index) {
        if (iterator.call(context, value, index)) {
          results.push(value);
        }
      });
      return results;
    },
  
    flatten: function() {
      return this.inject([], function(array, value) {
        $.protify(value);
        return $.protify(array.concat($.isArray(value) ?
          value.flatten() : [value]));
      });
    },
    
    first: function() {
      return this[0];
    },
  
    grep: function(filter, iterator, context) {
      iterator = iterator || function(x) { return x; };
      var results = $.protify([]);
      if (typeof filter === 'string') {
        filter = new RegExp(filter);
      }
  
      this.each(function(value, index) {
        if (filter.test(value)) {
          results.push(iterator.call(context, value, index));
        }
      });
      return results;
    },
  
    include: function(object) {
        if ($.isFunction(this.indexOf)) {
            if (this.indexOf(object) != -1) { 
              return true; 
            }
        }
  
      var found = false;
      this.each(function(value) {
        if (value == object) {
          found = true;
          throw $break;
        }
      });
      return found;
    },
    
    indexOf: function(item, i) {
        i || (i = 0);
        var length = this.length;
        if (i < 0) i = length + i;
        for (; i < length; i++)
          if (this[i] === item) return i;
        return -1;
    },
  
    inGroupsOf: function(number, fillWith) {
      fillWith = fillWith ? null : fillWith;
      return this.eachSlice(number, function(slice) {
        while(slice.length < number) { slice.push(fillWith); }
        return slice;
      });
    },
  
    inject: function(memo, iterator, context) {
      this.each(function(value, index) {
        memo = iterator.call(context, memo, value, index);
      });
      return memo;
    },
    
    inspect: function() {
      return '[' + this.map($.inspect).join(', ') + ']';
    },
  
    intersect: function(array) {
      $.protify(array);
      return this.uniq().findAll(function(item) {
        return array.detect(function(value) { return item === value; });
      });
    },
  
    invoke: function(method) {
      var args = $.makeArray(arguments).slice(1);
      return this.map(function(value) {
        return value[method].apply(value, args);
      });
    },
  
    last: function() {
      return this[this.length - 1];
    },

    lastIndexOf : function(item, i) {
        i = isNaN(i) ? this.length : (i < 0 ? this.length + i : i) + 1;
        var n = $.protify(this.slice(0, i).reverse()).indexOf(item);
        return (n < 0) ? n : i - n - 1;
    },
    max: function(iterator, context) {
      iterator = iterator || function(x) { return x; };
      var result;
      this.each(function(value, index) {
        value = iterator.call(context, value, index);
        if (result == null || value >= result) {
          result = value;
        }
      });
      return result;
    },
  
    min: function(iterator, context) {
      iterator = iterator || function(x) { return x; };
      var result;
      this.each(function(value, index) {
        value = iterator.call(context, value, index);
        if (result == null || value < result) {
          result = value;
        }
      });
      return result;
    },
  
    partition: function(iterator, context) {
      iterator = iterator || function(x) { return x; };
      var trues = [], falses = [];
      this.each(function(value, index) {
        (iterator.call(context, value, index) ? trues : falses).push(value);
      });
      return [trues, falses];
    },
  
    pluck: function(property) {
      var results = $.protify([]);
      this.each(function(value) {
        results.push(value[property]);
      });
      return results;
    },
    
    purge: function () {
      return [].concat(this);
    },
  
    reduce: function() {
      return this.length > 1 ? this : this[0];
    },
  
    reject: function(iterator, context) {
      var results = $.protify([]);
      this.each(function(value, index) {
        if (!iterator.call(context, value, index)) {
          results.push(value);
        }
      });
      return results;
    },
  
    size: function() {
      return this.length;
    },
  
    sortBy: function(iterator, context) {
      return this.map(function(value, index) {
        return {
          value: value,
          criteria: iterator.call(context, value, index)
        };
      }).sort(function(left, right) {
        var a = left.criteria, b = right.criteria;
        return a < b ? -1 : a > b ? 1 : 0;
      }).pluck('value');
    },
  
    toArray: function() {
      return $.protify(this.map());
    },
    
//  toJSON: function() {
//  var results = [];
//  this.each(function(object) {
//    var value = Object.toJSON(object);
//    if (!Object.isUndefined(value)) results.push(value);
//  });
//  return '[' + results.join(', ') + ']';
//},
  
    uniq: function(sorted) {
      return $.protify(this.inject([], function(array, value, index) {
        $.protify(array, true);
        if (0 === index || (sorted ? array.last() != value : !array.include(value))) {
          array.push(value);
        }
        return array;
      }));
    },
    
    without: function() {
      var values = $.protify($.makeArray(arguments));
      return $.protify(this.select(function(value) {
        return !values.include(value);
      }));
    },
  
    zip: function() {
      var iterator = function(x) { return x; }, args = $.protify($.makeArray(arguments));
      if ($.isFunction(args.last())) {
        iterator = args.pop();
      }
  
      var collections = $.protify([this].concat(args)).map();
      return this.map(function(value, index) {
        return iterator(collections.pluck(index));
      });
    }
  };
  
  $.extend(arrayFunc, {
    map:     arrayFunc.collect,
    find:    arrayFunc.detect,
    select:  arrayFunc.findAll,
    filter:  arrayFunc.findAll,
    member:  arrayFunc.include,
    entries: arrayFunc.toArray,
    every:   arrayFunc.all,
    some:    arrayFunc.any
  });  
  
  $.protify = function(target, permanent) {
    if (permanent) {
      $.extend(target, arrayFunc);
      return target;
    }
    target = $.extend(target.slice(), arrayFunc);
    return target;
  };
  
})(jQuery);