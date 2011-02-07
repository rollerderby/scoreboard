package com.carolinarollergirls.scoreboard.jetty;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.awt.*;
import java.awt.image.*;

import javax.imageio.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.file.*;
import com.carolinarollergirls.scoreboard.policy.*;
import com.carolinarollergirls.scoreboard.defaults.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.*;

public class XmlScoreBoardServlet extends AbstractXmlServlet
{
	public String getPath() { return "/XmlScoreBoard"; }

	protected void getAll(HttpServletRequest request, HttpServletResponse response) throws IOException,JDOMException {
		response.setContentType("text/xml");
		editor.sendToWriter(scoreBoardModel.getXmlScoreBoard().getDocument(), response.getWriter(), Format.getPrettyFormat());
		response.setStatus(HttpServletResponse.SC_OK);
	}

	protected void get(HttpServletRequest request, HttpServletResponse response) throws IOException,JDOMException {
		String key;
		XmlListener listener = null;
		synchronized (clientMap) {
			if ((null != (key = request.getParameter("key"))) && (null != (listener = (XmlListener)clientMap.get(key)))) {
				if (listener.isEmpty()) {
					response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
				} else {
					response.setContentType("text/xml");
					editor.sendToWriter(listener.resetDocument(), response.getWriter());
					response.setStatus(HttpServletResponse.SC_OK);
				}
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}
	}

	protected void reloadListeners(HttpServletRequest request, HttpServletResponse response) {
		Document d = editor.createDocument("Reload");
		d.getRootElement().setAttribute("persistentIgnore", "true");
		scoreBoardModel.getXmlScoreBoard().mergeDocument(d);
		response.setContentType("text/plain");
		response.setStatus(HttpServletResponse.SC_OK);
	}

	protected void set(HttpServletRequest request, HttpServletResponse response) throws IOException,JDOMException {
		Document requestDocument = null;

		try {
			if (ServletFileUpload.isMultipartContent(request)) {
				ServletFileUpload upload = new ServletFileUpload();

				FileItemIterator iter = upload.getItemIterator(request);
				while (iter.hasNext()) {
					FileItemStream item = iter.next();
					if (!item.isFormField()) {
						InputStream stream = item.openStream();
						requestDocument = editor.toDocument(stream);
						stream.close();
						break;
					}
				}
			} else {
				requestDocument = editor.toDocument(request.getReader());
			}
		} catch ( FileUploadException fuE ) {
			response.getWriter().print(fuE.getMessage());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		if (null == requestDocument) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

//FIXME - uncomment this for debugging XML set requests
//System.err.println(editor.toString(requestDocument));

		/* This should clear the scoreboard to prepare for loading a new one */
		/* This does not work with continuous-save-to-file! */
		if (Boolean.parseBoolean(request.getParameter("clearScoreBoard"))) {
			reloadListeners(request, response);

			//Document d = converter.toDocument(scoreBoardModel);
//FIXME - replacing doc is wrong!
			//documentManager.replaceDocument(d);
		}

		scoreBoardModel.getXmlScoreBoard().mergeDocument(requestDocument);

		response.setContentType("text/plain");
		response.setStatus(HttpServletResponse.SC_OK);
	}
 
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		super.doPost(request, response);

		try {
			if ("/get".equals(request.getPathInfo()))
				get(request, response);
			else if ("/set".equals(request.getPathInfo()))
				set(request, response);
			else if ("/reloadViewers".equals(request.getPathInfo()))
				reloadListeners(request, response);
			else if (request.getPathInfo().endsWith(".xml"))
				getAll(request, response);
		} catch ( JDOMException jE ) {
			response.getWriter().print(jE.getMessage());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public void setScoreBoardModel(ScoreBoardModel model) {
//FIXME - no events for policy adding/removing; need to implement that.
		super.setScoreBoardModel(model);
		model.addPolicyModel(new ScoreBoardHtmlIntermissionNamePolicy());		
		model.addPolicyModel(new ScoreBoardAdControlPolicy(model));		
	}

	// FIXME - don't much like this; we should not have page-specific stuff up here.
	public class ScoreBoardAdControlPolicy extends DefaultPolicyModel
	{
		public ScoreBoardAdControlPolicy(ScoreBoardModel sbM) {
			super("scoreboard.html AdControlPolicy");
			setName(getId());

//FIXME - get rid of this stuff, shouldn't be polcies inside servlet, and this modifies the document directly instead of sending back updates to merge
			Document d = sbM.getXmlScoreBoard().getRealDocument();
			synchronized (d) {
				Element sb = editor.getElement(d.getRootElement(), "ScoreBoard");
				Element sbpage = editor.getElement(sb, "Page", "scoreboard.html");

				Element ad_show_during_intermission = editor.getElement(sbpage, "AdsShowDuringIntermission");
				Element ad_use_lineup_clock = editor.getElement(sbpage, "AdsUseLineupClock");
				Element ad_random_order = editor.getElement(sbpage, "AdsRandomOrder");
				Element ad_display_seconds = editor.getElement(sbpage, "AdsDisplaySeconds");
				Element ad_auto_change = editor.getElement(sbpage, "AutoScoreBoardAdChange");

				if (null == editor.getContent(ad_show_during_intermission))
					editor.setContent(ad_show_during_intermission, String.valueOf(false));
				if (null == editor.getContent(ad_use_lineup_clock))
					editor.setContent(ad_use_lineup_clock, String.valueOf(true));
				if (null == editor.getContent(ad_random_order))
					editor.setContent(ad_random_order, String.valueOf(true));
				try {
					Integer.parseInt(editor.getContent(ad_display_seconds));
				} catch ( NumberFormatException nfE ) {
					editor.setContent(ad_display_seconds, String.valueOf(5));
				}
				if (null == editor.getContent(ad_auto_change))
					editor.setContent(ad_auto_change, String.valueOf(true));

				PolicyModel.ParameterModel adShowDuringIntermission = new DefaultPolicyModel.DefaultParameterModel(this, AD_SHOW_DURING_INTERMISSION, "Boolean", editor.getContent(ad_show_during_intermission));
				PolicyModel.ParameterModel adUseLineupClock = new DefaultPolicyModel.DefaultParameterModel(this, AD_USE_LINEUP_CLOCK, "Boolean", editor.getContent(ad_use_lineup_clock));
				PolicyModel.ParameterModel adRandomOrder = new DefaultPolicyModel.DefaultParameterModel(this, AD_RANDOM_ORDER, "Boolean", editor.getContent(ad_random_order));
				PolicyModel.ParameterModel adDisplaySeconds = new DefaultPolicyModel.DefaultParameterModel(this, AD_DISPLAY_SECONDS, "Integer", editor.getContent(ad_display_seconds));
				PolicyModel.ParameterModel adAutoChange = new DefaultPolicyModel.DefaultParameterModel(this, AD_AUTO_CHANGE, "Boolean", editor.getContent(ad_auto_change));

				addParameterModel(adShowDuringIntermission);
				new FilterScoreBoardListener(adShowDuringIntermission, "Value") {
					public void filteredScoreBoardChange(ScoreBoardEvent event) { changePageElement("AdsShowDuringIntermission", event.getValue().toString()); }
				};
				addParameterModel(adUseLineupClock);
				new FilterScoreBoardListener(adUseLineupClock, "Value") {
					public void filteredScoreBoardChange(ScoreBoardEvent event) { changePageElement("AdsUseLineupClock", event.getValue().toString()); }
				};
				addParameterModel(adRandomOrder);
				new FilterScoreBoardListener(adRandomOrder, "Value") {
					public void filteredScoreBoardChange(ScoreBoardEvent event) { changePageElement("AdsRandomOrder", event.getValue().toString()); }
				};
				addParameterModel(adDisplaySeconds);
				new FilterScoreBoardListener(adDisplaySeconds, "Value") {
					public void filteredScoreBoardChange(ScoreBoardEvent event) { changePageElement("AdsDisplaySeconds", event.getValue().toString()); }
				};
				addParameterModel(adAutoChange);
				new FilterScoreBoardListener(adAutoChange, "Value") {
					public void filteredScoreBoardChange(ScoreBoardEvent event) { changePageElement("AutoScoreBoardAdChange", event.getValue().toString()); }
				};
			}
		}

		protected void changePageElement(String pageElementName, String pageElementValue) {
			Document doc = editor.createDocument("ScoreBoard");
			Element sb = editor.getElement(doc.getRootElement(), "ScoreBoard");
			Element page = editor.addElement(sb, "Page", "scoreboard.html");
			editor.addElement(page, pageElementName, null, pageElementValue);
			scoreBoardModel.getXmlScoreBoard().mergeDocument(doc);
		}

		public static final String AD_SHOW_DURING_INTERMISSION = "Show Ads During Intermission";
		public static final String AD_USE_LINEUP_CLOCK = "Ad Change Use Lineup Clock";
		public static final String AD_RANDOM_ORDER = "Show Ads in Random Order";
		public static final String AD_DISPLAY_SECONDS = "Ad Display Seconds";
		public static final String AD_AUTO_CHANGE = "Automatically Change Ad Image";
	}
	public class ScoreBoardHtmlIntermissionNamePolicy extends AbstractClockNumberChangePolicy
	{
		public ScoreBoardHtmlIntermissionNamePolicy() {
			super("scoreboard.html IntermissionNamePolicy");
			setName(getId());

			PolicyModel.ParameterModel intermission1Name = new DefaultPolicyModel.DefaultParameterModel(this, INTERMISSION_1_NAME, "String", "Halftime");
			PolicyModel.ParameterModel intermission2Name = new DefaultPolicyModel.DefaultParameterModel(this, INTERMISSION_2_NAME, "String", "Final");
			PolicyModel.ParameterModel intermissionOtherName = new DefaultPolicyModel.DefaultParameterModel(this, INTERMISSION_OTHER_NAME, "String", "Time To Derby");

			FilterScoreBoardListener fsbL = new FilterScoreBoardListener() {
					public void filterScoreBoardChange(ScoreBoardEvent event) {
						synchronized (changeLock) {
							Clock ic = scoreBoardModel.getClock(Clock.ID_INTERMISSION);
							clockNumberChange(ic, ic.getNumber());
						}
					}
				};

			addParameterModel(intermission1Name);
			fsbL.addProperty(intermission1Name, "Value");
			intermission1Name.addScoreBoardListener(fsbL);
			addParameterModel(intermission2Name);
			fsbL.addProperty(intermission2Name, "Value");
			intermission2Name.addScoreBoardListener(fsbL);
			addParameterModel(intermissionOtherName);
			fsbL.addProperty(intermissionOtherName, "Value");
			intermissionOtherName.addScoreBoardListener(fsbL);

			addClock(Clock.ID_INTERMISSION);
		}

		public void setScoreBoardModel(ScoreBoardModel sbM) {
			super.setScoreBoardModel(sbM);
			Clock ic = sbM.getClock(Clock.ID_INTERMISSION);
			clockNumberChange(ic, ic.getNumber());
		}

		public void clockNumberChange(Clock clock, int number) {
			Document doc = editor.createDocument("ScoreBoard");
			Element sb = editor.getElement(doc.getRootElement(), "ScoreBoard");
			Element page = editor.addElement(sb, "Page", "scoreboard.html");
			String name;
			switch (number) {
				case 1: name = getParameter(INTERMISSION_1_NAME).getValue(); break;
				case 2: name = getParameter(INTERMISSION_2_NAME).getValue(); break;
				default: name = getParameter(INTERMISSION_OTHER_NAME).getValue(); break;
			}
			editor.addElement(page, "IntermissionName", null, name);
			scoreBoardModel.getXmlScoreBoard().mergeDocument(doc);
		}

		public static final String INTERMISSION_1_NAME = "Intermission1";
		public static final String INTERMISSION_2_NAME = "Intermission2";
		public static final String INTERMISSION_OTHER_NAME = "IntermissionOther";
	}

}
