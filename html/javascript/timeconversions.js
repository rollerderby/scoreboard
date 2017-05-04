
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */


_timeConversions = {
	/* Policy: default round ms up or down
	 * If true, ms values will be rounded up, i.e. 1001-2000 ms converts to 2 sec.
	 * If false, ms values will be rounded down, i.e. 1000-1999 ms converts to 1 sec.
	 *
	 * This is only used for non-$sb(Clock.Time) conversions;
	 * for $sb(Clock.Time) conversions, the rounding will be opposite the Clock.Direction.
	 */
	defaultRoundUp: true,

	/*
	 * These are conversion functions for external use
	 */

	/* Convert ms to seconds */
	msToSeconds: function(ms) {
		if (_timeConversions._roundClockUp.call(this))
			return String(Math.ceil(ms / 1000));
		else
			return String(Math.floor(ms / 1000));
	},
	/* Convert seconds to milliseconds */
	secondsToMs: function(sec) { return String(sec * 1000); },
	/* Convert milliseconds to min:sec */
	msToMinSec: function(ms) {
		return _timeConversions._msOnlyMin.call(this, ms)+":"+_timeConversions._msOnlySec.call(this, ms);
	},
	/* Convert min:sec to milliseconds */
	minSecToMs: function(time) {
		var min = Number(_timeConversions._timeOnlyMin.call(this, time) || 0);
		var sec = Number(_timeConversions._timeOnlySec.call(this, time) || 0);
		return _timeConversions.secondsToMs.call(this, (min * 60) + sec);
	},
	/* Convert milliseconds to min:sec, if min is 0 return only seconds portion */
	msToMinSecNoZero: function(ms) {
		var limit = (_timeConversions._roundClockUp.call(this) ? 59001 : 60000);
		if (ms >= limit)
			return _timeConversions.msToMinSec.call(this, ms);
		else
			return _timeConversions.msToSeconds.call(this, ms);
	},
	/* Same as minSecToMs() */
	minSecNoZeroToMs: function(time) { return _timeConversions.minSecToMs.call(this, time); },

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
	_msOnlySec: function(ms) {
		return _timeConversions.twoDigit.call(this, _timeConversions.msToSeconds.call(this, ms) % 60);
	},
	/* Convert ms to minutes portion of min:sec */
	_msOnlyMin: function(ms) {
		var min = Math.floor(ms / 60000);
		// if rounding up and seconds are 59001-59999, we need to add a minute
		min = min + Number(_timeConversions.msToSeconds.call(this, Math.max(0, (ms % 60000) - 59000)));
		return String(min);
	},
	/* Accepts min:sec time, returns only seconds portion */
	_timeOnlySec: function(time) {
		return _timeConversions._formatMinSec.call(this, time).split(":")[1];
	},
	/* Accepts min:sec time, returns only minutes portion */
	_timeOnlyMin: function(time) {
		return _timeConversions._formatMinSec.call(this, time).split(":")[0];
	},
	/* If converting the time from a $sb Clock, determine the rounding direction */
	_roundClockUp: function() {
		if (typeof $sb == "undefined")
			return _timeConversions.defaultRoundUp;
		if (!is$sb(this) || !(this.$sbName == "Time"))
			return _timeConversions.defaultRoundUp; // Not a Clock.Time, use default
		var countDir = this.parent().children("Direction");
		if (!countDir.length)
			return _timeConversions.defaultRoundUp; // Can't find Clock.Direction, use default
		// Round direction should be opposite count direction, e.g. count down == round up
		return $sb(countDir).$sbIsTrue();
	}
};
