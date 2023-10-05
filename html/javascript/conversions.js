function compareRosterNumber(a, b) {
  return _windowFunctions.alphaCompareByAttr('rosterNumber', a, b);
}

function toCssUrl(k, v) {
  if (v) {
    return 'url("' + v + '")';
  } else {
    return '';
  }
}

function empty(k, v) {
  return !v;
}

function notEmpty(k, v) {
  return !!v;
}

function toNbspIfEmpty(k, v) {
  return v === '' ? '\xa0' : v;
}

function toTime(k, v) {
  'use strict';
  var isCountDown = isTrue(WS.state['ScoreBoard.CurrentGame.Clock(' + k.Clock + ').Direction']);
  return _timeConversions.msToMinSecNoZero(v, isCountDown);
}
