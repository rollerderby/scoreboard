#!/bin/bash

cd "$(dirname $0)"

GUI="--gui"

# If fd 0 (stdin) exists, this is an interactive shell, so don't use the gui
test -t "0" && GUI=""

JAVA=java
if [ -x /usr/libexec/java_home ]; then
  # We're on OS X, which has its own way of doing Java
  JAVA="/usr/libexec/java_home -exec java"
fi

exec $JAVA -Done-jar.silent=true -Dorg.eclipse.jetty.server.LEVEL=WARN -jar lib/crg-scoreboard.jar "$GUI" "$@"
