# INSTALLING THE SCOREBOARD SOFTWARE

These are instructions for getting the software installed and running on a standalone computer to provide a functioning scoreboard. Once you complete them, you can go to the Quick Start Guide to Operating the Scoreboard below or browse the topics on the [Scoreboard Wiki Main Page](https://github.com/rollerderby/scoreboard/wiki/) for more detailed information about scoreboard procedures.

The Carolina Roller Derby Scoreboard is a browser-based scoreboard solution that also provides overlays for video production and the ability to track penalties. The project is currently hosted on [GitHub](https://github.com/rollerderby/scoreboard) and a mirror is available on [Google Drive](https://drive.google.com/drive/folders/0B5MEhWEBhGKVSTlTZW1mcFdobnM). It is recommended that you check there for the current version before setting up your scoreboard.

(Note that an older version of the project is still hosted on SourceForge and it is no longer maintained there.)

## Additional Resources

Before we get started, these are the best places for help in case you have problems:

* The [Derby Scoreboard Facebook group](https://www.facebook.com/groups/derbyscoreboard/) is very active and currently the best way to reach other users and developers.

* A mailing list for users and developers was available on SourceForge (the original location for this project) but it is not currently used. Subscribing is not recommended.

* A quick start [Google Document](https://docs.google.com/document/d/1m30n7C1zUHH-ZTarxVBZ3ZE0og9C-kLPnJY7w-odXM8/edit?usp=sharing) including screenshots is available courtesy of Paulie Walnuts.

* The Downunder Derby channel on YouTube has also created several helpful [how-to videos](https://www.youtube.com/watch?v=k8lYWrtmTLw&list=PLTKxJCQ9RlGQhKK6eRaviXLvySXWUeUm).

## Hardware Requirements

Most Apple or Windows computers that have been manufactured in the last ten years should be able to handle the scoreboard well on a standalone setup. In general, a machine with at least a dual-core 64-bit processor and 2 gigabytes of RAM should be sufficient. Using the scoreboard to provide video overlays or in a networked setup that includes a scoreboard assistant or penalty tracker typically requires more computing power.

Chromebooks that have been modified to run Linux distributions have been used to host the scoreboard but hardware limitations (lack of a suitable display output or low-powered CPUs) may cause issues.

There are experimental versions of the scoreboard available that will run on Android devices. Contact the developers for more information.

## Software Requirements

The scoreboard should be unzipped into a folder on the local machine. The user running the software requires write access to this folder. Do not put the scoreboard in a folder that requires administrator privileges to write to unless you intend to run the software as an administrator.

### Web Browser

[Google Chrome](https://www.google.com/chrome/) or its open source parent [Chromium](http://www.chromium.org/) are recommended for running the software. Some known issues may occur when using Mozilla Firefox or Apple Safari. Microsoft Edge and Microsoft Internet Explorer are not recommended.

### Java

Java is required for providing a Java Runtime Environment (JRE) version 1.5.0 or newer. Installing the latest version of Oracle's Java is recommended.

* Windows users can install the standard Java for Windows package that is available when clicking on Free Java Download from [Oracle’s Java site](https://java.com/).

* Apple users must install the complete [Java Platform (JDK)](http://www.oracle.com/technetwork/java/javase/downloads/index.html), which includes the JRE, to run the scoreboard properly.

* Linux users may already have a JRE from the OpenJDK project installed, if not, OpenJDK can be obtained from [their repositories](http://openjdk.java.net/install/).

## Setting up the Scoreboard

Once Chrome and Java are installed, use your file manager to navigate to the scoreboard folder and run the scoreboard background script by double-clicking on it.

* Windows users: Run scoreboard-Windows.exe to start the script.

* Apple users: Run scoreboard-Mac.app to start the script. If you are unable to start it, you may have to download and run the [startscoreboard-mac.zip package](https://drive.google.com/file/d/0B2fZmT3bqA9oS25KNHphZjFFcWs/view?fref=gc&dti=480408282040949) to get the scoreboard started.

* Linux users: Run scoreboard-Linux.sh to start the script. If you are unable to start it, you may have to allow script files to be executable as programs. The ``lib/crg-scoreboard.jar`` file can be built from source by installing ``apache-ant`` and running ``ant compile`` from this directory.

Once it starts successfully, the scoreboard script will open a new window and display a series of status messages. You must keep this script running in order for the scoreboard to function, so do not close the window. You may minimize the window without effect.

In your file manager, open start.html with the recommended browser. You may need to right-click on the file and choose the **Open With** option. The browser will open to localhost:8000 where several options are presented.

Assuming that your scoreboard computer is set up with a monitor/laptop screen as a primary display for the operator, and a separate projector as a second display, right-click on the second link for **Main Scoreboard** and choose **Open link in new window**. Drag the new window with the main scoreboard onto the second display, click inside the window, and press the F11 key to make the window full screen. In the first browser window that you opened on the primary display, click on **Main Operator Control Panel**.

When the control panel displays, it will ask you for an operator name. Enter your name and click Login. This operator name is used to store your personalized settings such as key controls.

# QUICK START GUIDE TO OPERATING THE SCOREBOARD

These instructions are intended to get you up and running as fast as possible with operating a scoreboard computer that is in the following state:

* The Carolina Roller Derby Scoreboard software is installed.

* The scoreboard script is running.

* The start page is open.

* The computer is connected to a second display.

* The Main Scoreboard page is on the second display, and the Main Operator Control Panel is on the primary display.

* The operator is logged in to the control panel.

For instructions on how to accomplish the above, see Installing the Scoreboard Software above.

If you have a scrimmage in ten minutes and just need a basic interface, this quick start guide is your baby. This guide will not allow you to display jammer names on the scoreboard, project penalties, or make toast, but it will allow you to operate the timing and score functions of the scoreboard during a regulation game of WFTDA roller derby.

## Initial Setup

In the operator control screen, click on the Save/Load tab.  Press the "Reset scoreboard only" button, and confirm that yes, this is a thing you are prepared to do.  (It's not that big a deal) Select the Team/Time tab to return to the main control panel. Click on the name of the left hand team.  Highlight the name of the team and type in the name for team 1. Repeat on the right side for team 2.

Finally, make sure the "undo" controls, "Un-Start Jam," "Un-Stop Jam," and "UN-Timeout" are visible.  If they are not, click the "Show UNDO controls" button at the top of the screen. 

## Starting the Game

If you need to get the scoreboard started quickly, simply click the Start Jam button when the first whistle blows. This will start the jam and period clocks.

If teams have been defined in the program and there is time before the game starts, click the Start New Game button. Select both teams and enter the start time in AM/PM format. Click Start Game. The Time To Derby clock will display on the scoreboard and begin counting down.

## During the Game

The scoreboard operator has two critical duties during the game - controlling the clock and updating the score.  As long as those two things happen smoothly, everything else is a bonus.

### Controlling the Clock

Controlling the clock should _always_ be done using the buttons at the top of the screen: "Start Jam," "Stop Jam," and "Timeout." Do not use the "start" and "stop" buttons under the individual clocks.

* *Start Jam:* Press this button when the jam timer blows the whistle to start the jam, which should _always_ come after a "five seconds" warning.
* *Stop Jam:* Press this button on the _fourth_ whistle blown by either the lead jam referee, or (rarely) another referee to end the jam. You do NOT need to press this button if the jam runs to two minutes and is ended by the jam timer.  Just let the scoreboard do its thing in that case.
* *Timeout:* Press this button if a referee or the jam timer blows a whistle for a timeout.  (Occasionally a referee will move to timeout without remembering to blow the whistle.  Use your judgement, and yell at them later.)

*Timeouts:* Once the timeout clock is started, take a deep breath, and then determine what _kind_ of timeout it is.  If it's an official timeout - do nothing, you're good! If it's a team timeout, click the "Timeout" button under the appropriate team.  If it's an official review, likewise click the "Off Review" button for the appropriate team. Once the official review is over, if the team has retained their review, click the "Retained" button for that team.

*Undo buttons*: If you screw up and press Start Jam, Stop Jam, or Timeout by mistake, press the corresponding "Undo" button immediately.  Probably nobody noticed.

You do NOT need to do anything for swoopy whistles blown by the jam timer or referees.

## Updating the Score

To raise or lower the score for a team, press the "Score +1" or "Score -1" button under the team name.  There is never a need to press the "Jam Score" buttons, so don't.

## Correcting Numbers

If you need to adjust a number in the top half of the scoreboard, such as timeouts remaining, just click on it, select the value, and type in the new value followed by "enter."

If you need to adjust the value of a clock, use the +1 and -1 buttons on either side of the clock. _Do not_ adjust the value of the period clock except during timeouts.

That's all you need for a basic game.  There's more to learn, but this should be enough to make the derby happen. For more detailed information about scoreboard procedures, browse the topics on the [Scoreboard Wiki Main Page](https://github.com/rollerderby/scoreboard/wiki/).
