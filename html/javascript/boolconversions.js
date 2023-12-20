'use strict';

/*
 * To bool
 */
function isTrue(value) {
  if (typeof value === 'boolean') {
    return value;
  } else {
    return String(value).toLowerCase() === 'true';
  }
}

function sbIsEmpty(k, v) {
  return !v;
}

function sbIsNotEmpty(k, v) {
  return !!v;
}

function sbIsSetAndFalse(k, v) {
  return v != null && !isTrue(v);
}

function sbFromX(k, v, elem) {
  return elem.text() === '';
}

function sbNoGame(k) {
  return !k.Game;
}

function sbIsGame(k) {
  return k.Game != null;
}

function sbIsCurrentPeriod(k, v) {
  return k.field === 'Number' && v === WS.state[k.upTo('Game') + '.CurrentPeriodNumber'];
}

function sbIsUpcomingJam(k) {
  return k.Period == null;
}

function sbIsOnTrackRole(k, v) {
  return v === 'Jammer' || v === 'Pivot' || v === 'Blocker';
}

/*
 * From bool
 */
function sbToX(k, v) {
  return isTrue(v) ? 'X' : '';
}

function sbToSP(k, v) {
  return isTrue(v) ? 'SP' : '';
}
function sbToSpJamNo(k, v) {
  return isTrue(v) ? 'SP' : 'SP*';
}
