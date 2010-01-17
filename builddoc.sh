#! /bin/sh

javadoc -quiet -d doc -author -version $(find . -name \*.java) > /dev/null 2> /dev/null
cp -ar doc /home/flx/public_html/websms/

