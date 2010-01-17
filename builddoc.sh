#! /bin/sh

javadoc -quiet -d doc -author -version de.ub0r.android $(find . -name \*.java) > /dev/null 2> /dev/null
cp -ar doc /home/flx/public_html/websms/

