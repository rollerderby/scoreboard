
$sb(function() {
  var sbTwitter = $sb("Viewers.Twitter");
  var auth_callback_url = window.location.protocol+"//"+window.location.host+"/controls/twitter_auth.html";
  sbTwitter.$sb("TestMode").$sbControl($("input:checkbox.TestMode")).button();
  $("button.Login").click(function() { sbTwitter.$sb("Start").$sbSet(auth_callback_url); }).button();
  $("button.Logout").click(function() { sbTwitter.$sb("Stop").$sbSet("true"); }).button();
  $("p.DirectTweet button.Tweet").click(function() {
    sbTwitter.$sb("Tweet").$sbSet($("p.DirectTweet input:text.Tweet").val());
    $("p.DirectTweet input:text.Tweet").val("").focus();
  }).button();
  $("p.DirectTweet input:text.Tweet").keydown(function(event) {
    if (event.which == 13) // Pressed Enter
      $("p.DirectTweet button.Tweet").click();
  });
  sbTwitter.$sb("ScreenName").$sbElement("p.LogInStatus span.ScreenName");
  sbTwitter.$sb("Authorized").$sbBindAndRun("content", function(event, value) {
    var directTweetEnabled = sbTwitter.$sb("TestMode").$sbIsTrue() || isTrue(value);
    $("button.Login").button("option", "disabled", isTrue(value));
    $("button.Logout").button("option", "disabled", !isTrue(value));
    $("p.DirectTweet button.Tweet").button("option", "disabled", !directTweetEnabled);
    $("p.DirectTweet input:text.Tweet").prop("disabled", !directTweetEnabled);
    $("p.LogInStatus").toggleClass("LoggedIn", isTrue(value));
  });
  sbTwitter.$sb("TestMode").$sbBindAndRun("content", function(event, value) {
    var directTweetEnabled = sbTwitter.$sb("Authorized").$sbIsTrue() || isTrue(value);
    $("p.DirectTweet button.Tweet").button("option", "disabled", !directTweetEnabled);
    $("p.DirectTweet input:text.Tweet").prop("disabled", !directTweetEnabled);
    $("input:checkbox.TestMode").button("option", "label", (isTrue(value)?"Stop Test Mode":"Start Test Mode"));
    $("p.TestMode").toggleClass("Show", isTrue(value));
  });
  sbTwitter.$sb("Error").$sbElement("p.Error a.Error");
  sbTwitter.$sb("Error").$sbBindAndRun("content", function(event, value) {
    $("p.Error").toggleClass("Show", !!value);
  });
  sbTwitter.$sb("Status").$sbOnAndRun("content", function(event, value) {
    if (value) {
      $("#UserTweets>tbody>tr.Template").clone(true).removeClass("Template")
        .prependTo("#UserTweets>tbody")
        .find("a.Tweet").html(value);
    }
  });

  $("p.AddConditionalTweet button.Add").click(function() {
    var p = $(this).closest("p");
    var conditionInput = p.find("input:text.Condition");
    var tweetInput = p.find("input:text.Tweet");
    var idInput = p.find("input:text.UpdateId");
    var condition = conditionInput.val();
    var tweet = tweetInput.val();
    var id = idInput.val();
    var sbE = $sb("Viewers.Twitter.ConditionalTweet"+(id?"("+id+")":""));
    var updateE = _crgScoreBoard.toNewElement(sbE);
    _crgScoreBoard.createScoreBoardElement(updateE, "Condition", null, condition);
    _crgScoreBoard.createScoreBoardElement(updateE, "Tweet", null, tweet);
    _crgScoreBoard.updateServer(updateE);
    idInput.val("");
    tweetInput.val("");
    conditionInput.val("").focus();
    p.removeClass("Update");
  }).button();
  $("p.AddConditionalTweet button.Cancel").click(function() {
    var p = $(this).closest("p");
    p.find("input:text.UpdateId").val("");
    p.find("input:text.Tweet").val("");
    p.find("input:text.Condition").val("").focus();
    p.removeClass("Update");    
  }).button();
  $("p.AddConditionalTweet input:text.Tweet").keydown(function(event) {
    if (event.which == 13) // Pressed Enter
      $("p.AddConditionalTweet button.Add").click();
  });
  sbTwitter.$sbBindAddRemoveEach("ConditionalTweet", function(event, node) {
    var tr = $("#ConditionalTweets>tbody>tr.ConditionalTweet.Template").clone(true)
      .removeClass("Template").attr("data-UUID", node.$sbId)
      .appendTo("#ConditionalTweets>tbody");
    node.$sb("Condition").$sbElement(tr.find("a.Condition"));
    node.$sb("Tweet").$sbElement(tr.find("a.Tweet"));
    tr.find("button.Remove").click(function() { node.$sbRemove(); });
    tr.find("button.Edit").click(function() {
      $("p.AddConditionalTweet").addClass("Update");
      $("p.AddConditionalTweet input:text.UpdateId").val(node.$sbId);
      $("p.AddConditionalTweet input:text.Condition").val(node.$sb("Condition").$sbGet());
      $("p.AddConditionalTweet input:text.Tweet").val(node.$sb("Tweet").$sbGet());
    });
  }, function(event, node) {
    $("#ConditionalTweets tr.ConditionalTweet[data-UUID='"+node.$sbId+"']").remove();
  });
  $("#ConditionalTweetConfigurationDialog").dialog({
    title: "Conditional Tweet Configuration",
    modal: true,
    width: 750,
    height: 550,
    autoOpen: false,
    buttons: { Close: function() { $(this).dialog("close"); } }
  });
  $("button.EditConditionalTweets").click(function() {
    $("#ConditionalTweetConfigurationDialog").dialog("open");
  }).button();
  $("#HelpText").insertBefore("#ConditionalTweets");
  $("#ShowHelp").change(function() {
    $("#HelpText").toggle(this.checked);
    $(this).button("option", "label", (this.checked?"Hide Help":"Show Help"));
  }).after("<label for=ShowHelp />").button().change();

  sbTwitter.$sb("AuthorizationURL").$sbBindAndRun("content", function(event, value) {
    if (value) {
      $sb(this).$sbSet("");
      window.location.assign(value);
    }
  });

  $.get("/FormatSpecifiers/descriptions", function(data) {
    $.each( data.trim().split("\n"), function(i,e) {
      $("<li>").text(e).appendTo("ul.FormatSpecifierDescriptions");
    });
  });

  parseInputFieldFormatSpecifiers();
});

function parseInputFieldFormatSpecifiers() {
  var dialog = $("#ConditionalTweetConfigurationDialog");
  if (dialog.dialog("isOpen")) {
    $.each( [ "Condition", "Tweet" ], function(i,e) {
      var input = dialog.find("input:text."+e);
      if (input.val()) {
        $.post("/FormatSpecifiers/parse", {
          format: input.val()
        }, function(data) {
          dialog.find("a.Preview."+e).text(data);
        });
      } else {
        dialog.find("a.Preview."+e).text("");
      }
    });
  }
  setTimeout(parseInputFieldFormatSpecifiers, 1000);
}

