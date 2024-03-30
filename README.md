The CRG ScoreBoard is a browser-based scoreboard solution that also provides overlays for video production and the ability to track full game data and export it to a WFTDA statsbook.

The topics on the [Scoreboard Wiki Main Page](https://github.com/rollerderby/scoreboard/wiki/) are the primary documentation for the scoreboard. In order to reach out to the developers, it's best to use the [Github Issues Page](https://github.com/rollerderby/scoreboard/issues).

A mailing list and wiki were available on SourceForge (the original location for this project) but they are not currently used. Subscribing to the SourceForge mailing list and consulting the wiki there is not recommended.

# Installing the Scoreboard Software

These are instructions for getting the software installed and running on a standalone computer to provide a functioning scoreboard. If you have already done this, see [Setting up the Scoreboard](#setting-up-the-scoreboard) below.

## Hardware Requirements

Most Apple or Windows computers that have been manufactured in the last ten years should be able to handle the scoreboard well on a standalone setup. In general, a machine with at least a dual-core 64-bit processor and 2 gigabytes of RAM should be sufficient. Using the scoreboard to provide video overlays or in a networked setup that includes penalty or lineup tracking typically requires more computing power.

Chromebooks that have been modified to run Linux distributions have been used to host the scoreboard but hardware limitations (lack of a suitable display output or low-powered CPUs) may cause issues.

## Software Requirements

The scoreboard should be unzipped into a folder on the local machine. The user running the software requires write access to this folder. Do not put the scoreboard in a folder that requires administrator privileges to write to unless you intend to run the software as an administrator.

### Web Browser

[Google Chrome](https://www.google.com/chrome/) and [Microsoft Edge](https://www.microsoft.com/edge/) (as well as their open-source parent [Chromium](http://www.chromium.org/) or other browsers derived from it) are recommended for running the software. Some known issues may occur when using Mozilla Firefox or Apple Safari. Microsoft Internet Explorer is not recommended.

### Java

Java is required for providing a Java Runtime Environment (JRE) version 8.0 or newer. Installing the latest version of Oracle's Java is recommended.

- Windows users can install the standard Java for Windows package that is available when clicking on Free Java Download from [Oracle’s Java site](https://java.com/).

- Apple users must install the complete [Java Platform (JDK)](http://www.oracle.com/technetwork/java/javase/downloads/index.html), which includes the JRE, to run the scoreboard properly.

- Linux users may already have a JRE from the OpenJDK project installed, if not, OpenJDK can be obtained from [their repositories](http://openjdk.java.net/install/).

## Downloading the Scoreboard

The project is currently hosted on GitHub, and ZIP files can be downloaded from the [GitHub Releases Page](https://github.com/rollerderby/scoreboard/releases). It is recommended that you use the version labeled "Latest release" (green box). The "Pre-release" (orange box) versions are currently in development and testing, and are not recommended for sanctioned games or tournaments.

Please note that an older version of the project is still hosted on SourceForge and it is no longer maintained there.

## Setting up the Scoreboard

Once Chrome and Java are installed, use your file manager to navigate to the scoreboard folder and run the scoreboard background script by double-clicking on it.

- Windows users: Run scoreboard-Windows.exe to start the script.

- Apple users: Run scoreboard.sh to start the script. (If clicking doesn't work, try pressing command+i (or right click on the file and select "Get info"). In the new info dialog in section "open with" select Terminal.app. (If it's not listed, choose other and navigate to /Applications/Utilities/Terminal.app.)

- Linux users: Run scoreboard.sh to start the script. If you are unable to start it, you may have to allow script files to be executable as programs.

Once it starts successfully, the scoreboard script will open a new window and display a series of status messages. You must keep this script running in order for the scoreboard to function, so do not close the window. You may minimize the window without effect.

In your file manager, open start.html with the recommended browser. You may need to right-click on the file and choose the **Open With** option. The browser will open to localhost:8000 where several options are presented.

Assuming that your scoreboard computer is set up with a monitor/laptop screen as a primary display for the operator, and a separate projector as a second display, right-click on the second link for **Main Scoreboard** and choose **Open link in new window**. Drag the new window with the main scoreboard onto the second display, click inside the window, and press the F11 key to make the window full screen. In the first browser window that you opened on the primary display, click on one of the documentation links. It will open in a new tab. Back in the original tab click on **Main Operator Control Panel**.

When the control panel displays, it will ask you for an operator name. Enter your name and click Login. This operator name is used to store your personalized settings such as key controls.

Now you can go to the tab with the documentation and either go to the Quick Start Guide or dive in deep right away and proceed with the section on the Controls page.
