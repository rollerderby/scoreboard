$(function () {
  $('#IntermissionControlDialog')
    .parent()
    .dialog({
      title: 'Intermission Display Labels',
      autoOpen: false,
      width: 700,
      modal: true,
      buttons: {
        Close: function () {
          $(this).dialog('close');
        },
      },
    });
});

$('.View')
  .clone()
  .removeClass('View')
  .addClass('Preview')
  .attr('sbPrefix', '$:ScoreBoard.Settings.Setting(ScoreBoard.Preview:)')
  .insertBefore('div.ViewFrameLabels');

function stgOpenIntermissionDialog() {
  $('#IntermissionControlDialog').parent().dialog('open');
}

function stgSetUsePreview(k, v) {
  $('#ScoreBoardSettings').toggleClass('UsePreview', v === 'preview');
}

function stgAplyPreview() {
  var done = {};
  $('#ScoreBoardSettings .Preview [ApplyPreview]').each(function (_, e) {
    var name = $(e).attr('ApplyPreview');
    if (done[name]) {
      return;
    }
    WS.Set(
      'ScoreBoard.Settings.Setting(ScoreBoard.View_' + name + ')',
      WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Preview_' + name + ')']
    );
    done[name] = true;
  });
}

function stgShowPreview(k, v, elem) {
  $('<iframe>Your browser does not support iframes.</iframe>')
    .attr({ scrolling: 'no', frameborder: '0', src: elem.attr('src') })
    .replaceAll(elem);
}
