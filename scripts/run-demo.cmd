@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul 2>&1
cd /d "%~dp0\.."

:: jink demo launcher for CMD
:: Usage: run-demo.cmd [main-class] [JDK_HOME]

set "MAIN_CLASS=io.mybatis.jink.demo.Counter"
if not "%~1"=="" set "MAIN_CLASS=%~1"

set "JDK_HOME=%~2"
set "JAVA_BIN=java"

:: Determine Java binary
if defined JDK_HOME (
    set "JAVA_HOME=%JDK_HOME%"
    set "PATH=%JDK_HOME%\bin;%PATH%"
    set "JAVA_BIN=%JDK_HOME%\bin\java.exe"
    goto :compile
)
if defined JINK_JAVA_HOME (
    set "JAVA_HOME=%JINK_JAVA_HOME%"
    set "PATH=%JINK_JAVA_HOME%\bin;%PATH%"
    set "JAVA_BIN=%JINK_JAVA_HOME%\bin\java.exe"
    goto :compile
)

:: Check system java version
set "JAVA_VER_RAW="
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    if not defined JAVA_VER_RAW set "JAVA_VER_RAW=%%v"
)
if not defined JAVA_VER_RAW (
    echo ^> 未找到 Java，请设置 JINK_JAVA_HOME 或通过第二个参数指定 JDK 8+ 路径
    exit /b 1
)
set "JAVA_VER=!JAVA_VER_RAW:"=!"
for /f "tokens=1 delims=." %%m in ("!JAVA_VER!") do set "JAVA_MAJOR=%%m"
if "!JAVA_MAJOR!"=="1" (
    for /f "tokens=2 delims=." %%m in ("!JAVA_VER!") do set "JAVA_MAJOR=%%m"
)
if !JAVA_MAJOR! LSS 8 (
    echo ^> 当前 Java !JAVA_MAJOR! ^< 8，请设置 JINK_JAVA_HOME 或通过第二个参数指定 JDK 8+ 路径
    exit /b 1
)

:compile
set "MAVEN_OPTS=-Dfile.encoding=UTF-8"

echo [jink] compiling project...
call mvn -q compile test-compile -DskipTests
if errorlevel 1 exit /b %errorlevel%

echo [jink] building dependency classpath...
call mvn -q "-Dmdep.outputFile=target\cp.txt" dependency:build-classpath
if errorlevel 1 exit /b %errorlevel%

set "DEPS="
set /p DEPS=<target\cp.txt

echo [jink] starting %MAIN_CLASS% ...
echo.

if defined DEPS (
    "!JAVA_BIN!" -Dfile.encoding=UTF-8 -cp "target\classes;target\test-classes;!DEPS!" %MAIN_CLASS%
) else (
    "!JAVA_BIN!" -Dfile.encoding=UTF-8 -cp "target\classes;target\test-classes" %MAIN_CLASS%
)
exit /b %errorlevel%

