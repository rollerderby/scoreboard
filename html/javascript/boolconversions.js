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

function sbIsToThisTeam(k, v) {
  const prefix = k.upTo('Game');
  return v === WS.state[prefix + '.TimeoutOwner'] && !isTrue(WS.state[prefix + '.OfficialReview']);
}

function sbIsOrThisTeam(k, v) {
  const prefix = k.upTo('Game');
  return v === WS.state[prefix + '.TimeoutOwner'] && isTrue(WS.state[prefix + '.OfficialReview']);
}

function sbIsOnTrackRole(k, v) {
  return v === 'Jammer' || v === 'Pivot' || v === 'Blocker';
}

function sbLineupTooLong(k) {
  const inLineup = isTrue(WS.state[k.upTo('Game') + '.Clock(Lineup).Running']);
  const overtime = isTrue(WS.state[k.upTo('Game') + '.InOvertime']);
  const curTime = WS.state[k.upTo('Game') + '.Clock(Lineup).Time'];
  const maxTime = sbFromTime(
    overtime ? WS.state[k.upTo('Game') + '.Rule(Lineup.OvertimeDuration)'] : WS.state[k.upTo('Game') + '.Rule(Lineup.Duration)']
  );

  return inLineup && curTime > maxTime;
}

function sbJamTooLong(k) {
  return isTrue(WS.state[k.upTo('Game') + 'InJam']) && !isTrue(WS.state[k.upTo('Game') + 'Clock(Jam).Running']);
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
