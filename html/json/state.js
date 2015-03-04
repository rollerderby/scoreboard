WS.Connect();
WS.Register("", display);

function display(k, v) {
	var row = findRow(k);
	if (v != null)
		row.find("td.Value").text(v);
	else
		row.remove();
}

function findRow(k) {
	var row = $('tr[key="' + k + '"]');
	if (row.length == 0) {
		row = $("<tr>").attr("key", k);
		$("<td>").addClass("Key").text(k).appendTo(row);
		$("<td>").addClass("Value").appendTo(row);
		_windowFunctions.appendAlphaSortedByAttr($("table tbody"), row, "key");
	}
	return row;
}
