package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;

public class MediaServlet extends DefaultScoreBoardControllerServlet {
    public MediaServlet() {
    }

    public String getPath() { return "/Media"; }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        super.doPost(request, response);

        if (request.getPathInfo().equals("/upload")) {
            upload(request, response);
        } else if (request.getPathInfo().equals("/remove")) {
            remove(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        super.doGet(request, response);

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    protected void upload(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        try {
            if (!ServletFileUpload.isMultipartContent(request)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            String media = null, type = null;
            FileItemFactory fiF = new DiskFileItemFactory();
            ServletFileUpload sfU = new ServletFileUpload(fiF);
            List<FileItem> fileItems = new LinkedList<FileItem>();
            Iterator<?> i = sfU.parseRequest(request).iterator();

            while (i.hasNext()) {
                FileItem item = (FileItem)i.next();
                if (item.isFormField()) {
                    if (item.getFieldName().equals("media")) {
                        media = item.getString();
                    } else if (item.getFieldName().equals("type")) {
                        type = item.getString();
                    }
                } else if (item.getName().matches(zipExtRegex)) {
                    processZipFileItem(fiF, item, fileItems);
                } else if (scoreBoardModel.getMediaModel().validFileName(item.getName())) {
                    fileItems.add(item);
                }
            }

            if (fileItems.size() == 0) {
                setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, "No files provided to upload");
                return;
            }

            processFileItemList(fileItems, media, type);

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

    protected void remove(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        String media = request.getParameter("media");
        String type = request.getParameter("type");
        String filename = request.getParameter("filename");

        boolean success = scoreBoardModel.getMediaModel().removeMediaFile(media, type, filename);
        if (!success) {
            setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Failed to remove file");
        } else {
            setTextResponse(response, HttpServletResponse.SC_OK, "Successfully removed");
        }
    }

    protected void processFileItemList(List<FileItem> fileItems, String media, String type) throws FileNotFoundException,IOException {
        File typeDir = getTypeDir(media, type);

        ListIterator<FileItem> fileItemIterator = fileItems.listIterator();
        while (fileItemIterator.hasNext()) {
            FileItem item = fileItemIterator.next();
            if (item.isFormField()) {
                fileItemIterator.remove();
            } else {
                createFile(typeDir, item);
            }
        }
    }

    protected File getTypeDir(String media, String type) throws FileNotFoundException,IllegalArgumentException {
        if (scoreBoardModel.getMedia().getMediaFiles(media, type) == null) {
            throw new IllegalArgumentException("Invalid media '"+media+"' or type '"+type+"'");
        }

        File htmlDir = new File(htmlDirName);
        File mediaDir = new File(htmlDir, media);
        File typeDir = new File(mediaDir, type);
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
            if (null != fos) {
                fos.close();
            }
        }
    }

    protected void processZipFileItem(FileItemFactory factory, FileItem zip, List<FileItem> fileItems) throws IOException {
        ZipInputStream ziS = new ZipInputStream(zip.getInputStream());
        ZipEntry zE;
        try {
            while (null != (zE = ziS.getNextEntry())) {
                if (zE.isDirectory() || !scoreBoardModel.getMediaModel().validFileName(zE.getName())) {
                    continue;
                }
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

    protected String htmlDirName = ScoreBoardManager.getDefaultPath() + File.separator + "html";

    public static final String zipExtRegex = "^.*[.][zZ][iI][pP]$";
}
