package com.carolinarollergirls.scoreboard.jetty;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;

import org.jdom.*;

import org.apache.commons.io.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.fileupload.servlet.*;

public class MediaServlet extends DefaultScoreBoardControllerServlet
{
  public MediaServlet() {
    mediaElementNameMap.put("images", "Images");
    mediaElementNameMap.put("videos", "Videos");
    mediaElementNameMap.put("customhtml", "CustomHtml");
    mediaChildNameMap.put("images", "Image");
    mediaChildNameMap.put("videos", "Video");
    mediaChildNameMap.put("customhtml", "Html");
  }

  public String getPath() { return "/Media"; }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    super.doPost(request, response);

    if (request.getPathInfo().equals("/upload"))
      upload(request, response);
    else
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    super.doGet(request, response);

    if (request.getPathInfo().equals("/list"))
      list(request, response);
    else
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  protected void list(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    File typeDir = getTypeDir(request.getParameter("media"), request.getParameter("type"), response, false);
    if (null == typeDir)
      return;

    StringBuffer fileList = new StringBuffer("");
    Iterator<File> files = Arrays.asList(typeDir.listFiles()).iterator();
    while (files.hasNext()) {
      File f = files.next();
      if (f.isFile())
        fileList.append(f.getName()+"\n");
    }

    response.setContentType("text/plain");
    response.getWriter().println(fileList);
    response.setStatus(HttpServletResponse.SC_OK);
  }

  protected void upload(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    try {
      if (!ServletFileUpload.isMultipartContent(request)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

      String media = null, type = null, name = null;
      FileItemFactory fiF = new DiskFileItemFactory();
      ServletFileUpload sfU = new ServletFileUpload(fiF);
      List<FileItem> fileItems = new LinkedList<FileItem>();
      Iterator i = sfU.parseRequest(request).iterator();

      while (i.hasNext()) {
        FileItem item = (FileItem)i.next();
        if (item.isFormField()) {
          if (item.getFieldName().equals("name"))
            name = item.getString();
          else if (item.getFieldName().equals("media"))
            media = item.getString();
          else if (item.getFieldName().equals("type"))
            type = item.getString();
        } else if (item.getName().matches("^.*[.][zZ][iI][pP]$")) {
          processZipFileItem(fiF, item, fileItems);
        } else {
          fileItems.add(item);
        }
      }

      if (fileItems.size() == 0) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No files provided to upload");
        return;
      }

      // Ignore any provided name if more than 1 uploaded file; use filenames
      if (fileItems.size() > 1)
        name = null;

      File typeDir = getTypeDir(media, type, response, true);
      if (null == typeDir)
        return;

      Iterator<FileItem> fI = fileItems.iterator();
      while (fI.hasNext()) {
        FileItem item = fI.next();
        if (!item.isFormField()) {
          File f = createFile(typeDir, item, response);
          if (null == f || !registerFile(f, media, type, name, response))
            return;
        }
      }

      int len = fileItems.size();
      response.setContentType("text/plain");
      response.getWriter().println("Successfully uploaded "+len+" file"+(len>1?"s":""));
      response.setStatus(HttpServletResponse.SC_OK);
    } catch ( FileUploadException fuE ) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, fuE.getMessage());
    }
  }

  protected boolean checkMediaTypeParams(String media, String type, HttpServletResponse response) throws IOException {
    if (null == media) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing media parameter");
      return false;
    }
    if (null == type) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing type parameter");
      return false;
    }
    if (!mediaElementNameMap.containsKey(media)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid media path '"+media+"'");
      return false;
    }
    if (type.matches(invalidTypePathRegex)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid type path '"+type+"'");
      return false;
    }
    return true;
  }

  protected File getTypeDir(String media, String type, HttpServletResponse response, boolean createTypeDir) throws IOException {
    if (!checkMediaTypeParams(media, type, response))
      return null;

    File htmlDir = new File(htmlDirName);
    if (!htmlDir.exists() || !htmlDir.isDirectory()) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not find html dir");
      return null;
    }
    File mediaDir = new File(htmlDir, media);
    if (!mediaDir.exists() || !mediaDir.isDirectory()) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not find media '"+media+"' dir");
      return null;
    }
    File typeDir = new File(mediaDir, type);
    if (!typeDir.exists() || !typeDir.isDirectory()) {
      if (createTypeDir) {
        if (!typeDir.mkdir()) {
          response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create type '"+type+"' dir");
          return null;
        }
      } else {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find type '"+type+"' dir");
        return null;
      }
    }
    return typeDir;
  }

  protected File createFile(File typeDir, FileItem item, HttpServletResponse response) throws IOException {
    File f = new File(typeDir, item.getName());
    FileOutputStream fos = null;
    InputStream is = item.getInputStream();
    try {
      fos = new FileOutputStream(f);
      IOUtils.copyLarge(is, fos);
      return f;
    } catch ( FileNotFoundException fnfE ) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create file '"+item.getName()+"'");
      return null;
    } finally {
      is.close();
      if (null != fos)
        fos.close();
    }
  }

  protected boolean registerFile(File f, String media, String type, String name, HttpServletResponse response) throws IOException {
    if (!checkMediaTypeParams(media, type, response))
      return false;

    Document d = editor.createDocument();
    Element mediaE = editor.addElement(d.getRootElement(), mediaElementNameMap.get(media));
    Element typeE = editor.addElement(mediaE, "Type", type);
    Element fileE = editor.addElement(typeE, mediaChildNameMap.get(media), f.getName());
    editor.addElement(fileE, "Name", null, (null==name?f.getName():name));
    editor.addElement(fileE, "Src", null, "/"+media+"/"+type+"/"+f.getName());
    scoreBoardModel.getXmlScoreBoard().mergeDocument(d);
    return true;
  }

  protected void processZipFileItem(FileItemFactory factory, FileItem zip, List<FileItem> fileItems) throws IOException {
    ZipInputStream ziS = new ZipInputStream(zip.getInputStream());
    ZipEntry zE;
    try {
      while (null != (zE = ziS.getNextEntry())) {
        FileItem item = factory.createItem(null, null, false, zE.getName());
        OutputStream oS = item.getOutputStream();
        IOUtils.copyLarge(ziS, oS);
        oS.close();
        fileItems.add(item);
      }
    } finally {
      ziS.close();
    }
  }

  private XmlDocumentEditor editor = new XmlDocumentEditor();

  private String htmlDirName = ScoreBoardManager.getProperties().getProperty(JettyServletScoreBoardController.PROPERTY_HTML_DIR_KEY);

  public static final Map<String,String> mediaElementNameMap = new ConcurrentHashMap<String,String>();
  public static final Map<String,String> mediaChildNameMap = new ConcurrentHashMap<String,String>();
  public static final String invalidTypePathRegex = "/|\\|[.][.]";
}
