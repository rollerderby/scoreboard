

_timeConversions = {
	/*
	 * These are conversion functions for external use
	 */

	/* Convert ms to seconds */
	msToSeconds: function(ms) { return String(Math.floor(ms / 1000)); },
	/* Convert seconds to milliseconds */
	secondsToMs: function(sec) { return String(sec * 1000); },
	/* Convert milliseconds to min:sec */
	msToMinSec: function(ms) { return _timeConversions._msOnlyMin(ms)+":"+_timeConversions._msOnlySec(ms); },
	/* Convert min:sec to milliseconds */
	minSecToMs: function(time) {
		var min = Number(_timeConversions._timeOnlyMin(time) || 0);
		var sec = Number(_timeConversions._timeOnlySec(time) || 0);
		return String(((min * 60) + sec) * 1000);
	},
	/* Convert milliseconds to min:sec, if min is 0 return only seconds portion */
	msToMinSecNoZero: function(ms) {
		return ((ms >= 60000) ? _timeConversions.msToMinSec(ms) : _timeConversions.msToSeconds(ms));
	},
	/* Same as minSecToMs() */
	minSecNoZeroToMs: function(time) { return _timeConversions.minSecToMs(time); },

	/*
	 * These are utility functions that can be used externally
	 */

	/* Format number into minimum 2 digits, prepending 0 if needed */
	twoDigit: function(n) { return (10 > n ? "0" : "")+n; },

	/*
	 * These are generally internal functions that should not be used externally
	 */

	/* If needed, this preprends ":" */
	_formatMinSec: function(time) { return ((String(time).indexOf(":") > -1) ? time : ":"+time); },
	/* Convert ms to seconds portion of min:sec */
	_msOnlySec: function(ms) { return _timeConversions.twoDigit(_timeConversions.msToSeconds(ms) % 60); },
	/* Convert ms to minutes portion of min:sec */
	_msOnlyMin: function(ms) { return String(Math.floor(ms / 60000)); },
	/* Accepts min:sec time, returns only seconds portion */
	_timeOnlySec: function(time) { return _timeConversions._formatMinSec(time).split(":")[1]; },
	/* Accepts min:sec time, returns only minutes portion */
	_timeOnlyMin: function(time) { return _timeConversions._formatMinSec(time).split(":")[0]; }
};
