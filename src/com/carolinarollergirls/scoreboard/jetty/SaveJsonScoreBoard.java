package com.carolinarollergirls.scoreboard.jetty;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.jr.ob.JSON;

import com.carolinarollergirls.scoreboard.json.JSONStateManager;

public class SaveJsonScoreBoard extends HttpServlet {

    public SaveJsonScoreBoard(JSONStateManager jsm) { this.jsm = jsm; }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        // Use a TreeMap to keep output sorted.
        Map<String, Object> state = new TreeMap<>(jsm.getState());

        String path = request.getParameter("path");
        if (path != null) {
            List<String> prefixes = Arrays.asList(path.split(","));
            Iterator<String> it = state.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                boolean keep = key.startsWith("ScoreBoard.Version");
                for (String prefix : prefixes) {
                    if (key.startsWith(prefix)) { keep = true; }
                }
                if (!keep) { it.remove(); }
            }
        }
        // Users may use saves to share with the world, so remove secrets.
        Iterator<String> it = state.keySet().iterator();
        while (it.hasNext()) {
            if (it.next().endsWith("Secret")) { it.remove(); }
        }

        if (state.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No objects found.");
        } else {
            response.setContentType("application/json");
            String json = JSON.std.with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                              .composeString()
                              .startObject()
                              .putObject("state", state)
                              .end()
                              .finish();

            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Expires", "-1");
            response.setCharacterEncoding("utf-8");
            response.getOutputStream().print(json);
            response.getOutputStream().flush();
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    protected final JSONStateManager jsm;
}
