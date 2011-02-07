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

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class ScoreBoardImageServlet extends DefaultScoreBoardControllerServlet
{
	public String getPath() { return "/Image"; }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		super.doPost(request, response);

		String id = request.getPathInfo();

		if (id.startsWith("/"))
			id = id.substring(1);

		ScoreBoardImage sbI = scoreBoardModel.getScoreBoardImage(id);

		if (sbI == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		try {
			BufferedImage img = sbI.getImage();
			double w = 0, h = 0;

			try { w = ((double)Integer.parseInt(request.getParameter("width")) / (double)img.getWidth()); } catch ( Exception e ) { }
			try { h = ((double)Integer.parseInt(request.getParameter("height")) / (double)img.getHeight()); } catch ( Exception e ) { }

			if (w == 0 && h == 0)
				w = h = 1;
			else if (w == 0)
				w = h;
			else if (h == 0)
				h = w;

			int newW = (int)(w * img.getWidth());
			int newH = (int)(h * img.getHeight());

			/* Sure, it's completely arbitrary, but not having some limit isn't good */
			if (newW > 2000)
				newW = 2000;
			if (newH > 2000)
				newH = 2000;

			BufferedImage newImg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = newImg.createGraphics();
			g.scale(w, h);
			g.drawImage(img, 0, 0, null);
			g.dispose();

			response.setContentType("image/png");
			ImageIO.write(newImg, "png", response.getOutputStream());
		} catch ( Exception e ) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

	}

}
