package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.*;
import java.net.*;
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
    else if (request.getPathInfo().equals("/localversion"))
      localVersion(request, response);
    else if (request.getPathInfo().equals("/latestversion"))
      latestVersion(request, response);
    else if (request.getPathInfo().equals("/update"))
      update(request, response);
    else
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  protected void localVersion(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    FileInputStream fos = null;
    try {
      File typeDir = getTypeDir(request.getParameter("media"), request.getParameter("type"), response, false);

      fos = new FileInputStream(new File(typeDir, versionFilename));
      String version = getVersion(fos);

      setTextResponse(response, HttpServletResponse.SC_OK, version);
    } catch ( FileNotFoundException fnfE ) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch ( IllegalArgumentException iaE ) {
      setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, iaE.getMessage());
    } finally {
      if (null != fos)
        fos.close();
    }
  }

  protected void latestVersion(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    String media = request.getParameter("media");
    String type = request.getParameter("type");

    try {
      setTextResponse(response, HttpServletResponse.SC_OK, getLatestVersion(media, type));
    } catch ( IllegalArgumentException iaE ) {
      setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, iaE.getMessage());
    }
  }

  protected String getLatestVersion(String media, String type) throws IllegalArgumentException,IOException {
    InputStream is = null;
    try {
      is = getMediaURL(BASE_NAME+"-"+media+"-"+type+"-latest-version").openStream();
      return getVersion(is);
    } finally {
      if (null != is)
        is.close();
    }    
  }

  protected String getVersion(InputStream is) throws IllegalArgumentException,IOException {
    Properties p = new Properties();
    p.load(is);
    return p.getProperty("version");
  }

  protected void update(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    String media = request.getParameter("media");
    String type = request.getParameter("type");
    FileItemFactory fiF = new DiskFileItemFactory();
    InputStream is = null;
    OutputStream os = null;

    try {
      String name = BASE_NAME+"-"+media+"-"+type+"-"+getLatestVersion(media, type)+".zip";

      is = getMediaURL(name).openStream();

      FileItem item = fiF.createItem(null, null, false, name);
      os = item.getOutputStream();
      IOUtils.copyLarge(is, os);

      List<FileItem> fileItems = new ArrayList<FileItem>();

      processZipFileItem(fiF, item, fileItems);

      processFileItemList(fileItems, media, type, null, response);

      int len = fileItems.size();
      setTextResponse(response, HttpServletResponse.SC_OK, "Successfully updated "+len+" file"+(len>1?"s":"")+" from "+name);
    } catch ( FileNotFoundException fnfE ) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch ( IllegalArgumentException iaE ) {
      setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, iaE.getMessage());
    } finally {
      if (null != is)
        is.close();
      if (null != os)
        os.close();
    }
  }

  protected void list(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    try {
      File typeDir = getTypeDir(request.getParameter("media"), request.getParameter("type"), response, false);

      StringBuffer fileList = new StringBuffer("");
      Iterator<File> files = Arrays.asList(typeDir.listFiles(listFilenameFilter)).iterator();
      while (files.hasNext()) {
        File f = files.next();
        if (f.isFile())
          fileList.append(f.getName()+"\n");
      }

      setTextResponse(response, HttpServletResponse.SC_OK, fileList.toString());
    } catch ( FileNotFoundException fnfE ) {
      setTextResponse(response, HttpServletResponse.SC_OK, "");
    } catch ( IllegalArgumentException iaE ) {
      setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, iaE.getMessage());
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
        } else if (item.getName().matches(zipExtRegex)) {
          processZipFileItem(fiF, item, fileItems);
        } else if (uploadFilenameFilter.accept(null, item.getName())) {
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

      processFileItemList(fileItems, media, type, name, response);

      int len = fileItems.size();
      setTextResponse(response, HttpServletResponse.SC_OK, "Successfully uploaded "+len+" file"+(len>1?"s":""));
    } catch ( FileNotFoundException fnfE ) {
      setTextResponse(response, HttpServletResponse.SC_NOT_FOUND, fnfE.getMessage());
    } catch ( IllegalArgumentException iaE ) {
      setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, iaE.getMessage());
    } catch ( FileUploadException fuE ) {
      setTextResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, fuE.getMessage());
    }
  }

  protected void processFileItemList(List<FileItem> fileItems, String media, String type, String name, HttpServletResponse response) throws FileNotFoundException,IOException {
    File typeDir = getTypeDir(media, type, response, true);

    ListIterator<FileItem> fileItemIterator = fileItems.listIterator();
    while (fileItemIterator.hasNext()) {
      FileItem item = fileItemIterator.next();
      if (item.isFormField()) {
        fileItemIterator.remove();
      } else {
        File f = createFile(typeDir, item, response);
        if (!item.getName().equals(versionFilename))
          registerFile(f, media, type, name, response);
        else
          fileItemIterator.remove();
      }
    }
  }

  protected void checkMediaTypeParams(String media, String type, HttpServletResponse response) throws IllegalArgumentException {
    if (null == media)
      throw new IllegalArgumentException("Missing media parameter");
    if (null == type)
      throw new IllegalArgumentException("Missing type parameter");
    if (!mediaElementNameMap.containsKey(media))
      throw new IllegalArgumentException("Invalid media path '"+media+"'");
    if (type.matches(invalidTypePathRegex))
      throw new IllegalArgumentException("Invalid type path '"+type+"'");
  }

  protected File getTypeDir(String media, String type, HttpServletResponse response, boolean createTypeDir) throws FileNotFoundException,IllegalArgumentException {
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

  protected void registerFile(File f, String media, String type, String name, HttpServletResponse response) throws IOException,IllegalArgumentException {
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
        if (zE.isDirectory() || !uploadFilenameFilter.accept(null, zE.getName()))
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

  protected URL getMediaURL(String filename) throws MalformedURLException {
    return new URL(BASE_URL+filename+"/download");
  }

  protected XmlDocumentEditor editor = new XmlDocumentEditor();

  protected String htmlDirName = ScoreBoardManager.getProperty(JettyServletScoreBoardController.PROPERTY_HTML_DIR_KEY);

  public static final String BASE_URL = "http://sourceforge.net/projects/derbyscoreboard/files/crg-scoreboard/media/";
  public static final String BASE_NAME = "crg-scoreboard";

  protected static final Map<String,String> mediaElementNameMap = new ConcurrentHashMap<String,String>();
  protected static final Map<String,String> mediaChildNameMap = new ConcurrentHashMap<String,String>();

  public static final String invalidTypePathRegex = "/|\\|[.][.]";

  public static final String versionFilename = ".version";

  public static final String zipExtRegex = "^.*[.][zZ][iI][pP]$";
  public static final String dbFileRegex = "^.*[.][dD][bB]$";
  public static final String dotFileRegex = "^[.].*$";

  public static final FilenameFilter listFilenameFilter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return (!name.matches(dbFileRegex+"|"+dotFileRegex));
      }
    };
  public static final FilenameFilter uploadFilenameFilter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return (!name.matches(dbFileRegex) && (!name.matches(dotFileRegex) || name.equals(versionFilename)));
      }
    };
}
