function createScoreBoardSettingsTab(tab) {
  var table = $('<table>').attr('id', 'ScoreBoardSettings').appendTo(tab);

  var usePreviewButton = $('<label for="UsePreviewButton"/><input type="checkbox" id="UsePreviewButton"/>');
  usePreviewButton.last().button();
  _crgUtils.bindAndRun(usePreviewButton.filter('input:checkbox'), 'change', function() {
    $(this).button('option', 'label', (isTrue(this.checked)?'Editing Live ScoreBoard':'Editing Preview'));
    table.toggleClass('UsePreview', !isTrue(this.checked));
  });
  var applyPreviewButton = $('<button>Apply Preview</button>').button()
    .on('click', function() {
      var done = {};
      table.find('.Preview [ApplyPreview]').each(function(_, e) {
        var name = $(e).attr('ApplyPreview');
        if (done[name]) { return; }
        WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.View_' + name + ')',
            WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Preview_' + name + ')']);
        done[name] = true;
      });
    });

  $('<tr><td/></tr>').appendTo(table)
    .find('td').addClass('Header NoChildren PreviewControl')
    .append(_crgUtils.createRowTable(3))
    .find('td:first')
    .next().append(usePreviewButton)
    .next().append(applyPreviewButton);

  $.each( [ 'View', 'Preview' ], function(i,p) {
    createScoreBoardViewPreviewRows(table, p);
  });

  $('<tr><td/></tr>').appendTo(table)
    .find('td').addClass('ViewFrames Header')
    .append(_crgUtils.createRowTable(2))
    .find('td')
    .first().append('<a>Current</a>')
    .next().append('<a>Preview</a>');

  var previewButton = $('<button>').html('Show Preview').on('click', function() {
    $('<iframe>Your browser does not support iframes.</iframe>')
    .attr({ scrolling: 'no', frameborder: '0', src: $(this).attr('src') })
    .replaceAll(this);
  });

  var sbUrl = '/views/standard/index.html?videomuted=true&videocontrols=true';
  $('<tr><td/></tr>').appendTo(table)
    .find('td').addClass('ViewFrames Footer')
    .append(_crgUtils.createRowTable(2))
    .find('td').append(previewButton)
    .find('button')
    .first().attr('src', sbUrl).end()
    .last().attr('src', sbUrl+'&preview=true');
}

