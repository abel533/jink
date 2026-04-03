@echo off
setlocal
chcp 65001 >nul 2>&1
cd /d "%~dp0\.."

:: Always use JDK 21 for this project
if defined JINK_JAVA_HOME (
    set "JAVA_HOME=%JINK_JAVA_HOME%"
) else (
    set "JAVA_HOME=D:\Dev\jdk-21"
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_OPTS=-Dfile.encoding=UTF-8"

:: Compile project without running tests
echo [jink] compiling project...
call mvn -q compile test-compile -DskipTests
if errorlevel 1 exit /b %errorlevel%

:: Build dependency classpath
echo [jink] building dependency classpath...
call mvn -q "-Dmdep.outputFile=target\cp.txt" dependency:build-classpath
if errorlevel 1 exit /b %errorlevel%

set "DEPS="
set /p DEPS=<target\cp.txt

:: Main class
set "MAIN_CLASS=io.mybatis.jink.demo.InteractiveDemo"
if not "%~1"=="" set "MAIN_CLASS=%~1"

echo [jink] starting %MAIN_CLASS% ...
echo.

java --enable-native-access=ALL-UNNAMED ^
    -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 ^
    -cp "target\classes;target\test-classes;%DEPS%" ^
    %MAIN_CLASS%

exit /b %errorlevel%
