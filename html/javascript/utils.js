'use strict';

function sbNewUuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = (Math.random() * 16) | 0,
      v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

function sbCloseDialog(k, v, elem) {
  elem.closest('.ui-dialog-content').dialog('close');
}

function sbCloseDialogIfNull(k, v, elem) {
  if (v == null) {
    elem.closest('.ui-dialog-content').dialog('close');
  }
}

function sbClockSelect(k) {
  var jam = isTrue(WS.state[k.upTo('Game') + '.InJam']);
  var timeout = isTrue(WS.state[k.upTo('Game') + '.Clock(Timeout).Running']);
  var lineup = isTrue(WS.state[k.upTo('Game') + '.Clock(Lineup).Running']);
  var intermission = isTrue(WS.state[k.upTo('Game') + '.Clock(Intermission).Running']);

  var clock = 'NoClock';
  if (jam) {
    clock = 'Jam';
  } else if (timeout) {
    clock = 'Timeout';
  } else if (lineup) {
    clock = 'Lineup';
  } else if (intermission) {
    clock = 'Intermission';
  }

  $('.Clock,.SlideDown').removeClass('Show');
  $('.ShowIn' + clock).addClass('Show');
}

function sbSetActiveTimeout(k) {
  var to = WS.state[k.upTo('Game') + '.TimeoutOwner'].slice(-1);
  var or = WS.state[k.upTo('Game') + '.OfficialReview'];

  $('.Team .Dot').removeClass('Current');

  if (to && to !== 'O') {
    var dotSel;
    if (or) {
      dotSel = '[Team=' + to + '] .OfficialReview1';
    } else {
      dotSel = '[Team=' + to + '] .Timeout' + (WS.state[k.upTo('Game') + '.Team(' + to + ').Timeouts'] + 1);
    }
    $(dotSel).addClass('Current');
  }
}

function sbReverseOnNonSheet(k, v, elem) {
  if (elem.closest('[sbSheetType]').attr('sbSheetType') !== 'sheet') {
    elem.append(elem.children('tr').get().reverse());
  }
}

var _crgUtils = {
  /* Convert a string to a "safe" id.
   * This removes illegal characters from the string,
   * so it's safe to use as an element's id.
   */
  checkSbId: function (s) {
    'use strict';
    return s.replace(/['"()]/g, '');
  },

  /* Bind, using on(), and run a function.
   * This is more restrictive than the actual on() function,
   * as only one eventType can be specified, and this does
   * not support a map as the jQuery on() function does.
   * The eventData and initialParams parameters are optional.
   * The initialParams, if provided, is an array of the parameters
   * to supply to the initial call of the handler.
   * The handler is initially run once for each element
   * in the jQuery target object.
   * The useBind parameter, if true, will cause bind() to be used
   * instead of on().
   */
  onAndRun: function (target, eventType, eventData, handler, initialParams, useBind) {
    'use strict';
    if (!$.isjQuery(target)) {
      target = $(target);
    }
    if ($.isFunction(eventData)) {
      initialParams = handler;
      handler = eventData;
      eventData = undefined;
      if (useBind) {
        target.on(eventType, handler);
      } else {
        target.live(eventType, handler);
      }
    } else {
      if (useBind) {
        target.on(eventType, eventData, handler);
      } else {
        target.live(eventType, eventData, handler);
      }
    }
    target.each(function () {
      var params = [];
      if (initialParams) {
        params = initialParams;
      }
      //FIXME - call once for each eventType after splitting by spaces?
      var event = jQuery.Event(eventType);
      event.target = event.currentTarget = this;
      if (eventData) {
        event.data = eventData;
      }
      handler.apply(this, $.merge([event], params));
    });
    return target;
  },
  bindAndRun: function (target, eventType, eventData, handler, initialParams) {
    'use strict';
    return _crgUtils.onAndRun(target, eventType, eventData, handler, initialParams, true);
  },

  showLoginDialog: function (titleText, nameText, buttonText, callback) {
    'use strict';
    var dialog = $('<div>').append($('<a>').html(nameText)).append('<input type="text"/>');
    var login = function () {
      if (callback(dialog.find('input:text').val())) {
        dialog.dialog('destroy');
      }
    };
    dialog.find('input:text').keydown(function (event) {
      if (event.which === 13) {
        login();
      }
    });
    dialog.dialog({
      modal: true,
      closeOnEscape: false,
      title: titleText,
      buttons: [{ text: buttonText, click: login }],
      close: function () {
        if (callback('default')) {
          dialog.dialog('destroy');
        }
      },
    });
    // If we're in period 2, the dialog can end up off screen once all the
    // SK rows finish loading. This ensures the dialog stays up top.
    dialog.dialog('widget').css({ top: '5em', position: 'fixed' });
  },

  createRowTable: function (n, r) {
    'use strict';
    var table = $('<table>').css('width', '100%').addClass('RowTable');
    var w = 100 / n + '%';
    r = r || 1;
    while (0 < r--) {
      var count = n;
      var row = $('<tr>').appendTo(table);
      while (0 < count--) {
        $('<td>').css('width', w).appendTo(row);
      }
    }
    return table;
  },
};
