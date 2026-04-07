@echo off
setlocal enabledelayedexpansion
:: jink 交互式 Demo 启动器
:: 用法: run.cmd [JDK_HOME]
::   run.cmd                     （使用系统 java，须 >= 8）
::   run.cmd C:\Dev\jdk-8        （指定 JDK 路径）

chcp 65001 >nul 2>&1
cd /d "%~dp0\.."

set "JDK_HOME=%~1"
set "JAVA_BIN=java"

:: 1. 确定 Java 可执行路径
if defined JDK_HOME (
    set "JAVA_HOME=%JDK_HOME%"
    set "PATH=%JDK_HOME%\bin;%PATH%"
    set "JAVA_BIN=%JDK_HOME%\bin\java.exe"
    echo [jink] 使用指定 JDK: %JDK_HOME%
    goto :compile
)

:: 检查系统 java 版本
set "JAVA_VER_RAW="
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    if not defined JAVA_VER_RAW set "JAVA_VER_RAW=%%v"
)
if not defined JAVA_VER_RAW (
    echo ^> 未找到 Java，请安装 JDK 8+ 或通过参数指定路径：
    echo    run.cmd C:\path\to\jdk8
    exit /b 1
)
:: 去除引号：1.8.0_xxx 或 11.0.x
set "JAVA_VER=!JAVA_VER_RAW:"=!"
:: 提取主版本号
for /f "tokens=1 delims=." %%m in ("!JAVA_VER!") do set "JAVA_MAJOR=%%m"
:: Java 8 的主版本是 1，次版本才是 8
if "!JAVA_MAJOR!"=="1" (
    for /f "tokens=2 delims=." %%m in ("!JAVA_VER!") do set "JAVA_MAJOR=%%m"
)
if !JAVA_MAJOR! LSS 8 (
    echo ^> 当前 Java 版本 !JAVA_MAJOR! ^< 8，请通过参数指定 JDK 8+ 路径：
    echo    run.cmd C:\path\to\jdk8
    exit /b 1
)
echo [jink] 使用系统 Java !JAVA_MAJOR!

:compile
set "MAVEN_OPTS=-Dfile.encoding=UTF-8"

:: 2. 编译
echo [jink] 编译项目...
call mvn -q compile test-compile -DskipTests
if errorlevel 1 exit /b %errorlevel%

:: 3. 构建依赖 classpath
call mvn -q "-Dmdep.outputFile=target\cp.txt" dependency:build-classpath
if errorlevel 1 exit /b %errorlevel%
set "DEPS="
set /p DEPS=<target\cp.txt

:: 4. 动态扫描 demo 类
set "DEMO_DIR=target\test-classes\io\mybatis\jink\demo"
if not exist "%DEMO_DIR%" (
    echo ^> 未找到 demo 类^（先编译项目^）
    exit /b 1
)

set /a CLASS_COUNT=0
for /f "eol=: tokens=*" %%f in ('dir /b /on "%DEMO_DIR%\*.class" 2^>nul') do (
    echo %%f | findstr /v "\$" >nul 2>&1
    if not errorlevel 1 (
        set /a CLASS_COUNT+=1
        set "CLASS_!CLASS_COUNT!=io.mybatis.jink.demo.%%~nf"
        set "NAME_!CLASS_COUNT!=%%~nf"
    )
)

if !CLASS_COUNT!==0 (
    echo ^> demo 目录为空
    exit /b 1
)

:: 5. 显示交互菜单
echo.
echo ==== jink Demo 列表 ====
for /l %%i in (1,1,!CLASS_COUNT!) do (
    echo   %%i. !NAME_%%i!
)
echo.
set /p CHOICE="请输入序号 (1-!CLASS_COUNT!): "

:: 验证输入
if not defined CHOICE goto :invalid
set /a IDX=%CHOICE% 2>nul
if !IDX! LSS 1 goto :invalid
if !IDX! GTR !CLASS_COUNT! goto :invalid

set "MAIN_CLASS=!CLASS_%CHOICE%!"
echo.
echo [jink] 启动 !MAIN_CLASS! ...
echo.

:: 6. 启动
if defined DEPS (
    "!JAVA_BIN!" -Dfile.encoding=UTF-8 -cp "target\classes;target\test-classes;!DEPS!" !MAIN_CLASS!
) else (
    "!JAVA_BIN!" -Dfile.encoding=UTF-8 -cp "target\classes;target\test-classes" !MAIN_CLASS!
)
exit /b %errorlevel%

:invalid
echo ^> 无效选择
exit /b 1
