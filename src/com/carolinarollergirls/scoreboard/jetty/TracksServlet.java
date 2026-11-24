package com.carolinarollergirls.scoreboard.jetty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.jr.ob.JSON;

import com.carolinarollergirls.scoreboard.core.interfaces.Clients.Device;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Tracks.TracksException;
import com.carolinarollergirls.scoreboard.utils.Logger;

/** Requests from the scoreboards of the tracks, when this scoreboard is the tournament server. */
public class TracksServlet extends HttpServlet {
    public TracksServlet(ScoreBoard sb) { scoreBoard = sb; }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        answer(request, response, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Device device = scoreBoard.getClients().getOrAddDevice(request.getSession().getId());
        if (!device.mayWrite()) {
            error(response, HttpServletResponse.SC_FORBIDDEN,
                  "No write access on the tournament server (Devices page).");
            return;
        }
        device.write();
        String text = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        Map<String, Object> body;
        try {
            body = text.isEmpty() ? new HashMap<String, Object>() : JSON.std.mapFrom(text);
        } catch (IOException e) {
            error(response, HttpServletResponse.SC_BAD_REQUEST, "Not JSON: " + e.getMessage());
            return;
        }
        answer(request, response, body);
    }

    private void answer(HttpServletRequest request, HttpServletResponse response, Map<String, Object> body)
        throws IOException {
        Device device = scoreBoard.getClients().getOrAddDevice(request.getSession().getId());
        device.access();
        String action = request.getPathInfo() == null ? "" : request.getPathInfo().replaceFirst("^/", "");
        Map<String, String> params = new HashMap<>();
        for (String name : request.getParameterMap().keySet()) { params.put(name, request.getParameter(name)); }
        try {
            Map<String, Object> result = scoreBoard.getTracks().handle(action, params, body);
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Cache-Control", "no-cache");
            response.getOutputStream().write(JSON.std.asString(result).getBytes(StandardCharsets.UTF_8));
        } catch (TracksException e) { error(response, e.getStatus(), e.getMessage()); } catch (Exception e) {
            Logger.printStackTrace("answering a track", e);
            error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
        }
    }

    private void error(HttpServletResponse response, int status, String message) throws IOException {
        Map<String, Object> e = new HashMap<>();
        e.put("error", message);
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.getOutputStream().write(JSON.std.asString(e).getBytes(StandardCharsets.UTF_8));
    }

    private final ScoreBoard scoreBoard;
}
