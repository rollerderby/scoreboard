$(function() {


  var deviceTable = $("#devices");

  function getDeviceTr(id) {
    var tr = deviceTable.find("[deviceId='"+ id +"']");
    if (tr.length == 0) {
      tr = $("<tr>").attr("deviceId", id)
        .append("<td class='Name'>")
        .append($("<td class='Clients'>").append("<table>"))
        .appendTo(deviceTable);
    }
    return tr;
  }

	WS.Register(["ScoreBoard.Clients.Device(*)"], function(k, v) {
    var id = k.Device;
    if (v == null) {
      deviceTable.find("tr[deviceId='"+ id +"']").remove();
      return;
    }

    var tr = getDeviceTr(id);

    switch (k.field) {
      case "Name":
        tr.children("td.Name").text(v);
        break;
    }
  });

	WS.Register(["ScoreBoard.Clients.Client(*)"], function(k, v) {
    var id = k.Client;
    if (v == null) {
      deviceTable.find("tr[clientId='"+ id +"']").remove();
      return;
    }

    var deviceId = WS.state["ScoreBoard.Clients.Client(" + id + ").DeviceId"];
    var clientsTable = getDeviceTr(deviceId).find("td.Clients table");
    var tr = clientsTable.find("tr[clientId='"+ id +"']");
    if (tr.length == 0) {
      tr = $("<tr>").attr("clientId", id)
        .append("<td class='Source'>")
        .append("<td class='RemoteAddr'>")
        .appendTo(clientsTable);
    }

    switch (k.field) {
      case "RemoteAddr":
        tr.children(".RemoteAddr").text(v);
        break;
      case "Source":
        tr.children(".Source").text(v);
        break;

    }
  });


  WS.AutoRegister();
  WS.Connect();
});

//# sourceURL=settings\client\index.js
