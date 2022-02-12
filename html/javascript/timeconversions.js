var _timeConversions = {
  /*
   * These are conversion functions for external use
   */

  /* Convert ms to seconds */
  msToSeconds: function (ms, roundUp) {
    'use strict';
    if (roundUp) {
      return String(Math.ceil(ms / 1000));
    } else {
      return String(Math.floor(ms / 1000));
    }
  },
  /* Convert seconds to milliseconds */
  secondsToMs: function (sec) {
    'use strict';
    return String(sec * 1000);
  },
  /* Convert milliseconds to min:sec */
  msToMinSec: function (ms, roundUp) {
    'use strict';
    return _timeConversions._msOnlyMin(ms, roundUp) + ':' + _timeConversions._msOnlySec(ms, roundUp);
  },
  /* Convert min:sec to milliseconds */
  minSecToMs: function (time) {
    'use strict';
    var min = Number(_timeConversions._timeOnlyMin(time) || 0);
    var sec = Number(_timeConversions._timeOnlySec(time) || 0);
    return _timeConversions.secondsToMs(min * 60 + sec);
  },
  /* Convert milliseconds to min:sec, if min is 0 return only seconds portion */
  msToMinSecNoZero: function (ms, roundUp) {
    'use strict';
    var limit = roundUp ? 59001 : 60000;
    if (ms >= limit) {
      return _timeConversions.msToMinSec(ms, roundUp);
    } else {
      return _timeConversions.msToSeconds(ms, roundUp);
    }
  },
  /* Same as minSecToMs() */
  minSecNoZeroToMs: function (time) {
    'use strict';
    return _timeConversions.minSecToMs(time);
  },

  /*
   * These are utility functions that can be used externally
   */

  /* Format number into minimum 2 digits, prepending 0 if needed */
  twoDigit: function (n) {
    'use strict';
    return (10 > n ? '0' : '') + n;
  },

  /*
   * These are generally internal functions that should not be used externally
   */

  /* If needed, this preprends ':' */
  _formatMinSec: function (time) {
    'use strict';
    return String(time).indexOf(':') > -1 ? time : ':' + time;
  },
  /* Convert ms to seconds portion of min:sec */
  _msOnlySec: function (ms, roundUp) {
    'use strict';
    return _timeConversions.twoDigit(_timeConversions.msToSeconds(ms, roundUp) % 60);
  },
  /* Convert ms to minutes portion of min:sec */
  _msOnlyMin: function (ms, roundUp) {
    'use strict';
    var min = Math.floor(ms / 60000);
    // if rounding up and seconds are 59001-59999, we need to add a minute
    min = min + Number(_timeConversions.msToSeconds(Math.max(0, (ms % 60000) - 59000), roundUp));
    return String(min);
  },
  /* Accepts min:sec time, returns only seconds portion */
  _timeOnlySec: function (time) {
    'use strict';
    return _timeConversions._formatMinSec(time).split(':')[1];
  },
  /* Accepts min:sec time, returns only minutes portion */
  _timeOnlyMin: function (time) {
    'use strict';
    return _timeConversions._formatMinSec(time).split(':')[0];
  },
};
