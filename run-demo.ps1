# jink demo launcher for PowerShell
# Usage: .\run-demo.ps1 [main-class]
# Example: .\run-demo.ps1
#          .\run-demo.ps1 io.mybatis.jink.demo.SimpleDemo
param(
    [string]$MainClass = 'io.mybatis.jink.demo.InteractiveDemo'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Switch to script directory so Maven always sees the project root
Set-Location -LiteralPath $PSScriptRoot

# Enable UTF-8 for console input/output
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
cmd /c chcp 65001 > $null

# Always use JDK 21 for this project
$jdkHome = if ($env:JINK_JAVA_HOME) { $env:JINK_JAVA_HOME } else { 'D:\Dev\jdk-21' }
$env:JAVA_HOME = $jdkHome
$env:PATH = ('{0}\bin;{1}' -f $env:JAVA_HOME, $env:PATH)
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
$cp = 'target\classes;target\test-classes'
if ($deps) {
    $cp = '{0};{1}' -f $cp, $deps
}

Write-Host ('[jink] starting {0} ...' -f $MainClass) -ForegroundColor Cyan
Write-Host ''

& (Join-Path $env:JAVA_HOME 'bin\java.exe') `
    '--enable-native-access=ALL-UNNAMED' `
    '-Dfile.encoding=UTF-8' `
    '-Dstdout.encoding=UTF-8' `
    '-Dstderr.encoding=UTF-8' `
    '-cp' $cp `
    $MainClass

exit $LASTEXITCODE
