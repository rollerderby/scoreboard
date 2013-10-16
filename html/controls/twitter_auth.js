
$sb(function() {
	var sbTwitter = $sb("Viewers.Twitter");

	if (_windowFunctions.getParam("oauth_verifier"))
		sbTwitter.$sb("SetOAuthVerifier").$sbSet(_windowFunctions.getParam("oauth_verifier"));
	else if (_windowFunctions.hasParam("denied"))
		sbTwitter.$sb("Denied").$sbSet("true");
	else {
		$("a#error").text("Error!	 Twitter did not provide an oauth_verifier when redirecting to this page.");
		sbTwitter.$sb("AuthURL").$sbSet("");
	}

	sbTwitter.$sb("AuthURL").$sbBindAndRun("sbchange", function(event, value) {
		if (value == "")
			window.location.replace(window.location.protocol+"//"+window.location.host+"/controls/twitter.html");
	});
});
