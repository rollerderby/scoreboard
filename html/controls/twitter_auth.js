
$sb(function() {
  var sbTwitter = $sb("Viewers.Twitter");

  if (_windowFunctions.getParam("oauth_verifier"))
    sbTwitter.$sb("SetOAuthVerifier").$sbSet(_windowFunctions.getParam("oauth_verifier"));
  else if (_windowFunctions.hasParam("denied"))
    sbTwitter.$sb("Denied").$sbSet("true");
  window.location.replace(window.location.protocol+"//"+window.location.host+"/controls/twitter.html");
});
