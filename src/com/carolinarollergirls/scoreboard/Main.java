package com.carolinarollergirls.scoreboard;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Main implements ScoreBoardManager.Logger {
    public static void main(String argv[]) {
        new Main(argv);
    }

    public Main(String argv[]) {
        parseArgv(argv);
        ScoreBoardManager.setLogger(this);
        ScoreBoardManager.start();
        if (guiFrameText != null) {
            guiFrameText.setText("ScoreBoard status: running (close this window to exit scoreboard)");
        }
    }

    public void log(String msg) {
        if (guiMessages != null) {
            guiMessages.append(msg+"\n");
        } else {
            System.err.println(msg);
        }
    }

    private void parseArgv(String[] argv) {
        boolean gui = false;


        for(String arg : argv) {
            if(arg.equals("--gui") || arg.equals("-g")) {
                gui = true;
            }

            else if(arg.equals("--nogui") || arg.equals("-G")) {
                gui = false;
            }

            else if(arg.startsWith("--port=") || arg.startsWith("-p=")) {
                String port = arg.split("=")[1];
                ScoreBoardManager.setPropertyOverride("com.carolinarollergirls.scoreboard.jetty.JettyServletScoreBoardController.port", port);
            }
        }

        if (gui) {
            createGui();
        }
    }




    private void createGui() {
        if (guiFrame != null) {
            return;
        }

        guiFrame = new JFrame("Carolina Rollergirls ScoreBoard");
        guiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        guiMessages = new JTextArea();
        guiMessages.setEditable(false);
        guiFrameText = new JLabel("ScoreBoard status: starting...");
        guiFrame.getContentPane().setLayout(new BoxLayout(guiFrame.getContentPane(), BoxLayout.Y_AXIS));
        guiFrame.getContentPane().add(guiFrameText);
        guiFrame.getContentPane().add(new JScrollPane(guiMessages));
        guiFrame.setSize(800, 600);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int w = guiFrame.getSize().width;
        int h = guiFrame.getSize().height;
        int x = (dim.width-w)/2;
        int y = (dim.height-h)/2;
        guiFrame.setLocation(x, y);
        guiFrame.setVisible(true);
    }

    private JFrame guiFrame = null;
    private JTextArea guiMessages = null;
    private JLabel guiFrameText = null;
}
