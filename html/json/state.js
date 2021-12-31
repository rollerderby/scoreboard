WS.Connect();
WS.Register('', display);

function display(k, v) {
  'use strict';
  var row = findRow(k);
  if (v != null) {
    if ($.isPlainObject(v)) {
      row.find('td.Value').text(JSON.stringify(v));
    } else {
      row.find('td.Value').text(v);
    }
  } else {
    row.remove();
  }
}

function findRow(k) {
  'use strict';
  var row = $('tr[key="' + k + '"]');
  if (row.length === 0) {
    row = $('<tr>').attr('key', k);
    $('<td>').addClass('Key').text(k).appendTo(row);
    $('<td>').addClass('Value').appendTo(row);
    _windowFunctions.appendAlphaSortedByAttr($('table tbody'), row, 'key');
  }
  return row;
}
