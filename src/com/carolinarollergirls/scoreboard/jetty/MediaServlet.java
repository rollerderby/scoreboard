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
import org.apache.commons.io.filefilter.*;

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
		else if (request.getPathInfo().equals("/download"))
			download(request, response);
		else if (request.getPathInfo().equals("/remove"))
			remove(request, response);
		else
			response.sendError(response.SC_NOT_FOUND);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		super.doGet(request, response);

		response.sendError(response.SC_NOT_FOUND);
	}

	protected void upload(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		try {
			if (!ServletFileUpload.isMultipartContent(request)) {
				response.sendError(response.SC_BAD_REQUEST);
				return;
			}

			String media = null, type = null;
			FileItemFactory fiF = new DiskFileItemFactory();
			ServletFileUpload sfU = new ServletFileUpload(fiF);
			List<FileItem> fileItems = new LinkedList<FileItem>();
			Iterator i = sfU.parseRequest(request).iterator();

			while (i.hasNext()) {
				FileItem item = (FileItem)i.next();
				if (item.isFormField()) {
					if (item.getFieldName().equals("media"))
						media = item.getString();
					else if (item.getFieldName().equals("type"))
						type = item.getString();
				} else if (item.getName().matches(zipExtRegex)) {
					processZipFileItem(fiF, item, fileItems);
				} else if (uploadFileNameFilter.accept(null, item.getName())) {
					fileItems.add(item);
				}
			}

			if (fileItems.size() == 0) {
				setTextResponse(response, response.SC_BAD_REQUEST, "No files provided to upload");
				return;
			}

			processFileItemList(fileItems, media, type);

			int len = fileItems.size();
			setTextResponse(response, response.SC_OK, "Successfully uploaded "+len+" file"+(len>1?"s":""));
		} catch ( FileNotFoundException fnfE ) {
			setTextResponse(response, response.SC_NOT_FOUND, fnfE.getMessage());
		} catch ( IllegalArgumentException iaE ) {
			setTextResponse(response, response.SC_BAD_REQUEST, iaE.getMessage());
		} catch ( FileUploadException fuE ) {
			setTextResponse(response, response.SC_INTERNAL_SERVER_ERROR, fuE.getMessage());
		}
	}

	protected void download(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		String media = request.getParameter("media");
		String type = request.getParameter("type");

		try {
			URL url = new URL(request.getParameter("url"));
			File typeDir = getTypeDir(media, type, true);
			String name = url.getPath().replaceAll("^([^/]*/)*", "");
			InputStream iS = url.openStream();
			OutputStream oS = new FileOutputStream(new File(typeDir, name));
			IOUtils.copyLarge(iS, oS);
			IOUtils.closeQuietly(iS);
			IOUtils.closeQuietly(oS);

			setTextResponse(response, response.SC_OK, "Successfully downloaded 1 remote file");
		} catch ( MalformedURLException muE ) {
			setTextResponse(response, response.SC_BAD_REQUEST, muE.getMessage());
		} catch ( IllegalArgumentException iaE ) {
			setTextResponse(response, response.SC_BAD_REQUEST, iaE.getMessage());
		} catch ( FileNotFoundException fnfE ) {
			setTextResponse(response, response.SC_BAD_REQUEST, fnfE.getMessage());
		}
	}

	protected void remove(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		String media = request.getParameter("media");
		String type = request.getParameter("type");
		String filename = request.getParameter("filename");

		try {
			File typeDir = getTypeDir(media, type, false);
			File f = new File(typeDir, filename);
			String path = f.getAbsolutePath();

			if (!f.exists())
				setTextResponse(response, response.SC_BAD_REQUEST, "File does not exist : "+path);
			else if (f.isDirectory())
				setTextResponse(response, response.SC_BAD_REQUEST, "Path is a directory : "+path);
			else if (!f.delete())
				setTextResponse(response, response.SC_BAD_REQUEST, "Could not delete file "+path);
			else
				setTextResponse(response, response.SC_OK, "Successfully removed "+path);
		} catch ( IllegalArgumentException iaE ) {
			setTextResponse(response, response.SC_BAD_REQUEST, iaE.getMessage());
		} catch ( FileNotFoundException fnfE ) {
			setTextResponse(response, response.SC_BAD_REQUEST, fnfE.getMessage());
		}
	}

	protected void processFileItemList(List<FileItem> fileItems, String media, String type) throws FileNotFoundException,IOException {
		File typeDir = getTypeDir(media, type, true);

		ListIterator<FileItem> fileItemIterator = fileItems.listIterator();
		while (fileItemIterator.hasNext()) {
			FileItem item = fileItemIterator.next();
			if (item.isFormField()) {
				fileItemIterator.remove();
			} else {
				File f = createFile(typeDir, item);
			}
		}
	}

	protected void checkMediaTypeParams(String media, String type) throws IllegalArgumentException {
		if (null == media)
			throw new IllegalArgumentException("Missing media parameter");
		if (null == type)
			throw new IllegalArgumentException("Missing type parameter");
		if (!mediaElementNameMap.containsKey(media))
			throw new IllegalArgumentException("Invalid media path '"+media+"'");
		if (type.matches(invalidTypePathRegex))
			throw new IllegalArgumentException("Invalid type path '"+type+"'");
	}

	protected File getTypeDir(String media, String type, boolean createTypeDir) throws FileNotFoundException,IllegalArgumentException {
		checkMediaTypeParams(media, type);

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

	protected File createFile(File typeDir, FileItem item) throws IOException,FileNotFoundException {
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

	protected void processZipFileItem(FileItemFactory factory, FileItem zip, List<FileItem> fileItems) throws IOException {
		ZipInputStream ziS = new ZipInputStream(zip.getInputStream());
		ZipEntry zE;
		try {
			while (null != (zE = ziS.getNextEntry())) {
				if (zE.isDirectory() || !uploadFileNameFilter.accept(null, zE.getName()))
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

	protected XmlDocumentEditor editor = new XmlDocumentEditor();

	protected String htmlDirName = ScoreBoardManager.getProperty(JettyServletScoreBoardController.PROPERTY_HTML_DIR_KEY);

	protected static final Map<String,String> mediaElementNameMap = new ConcurrentHashMap<String,String>();
	protected static final Map<String,String> mediaChildNameMap = new ConcurrentHashMap<String,String>();

	public static final String invalidTypePathRegex = "/|\\|[.][.]";

	public static final String zipExtRegex = "^.*[.][zZ][iI][pP]$";

	public static final IOFileFilter uploadFileNameFilter = MediaXmlDocumentManager.mediaFileNameFilter;
}
