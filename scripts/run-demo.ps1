# jink demo launcher for PowerShell
# Usage: .\run-demo.ps1 [main-class] [JDK_HOME]
# Example: .\run-demo.ps1
#          .\run-demo.ps1 io.mybatis.jink.demo.SimpleDemo
#          .\run-demo.ps1 io.mybatis.jink.demo.SimpleDemo C:\Dev\jdk-21
param(
    [string]$MainClass = 'io.mybatis.jink.demo.Counter',
    [string]$JdkHome   = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-JavaMajorVersion {
    param([string]$JavaBin)
    try {
        $output = & $JavaBin "-version" 2>&1 | Select-Object -First 1 | Out-String
        if ($output -match '"1\.(\d+)') { return [int]$Matches[1] }
        if ($output -match '(\d+)\.\d+')  { return [int]$Matches[1] }
        if ($output -match '(\d+)')        { return [int]$Matches[1] }
    } catch {}
    return 0
}

# Switch to project root (scripts are in scripts/ subdirectory)
Set-Location -LiteralPath (Split-Path $PSScriptRoot -Parent)

# Enable UTF-8 for console input/output
[Console]::InputEncoding  = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
cmd /c chcp 65001 > $null

# Determine Java binary
$javaBin = "java"
if ($JdkHome -ne "") {
    $javaBin = Join-Path $JdkHome "bin\java.exe"
    $env:JAVA_HOME = $JdkHome
    $env:PATH = "$JdkHome\bin;$env:PATH"
} elseif ($env:JINK_JAVA_HOME) {
    $javaBin = Join-Path $env:JINK_JAVA_HOME "bin\java.exe"
    $env:JAVA_HOME = $env:JINK_JAVA_HOME
    $env:PATH = "$env:JINK_JAVA_HOME\bin;$env:PATH"
} else {
    $major = Get-JavaMajorVersion "java"
    if ($major -lt 21) {
        if ($major -eq 0) {
            Write-Host "❌ 未找到 Java，请设置 JINK_JAVA_HOME 或通过第二个参数指定 JDK 21+ 路径" -ForegroundColor Red
        } else {
            Write-Host ("❌ 当前 Java {0} < 21，请设置 JINK_JAVA_HOME 或通过第二个参数指定 JDK 21+ 路径" -f $major) -ForegroundColor Red
        }
        exit 1
    }
    # 从 java.exe 路径推导 JAVA_HOME（供 Maven 使用）
    $javaExe = (Get-Command java -ErrorAction SilentlyContinue)?.Source
    if ($javaExe) {
        $detectedHome = Split-Path (Split-Path $javaExe -Parent) -Parent
        $env:JAVA_HOME = $detectedHome
        $env:PATH = "$detectedHome\bin;$env:PATH"
    }
}

$env:MAVEN_OPTS = '-Dfile.encoding=UTF-8'

Write-Host '[jink] compiling project...' -ForegroundColor Cyan
& mvn '-q' 'compile' 'test-compile' '-DskipTests'
if ($LASTEXITCODE -ne 0) {
    Write-Host '[jink] compile failed.' -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host '[jink] building dependency classpath...' -ForegroundColor Cyan
& mvn '-q' '-Dmdep.outputFile=target\cp.txt' 'dependency:build-classpath'
if ($LASTEXITCODE -ne 0) {
    Write-Host '[jink] classpath build failed.' -ForegroundColor Red
    exit $LASTEXITCODE
}

$deps = (Get-Content -LiteralPath 'target\cp.txt' -Raw).Trim()
$cp   = if ($deps) { "target\classes;target\test-classes;$deps" } else { "target\classes;target\test-classes" }

Write-Host ('[jink] starting {0} ...' -f $MainClass) -ForegroundColor Cyan
Write-Host ''

& $javaBin `
    '--enable-native-access=ALL-UNNAMED' `
    '-Dfile.encoding=UTF-8' `
    '-Dstdout.encoding=UTF-8' `
    '-Dstderr.encoding=UTF-8' `
    '-cp' $cp `
    $MainClass

exit $LASTEXITCODE

