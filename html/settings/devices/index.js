WS.Register('WS.Device.Id');

WS.AfterLoad(function () {
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

function devColorRows() {
  $('.Devices tr:visible').each(function (idx) {
    $(this).toggleClass('Darker', idx % 2 === 1);
  });
}

function devToggleSelection(k, v, elem) {
  elem = $(elem);
  elem.toggleClass('sbActive');
  $('.Devices').toggleClass('Show' + elem.text(), elem.hasClass('sbActive'));
  devColorRows();
}

function devIsLocalAddr(k, v) {
  return v === '127.0.0.1' || v === '0:0:0:0:0:0:0:1';
}

function devIsOwnId(k, v) {
  return v === WS.state['WS.Device.Id'];
}

function devToRwStatus(k, v) {
  return isTrue(v) ? 'Write' : 'Read only';
}

function devToDate(k, v) {
  if (v == 0) {
    return 'never';
  } else {
    return new Date(v);
  }
}
