$(function() {


  var deviceTable = $("#devices");

  function getDeviceTr(id) {
    var tr = deviceTable.find("[deviceId='"+ id +"']");
    if (tr.length == 0) {
      tr = $("<tr>").attr("deviceId", id)
        .append("<td class='Name'>")
        .append("<td class='Clients'>")
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
      deviceTable.find("div[clientId='"+ id +"']").remove();
      return;
    }

    var deviceId = WS.state["ScoreBoard.Clients.Client(" + id + ").DeviceId"];
    var clientsTd = getDeviceTr(deviceId).children("td.Clients");
    var div = clientsTd.children("[clientId='"+ id +"']");
    if (div.length == 0) {
      div = $("<div>").attr("clientId", id)
        .append("<span class='URL'>")
        .append("<span class='RemoteAddr'>")
        .appendTo(clientsTd);
    }

    switch (k.field) {
      case "RemoteAddr":
        div.children(".RemoteAddr").text(v);
        break;
    }
  });


  WS.AutoRegister();
  WS.Connect();
});

//# sourceURL=settings\client\index.js
