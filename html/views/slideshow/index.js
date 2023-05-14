$(initialize);

function initialize() {
 

  (function () {
    var SldShwSwitchTimeMs = 8000;
    var SldShwBanners = [];
    var SldShwDiv = $('#SlideshowBox');
    var setSldShwNextSrc = function () {
      if (SldShwBanners.length === 0) {
        SldShwDiv.find('.NextSldShwImg>img').prop('src', '').toggle(false);
      } else {
        // Use time so different scoreboards will be using the same images.
        var index = Math.round((new Date().getTime() / SldShwSwitchTimeMs) % SldShwBanners.length);
        SldShwDiv.find('.NextSldShwImg>img').prop('src', SldShwBanners[index].Src).toggle(true);

        // Also set the current image. This gets a banner up when the page is loaded,
        // and is otherwise a noop.
        SldShwDiv.find('.CurrentSldShwImg>img').prop('src', SldShwBanners[index].Src).toggle(true);
      }
    };
    var nextSldShwImgFunction = function () {
      var SldShwCur = $(SldShwDiv.find('.CurrentSldShwImg')[0]);
      var SldShwNex = $(SldShwDiv.find('.NextSldShwImg')[0]);
      var SldShwFin = $(SldShwDiv.find('.FinishedSldShwImg')[0]);
      SldShwCur.removeClass('CurrentSldShwImg').addClass('FinishedSldShwImg');
      SldShwNex.removeClass('NextSldShwImg').addClass('CurrentSldShwImg');
      SldShwFin.removeClass('FinishedSldShwImg').addClass('NextSldShwImg');
      setSldShwNextSrc();
      // Align to clock, so different scoreboards will be synced.
      setTimeout(nextSldShwImgFunction, SldShwSwitchTimeMs - (new Date().getTime() % SldShwSwitchTimeMs));
    };
    WS.Register(['ScoreBoard.Media.Format(images).Type(slideshow)'], {
      triggerBatchFunc: function () {
        var images = {};
        for (var prop in WS.state) {
          if (WS.state[prop] == null) {
            continue;
          }
          var re = /ScoreBoard.Media.Format\(images\).Type\(slideshow\)\.File\((.*)\)\.(\w+)/;
          var m = prop.match(re);
          if (m != null) {
            images[m[1]] = images[m[1]] || {};
            images[m[1]][m[2]] = WS.state[prop];
          }
        }
        SldShwBanners = Object.values(images).sort(function (a, b) {
          a.Id.localeCompare(b.Id, 'en');
        });
      },
    });

    setSldShwNextSrc();
    nextSldShwImgFunction();
  })();
}