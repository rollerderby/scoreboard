'use strict';

function jtMoveOto(k, v, elem) {
  if (k.Team === '2') {
    elem.siblings('.OTO').insertBefore(elem);
  }
}

function jtToggleSetting(k, v, elem) {
  elem.toggleClass('sbActive');
}

function jtPossiblInjCont(k) {
  return !isTrue(k.upTo('Game') + '.Rule(Jam.InjuryContinuation)') || !isTrue(k.upTo('Game') + '.Team(1).Injury');
}

function jtUndo(k) {
  if ($('#ReplaceButton_Setting').hasClass('sbActive')) {
    WS.Set(k + '.ClockReplace', true);
  } else {
    WS.Set(k + '.ClockUndo', true);
  }
}
