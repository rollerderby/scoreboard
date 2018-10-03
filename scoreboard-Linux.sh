#!/bin/bash

cd "$(dirname $0)"

GUI="--gui"

# If fd 0 (stdin) exists, this is an interactive shell, so don't use the gui
test -t "0" && GUI=""

java -Done-jar.silent=true -Dorg.eclipse.jetty.server.LEVEL=WARN -jar lib/crg-scoreboard.jar "$GUI" "$@"
