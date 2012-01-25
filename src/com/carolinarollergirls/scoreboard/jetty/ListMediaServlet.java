package com.carolinarollergirls.scoreboard.jetty;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.carolinarollergirls.scoreboard.*;

public class ListMediaServlet extends DefaultScoreBoardControllerServlet
{
  public String getPath() { return "/listmedia"; }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    super.doPost(request, response);
    response.sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    super.doGet(request, response);

    String media = request.getParameter("media");
    boolean valid = false;
    for (int i=0; i<validMedia.length; i++) {
      if (validMedia[i].equals(media)) {
        valid = true;
        break;
      }
    }
    if (!valid) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    String type = request.getParameter("type");

    File htmlDir = new File(htmlDirName);
    if (!htmlDir.exists() || !htmlDir.isDirectory()) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }
    File mediaDir = new File(htmlDir, media+"/"+type);
    if (!mediaDir.exists() || !mediaDir.isDirectory()) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    StringBuffer fileList = new StringBuffer("");
    Iterator<File> mediaFiles = Arrays.asList(mediaDir.listFiles()).iterator();
    while (mediaFiles.hasNext()) {
      File f = mediaFiles.next();
      if (f.isFile())
        fileList.append(f.getName()+"\n");
    }

    response.setContentType("text/plain");
    response.getWriter().println(fileList);
    response.setStatus(HttpServletResponse.SC_OK);
  }

  private String htmlDirName = ScoreBoardManager.getProperties().getProperty(JettyServletScoreBoardController.PROPERTY_HTML_DIR_KEY);

  public static final String[] validMedia = { "images", "videos", "customhtml" };
}
