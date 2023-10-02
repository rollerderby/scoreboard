function createScoreBoardSettingsTab(tab) {
  'use strict';
  var table = $('<table>').addClass('UsePreview').attr('id', 'ScoreBoardSettings').appendTo(tab);

  var usePreviewLabel = $('<span>').text('Edit: ');
  var usePreviewDropdown = $('<select id="usePreviewDropdown"/>')
    .append($('<option>').val('preview').text('Preview'))
    .append($('<option>').val('live').text('Live ScoreBoard'))
    .on('change', function () {
      table.toggleClass('UsePreview', $(this).val() === 'preview');
    });
  var applyPreviewButton = $('<button>Apply Preview</button>')
    .button()
    .on('click', function () {
      var done = {};
      table.find('.Preview [ApplyPreview]').each(function (_, e) {
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
    });

  createNonViewRows(table);

  $('<tr>')
    .appendTo(table)
    .append($('<td>').addClass('Header').append($('<span>').text('Main Display Settings')));
  $('<tr><td/></tr>')
    .appendTo(table)
    .find('td')
    .addClass('PreviewControl SubHeader')
    .append(_crgUtils.createRowTable(3))
    .find('td:first')
    .next()
    .append(usePreviewLabel)
    .append(usePreviewDropdown)
    .next()
    .append(applyPreviewButton);

  $.each(['View', 'Preview'], function (i, p) {
    createScoreBoardViewPreviewRows(table, p);
  });

  $('<tr><td/></tr>')
    .appendTo(table)
    .find('td')
    .addClass('ViewFrames SubHeader')
    .append(_crgUtils.createRowTable(2))
    .find('td')
    .first()
    .append('<a>Current</a>')
    .next()
    .append('<a>Preview</a>');

  var previewButton = $('<button>')
    .html('Show Preview')
    .on('click', function () {
      $('<iframe>Your browser does not support iframes.</iframe>')
        .attr({ scrolling: 'no', frameborder: '0', src: $(this).attr('src') })
        .replaceAll(this);
    });

  var sbUrl = '/views/standard/index.html?videomuted=true&videocontrols=true';
  $('<tr><td/></tr>')
    .appendTo(table)
    .find('td')
    .addClass('ViewFrames Footer')
    .append(_crgUtils.createRowTable(2))
    .find('td')
    .append(previewButton)
    .find('button')
    .first()
    .attr('src', sbUrl)
    .end()
    .last()
    .attr('src', sbUrl + '&preview=true');
}

function createNonViewRows(table) {
  'use strict';
  var syncClocksButton = WSActiveButton('ScoreBoard.Settings.Setting(ScoreBoard.Clock.Sync)', $('<button>').text('Sync Clocks').button());
  var clockAfterTimeout = $('<label>Clock shown after Timeout: </label>').add(
    WSControl(
      'ScoreBoard.Settings.Setting(ScoreBoard.ClockAfterTimeout)',
      $('<select>').append('<option value="Lineup">Lineup</option>').append('<option value="Timeout">Timeout</option>')
    )
  );

  var intermissionControlDialog = createIntermissionControlDialog();
  var intermissionControlButton = $('<button>Intermission Labels</button>')
    .button()
    .addClass('ui-button-small')
    .on('click', function () {
      intermissionControlDialog.dialog('open');
    });

  var autoStartType = $('<label>Auto Start: </label>').add(
    WSControl(
      'ScoreBoard.Settings.Setting(ScoreBoard.AutoStart)',
      $('<select>')
        .append('<option value="">Off</option>')
        .append('<option value="Jam">Jam</option>')
        .append('<option value="Timeout">Timeout</option>')
    )
  );
  var autoStartBuffer = $('<label>Time between end of Lineup and auto start being applied: </label>').add(
    WSControl('ScoreBoard.Settings.Setting(ScoreBoard.AutoStartBuffer)', $('<input type="text" size="5">'))
  );
  var autoEndJam = WSActiveButton('ScoreBoard.Settings.Setting(ScoreBoard.AutoEndJam)', $('<button>').text('Auto End Jams').button());
  var autoEndTTO = WSActiveButton(
    'ScoreBoard.Settings.Setting(ScoreBoard.AutoEndTTO)',
    $('<button>').text('Auto End Team Timeouts').button()
  );

  var useLTButton = WSActiveButton(
    'ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)',
    $('<button>').text('CRG Tracks Lineups').button()
  );
  var statsbookFile = $('<label>Blank Statsbook File: </label>')
    .add(WSControl('ScoreBoard.Settings.Setting(ScoreBoard.Stats.InputFile)', $('<input type="text" size="40">')))
    .add($('<div>').addClass('spin'));
  WS.Register('ScoreBoard.BlankStatsbookFound', function (k, v) {
    statsbookFile
      .parent()
      .addClass('StatsFile')
      .toggleClass('Readable', v === 'true')
      .toggleClass('Progress', v === 'checking');
  });
  var teamDisplayName = $('<label>Team Name for Display: </label>').add(
    WSControl(
      'ScoreBoard.Settings.Setting(ScoreBoard.Teams.DisplayName)',
      $('<select>')
        .append('<option value="League">League Name</option>')
        .append('<option value="Team">Team Name</option>')
        .append('<option value="Full">Full Name</option>')
    )
  );
  var teamFileName = $('<label>Team Name for Files: </label>').add(
    WSControl(
      'ScoreBoard.Settings.Setting(ScoreBoard.Teams.FileName)',
      $('<select>')
        .append('<option value="League">League Name</option>')
        .append('<option value="Team">Team Name</option>')
        .append('<option selected value="Full">Full Name</option>')
    )
  );
  var defaultGameNameFormat = $('<label>Name format for new games: </label>').add(
    WSControl('ScoreBoard.Settings.Setting(ScoreBoard.Game.DefaultNameFormat)', $('<input type="text" size="25">'))
  );

  $('<tr>')
    .appendTo(table)
    .append($('<td>').addClass('Header').append($('<span>').text('General Settings')));
  var optionsTable = $('<table/>').addClass('RowTable').css('width', '100%');
  $('<tr>').append($('<td>').addClass('Footer').append(optionsTable)).appendTo(table);
  $('<tr><td/><td/><td/></tr>')
    .appendTo(optionsTable)
    .addClass('ScoreBoardOptions EndSubSection')
    .find('td')
    .first()
    .append(intermissionControlButton)
    .next()
    .append(syncClocksButton)
    .next()
    .append(clockAfterTimeout);
  $('<tr><td/><td colspan="2"/></tr>')
    .appendTo(optionsTable)
    .addClass('ScoreBoardOptions')
    .find('td')
    .first()
    .append(autoStartType)
    .next()
    .append(autoStartBuffer);
  $('<tr><td/><td/><td/></tr>')
    .appendTo(optionsTable)
    .addClass('ScoreBoardOptions EndSubSection')
    .find('td')
    .first()
    .next()
    .append(autoEndJam)
    .next()
    .append(autoEndTTO);
  $('<tr><td/><td colspan="2"/></tr>')
    .appendTo(optionsTable)
    .addClass('ScoreBoardOptions')
    .find('td')
    .first()
    .append(useLTButton)
    .next()
    .append(statsbookFile);
  $('<tr><td/><td/><td/></tr>')
    .appendTo(optionsTable)
    .addClass('ScoreBoardOptions EndSubSection Footer')
    .find('td')
    .first()
    .append(teamDisplayName)
    .next()
    .append(teamFileName)
    .next()
    .append(defaultGameNameFormat);
}

function createScoreBoardViewPreviewRows(table, type) {
  'use strict';
  $('<tr><td/></tr>')
    .addClass(type + ' EndSubSection')
    .appendTo(table)
    .children('td')
    .addClass('NoChildren CurrentView')
    .attr('ApplyPreview', 'CurrentView')
    .append($('<span>').text('Current View: '))
    .append(
      WSControl(
        'ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_CurrentView)',
        $('<select>')
          .append('<option value="scoreboard">ScoreBoard</option>')
          .append('<option value="whiteboard">WhiteBoard</option>')
          .append('<option value="image">Image</option>')
          .append('<option value="video">Video</option>')
          .append('<option value="html">Custom Page</option>')
      )
    );

  var swapTeamsButton = WSActiveButton(
    'ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_SwapTeams)',
    $('<button>').text('Swap Team sides').button().attr('ApplyPreview', 'SwapTeams')
  );
  var hideLogosButton = WSActiveButton(
    'ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_HideLogos)',
    $('<button>').text('Hide Logos').button().attr('ApplyPreview', 'HideLogos')
  );

  var boxStyle = $('<label>Box Style: </label>').add(
    WSControl(
      'ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_BoxStyle)',
      $('<select>')
        .attr('ApplyPreview', 'BoxStyle')
        .append('<option value="">Rounded</option>')
        .append('<option value="box_flat">Flat</option>')
        .append('<option value="box_flat_bright">Flat & Bright</option>')
    )
  );
  var sidePadding = $('<label>Side Padding: </label>').add(
    WSControl(
      'ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_SidePadding)',
      $('<select>')
        .attr('ApplyPreview', 'SidePadding')
        .append('<option value="">None</option>')
        .append('<option value="2">2%</option>')
        .append('<option value="4">4%</option>')
        .append('<option value="6">6%</option>')
        .append('<option value="8">8%</option>')
        .append('<option value="10">10%</option>')
    )
  );

  var imageViewSelect = $('<label>Image View: </label>')
    .add(mediaSelect('ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_Image)', 'images', 'fullscreen', 'Image'))
    .attr('ApplyPreview', 'Image');

  var imageScaleSelect = $('<label>Image Scaling: </label>').add(
    WSControl(
      'ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_ImageScaling)',
      $('<select>')
        .attr('ApplyPreview', 'ImageScaling')
        .append('<option value="contain">Scale to fit</option>')
        .append('<option value="cover">Scale to fill</option>')
        .append('<option value="fill">Stretch</option>')
    )
  );

  var videoViewSelect = $('<label>Video View: </label>')
    .add(mediaSelect('ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_Video)', 'videos', 'fullscreen', 'Video'))
    .attr('ApplyPreview', 'Video');

  var videoScaleSelect = $('<label>Video Scaling: </label>').add(
    WSControl(
      'ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_VideoScaling)',
      $('<select>')
        .attr('ApplyPreview', 'VideoScaling')
        .append('<option value="contain">Scale to fit</option>')
        .append('<option value="cover">Scale to fill</option>')
        .append('<option value="fill">Stretch</option>')
    )
  );

  var customPageViewSelect = $('<label>Custom Page View: </label>')
    .add(mediaSelect('ScoreBoard.Settings.Setting(ScoreBoard.' + type + '_CustomHtml)', 'custom', 'view', 'Page'))
    .attr('ApplyPreview', 'CustomHtml');

  var optionsTable = $('<table/>').addClass(type).addClass('RowTable').css('width', '100%');
  $('<tr><td></td></tr>').addClass(type).appendTo(table).find('td').append(optionsTable);

  let optionsTableRow = '<tr><td/><td/><td/><td/></tr>';
  $(optionsTableRow)
    .addClass(type)
    .appendTo(optionsTable)
    .addClass('ScoreBoardOptions')
    .find('td')
    .first()
    .append(hideLogosButton)
    .next()
    .append(boxStyle)
    .next()
    .append(imageViewSelect)
    .next()
    .append(imageScaleSelect);
  $(optionsTableRow)
    .addClass(type)
    .appendTo(optionsTable)
    .addClass('ScoreBoardOptions')
    .find('td')
    .first()
    .append(swapTeamsButton)
    .next()
    .append(sidePadding)
    .next()
    .append(videoViewSelect)
    .next()
    .append(videoScaleSelect);
  $(optionsTableRow)
    .addClass(type)
    .addClass('ScoreBoardOptions EndSubSection Footer')
    .appendTo(optionsTable)
    .find('td')
    .first()
    .next()
    .next()
    .append(customPageViewSelect);
}

function createIntermissionControlDialog() {
  'use strict';
  var table = $('<table>').addClass('IntermissionControlDialog');

  var fields = [
    { id: 'ScoreBoard.Intermission.PreGame', display: 'Pre Game' },
    { id: 'ScoreBoard.Intermission.Intermission', display: 'Intermission' },
    { id: 'ScoreBoard.Intermission.Unofficial', display: 'Unofficial Score' },
    { id: 'ScoreBoard.Intermission.Official', display: 'Official Score' },
    { id: 'ScoreBoard.Intermission.OfficialWithClock', display: 'Official Score with clock' },
  ];
  $.each(fields, function (i, field) {
    var path = 'ScoreBoard.Settings.Setting(' + field.id + ')';
    var row = $('<tr>').appendTo(table);
    $('<td>').addClass('Name').text(field.display).appendTo(row);
    var input = $('<input>')
      .attr('type', 'text')
      .val(WS.state[path])
      .on('input', function (e) {
        WS.Set(path, e.target.value);
      })
      .appendTo($('<td>').addClass('Value').appendTo(row));
    WS.Register(path, function (k, v) {
      input.val(v);
    });
  });

  return $('<div>')
    .append(table)
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
}
