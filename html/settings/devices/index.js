$(function() {


  var deviceTable = $(".Devices");

  function getDeviceTbody(id) {
    var tbody = deviceTable.children("[deviceId='"+ id +"']");
    if (tbody.length == 0) {
      var name = WS.state["ScoreBoard.Clients.Device(" + id + ").Name"];
      tbody = $("<tbody>").attr("deviceId", id).attr("name", name)
        .append($("<tr>").append("<td class='Name' rowspan='0'>")
          .append("<td class='Blank'>").append("<td class='Blank'>")
          .append("<td class='LastSeen' rowspan='0'>")
          .append("<td class='Created'>"));
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

    var tr = getDeviceTbody(id).children().first();

    switch (k.field) {
      case "Name":
        tr.children("td.Name").text(v);
        break;
      case "Accessed":
        updateAge(tr.children("td.LastSeen").attr("age", v).attr("title", new Date(v)));
        break;
      case "Created":
        updateAge(tr.children("td.Created").attr("age", v).attr("title", new Date(v)));
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
      var created = WS.state["ScoreBoard.Clients.Client(" + id + ").Created"];
      tr = $("<tr>").attr("clientId", id).attr("created", created)
        .append("<td class='Source'>")
        .append("<td class='RemoteAddr'>")
        .append("<td class='Created'>");
        _windowFunctions.appendAlphaSortedByAttr(tbody, tr, "created", 1)
    }

    switch (k.field) {
      case "RemoteAddr":
        tr.children(".RemoteAddr").text(v);
        break;
      case "Source":
        tr.children(".Source").text(v);
        break;
      case "Created":
        updateAge(tr.children("td.Created").attr("age", v).attr("title", new Date(v)));
        break;

    }
  });

  function updateAge(e, now) {
    if (!now) {
      now = new Date().getTime();
    }
    var t = (now - e.attr("age")) / 1000;
    if (t < 60) {
      e.text(Math.floor(t) + "s");
    } else if (t < 3600) {
      e.text(Math.floor(t / 60) + "m");
    } else if (t < 86400) {
      e.text(Math.floor(t / 3600) + "h");
    } else {
      e.text(Math.floor(t / 86400) + "d");
    }
  }

  setInterval(function(){
    var now = new Date().getTime()
    $.each(deviceTable.find("td[age]"), function(e) {
      updateAge($(this), now);
    });
  }, 1000);


  WS.AutoRegister();
  WS.Connect();
});

//# sourceURL=settings\client\index.js
