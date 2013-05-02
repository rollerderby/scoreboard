
var map;
var markers = [];

var leagues;
var countries;
var autoCompleteKeys = [];

$(function () {
  map = createMap();
  var mc;

  if (!_windowFunctions.hasParam("noClustering"))
    mc = new MarkerClusterer(map);

  $.getJSON("/maptest/countries.json")
    .done(function(c) { countries = c; })
    .fail(function(jqxhr, textStatus, error) {
      alert("Could not get list of countries (status "+textStatus+") : "+error);
    });

  $.getJSON("/maptest/leagues.json")
    .done(function(l) {
      leagues = l;
      $.each(leagues, function(i, league) {
        addAutoCompleteLeague(league);
        var marker = createMarker(map, league);
        markers[league.name] = marker;
        if (mc)
          mc.addMarker(marker);
        else
          marker.setMap(map);
      });
      setupAutocomplete(leagues);
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
    position: new google.maps.LatLng(league.latitude, league.longitude),
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
  openInfoWindow.get("PrivateCloseFunction").call();
}

function showInfoWindow(map, marker) {
  if (openInfoWindow && (openInfoWindow.get("PrivateMarker") == marker))
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
  infoWindow.set("PrivateCloseFunction", function() {
    unselectLeague(league);
    openInfoWindow = undefined;
  });
  infoWindow.set("PrivateMarker", marker);
  google.maps.event.addListener(infoWindow, "closeclick", infoWindow.get("PrivateCloseFunction"));
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
    .done(function() { alert("success"); })
    .fail(function(xhr, textStatus, error) { alert("failure "+error); });
}

function addAutoCompleteLeague(league) {
  if (league.name)
    if ($.inArray(league.name, autoCompleteKeys) < 0)
      autoCompleteKeys.push(league.name);
  if (league.country) {
    if (!countries[league.country])
      alert("Country "+league.country+" not in list");
    if ($.inArray(league.country, autoCompleteKeys) < 0)
      autoCompleteKeys.push(league.country);
  }
}

function autoCompleteSelect(value) {
  if (markers[value]) {
    var league = markers[value].get("League");
    map.setZoom(8);
    map.panTo(new google.maps.LatLng(league.latitude, league.longitude));
    google.maps.event.trigger(markers[value], "click");
  } else if (countries[value]) {
    var country = countries[value];
    map.setZoom(country.zoom);
    map.panTo(new google.maps.LatLng(country.latitude, country.longitude));
  }
}

function setupAutocomplete(keys) {
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
    }
  });
}
