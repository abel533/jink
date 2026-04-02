@echo off
setlocal
set JAVA_HOME=D:\Dev\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d %~dp0
echo Running Input Diagnostic...
echo.

rem Build classpath
call mvn -q dependency:build-classpath -Dmdep.outputFile=target\cp.txt -Dmdep.includeScope=test 2>nul
set /p CP=<target\cp.txt

java --enable-native-access=ALL-UNNAMED -cp "target\test-classes;target\classes;%CP%" io.mybatis.jink.demo.InputDiagnostic
pause
