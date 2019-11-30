$(function() {


  var deviceTable = $(".Devices");

  function getDeviceTbody(id) {
    var tbody = deviceTable.children("[deviceId='"+ id +"']");
    if (tbody.length == 0) {
      var name = WS.state["ScoreBoard.Clients.Device(" + id + ").Name"];
      tbody = $("<tbody>").attr("deviceId", id).attr("name", name)
        .append($("<tr>").append("<td class='Name' rowspan='0'>")
        .append("<td class='Created'>&nbsp;</td>")
        .append("<td>").append("<td>"));
      _windowFunctions.appendAlphaSortedByAttr(deviceTable, tbody, "name", 1)
    }
    return tbody;
  }

	WS.Register(["ScoreBoard.Clients.Device(*)"], function(k, v) {
    var id = k.Device;
    if (v == null) {
      deviceTable.children("tbody[deviceId='"+ id +"']").remove();
      return;
    }

    var tbody = getDeviceTbody(id);

    switch (k.field) {
      case "Name":
        tbody.find("td.Name").text(v);
        break;
    }
  });

	WS.Register(["ScoreBoard.Clients.Client(*)"], function(k, v) {
    var id = k.Client;
    if (v == null) {
      deviceTable.find("tr[clientId='"+ id +"']").remove();
      return;
    }

    var tbody = getDeviceTbody(WS.state["ScoreBoard.Clients.Client(" + id + ").DeviceId"]);
    var tr = tbody.children("tr[clientId='"+ id +"']");
    if (tr.length == 0) {
      tr = $("<tr>").attr("clientId", id)
        .append("<td class='Source'>")
        .append("<td class='RemoteAddr'>")
        .append("<td class='Created'>");
        _windowFunctions.appendAlphaSortedByAttr(tbody, tr, "clientId", 1)
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
