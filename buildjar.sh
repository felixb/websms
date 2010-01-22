#! /bin/sh

if [ "$1" == "dev" ] ; then
	# just jar files
	ant clean
	ant debug || exit -1
	jar cvf WebSMS-Connector-API.jar -C bin/classes/ de/ub0r/android/websms/connector/common/
else
	# build release version. the key does not matter at all. but is used for consistent handling
	ant clean

	# comment out all Log.d and Log.v
	./preDeploy.sh src/de/ub0r/android/websms/connector/common/

	ant release < ../release.ks.pw || exit -1

	# remove comments for Log.d and Log.v
	./postDeploy.sh src/de/ub0r/android/websms/connector/common/

	find bin/ -name package-info.class -exec rm {} \;
	jar cvf WebSMS-Connector-API.jar -C bin/classes/ de/ub0r/android/websms/connector/common/
	[ -n "$1" ] && cp WebSMS-Connector-API.jar connectors/example/libs/
fi

ant debug
./builddoc.sh
ant clean

