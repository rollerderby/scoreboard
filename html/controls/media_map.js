
$(function () {
  var map = createMap();
  var mc;

  if (!_windowFunctions.hasParam("noClustering"))
    mc = new MarkerClusterer(map);

  $.getJSON("/maptest/leagues.json")
    .done(function(leagues) {
      $.each(leagues, function(i, league) {
        var marker = createMarker(map, league);
        if (mc)
          mc.addMarker(marker);
        else
          marker.setMap(map);
      });
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
  var content = $("<a>").text(league.name).data({ league: league });
  var marker = new MarkerWithLabel({
    position: new google.maps.LatLng(league.latitude, league.longitude),
    labelContent: content[0],
    labelAnchor: new google.maps.Point(50, 0),
    labelClass: "MarkerLabel",
    labelStyle: { opacity: 0.75 }
  });
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
  closeOpenInfoWindow();
  var league = $(marker.labelContent).data("league");
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
  google.maps.event.addListener(infoWindow, "closeclick", infoWindow.get("PrivateCloseFunction"));
  infoWindow.open(map, marker);
  selectLeague(league);
}

function selectLeague(league) {
  var name = league.name;

  var controls = $("div#controls");
  controls.find("div.LeagueName").addClass("Show").find("a.LeagueName").text(name);

  $.each( league.image.teamlogo, function(i, teamlogo) {
    var src = teamlogo.src;
    var imgname = src.replace(/^(?:[^\/]*\/)*/, "");
    var div = controls.find("div.TeamLogo.Template").clone()
      .removeClass("Template").addClass("Show").appendTo(controls);
    div.find("div.TeamLogoImage img.TeamLogo").attr("src", src);
    div.find("div.ImageControl a.ImageName").text(imgname);
    div.find("div.ImageBox div.ImageControl button.ImageButton").click(function() {
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

