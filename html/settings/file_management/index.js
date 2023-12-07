$(function () {
  'use strict';

  WS.Connect();
  WS.AutoRegister();

  $('#tabsDiv').tabs();
});

function isNotImages(k, v) {
  'use strict';
  return v !== 'images';
}

function noneIfNotImages(k, v) {
  'use strict';
  return v !== 'images' ? 'none' : '';
}

function getSubId(k, v) {
  'use strict';
  return v.split('_')[1];
}

function showTable(k, v, elem) {
  'use strict';
  elem.closest('table').removeClass('Hide');
}

function hideTable(k, v, elem) {
  'use strict';
  elem.closest('table').addClass('Hide');
}

function gameData(k, v) {
  'use strict';
  return k.Format === 'game-data';
}

function noGameData(k, v) {
  'use strict';
  return k.Format !== 'game-data';
}

function toPreviewElem(k, v) {
  'use strict';
  return k.Format === 'game-data'
    ? ''
    : (k.Format === 'images' ? '<img' : k.Format === 'videos' ? '<video' : '<iframe') +
        ' src="/' +
        k.Format +
        '/' +
        k.Type +
        '/' +
        k.File +
        '">';
}

function createRemoveDialog(k, v, elem) {
  'use struct';
  var div = $('div.RemoveMediaDialog').clone(true);
  div.find('a.File').text(k.Format + '/' + k.Type + '/' + k.File);
  div.dialog({
    title: 'Remove media',
    modal: true,
    width: 700,
    close: function () {
      $(this).dialog('destroy').remove();
    },
    buttons: {
      'Yes, Remove': function () {
        div.find('p.Warning,p.Confirm').text('');
        div.find('p.Status').text('Removing file...');
        $.post('/Media/remove', {
          media: k.Format,
          type: k.Type,
          filename: k.File,
        })
          .fail(function (jqxhr, textStatus, errorThrown) {
            div.find('p.Status').text('Error removing media file: ' + jqxhr.responseText);
            div.dialog('option', 'buttons', {
              Close: function () {
                div.dialog('close');
              },
            });
          })
          .done(function (data, textStatus, jqXHR) {
            div.dialog('close');
          });
      },
      No: function () {
        div.dialog('close');
      },
    },
  });
}

function createUploadDialog(k, v, elem) {
  var div = $('div.UploadMediaDialog').clone(true);
  var uploader = div.find('div.Upload').fileupload({
    url: '/Media/upload',
    dropZone: null,
    singleFileUploads: false,
  });
  var inputFile = div.find('input:file.File');
  var uploadFunction = function () {
    var data = { files: $(this).find('input:file.File')[0].files };
    var length = data.files.length;
    var statustxt = length + ' file' + (length > 1 ? 's' : '') + ' uploaded successfully';
    uploader.fileupload('option', 'formData', [
      { name: 'media', value: k.Format },
      { name: 'type', value: k.Type },
    ]);
    uploader
      .fileupload('send', data)
      .done(function (d, textStatus, jqxhr) {
        div.find('a.Status').text(statustxt);
      })
      .fail(function (jqxhr, textStatus, errorThrown) {
        div.find('a.Status').text('Error while uploading : ' + jqxhr.responseText);
      })
      .always(function () {
        var newInputFile = inputFile.clone(true).insertAfter(inputFile);
        inputFile.remove();
        inputFile = newInputFile.trigger('change');
      });
    uploader.fileupload('option', 'formData', []);
  };
  var closeFunction = function () {
    $(this).dialog('close');
  };
  var buttonsCloseOnly = { Close: closeFunction };
  var buttonsUploadClose = { Upload: uploadFunction, Close: closeFunction };

  div.dialog({
    title: 'Upload media ' + format + ' : ' + type,
    modal: true,
    width: 700,
    close: function () {
      $(this).dialog('destroy').remove();
    },
    buttons: buttonsCloseOnly,
  });
  inputFile
    .on('change', function () {
      var files = this.files;
      if (!files || !files.length) {
        div.dialog('option', 'buttons', buttonsCloseOnly);
        return;
      }
      div.dialog('option', 'buttons', buttonsUploadClose);
    })
    .trigger('change');
}
