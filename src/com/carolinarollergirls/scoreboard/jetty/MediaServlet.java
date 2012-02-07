package com.carolinarollergirls.scoreboard.jetty;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;

import javax.servlet.*;
import javax.servlet.http.*;

import java.security.InvalidParameterException;

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
    try {
      File typeDir = getTypeDir(request.getParameter("media"), request.getParameter("type"), response, false);

      StringBuffer fileList = new StringBuffer("");
      Iterator<File> files = Arrays.asList(typeDir.listFiles()).iterator();
      while (files.hasNext()) {
        File f = files.next();
        if (f.isFile())
          fileList.append(f.getName()+"\n");
      }

      setTextResponse(response, HttpServletResponse.SC_OK, fileList.toString());
    } catch ( FileNotFoundException fnfE ) {
      setTextResponse(response, HttpServletResponse.SC_OK, "");
    } catch ( InvalidParameterException ipE ) {
      setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, ipE.getMessage());
    }
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
        setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, "No files provided to upload");
        return;
      }

      // Ignore any provided name if more than 1 uploaded file; use filenames
      if (fileItems.size() > 1)
        name = null;

      File typeDir = getTypeDir(media, type, response, true);

      Iterator<FileItem> fI = fileItems.iterator();
      while (fI.hasNext()) {
        FileItem item = fI.next();
        if (!item.isFormField())
          registerFile(createFile(typeDir, item, response), media, type, name, response);
      }

      int len = fileItems.size();
      setTextResponse(response, HttpServletResponse.SC_OK, "Successfully uploaded "+len+" file"+(len>1?"s":""));
    } catch ( FileNotFoundException fnfE ) {
      setTextResponse(response, HttpServletResponse.SC_NOT_FOUND, fnfE.getMessage());
    } catch ( InvalidParameterException ipE ) {
      setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, ipE.getMessage());
    } catch ( FileUploadException fuE ) {
      setTextResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, fuE.getMessage());
    }
  }

  protected void checkMediaTypeParams(String media, String type, HttpServletResponse response) throws InvalidParameterException {
    if (null == media)
      throw new InvalidParameterException("Missing media parameter");
    if (null == type)
      throw new InvalidParameterException("Missing type parameter");
    if (!mediaElementNameMap.containsKey(media))
      throw new InvalidParameterException("Invalid media path '"+media+"'");
    if (type.matches(invalidTypePathRegex))
      throw new InvalidParameterException("Invalid type path '"+type+"'");
  }

  protected File getTypeDir(String media, String type, HttpServletResponse response, boolean createTypeDir) throws FileNotFoundException,InvalidParameterException {
    checkMediaTypeParams(media, type, response);

    File htmlDir = new File(htmlDirName);
    if (!htmlDir.exists() || !htmlDir.isDirectory())
      throw new FileNotFoundException("Could not find html dir");
    File mediaDir = new File(htmlDir, media);
    if (!mediaDir.exists() || !mediaDir.isDirectory())
      throw new FileNotFoundException("Could not find '"+media+"' dir");
    File typeDir = new File(mediaDir, type);
    if (!typeDir.exists() || !typeDir.isDirectory()) {
      if (!createTypeDir)
        throw new FileNotFoundException("Could not find '"+type+"' dir");
      if (!typeDir.mkdir())
        throw new FileNotFoundException("Could not create '"+type+"' dir");
    }
    return typeDir;
  }

  protected File createFile(File typeDir, FileItem item, HttpServletResponse response) throws IOException,FileNotFoundException {
    File f = new File(typeDir, item.getName());
    FileOutputStream fos = null;
    InputStream is = item.getInputStream();
    try {
      fos = new FileOutputStream(f);
      IOUtils.copyLarge(is, fos);
      return f;
    } finally {
      is.close();
      if (null != fos)
        fos.close();
    }
  }

  protected void registerFile(File f, String media, String type, String name, HttpServletResponse response) throws IOException,InvalidParameterException {
    checkMediaTypeParams(media, type, response);

    Document d = editor.createDocument();
    Element mediaE = editor.addElement(d.getRootElement(), mediaElementNameMap.get(media));
    Element typeE = editor.addElement(mediaE, "Type", type);
    Element fileE = editor.addElement(typeE, mediaChildNameMap.get(media), f.getName());
    editor.addElement(fileE, "Name", null, (null==name?f.getName():name));
    editor.addElement(fileE, "Src", null, "/"+media+"/"+type+"/"+f.getName());
    scoreBoardModel.getXmlScoreBoard().mergeDocument(d);
  }

  protected void processZipFileItem(FileItemFactory factory, FileItem zip, List<FileItem> fileItems) throws IOException {
    ZipInputStream ziS = new ZipInputStream(zip.getInputStream());
    ZipEntry zE;
    try {
      while (null != (zE = ziS.getNextEntry())) {
        if (zE.isDirectory())
          continue;
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
