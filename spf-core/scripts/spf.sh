#! /bin/sh

if [ -n "${JAVA_HOME+x}" ]; then
	echo using java at $JAVA_HOME
else
	echo \$JAVA_HOME environment variable is not set. please set in your system or in this script
fi

#use this if $JAVA_HOME is not set in your system:
#JAVA_HOME=/opt/java/sun-jdk6


#additional classpath entries. place your libs for plugins (also third party libs) here. multiple entries seperated by ':' (normal java syntax)
ADDITIONAL_CLASSPATH=

#native library path. add native libraries here. multiple folder seperated by a ':' (normal java syntax)
NATIVE_LIBRARY_PATH=lib/native/x86

startApp() {

	$JAVA_HOME/bin/java -Dlog4j.configuration=file:config/spf-log4j.properties -Djava.library.path=$NATIVE_LIBRARY_PATH -Dfile.encoding=UTF-8 -classpath lib/'*':plugins/'*':bin/'*':$ADDITIONAL_CLASSPATH org.n52.ifgicopter.spf.SPFRegistry
	OUT=$?
	if [ $OUT -eq 100 ];then
	   echo ""
	   echo "[SPF] Restarting application..."
	   echo ""
	   startApp

	else
	   echo "[SPF] Exiting"
	fi

}

startApp
