#! /bin/sh

javadoc -quiet -d doc -author -version $(find . -name \*.java)
cp -ar doc /home/flx/public_html/websms/

