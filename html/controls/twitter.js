
$sb(function() {
  var sbTwitter = $sb("Viewers.Twitter");
  $("button.Login").click(function() { sbTwitter.$sb("Start").$sbSet(window.location.href); }).button();
  $("button.Logout").click(function() { sbTwitter.$sb("Stop").$sbSet("true"); }).button();
  $("button.Tweet").click(function() {
    sbTwitter.$sb("Tweet").$sbSet($("input:text.Tweet").val());
    $("input:text.Tweet").val("").focus();
  }).button();
  $("input:text.Tweet").keydown(function(event) {
    if (event.which == 13) // Pressed Enter
      $("button.Tweet").click();
  });
  sbTwitter.$sb("ScreenName").$sbElement("span.ScreenName");
  sbTwitter.$sb("Authorized").$sbBindAndRun("content", function(event, value) {
    $("button.Login").button("option", "disabled", isTrue(value));
    $("button.Logout,button.Tweet").button("option", "disabled", !isTrue(value));
    $("input:text.Tweet").prop("disabled", !isTrue(value));
    $("p.LogInStatus").toggleClass("LoggedIn", isTrue(value));
  });
  sbTwitter.$sb("Error").$sbElement("a.Error");
  sbTwitter.$sb("Error").$sbBindAndRun("content", function(event, value) {
    $("p.Error").toggleClass("Show", !!value);
  });
  sbTwitter.$sb("Status").$sbBindAndRun("content", function(event, value) {
    if (value)
      $("<a>").html(value).append("<br>").prependTo("p.StatusUpdates");
  });

  if (_windowFunctions.hasParam("denied")) {
    sbTwitter.$sb("Denied").$sbSet("true");
    window.location.replace("http://"+window.location.host+window.location.pathname);
  }
  var oauth_verifier = _windowFunctions.getParam("oauth_verifier");
  if (oauth_verifier) {
    sbTwitter.$sb("SetOAuthVerifier").$sbSet(oauth_verifier);
    window.location.replace("http://"+window.location.host+window.location.pathname);
  } else {
    sbTwitter.$sb("AuthorizationURL").$sbBindAndRun("content", function(event, value) {
      if (value) {
        $sb(this).$sbSet("");
        window.location.assign(value);
      }
    });
  }
});
