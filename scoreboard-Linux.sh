#!/bin/bash

GUI="--gui"

# If fd 0 (stdin) exists, this is an interactive shell, so don't use the gui
test -t "0" && GUI=""

exec java -Xmx256m -jar lib/crg-scoreboard.jar $GUI
