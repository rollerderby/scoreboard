const DEFAULT_CALLED_BY_POSITIONS = ["IPRF", "IPRR", "JR1", "JR2", "OPRF", "OPRM", "OPRR", "PLT1", "PLT2", "PBM"]

function pltToggleReassign(k, v, elem) {
  // toggles the ability to edit the officials assigned to the positions of the default positions
  elem.closest('.CalledBy').toggleClass('Reassign');
  elem.toggleClass('sbActive', elem.closest('.CalledBy').hasClass('Reassign'));
}

function _assignCaller(penaltyStatePath, callingPosition, callingOfficial) {
  WS.Set(penaltyStatePath + '.CallingPosition', callingPosition);
  WS.Set(penaltyStatePath + '.CallingOfficial', callingOfficial);
}

function _getPositionShortNameFromPositionID(officialPositionID) {
  // extracts the short name (such as IPRR) from the officialPositionID
  return officialPositionID ? officialPositionID.split('_').pop() : '';
}

function _getOfficialIDFromOfficialPositionShortName(game, positionShortName) {
  // looks up the ID of the Official currently assigned to the OfficialPosition
  return WS.state['ScoreBoard.Game(' + game + ').OfficialPosition(' + positionShortName + ').CurrentOfficial'] || null;
}

function _callingOfficialIsAssignedToCallingPosition (game, penaltyStatePath) {
  const callingPositionID = WS.state[penaltyStatePath + '.CallingPosition']
  const callingOfficialID = WS.state[penaltyStatePath + '.CallingOfficial']
  const callingPositionShortName = _getPositionShortNameFromPositionID(callingPositionID)
  const officialAssignedToPositionID = _getOfficialIDFromOfficialPositionShortName(game, callingPositionShortName) || null
  return Boolean(callingPositionID) && officialAssignedToPositionID == callingOfficialID
}

function _closeDialogOnCallerSelection(k, v, elem, event) {
  // Ignore clicks inside .OnEdit (propagated from position editing controls) and keep the current value
  if (event && $(event.target).closest('.OnEdit').length) {
    return;
  }

  // close the dialog on caller selection, if the penalty code has also been assigned
  const penaltyStatePath = _getPenaltyStatePath(elem);
  if (WS.state[penaltyStatePath + '.Code']) {
    sbCloseDialog(k, v, elem, event);
  }
}

function pltGetCallingPositionName(k) {
  return WS.state[k.upTo('Game') + '.OfficialPosition(' + _getPositionShortNameFromPositionID(WS.state[k.upTo('Penalty') + '.CallingPosition']) + ').Name']
}

function pltGetCallingOfficialName(k) {
  const callingOfficialID = WS.state[k.upTo('Penalty') + '.CallingOfficial']
  return WS.state[k.upTo('Game') + '.Nso(' + callingOfficialID + ').Name'] || WS.state[k.upTo('Game') + '.Ref(' + callingOfficialID + ').Name']
}

function pltToCalledByDisplay(k) {
  // generates the Position: Name label for the read-only "Called By" field
  const callingPositionName = pltGetCallingPositionName(k)
  const callingOfficialName = pltGetCallingOfficialName(k)

  if (callingPositionName && callingOfficialName) {
    return callingPositionName + ' : ' + callingOfficialName;
  }
  return callingPositionName || callingOfficialName || '';
}

//###################################################################
// Default OfficialPositions 
//###################################################################
function pltIsDefaultOfficialPositionActive (k, v, elem) {
  // Default section is active when:
  //  the CallingPosition is one of the default positions, and  
  //  the CallingOfficial matches the CallingPosition.CurrentOfficial
  const penaltyStatePath = _getPenaltyStatePath(elem);

  const divPositionShortName = elem.attr('OfficialPosition');
  const divPositionID = k.Game + '_' + divPositionShortName;
  const divPositionIsDefaultPosition = DEFAULT_CALLED_BY_POSITIONS.includes(divPositionShortName)
  
  const divIDMatchesCallingPositionID = divPositionID == WS.state[penaltyStatePath + '.CallingPosition']

  return divPositionIsDefaultPosition && divIDMatchesCallingPositionID && _callingOfficialIsAssignedToCallingPosition(k.Game, penaltyStatePath)
}

function pltToggleDefaultOfficialPosition(k, v, elem, event) {
  // Handles a click in a default position
  // When the div has class sbActive, it clears the assignment.
  // Otherwise, it sets the assignment.

  if (event && $(event.target).closest('.OnEdit').length) {
    // Ignore clicks inside .OnEdit (propagated from position editing controls)
    return;
  }

  const penaltyStatePath = _getPenaltyStatePath(elem);
  
  if (elem.hasClass('sbActive')) {
    _assignCaller(penaltyStatePath, null, null);
  } else {
    const positionShortName = elem.attr('OfficialPosition');
    const callingPositionID = k.Game + '_' + positionShortName;
    const callingOfficialID = _getOfficialIDFromOfficialPositionShortName(k.Game, positionShortName);
    _assignCaller(penaltyStatePath, callingPositionID, callingOfficialID);
  }

  _closeDialogOnCallerSelection(k, v, elem, event);
}

