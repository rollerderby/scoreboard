
var map;
var markers = [];

var geocoder;

var leagues = [];
var countries = [];
var states = [];
var autoCompleteKeys = [];

$(function () {
  if (!Modernizr.flexbox)
    $("#controls,#mapCanvas").addClass("NoFlexbox");

  map = createMap();
  var mc;

  geocoder = new google.maps.Geocoder();

  if (!$.string(window.location.search).toQueryParams().hasOwnProperty("noClustering"))
    mc = new MarkerClusterer(map);

  $.getJSON("/maptest/countries.json")
    .done(function(list) {
      $.each(list, function(i, c) {
        countries[c.name] = c;
        if (c.states)
          $.each(c.states, function(ii, s) {
            c.states[s.name] = s;
            states[getStateName(c.name, s.name)] = s;
            s.country = c;
          });
      });
    })
    .fail(function(jqxhr, textStatus, error) {
      alert("Could not get list of countries (status "+textStatus+") : "+error);
    });

  $.getJSON("/maptest/leagues.json")
    .done(function(list) {
      $.each(list, function(i, l) {
        leagues[l.name] = l;
        addAutoCompleteLeague(l);
        var marker = createMarker(map, l);
        markers[l.name] = marker;
        if (mc)
          mc.addMarker(marker);
        else
          marker.setMap(map);
      });
      setupAutocomplete();
    })
    .fail(function(jqxhr, textStatus, error) {
      alert("Could not get list of leagues (status "+textStatus+") : "+error);
    });
});

function createMap() {
  // This centers on the so-called "geographic center" of the (contiguous) USA
  var mapOptions = {
    center: new google.maps.LatLng(39.8282,-98.5795),
    zoom: 4,
    mapTypeId: google.maps.MapTypeId.ROADMAP
  };
  var map = new google.maps.Map($("#mapCanvas")[0], mapOptions);
  $("#mapCanvas").keyup(function (event) {
    if (event.which == 27) // Esc
      closeOpenInfoWindow();
  });
  return map;
}

function createMarker(map, league) {
  var content = $("<a>").text(league.name);
  var marker = new MarkerWithLabel({
    position: new google.maps.LatLng(league.lat, league.lng),
    labelContent: content[0],
    labelAnchor: new google.maps.Point(50, 0),
    labelClass: "MarkerLabel",
    labelStyle: { opacity: 0.75 }
  });
  marker.set("League", league);
  google.maps.event.addListener(marker, "click", function() {
    showInfoWindow(map, this);
  });
  return marker;
}

var openInfoWindow;
function closeOpenInfoWindow() {
  if (!openInfoWindow)
    return;
  openInfoWindow.close();
  openInfoWindow.get("CloseFunction").call();
}

function showInfoWindow(map, marker) {
  if (openInfoWindow && (openInfoWindow.get("Marker") == marker))
    return;
  closeOpenInfoWindow();
  var league = marker.get("League");
  var content = $("<div>").addClass("InfoWindowContent");
  $("<a>").addClass("Name").text(league.name).appendTo(content);
  $("<br>").appendTo(content);
  if (league.image.teamlogo[0])
    $("<img>").addClass("TeamLogo").attr("src", league.image.teamlogo[0].src).appendTo(content);
  var infoWindow = new google.maps.InfoWindow({ content: content[0] });
  openInfoWindow = infoWindow;
  infoWindow.set("CloseFunction", function() {
    unselectLeague(league);
    openInfoWindow = undefined;
  });
  infoWindow.set("Marker", marker);
  google.maps.event.addListener(infoWindow, "closeclick", infoWindow.get("CloseFunction"));
  infoWindow.open(map, marker);
  selectLeague(league);
}

function selectLeague(league) {
  var name = league.name;

  var controls = $("div#controls");
  controls.find("div.LeagueName").addClass("Show").find("a.LeagueName").text(name);

  var box = controls.find("div.TeamLogo.Template").clone()
    .removeClass("Template").addClass("Show").appendTo(controls);

  $.each( league.image.teamlogo, function(i, teamlogo) {
    var src = teamlogo.src;
    var imgname = src.replace(/^(?:[^\/]*\/)*/, "");
    var imgbox = box.find("div.ImageBox.Template").clone()
      .removeClass("Template").appendTo(box);
    imgbox.find("div.TeamLogoImage img.TeamLogo").attr("src", src);
    imgbox.find("div.ImageControl a.ImageName").text(imgname);
    imgbox.find("div.ImageBox div.ImageControl button.ImageButton").click(function() {
      getMedia(league, "images", "teamlogo", src)
    });
  });
}

