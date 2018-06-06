package com.carolinarollergirls.scoreboard.gdata;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */
/*
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
*/
// Disable this incomplete class until we have time
// to update to the latest gdata code and complete
// this functionality.

/*
import com.google.gdata.util.*;
import com.google.gdata.data.*;
import com.google.gdata.client.*;
import com.google.gdata.data.docs.*;
import com.google.gdata.client.docs.*;
*/

public class GoogleDocs
{
/*
  public GoogleDocs() {
    docsService = new DocsService(APPLICATION_NAME);
  }

  public void login(String username, String password) throws AuthenticationException {
    docsService.setUserCredentials(username, password);
  }

  public SpreadsheetEntry createSpreadsheet() throws IOException,ServiceException,MalformedURLException {
    return createSpreadsheet(getScoreboardFolder(), createNameByTime());
  }

  public SpreadsheetEntry getSpreadsheet() { return spreadsheet; }

  protected String createNameByTime() {
    SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
    return format.format(new Date());
  }

  protected SpreadsheetEntry createSpreadsheet(DocumentListEntry folder, String name)
      throws IOException,ServiceException,MalformedURLException {
    SpreadsheetEntry spreadsheetEntry = new SpreadsheetEntry();
    spreadsheetEntry.setTitle(new PlainTextConstruct(name));
    return docsService.insert(new URL(((MediaContent)folder.getContent()).getUri()), spreadsheetEntry);
  }

  protected DocumentListEntry getScoreboardFolder() throws IOException,ServiceException,MalformedURLException {
//FIXME - probably need to do the "get rest of them" thing described here:
//http://code.google.com/apis/documents/docs/3.0/developers_guide_java.html#pagingThroughResults
    DocumentListFeed feed = docsService.getFeed(createUrl("/-/folder"), DocumentListFeed.class);
    for (DocumentListEntry entry : feed.getEntries()) {
      if (SCOREBOARD_FOLDER_NAME.equals(entry.getTitle().getPlainText()))
        return entry;
    }
    return createFolder(SCOREBOARD_FOLDER_NAME);
  }

  protected DocumentListEntry createFolder(String name) throws IOException,ServiceException,MalformedURLException {
    FolderEntry entry = new FolderEntry();
    entry.setTitle(new PlainTextConstruct(name));
    return docsService.insert(createUrl(""), entry);
  }

  protected URL createUrl(String suffix) throws MalformedURLException {
    return new URL(DOCS_URL + suffix);
  }

  protected DocsService docsService;
  protected SpreadsheetEntry spreadsheet = null;

  public static final String APPLICATION_NAME = "CarolinaRollergirls-ScoreBoard-0.2";
  public static final String SCOREBOARD_FOLDER_NAME = "DerbyScoreBoard bouts";

  public static final String DOCS_URL = "https://docs.google.com/feeds/default/private/full";
*/
}