//###################################################################
// Other OfficialPositions 
//###################################################################
function isOtherOfficialPositionActive (k, v, elem) {
  // Other section is active when:
  //  the CallingPosition is not one of the default positions, and 
  //  the CallingOfficial matches the CallingPosition.CurrentOfficial
  const penaltyStatePath = _getPenaltyStatePath(elem);
  const callingPositionIsDefaultPosition = DEFAULT_CALLED_BY_POSITIONS.includes(_getPositionShortNameFromPositionID(  WS.state[penaltyStatePath + '.CallingPosition']))
    return !callingPositionIsDefaultPosition && _callingOfficialIsAssignedToCallingPosition(k.Game, penaltyStatePath)
}

function pltGetOtherOfficialPositionOptionText(k) {
  // generates the Position: Official labels for the 'Other' select control
  const positionPath = k.substring(0, k.lastIndexOf('.'));
  const positionName = WS.state[positionPath + '.Name'];
  const currentOfficialName = WS.state[positionPath + '.CurrentOfficialName'];
  if (positionName && currentOfficialName) {
    return positionName + ': ' + currentOfficialName;
  }
  return positionName || currentOfficialName || '';
}

function pltIsOtherOfficialPositionOptionSelectedInitialValue(k, v, elem) {
  // option is selected when:
  //  it's value matches the CallingPosition, and
  //  the CallingOfficial matches the CallingPosition.CurrentOfficial
  const penaltyStatePath = _getPenaltyStatePath(elem);
  return (elem.attr('value') == WS.state[penaltyStatePath + '.CallingPosition']) && _callingOfficialIsAssignedToCallingPosition(k.Game, penaltyStatePath)
}

function pltSelectOtherOfficialPosition(k, callingPositionID, elem, event) {
  // handles a selection change in the Other input
  // an empty selection clears the assignment
  // a non-empty selection sets the assignment
  const penaltyStatePath = _getPenaltyStatePath(elem);

  if (!callingPositionID) {
    _assignCaller(penaltyStatePath, null, null)
  } else {
    const positionShortName = _getPositionShortNameFromPositionID(callingPositionID) 
    const callingOfficialID = _getOfficialIDFromOfficialPositionShortName(k.Game, positionShortName)
    _assignCaller(penaltyStatePath, callingPositionID, callingOfficialID)
  }

  _closeDialogOnCallerSelection(k, callingPositionID, elem, event);
}

function pltClearOtherOfficialPosition(k, v, elem, event) {
  // Handles a click in the Other section
  //  When the div has class sbActive, it clears the assignment.
  //  Otherwise, the click is ignored

  // Ignore clicks on the select controls inside this div
  if (event && $(event.target).closest('select').length) {
    return;
  }

  if (elem.hasClass('sbActive')) {
    _assignCaller(_getPenaltyStatePath(elem), null, null);
    _closeDialogOnCallerSelection(k, v, elem, event);
  }
}

//###################################################################
// Temporary OfficialPosition + Official Assignment
//###################################################################
function pltTemporaryOfficialPositionValue (k, v, elem) {
  return pltGetCallingPositionName(k)
}

function pltTemporaryOfficialValue (k) {
  return pltGetCallingOfficialName(k)
}

function pltIsTemporaryAssignmentActive (k, v, elem) {
  // Temporary Assignment section is active when:
  //  a CallingPosition or CallingOfficial is assigned, and
  //  the CallingOfficial does not match CallingPosition.CurrentOfficial
  const penaltyStatePath = _getPenaltyStatePath(elem);
  const callingPositionID = WS.state[penaltyStatePath + '.CallingPosition'] || ""
  const callingOfficialID = WS.state[penaltyStatePath + '.CallingOfficial'] || ""
  return (callingPositionID || callingOfficialID) ? !_callingOfficialIsAssignedToCallingPosition(k.Game, penaltyStatePath) : false
}

function pltToggleTemporaryAssignment(k, v, elem, event) {
  // Handles a click in the Temporary Assignment section.
  // When the div has class sbActive, it clears the assignment.
  // Otherwise, it assigns the OfficialPosition + Official currently selected in the drop downs

  // Ignore clicks on the select controls inside this div
  if (event && $(event.target).closest('select').length) {
    return;
  }
  
  const penaltyStatePath = _getPenaltyStatePath(elem);
  
  if (elem.hasClass('sbActive')) {
    _assignCaller(penaltyStatePath, null, null);
  } else {
    const selects = elem.find('select')
  
    const selectedCallingPositionID = selects.eq(0).val();
    const selectedCallingOfficialID = selects.eq(1).val();

    _assignCaller(penaltyStatePath, selectedCallingPositionID, selectedCallingOfficialID);
  }

  _closeDialogOnCallerSelection(k, v, elem, event);
}