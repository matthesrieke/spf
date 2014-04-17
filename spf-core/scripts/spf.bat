@echo off
if not "%JAVA_HOME%" == "" goto javaHomeAlreadySet
	echo Please set JAVA_HOME environment variable. Will try to find java.exe on normal PATH
	for %%P in (%PATH%) do if exist %%P\java.exe set JAVA_HOME=%%P..\
:javaHomeAlreadySet

REM additional classpath entries. place your libs for plugins (also third party libs) here. multiple entries seperated by ';' (normal java syntax)
set CLASSPATH=lib/*;bin/*;plugins/*;

REM native library path. add native libraries here. multiple folder seperated by a ';' (normal java syntax)
set NATIVE_LIBRARY_PATH=lib/native/x86;

echo [SPF] Java path  : %JAVA_HOME%
echo [SPF] Classpath  : %CLASSPATH%
echo [SPF] Native libs: %NATIVE_LIBRARY_PATH%

:startApp
"%JAVA_HOME%/bin/java" -Dlog4j.configuration=file:config/spf-log4j.properties -Djava.library.path=%NATIVE_LIBRARY_PATH% -Dfile.encoding=UTF-8 -classpath %CLASSPATH% org.n52.ifgicopter.spf.SPFRegistry
set restartcode=%errorlevel%

if %restartcode%==100 goto startApp

pause