function createScoreBoardViewPreviewRows(table, type) {
  var currentViewTd = $('<tr><td/></tr>').addClass(type).appendTo(table)
    .children('td').addClass('Header NoChildren CurrentView')
    .attr('ApplyPreview', 'CurrentView')
    .append($('<span>')
      .append('<label >ScoreBoard</label><input type="radio" value="scoreboard"/>')
      .append('<label >WhiteBoard</label><input type="radio" value="whiteboard"/>')
      .append('<label >Image</label><input type="radio" value="image"/>')
      .append('<label >Video</label><input type="radio" value="video"/>')
      .append('<label >Custom Page</label><input type="radio" value="html"/>'));

  currentViewTd.children('span').children('input')
    .attr('name', 'createScoreBoardViewPreviewRows' + type)
    .each(function(_, e) {
      e = $(e);
      e.attr('id', 'createScoreBoardViewPreviewRows' + e.attr('value') + type);
      e.prev().attr('for', 'createScoreBoardViewPreviewRows' + e.attr('value') + type);
    })
    .on('change', function(e) {
      WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_CurrentView)', e.target.value);
    });

  currentViewTd.children('span').controlgroup({items: {button: 'input'}}).end().prepend('<a>Current View : </a>');

  WS.Register('ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_CurrentView)', function(k, v) {
    currentViewTd.children('input[value=' + v + ']').prop('checked', true).button('refresh');
  });


  $('<tr><td><a>ScoreBoard Options</a></td></tr>').addClass(type).appendTo(table)
    .find('td').addClass('ScoreBoardOptions Header');

  var intermissionControlDialog = createIntermissionControlDialog();
  var intermissionControlButton = $('<button>Intermission Labels</button>').button().addClass('ui-button-small')
    .on('click', function() { intermissionControlDialog.dialog('open'); });

  var syncClocksButton = toggleButton('ScoreBoard.Settings.Setting(ScoreBoard.Clock.Sync)', 'Clocks Synced', 'Clocks Unsynced');
  var useLTButton = toggleButton('ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)', 'CRG Tracks Lineups', 'Lineup Tracking Disabled');
  var swapTeamsButton = toggleButton('ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_SwapTeams)', 'Team sides swapped', 'Team sides normal');
  swapTeamsButton.attr('ApplyPreview', 'SwapTeams');
  var hideLogosButton = toggleButton('ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_HideLogos)', 'Hide Logos', 'Show Logos');
  hideLogosButton.attr('ApplyPreview', 'HideLogos');

  var clockAfterTimeout = $('<label>Clock shown after Timeout: </label>').add(WSControl('ScoreBoard.Settings.Setting(ScoreBoard.ClockAfterTimeout)',
      $('<select>')
        .append('<option value="Lineup">Lineup</option>')
        .append('<option value="Timeout">Timeout</option>')));

  var boxStyle = $('<label>Box Style: </label>').add(WSControl('ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_BoxStyle)',
      $('<select>').attr('ApplyPreview', 'BoxStyle')
        .append('<option value="">Rounded</option>')
        .append('<option value="box_flat">Flat</option>')
        .append('<option value="box_flat_bright">Flat & Bright</option>')));
  var sidePadding = $('<label>Side Padding: </label>').add(WSControl('ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_SidePadding)',
      $('<select>').attr('ApplyPreview', 'SidePadding')
        .append('<option value="">None</option>')
        .append('<option value="2">2%</option>')
        .append('<option value="4">4%</option>')
        .append('<option value="6">6%</option>')
        .append('<option value="8">8%</option>')
        .append('<option value="10">10%</option>')));

  var imageViewSelect = $('<label>Image View: </label>')
    .add(mediaSelect('ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_Image)', 'images', 'fullscreen', 'Image'))
    .attr('ApplyPreview', 'Image');
  var videoViewSelect = $('<label>Video View: </label>')
    .add(mediaSelect('ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_Video)', 'videos', 'fullscreen', 'Video'))
    .attr('ApplyPreview', 'Video');
  var customPageViewSelect = $('<label>Custom Page View: </label>')
    .add(mediaSelect('ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_CustomHtml)', 'custom', 'view', 'Page'))
    .attr('ApplyPreview', 'CustomHtml');

  var optionsTable = $('<table/>')
    .addClass(type)
    .addClass('RowTable')
    .css('width', '100%');
  $('<tr><td></td></tr>').addClass(type).appendTo(table).find('td').append(optionsTable);
  $('<tr><td/><td/><td/></tr>').addClass(type).appendTo(optionsTable)
    .find('td').addClass('ScoreBoardOptions Footer')
    .first().append(intermissionControlButton)
    .next().append(swapTeamsButton)
    .next().append(imageViewSelect);
  $('<tr><td/><td/><td/></tr>').addClass(type).appendTo(optionsTable)
    .find('td').addClass('ScoreBoardOptions Footer')
    .first().append(syncClocksButton)
    .next().append(boxStyle)
    .next().append(videoViewSelect);
  $('<tr><td/><td/><td/></tr>').addClass(type).appendTo(optionsTable)
    .find('td').addClass('ScoreBoardOptions Footer')
    .first().append(clockAfterTimeout)
    .next().append(sidePadding)
    .next().append(customPageViewSelect);
  $('<tr><td/><td/><td/></tr>').addClass(type).appendTo(optionsTable)
    .find('td').addClass('ScoreBoardOptions Footer')
    .first().append(useLTButton)
    .next().append(hideLogosButton);

}

function createIntermissionControlDialog() {
  var table = $('<table>').addClass('IntermissionControlDialog');

  var fields = [
    { id: 'ScoreBoard.Intermission.PreGame', display: 'Pre Game'},
    { id: 'ScoreBoard.Intermission.Intermission', display: 'Intermission'},
    { id: 'ScoreBoard.Intermission.Unofficial', display: 'Unofficial Score'},
    { id: 'ScoreBoard.Intermission.Official', display: 'Official Score'},
    ];
  $.each( fields, function(i, field) {
    var path = 'ScoreBoard.Settings.Setting(' + field.id + ')';
    var row = $('<tr>').appendTo(table);
    $('<td>').addClass('Name').text(field.display).appendTo(row);
    var input = $('<input>').attr('type', 'text').val(WS.state[path])
      .on('input', function(e) { WS.Set(path, e.target.value); })
      .appendTo($('<td>').addClass('Value').appendTo(row));
    WS.Register(path, function(k, v) { input.val(v); } );
  });

  return $('<div>').append(table).dialog({
    title: 'Intermission Display Labels',
    autoOpen: false,
    width: 700,
    modal: true,
    buttons: { Close: function() { $(this).dialog('close'); } }
  });
}
