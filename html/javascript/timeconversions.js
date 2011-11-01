

//FIXME - need to set policy based on direction of clock...
_timeConversions = {
  /* Policy: round ms up or down
   * If true, ms values will be rounded up, i.e. 1001-2000 ms converts to 2 sec.
   * If false, ms values will be rounded down, i.e. 1000-1999 ms converts to 1 sec.
   */
  roundUp: true,

  /*
   * These are conversion functions for external use
   */

  /* Convert ms to seconds */
  msToSeconds: function(ms) {
    return String(_timeConversions.roundUp ? Math.ceil(ms / 1000) : Math.floor(ms / 1000));
  },
  /* Convert seconds to milliseconds */
  secondsToMs: function(sec) { return String(sec * 1000); },
  /* Convert milliseconds to min:sec */
  msToMinSec: function(ms) { return _timeConversions._msOnlyMin(ms)+":"+_timeConversions._msOnlySec(ms); },
  /* Convert min:sec to milliseconds */
  minSecToMs: function(time) {
    var min = Number(_timeConversions._timeOnlyMin(time) || 0);
    var sec = Number(_timeConversions._timeOnlySec(time) || 0);
    return _timeConversions.secondsToMs((min * 60) + sec);
  },
  /* Convert milliseconds to min:sec, if min is 0 return only seconds portion */
  msToMinSecNoZero: function(ms) {
    var limit = (_timeConversions.roundUp ? 59001 : 60000);
    return ((ms >= limit) ? _timeConversions.msToMinSec(ms) : _timeConversions.msToSeconds(ms));
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
  _msOnlyMin: function(ms) {
    var min = Math.floor(ms / 60000);
    min = min + Number(_timeConversions.msToSeconds(Math.max(0, (ms % 60000) - 59000)));
    return String(min);
  },
  /* Accepts min:sec time, returns only seconds portion */
  _timeOnlySec: function(time) { return _timeConversions._formatMinSec(time).split(":")[1]; },
  /* Accepts min:sec time, returns only minutes portion */
  _timeOnlyMin: function(time) { return _timeConversions._formatMinSec(time).split(":")[0]; }
};
