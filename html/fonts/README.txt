To add a font to the scoreboard, go to:
http://www.fontsquirrel.com/tools/webfont-generator

Use that page to add your font files (it currently limits you
to 3 font files at once).  Select the "expert" conversion
and check the TrueType, WOFF, EOT Compressed, and SVG output
font formats.  You generally should be able to leave the rest
of the parameters at their defaults.  Then click "Download
Your Kit" to get the resulting zip.  Repeat that with any
other font files in your font group (i.e. any other variations
of your single font, e.g. -italics, -bold, etc).

Once you have the generated zip files, create a new subdirectory
here, named for your new font.  Move all the generated zip files
into the new font directory.  Extract each of them into a separate
subdirectory.  Move all the actual font files (ending in .eot, .svg,
.tff, or .woff) into the main directory.  Move any one of the
"generator_config.txt" files into the main directory (this is optional,
you don't really need to save that file unless you want to reproduce
these generated font files).  Then combine the contents of all the
stylesheet.css files into a single stylesheet.css file in the main
directory.  Then, you can remove all the subdirectories and the zip
files.  Check everything in your new font directory in.

One last step before using the font in the scoreboard is, edit the
file at html/javascript/scoreboard.js and find the lines similar to:
_include("/fonts", [
  "liberationsans/stylesheet.css" ]);

Edit that to add your new font in, for example if you added a font
in a new directory called "mynewfont" you would edit the line to:

_include("/fonts", [
  "liberationsans/stylesheet.css", "mynewfont/stylesheet.css" ]);

Then in any page's css you can use any of the new font-family
definitions from your new stylesheet.css.  For example, if your
stylesheet.css defined:

@font-face {
    font-family: 'mynewfont';
...

You could use those fonts in an element css like:

a { font-family: 'mynewfont'; }

