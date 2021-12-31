$(function () {
  'use strict';
  var formatSpecifiers = {};

  $('button.TestMode')
    .on('click', function () {
      WS.Set('ScoreBoard.Twitter.TestMode', $(this).attr('checked') == null);
    })
    .button();

  $('button.Login')
    .on('click', function () {
      WS.Set('ScoreBoard.Twitter.CallbackUrl', window.location.protocol + '//' + window.location.host + '/controls/twitter_auth.html');
      WS.Set('ScoreBoard.Twitter.Login', true);
    })
    .button();
  WS.Register(['ScoreBoard.Twitter.AuthUrl'], function (k, v) {
    if (v) {
      window.location.assign(v);
    }
  });

  $('button.Logout')
    .on('click', function () {
      WS.Set('ScoreBoard.Twitter.Logout', true);
    })
    .button();

  $('p.DirectTweet button.Tweet')
    .on('click', function () {
      WS.Set('ScoreBoard.Twitter.ManualTweet', $('p.DirectTweet input:text.Tweet').val());
      $('p.DirectTweet input:text.Tweet').val('').focus();
    })
    .button();
  $('p.DirectTweet input:text.Tweet').on('keydown', function (event) {
    if (event.which === 13) {
      // Pressed Enter
      $('p.DirectTweet button.Tweet').trigger('click');
    }
  });

  WS.Register(['ScoreBoard.Twitter.LoggedIn', 'ScoreBoard.Twitter.TestMode'], function (k, v) {
    var loggedIn = isTrue(WS.state['ScoreBoard.Twitter.LoggedIn']);
    var testMode = isTrue(WS.state['ScoreBoard.Twitter.TestMode']);
    var directTweetEnabled = loggedIn || testMode;
    $('button.Login').button('option', 'disabled', loggedIn);
    $('button.Logout').button('option', 'disabled', !loggedIn);
    $('p.DirectTweet button.Tweet').button('option', 'disabled', !directTweetEnabled);
    $('p.DirectTweet input:text.Tweet').prop('disabled', !directTweetEnabled);
    $('p.LogInStatus').toggleClass('LoggedIn', loggedIn);
    $('p.TestMode').toggleClass('Show', testMode);
    $('button.TestMode').button('option', 'label', testMode ? 'Stop Test Mode' : 'Start Test Mode');
    $('button.TestMode').attr('checked', testMode);
  });
  WS.Register(['ScoreBoard.Twitter.Error'], function (k, v) {
    $('a.Error').text(v);
    $('p.Error').toggleClass('Show', v !== '');
  });
  WS.Register(['ScoreBoard.Twitter.Status'], function (k, v) {
    if (v !== '') {
      $('#UserTweets>tbody>tr.Template').clone(true).removeClass('Template').prependTo('#UserTweets>tbody').find('a.Tweet').html(v);
    }
  });

  $('button.EditConditionalTweets')
    .on('click', function () {
      var checked = $(this).attr('checked') == null;
      $('#ConditionalTweetConfiguration').toggleClass('show', checked);
      $(this).button('option', 'label', checked ? 'Hide Conditional Tweets' : 'Edit Conditional Tweets');
      $(this).attr('checked', checked);
    })
    .button();

  $('p.AddConditionalTweet button.Add')
    .on('click', function () {
      var p = $(this).closest('p');
      var conditionInput = p.find('input:text.Condition');
      var tweetInput = p.find('input:text.Tweet');
      var idInput = p.find('input:text.UpdateId');
      var condition = conditionInput.val();
      var tweet = tweetInput.val();
      var id = idInput.val() || newUUID();
      WS.Set('ScoreBoard.Twitter.ConditionalTweet(' + id + ').Condition', condition);
      WS.Set('ScoreBoard.Twitter.ConditionalTweet(' + id + ').Tweet', tweet);
      idInput.val('');
      tweetInput.val('').trigger('input');
      conditionInput.val('').trigger('input').focus();
      p.removeClass('Update');
    })
    .button();
  $('p.AddConditionalTweet button.Cancel')
    .on('click', function () {
      var p = $(this).closest('p');
      p.find('input:text.UpdateId').val('');
      p.find('input:text.Tweet').val('');
      p.find('input:text.Condition').val('').focus();
      p.find('input:text').trigger('input');
      p.removeClass('Update');
    })
    .button();
  $('p.AddConditionalTweet input:text.Tweet').on('keydown', function (event) {
    if (event.which === 13) {
      // Pressed Enter
      $('p.AddConditionalTweet button.Add').trigger('click');
    }
  });
  WS.Register(['ScoreBoard.Twitter.ConditionalTweet(*).Condition', 'ScoreBoard.Twitter.ConditionalTweet(*).Tweet'], function (k, v) {
    var id = k.ConditionalTweet;
    if (v == null) {
      $('#ConditionalTweets>tbody>tr[conditionId="' + id + '"]').remove();
      return;
    }
    var prefix = 'ScoreBoard.Twitter.ConditionalTweet(' + id + ')';

    var tr = $('#ConditionalTweets>tbody>tr[conditionId="' + id + '"]');
    if (tr.length === 0) {
      tr = $('#ConditionalTweets>tbody>tr.ConditionalTweet.Template')
        .clone(true)
        .removeClass('Template')
        .attr('conditionId', id)
        .appendTo('#ConditionalTweets>tbody');
      tr.find('button.Remove').on('click', function () {
        WS.Set(prefix, null);
      });
      tr.find('button.Edit').on('click', function () {
        $('p.AddConditionalTweet').addClass('Update');
        $('p.AddConditionalTweet input:text.UpdateId').val(id);
        $('p.AddConditionalTweet input:text.Condition').val(WS.state[prefix + '.Condition']);
        $('p.AddConditionalTweet input:text.Tweet').val(WS.state[prefix + '.Tweet']);
        $('p.AddConditionalTweet input:text').trigger('input');
      });
    }
    switch (k.field) {
      case 'Condition':
        tr.find('a.Condition').text(v);
        break;
      case 'Tweet':
        tr.find('a.Tweet').text(v);
        break;
    }
  });

  $('#HelpText').insertBefore('#ConditionalTweets');
  $('#ShowHelp')
    .on('change', function () {
      $('#HelpText').toggle(this.checked);
      $(this).button('option', 'label', this.checked ? 'Hide Help' : 'Show Help');
    })
    .after('<label for="ShowHelp" />')
    .button()
    .trigger('change');

  WS.Register(['ScoreBoard.Twitter.FormatSpecifier'], function (k, v) {
    var ul = $('ul.FormatSpecifierDescriptions');
    var li = ul.children('li#' + k.FormatSpecifier);
    if (li.length === 0) {
      li = $('<li>')
        .attr('id', k.FormatSpecifier)
        .append($('<span class="Key">'))
        .append($('<span>').text(' : '))
        .append($('<span class="Description">'))
        .append($('<span>').text(' : '))
        .append($('<span class="CurrentValue">'));
      _windowFunctions.appendAlphaNumSortedByAttr(ul, li, 'id');
    }
    li.children('.' + k.field).text(v);
    if (k.field === 'Key') {
      formatSpecifiers[v] = k.FormatSpecifier;
    } else if (k.field === 'CurrentValue') {
      $('p.AddConditionalTweet input:text').trigger('input');
    }
  });

  var replaceSpecifiers = function (s) {
    for (var k in formatSpecifiers) {
      s = s.replace(k, WS.state['ScoreBoard.Twitter.FormatSpecifier(' + formatSpecifiers[k] + ').CurrentValue']);
    }
    return s;
  };

  $.each(['Condition', 'Tweet'], function (i, e) {
    var div = $('#ConditionalTweetConfiguration');
    var input = div.find('input:text.' + e);
    input.on('input', function () {
      div.find('a.Preview.' + e).text(replaceSpecifiers(input.val()));
    });
  });

  $('#ConditionalTweetConfiguration')
    .find('input:text.Condition,input:text.Tweet')
    .autocomplete({
      source: function (request, response) {
        var key = request.term.split(' ').pop();
        response(key ? $.ui.autocomplete.filter(Object.keys(formatSpecifiers), key) : '');
      },
      focus: function () {
        return false;
      },
      select: function (event, ui) {
        var words = this.value.split(' ');
        words.pop();
        words.push(ui.item.value);
        this.value = words.join(' ');
        $(this).trigger('input');
        return false;
      },
    });

  WS.AutoRegister();
  WS.Connect();
});
