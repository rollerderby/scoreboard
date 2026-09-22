function advanceGame() {
  window.location.replace(_urlWithParam('game', WS.state['ScoreBoard.CurrentGame.Game']));
}
