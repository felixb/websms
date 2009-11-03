#! /bin/sh

v=$1
vv=$(echo ${v} | sed -e 's:0\.::g' | tr -d .)
n=$(fgrep app_name res/values/strings.xml | cut -d\> -f2 | cut -d\< -f1 | tr -d \ )

sed -i -e "s/android:versionName=[^ >]*/android:versionName=\"${v}\"/" AndroidManifest.xml
sed -i -e "s/android:versionCode=[^ >]*/android:versionCode=\"${vv}\"/" AndroidManifest.xml
sed -i -e "s/app_version\">[^<]*/app_version\">${v}/" res/values/strings.xml

git diff

ant debug || exit

rm "bin/${n}-aligned.apk" 2> /dev/null
zipalign -v 4 bin/*-debug.apk "bin/${n}-aligned.apk"
mv "bin/${n}-aligned.apk" ~/public_html/h/flx/ 2> /dev/null

echo "enter for commit+tag"
read a
git commit -am "bump to v${v}"
git tag -a "v${v}" -m "${n}-${v}"

