#!/bin/sh

# InstallCommandLineTool.sh
# Platypus
#
# Created by Sveinbjorn Thordarson on 6/17/08.
# Copyright (C) 2003-2010. All rights reserved.

# Create directories if they don't exist
mkdir -p "/usr/local/bin"
mkdir -p "/usr/local/share/platypus"
mkdir -p "/usr/local/share/man/man1"

# Change to Resources directory of Platypus application, which is first argument
cd "$1"

# Copy resouces over
cp "platypus" "/usr/local/bin/platypus"
cp "ScriptExec" "/usr/local/share/platypus/ScriptExec"
cp "platypus.1" "/usr/local/share/man/man1/platypus.1"
cp "PlatypusDefault.icns" "/usr/local/share/platypus/PlatypusDefault.icns"
cp -r "English.lproj/MainMenu.nib" "/usr/local/share/platypus/"

chmod -R 755 "/usr/local/share/platypus/"

# Create text file with version
echo -n "4.4" > "/usr/local/share/platypus/Version"
