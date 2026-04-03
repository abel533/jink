# jink 交互式 Demo 启动器
# 用法: .\run.ps1 [JDK_HOME]
#   .\run.ps1                           # 使用系统 java（须 >= 21）
#   .\run.ps1 C:\Dev\jdk-21.0.10        # 指定 JDK 路径
param([string]$JdkHome = "")

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-JavaMajorVersion {
    param([string]$JavaBin)
    try {
        $output = & $JavaBin "-version" 2>&1 | Select-Object -First 1 | Out-String
        if ($output -match '"1\.(\d+)') { return [int]$Matches[1] }  # 1.8 style
        if ($output -match '(\d+)\.\d+') { return [int]$Matches[1] }  # 11+ style
        if ($output -match '(\d+)') { return [int]$Matches[1] }
    } catch {}
    return 0
}

# 切换到项目根目录
Set-Location -LiteralPath (Split-Path $PSScriptRoot -Parent)

# 1. 确定 Java 可执行路径
$javaBin = "java"
if ($JdkHome -ne "") {
    $javaBin = Join-Path $JdkHome "bin\java.exe"
    $env:JAVA_HOME = $JdkHome
    $env:PATH = "$JdkHome\bin;$env:PATH"
    Write-Host ("[jink] 使用指定 JDK: {0}" -f $JdkHome) -ForegroundColor Cyan
} else {
    $major = Get-JavaMajorVersion "java"
    if ($major -ge 21) {
        Write-Host ("[jink] 使用系统 Java {0}" -f $major) -ForegroundColor Green
    } else {
        if ($major -eq 0) {
            Write-Host "❌ 未找到 Java，请安装 JDK 21+ 或通过参数指定路径：" -ForegroundColor Red
        } else {
            Write-Host ("❌ 当前 Java 版本 {0} < 21，请通过参数指定 JDK 21+ 路径：" -f $major) -ForegroundColor Red
        }
        Write-Host "   .\run.ps1 C:\path\to\jdk21" -ForegroundColor Yellow
        exit 1
    }
}

# 2. 设置编码
[Console]::InputEncoding  = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
cmd /c chcp 65001 > $null
$env:MAVEN_OPTS = '-Dfile.encoding=UTF-8'

# 3. 编译
Write-Host '[jink] 编译项目...' -ForegroundColor Cyan
& mvn '-q' 'compile' 'test-compile' '-DskipTests'
if ($LASTEXITCODE -ne 0) { Write-Host '❌ 编译失败' -ForegroundColor Red; exit 1 }

# 4. 构建依赖 classpath
& mvn '-q' '-Dmdep.outputFile=target\cp.txt' 'dependency:build-classpath'
if ($LASTEXITCODE -ne 0) { exit 1 }
$deps = (Get-Content -LiteralPath 'target\cp.txt' -Raw).Trim()
$cp   = if ($deps) { "target\classes;target\test-classes;$deps" } else { "target\classes;target\test-classes" }

# 5. 动态扫描 demo 类
$demoDir = "target\test-classes\io\mybatis\jink\demo"
if (-not (Test-Path $demoDir)) {
    Write-Host '❌ 未找到 demo 类（先编译项目）' -ForegroundColor Red; exit 1
}
$classes = Get-ChildItem -Path $demoDir -Filter '*.class' |
    Where-Object { $_.Name -notmatch '\$' } |
    ForEach-Object { 'io.mybatis.jink.demo.' + $_.BaseName } |
    Sort-Object

if ($classes.Count -eq 0) { Write-Host '❌ demo 目录为空' -ForegroundColor Red; exit 1 }

# 6. 显示交互菜单
Write-Host ''
Write-Host '╔══════════════════════════════╗' -ForegroundColor Green
Write-Host '║     jink Demo 列表           ║' -ForegroundColor Green
Write-Host '╚══════════════════════════════╝' -ForegroundColor Green
for ($i = 0; $i -lt $classes.Count; $i++) {
    $name = $classes[$i] -replace '^io\.mybatis\.jink\.demo\.', ''
    Write-Host ('  {0,2}. {1}' -f ($i + 1), $name)
}
Write-Host ''
$raw = Read-Host ("请输入序号 (1-{0})" -f $classes.Count)
$idx = [int]$raw - 1
if ($idx -lt 0 -or $idx -ge $classes.Count) {
    Write-Host '❌ 无效选择' -ForegroundColor Red; exit 1
}
$mainClass = $classes[$idx]

# 7. 启动
Write-Host ("`n[jink] 启动 {0} ..." -f $mainClass) -ForegroundColor Cyan
Write-Host ''
& $javaBin `
    '--enable-native-access=ALL-UNNAMED' `
    '-Dfile.encoding=UTF-8' `
    '-Dstdout.encoding=UTF-8' `
    '-Dstderr.encoding=UTF-8' `
    '-cp' $cp `
    $mainClass

exit $LASTEXITCODE