function unselectLeague(league) {
  $("div#controls")
    .find("div.ControlBox").removeClass("Show").end()
    .find("div.LeagueName a.LeagueName").text("").end()
    .find("div.TeamLogo:not(.Template)").remove().end();
}

function getMedia(league, media, type, src) {
  $.post("/Media/download", { media: media, type: type, url: src })
    .done(function() { alert("FIXME - success getting img; improve handling"); })
    .fail(function(xhr, textStatus, error) { alert("failure "+error); });
}

function addAutoCompleteLeague(league) {
  if (!league.zoom)
    league.zoom = 8; // default zoom into league with autocomplete
  if (league.name)
    if ($.inArray(league.name, autoCompleteKeys) < 0)
      autoCompleteKeys.push(league.name);
  if (league.country) {
    if (!countries[league.country])
      countries[league.country] = { name: league.country };
    if ($.inArray(league.country, autoCompleteKeys) < 0)
      autoCompleteKeys.push(league.country);
    if (league.state) {
      var statename = getStateName(league.country, league.state);
      if (!states[statename]) {
        if (!countries[league.country].states)
          countries[league.country].states = [];
        if (!countries[league.country].states[league.state])
          countries[league.country].states[league.state] = { name: league.state, country: countries[league.country] };
        states[statename] = countries[league.country].states[league.state];
      }
      if ($.inArray(statename, autoCompleteKeys) < 0)
        autoCompleteKeys.push(statename);
    }
  }
}

function autoCompleteSelect(value) {
  if (markers[value]) {
    mapToLoc(markers[value].get("League"));
    google.maps.event.trigger(markers[value], "click");
  } else if (countries[value]) {
    mapToLoc(countries[value]);
    closeOpenInfoWindow();
  } else if (states[value]) {
    mapToLoc(states[value]);
    closeOpenInfoWindow();
  }
}

function setupAutocomplete() {
  $("#controls div.SearchBox input.Search").autocomplete({
    source: autoCompleteKeys,
    minLength: 3,
    response: function(event, ui) {
      if (ui.content.length == 1)
        autoCompleteSelect(ui.content[0].value);
      else
        closeOpenInfoWindow();
    },
    delay: 50,
    select: function(event, ui) {
      this.value = ui.item.value;
      autoCompleteSelect(this.value);
    }
  });
}

function getStateName(countryName, stateName) { return stateName + ", " + countryName; }

$(function() {
  // until update to jquery 1.7
  if ($.isNumeric)
    alert("jquery updated to 1.7 - remove $.isNumeric workaround");
  else
    $.isNumeric = function(o) { return !isNaN(o); };
});

function mapToLoc(l) {
  if ($.isNumeric(l.lat) && $.isNumeric(l.lng) && $.isNumeric(l.zoom)) {
    map.setZoom(l.zoom);
    map.panTo(new google.maps.LatLng(l.lat, l.lng));
  } else {
    var hasSw = ($.isNumeric(l.swLat) && $.isNumeric(l.swLng));
    var hasNe = ($.isNumeric(l.neLat) && $.isNumeric(l.neLng));
    if (hasSw && hasNe) {
      var sw = new google.maps.LatLng(l.swLat, l.swLng);
      var ne = new google.maps.LatLng(l.neLat, l.neLng);
      map.fitBounds(new google.maps.LatLngBounds(sw, ne));
    } else {
      geocoder.geocode({ address: l.name }, function(r,s) { geocodeResult(r, s, l); });
    }
  }
}

function geocodeResult(results, status, l) {
  if (status != google.maps.GeocoderStatus.OK) {
    alert("Could not geocode "+l.name+" : "+status);
    return;
  }
  var r = results[0];
  l.swLat = r.geometry.viewport.getSouthWest().lat();
  l.swLng = r.geometry.viewport.getSouthWest().lng();
  l.neLat = r.geometry.viewport.getNorthEast().lat();
  l.neLng = r.geometry.viewport.getNorthEast().lng();
  var sw = new google.maps.LatLng(l.swLat, l.swLng);
  var ne = new google.maps.LatLng(l.neLat, l.neLng);
  map.fitBounds(new google.maps.LatLngBounds(sw, ne));
}

