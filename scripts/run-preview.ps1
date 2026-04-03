# CopilotDemo 静态预览（不进入 raw mode，可指定尺寸）
# Usage: .\run-preview.ps1 [width] [height]
param(
    [int]$Width = 80,
    [int]$Height = 24
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path $PSScriptRoot -Parent)

[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
cmd /c chcp 65001 > $null

$jdkHome = if ($env:JINK_JAVA_HOME) { $env:JINK_JAVA_HOME } else { 'D:\Dev\jdk-21' }
$env:JAVA_HOME = $jdkHome
$env:PATH = ('{0}\bin;{1}' -f $env:JAVA_HOME, $env:PATH)
$env:MAVEN_OPTS = '-Dfile.encoding=UTF-8'

& mvn '-q' 'compile' 'test-compile' '-DskipTests'
& mvn '-q' '-Dmdep.outputFile=target\cp.txt' 'dependency:build-classpath'

$deps = (Get-Content -LiteralPath 'target\cp.txt' -Raw).Trim()
$cp = 'target\classes;target\test-classes'
if ($deps) { $cp = '{0};{1}' -f $cp, $deps }

& (Join-Path $env:JAVA_HOME 'bin\java.exe') `
    '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' `
    '-cp' $cp `
    'io.mybatis.jink.demo.CopilotDemoPreview' $Width $Height
