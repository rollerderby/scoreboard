$(function () {
  'use strict';
  WS.Connect(function () {
    if (_windowFunctions.getParam('oauth_verifier')) {
      WS.Set('ScoreBoard.Twitter.OauthVerifier', _windowFunctions.getParam('oauth_verifier'));
    } else if (_windowFunctions.hasParam('denied')) {
      $('a#error').text('Error!  Twitter did not provide an oauth_verifier when redirecting to this page.');
    }
    WS.Set('ScoreBoard.Twitter.AuthUrl', '');
    WS.Register('ScoreBoard.Twitter.AuthUrl', function (k, v) {
      if (v === '') {
        window.location.replace('/settings/twitter');
      }
    });
  });
});
