#!/bin/bash

loc="$(dirname "${0}")"
cd "${loc}" || exit 1


GUI="--gui"

# If fd 0 (stdin) exists, this is an interactive shell, so don't use the gui
test -t "0" && GUI=""

JAVA=""
if [ -n "${JAVA_HOME}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
  # An explicitly configured JDK takes priority
  JAVA="${JAVA_HOME}/bin/java"
elif [ -x /usr/libexec/java_home ] && [ -n "$(/usr/libexec/java_home -F 2>/dev/null)" ]; then
  # We're on macOS, which has its own way of doing Java
  JAVA="/usr/libexec/java_home -exec java"
elif JAVA_PATH="$(which java 2>/dev/null)" || JAVA_PATH="$(command -v java 2>/dev/null)"; then
  # Fall back to java on the PATH (covers Homebrew/OpenJDK installs
  # that aren't registered with java_home)
  JAVA="$JAVA_PATH"
fi

if [ -z "$JAVA" ]; then
  if [[ "$SHELL" == *"zsh"* ]]; then
    echo "Could not find a Java runtime. Install one (e.g., \`brew install openjdk\`) and set \`JAVA_HOME\` in ~/.zshrc if needed, then restart your shell." >&2
  else
    echo "Could not find a Java runtime. Install one (e.g., \`brew install openjdk\`) and set \`JAVA_HOME\` in your shell rc file if needed, then restart your shell." >&2
  fi
  exit 1
fi

exec $JAVA -Done-jar.silent=true -Dorg.eclipse.jetty.server.LEVEL=WARN -jar lib/crg-scoreboard.jar ${GUI} "$@"
