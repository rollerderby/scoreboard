WS.Register('WS.Device.Id');

WS.AfterLoad(function () {
  'use strict';
  function updateAge(e, now) {
    if (!now) {
      now = new Date().getTime();
    }
    var age = e.attr('age');
    if (age == 0) {
      e.text('never');
      return;
    }
    var t = (now - age) / 1000;
    if (t < 60) {
      e.text(Math.floor(t) + 's');
    } else if (t < 3600) {
      e.text(Math.floor(t / 60) + 'm');
    } else if (t < 86400) {
      e.text(Math.floor(t / 3600) + 'h');
    } else {
      e.text(Math.floor(t / 86400) + 'd');
    }
  }

  setInterval(function () {
    var now = new Date().getTime();
    $.each($('.Devices td[age]'), function (e) {
      updateAge($(this), now);
    });
  }, 1000);
});

function toggleSelection(k, v, elem) {
  'use strict';
  elem = $(elem);
  elem.toggleClass('sbActive');
  $('.Devices').toggleClass('Show' + elem.text(), elem.hasClass('sbActive'));
}

function plus1(k, v) {
  'use strict';
  return v + 1;
}

function isLocalAddr(k, v) {
  'use strict';
  return v === '127.0.0.1' || v === '0:0:0:0:0:0:0:1';
}

function isOwnId(k, v) {
  'use strict';
  return v === WS.state['WS.Device.Id'];
}

function toRwStatus(k, v) {
  'use strict';
  return isTrue(v) ? 'Write' : 'Read only';
}

function toDate(k, v) {
  'use strict';
  if (v == 0) {
    return 'never';
  } else {
    return new Date(v);
  }
}
