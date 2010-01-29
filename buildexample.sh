#! /bin/sh

./buildjar.sh x || exit -1

for r in drawable drawable-hdpi values values-de
do
	cp -a connectors/common/res/$r/* connectors/example/res/$r/
done


cd connectors/example

ant clean || exit -1
ant debug || exit -1
ant clean || exit -1

cd ..

ver=$(grep -Fr app_version example/res/values/ | cut -d\> -f2 | cut -d\< -f1)
zip -r Connector-Example-${ver}.zip example

