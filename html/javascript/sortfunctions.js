'use strict';

/*
 * Generally the functions in this file are intended to be used with _windowfunctions.appendSorted
 * In this context they will be given two DOM elements a,b as input and should return true,
 * iff b is supposed to show up before a.
 */

function _sbCompareAttrThenSubId(attr, a, b) {
  if ($(a).attr(attr) == null) {
    return true;
  } else if ($(a).attr(attr) === $(b).attr(attr)) {
    return _windowFunctions.numCompareByAttr('sbSubId', a, b);
  } else {
    return _windowFunctions.alphaCompareByAttr(attr, a, b);
  }
}

function _sbnumCompareAttrThenSubId(attr, a, b) {
  if ($(a).attr(attr) == null) {
    return true;
  } else if ($(a).attr(attr) === $(b).attr(attr)) {
    return _windowFunctions.numCompareByAttr('sbSubId', a, b);
  } else {
    return _windowFunctions.numCompareByAttr(attr, a, b);
  }
}

function sbComparePeriod(a, b) {
  const comp = _sbnumCompareAttrThenSubId('Period', a, b);
  return $(a).closest('[sbSheetStyle]').attr('sbSheetStyle') === 'sheet' ? comp : !comp;
}

function sbCompareJam(a, b) {
  const comp = _sbnumCompareAttrThenSubId('Jam', a, b);
  return $(a).closest('[sbSheetStyle]').attr('sbSheetStyle') === 'sheet' ? comp : !comp;
}

function sbOrderRsAsTree(a, b) {
  const rsA = $(a).attr('value');
  const rsB = $(b).attr('value');
  const setA = _sbRsGetParents(rsA);
  const setB = _sbRsGetParents(rsB);

  for (let i = 0; i < Math.min(setA.length, setB.length); i++) {
    if (setA[i] !== setB[i]) {
      return WS.state['ScoreBoard.Rulesets.Ruleset(' + setA[i] + ').Name'] > WS.state['ScoreBoard.Rulesets.Ruleset(' + setB[i] + ').Name'];
    }
  }
  return setA.length > setB.length;
}
