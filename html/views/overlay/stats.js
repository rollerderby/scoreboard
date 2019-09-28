jQuery.fn.sortRows = function sortRows() {
    $("> tr", this[0]).sort(dec_sort).appendTo(this[0]);
    function dec_sort(a, b){ return ($(b).attr("data-sort")) > ($(a).attr("data-sort")) ? 1 : -1; }
}

$(document).ready(function() {
	StatsEngine.Initialize({ debug: true });

	StatsEngine.GetSomething()['Jams'].AddTrigger('INSERT', '*', { }, function(nv, ov, key) { 
		c = (parseInt(this.Period) * 1000) + parseInt(this.Jam);
		row = $('<tr>').attr('id', 'Jam-P' + this.Period + '-J' + this.Jam).attr('period', this.Period).attr('jam', this.Jam).attr('data-sort', c);

		if(this.Period) { $('<td class="DataPeriod">').text(this.Period).appendTo(row); }
		if(this.Jam)    { $('<td class="DataJam">').text(this.Jam).appendTo(row); }

		$('<td class="DataPeriodClockStart">').appendTo(row);
		$('<td class="DataDurationHuman">').appendTo(row);
		$('table#Jams tbody').prepend(row);
		$('table#Jams tbody').sortRows();	
	});
	StatsEngine.GetSomething()['Jams'].AddTrigger('VALUE', ['DurationHuman', 'PeriodClockStart'], { }, function(nv, ov, key) { 
		$('tr#Jam-P'+this.Period+'-J'+this.Jam + ' .Data'+key).text(nv);
	});
});

