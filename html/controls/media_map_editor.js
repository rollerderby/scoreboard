
var map;

$(function() {
  if ($.string(window.location.search).toQueryParams().hasOwnProperty("showBounds"))
    $("#controls div.Location.Bounds").addClass("Show");
  if ($.string(window.location.search).toQueryParams().hasOwnProperty("showCenter"))
    $("#controls div.Location.Center").addClass("Show");
});

$(function () {
  map = createMap();

  google.maps.event.addListener(map, "bounds_changed", function() {
    $("#controls div.Location.Bounds")
      .find("a.swLat").text(map.getBounds().getSouthWest().lat().toFixed(4)).end()
      .find("a.swLng").text(map.getBounds().getSouthWest().lng().toFixed(4)).end()
      .find("a.neLat").text(map.getBounds().getNorthEast().lat().toFixed(4)).end()
      .find("a.neLng").text(map.getBounds().getNorthEast().lng().toFixed(4));
  });
  google.maps.event.addListener(map, "center_changed", function() {
    $("#controls div.Location.Center")
      .find("a.Lat").text(map.getCenter().lat().toFixed(4)).end()
      .find("a.Lng").text(map.getCenter().lng().toFixed(4));
  });
  google.maps.event.addListener(map, "zoom_changed", function() {
    $("#controls div.Location.Center")
      .find("a.Zoom").text(map.getZoom());
  });
  var firstUpdateListener = google.maps.event.addListener(map, "tilesloaded", function() {
    google.maps.event.trigger(map, "bounds_changed");
    google.maps.event.trigger(map, "center_changed");
    google.maps.event.trigger(map, "zoom_changed");
    google.maps.event.removeListener(firstUpdateListener);
  });
  google.maps.event.addListener(map, "rightclick", function(event) {
    updateMarker(event.latLng.lat(), event.latLng.lng());
  });

  $("#controls div.TeamLogo button.Remove").click(function() {
    $(this).parent().remove();
  });
  $("#controls div.TeamLogo button.Add").click(function() {
    $("#controls div.TeamLogo div.FileBox.Template").clone(true, true).removeClass("Template")
      .appendTo("#controls div.TeamLogo");
  });
});

function createMap() {
  // This centers on the so-called "geographic center" of the (contiguous) USA
  var mapOptions = {
    center: new google.maps.LatLng(39.8282,-98.5795),
    zoom: 4,
    mapTypeId: google.maps.MapTypeId.ROADMAP
  };
  return new google.maps.Map($("#mapCanvas")[0], mapOptions);
}

var marker;

function updateMarker(lat, lng) {
  if (marker) {
    marker.setMap(null);
  }
  marker = new google.maps.Marker({
    map: map,
    position: new google.maps.LatLng(lat, lng),
    draggable: true
  });
  marker.addListener("position_changed", updateMarkerLoc);
  updateMarkerLoc();
  $("#checklist").addClass("MarkerSet");
}

function updateMarkerLoc() {
  if (marker) {
    $("#controls div.Location.Marker").addClass("Set")
      .find("a.Lat").text(marker.getPosition().lat().toFixed(4)).end()
      .find("a.Lng").text(marker.getPosition().lng().toFixed(4));
  }
}
