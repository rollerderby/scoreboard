$(function () {
  $.get('/urls', function (data) {
    var urls = data.trim();
    if (!urls) {
      $('#URLtext').html('<b>The scoreboard is not networked</b>.	 You can access it only from this computer.');
    } else {
      $.each(urls.split('\n'), function (i, e) {
        $('<li>')
          .append(
            $('<a>')
              .addClass('qrButton')
              .on('click', function () {
                $('#qrcode').empty().qrcode(e);
                $('.qrButton').removeClass('sbActive');
                $(this).addClass('sbActive');
              })
              .text('QR')
              .button()
          )
          .append($('<a>').attr('href', e).text(e))
          .appendTo('#URLs');
      });
      $('a.qrButton').first().trigger('click');
    }
  });
  WS.Register(['WS.Device.Id', 'WS.Device.Name', 'ScoreBoard.Clients.Device(*).Comment'], function (k, v) {
    var name = WS.state['WS.Device.Name'];
    var comment = WS.state['ScoreBoard.Clients.Device(' + WS.state['WS.Device.Id'] + ').Comment'];
    if (comment) {
      name += ' (' + comment + ')';
    }
    $('#deviceName').text(name);
  });
  WS.Register('ScoreBoard.CurrentGame.Game', function (k, v) {
    var gId = v || '';
    $('.curGame').each(function () {
      var old = $(this).attr('href');
      $(this).attr('href', old.substring(0, old.lastIndexOf('=') + 1) + v);
    });
  });
});
