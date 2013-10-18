
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

(function($) {
	var SORTED_TABLE_TAG = "sortedtable-table";
	var SORT_ASCEND = "sortedtable-sort-ascend";
	var SORT_DESCEND = "sortedtable-sort-descend";

	var doAscend = function(table) {
		return table.find("th."+SORT_ASCEND+",th."+SORT_DESCEND).first().hasClass(SORT_ASCEND);
	};

	var getCol = function(table) {
		return table.find("th."+SORT_ASCEND+",th."+SORT_DESCEND).index();
	};

	var compareRows = function(a, b, col) {
		var c = a.find("td:eq("+col+")").text() > b.find("td:eq("+col+")").text();
		return c;
	};

	var insertRow = function(table, row) {
		var a = doAscend(table);
		var col = getCol(table);
		row = $(row);
		table.find("tbody tr").each(function(i, r) {
			if (a == compareRows($(r), row, col)) {
				row.insertBefore(r);
				return false;
			}
		});
		if (!$.contains(table[0], row[0]))
			table.find("tbody").append(row);
	};

	var doSort = function(table) {
		table.find("tbody tr").detach().each(function(i, r) {
			insertRow(table, r);
		});
	};

	var setup = function(table, args) {
		if (table.hasClass(SORTED_TABLE_TAG))
			return;
		args = args || {};
		var header = args.header || table.find("thead tr").first();
		if (!header.length)
			return;
		var th = header.find("th")
			.click(function() {
				if ($(this).hasClass(SORT_ASCEND))
					$(this).removeClass(SORT_ASCEND).addClass(SORT_DESCEND);
				else if ($(this).hasClass(SORT_DESCEND))
					$(this).removeClass(SORT_DESCEND).addClass(SORT_ASCEND);
				else
					$(this).addClass(SORT_ASCEND);
				header.find("th").not(this).removeClass(SORT_ASCEND+" "+SORT_DESCEND);
				doSort(table);
			})
			.each(function(i, e) {
				$("<span>^</span>").addClass(SORT_ASCEND).appendTo(e);
				$("<span>v</span>").addClass(SORT_DESCEND).appendTo(e);
			})
			.first();
		if (!th.length)
			return;

		table.addClass(SORTED_TABLE_TAG);
		th.addClass(SORT_ASCEND);
		doSort(table);
	};

	/**
	 * Create a "sorted" table
	 *
	 * To setup, call without params or pass an object.
	 * The object fields can be:
	 *	 header: The row in the table header to use
	 *
	 * To insert a new row, call with the args
	 *	 "insert", row
	 * To (re)sort the table rows, call with the arg
	 *	 "sort"
	 */
	$.fn.sortedtable = function(arg1, arg2) {
		var table = this.filter("table");
		if (!table.length)
			return this;

		if (!arg1 || $.isPlainObject(arg1)) {
			setup(table, arg1);
		} else if (!table.hasClass(SORTED_TABLE_TAG)) {
			return table;
		} else if (arg1 == "insert") {
			insertRow(table, arg2);
		} else if (arg1 == "sort") {
			doSort(table);
		}

		return this;
	};

	$(function() {
		$("style").attr("type", "text/css")
			.text(
				"table."+SORTED_TABLE_TAG+" th span."+SORT_ASCEND+" { float: right; } "+
				"table."+SORTED_TABLE_TAG+" th span."+SORT_DESCEND+" { float: right; } "+
				"table."+SORTED_TABLE_TAG+" th:not(."+SORT_ASCEND+") span."+SORT_ASCEND+" { display: none; } "+
				"table."+SORTED_TABLE_TAG+" th:not(."+SORT_DESCEND+") span."+SORT_DESCEND+" { display: none; } "
			)
			.insertAfter("head");
	});
})(jQuery);

