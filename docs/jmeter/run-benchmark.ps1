param(
    [Parameter(Mandatory = $true)]
    [string]$Scenario,

    [Parameter(Mandatory = $true)]
    [string]$Jmx,

    [string]$JMeterBat = "",
    [string]$MysqlExe = "",

    [string]$MysqlHost = "127.0.0.1",
    [int]$MysqlPort = 3306,
    [string]$MysqlUser = "root",

    [string]$OutputRoot = "docs/jmeter",
    [switch]$SkipMysql,
    [switch]$AllowDirtyGit
)

$ErrorActionPreference = "Stop"

function Resolve-Executable([string]$PathOrCommand, [string]$CommandName) {
    if ($PathOrCommand -and (Test-Path $PathOrCommand)) {
        return (Resolve-Path $PathOrCommand).Path
    }
    $cmd = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw "找不到 $CommandName。请通过参数指定完整路径。"
}

function Get-ComSelect([string]$MysqlPath, [string]$Password) {
    $oldPwd = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $Password
        $result = & $MysqlPath `
            "--host=$MysqlHost" `
            "--port=$MysqlPort" `
            "--user=$MysqlUser" `
            "--batch" `
            "--skip-column-names" `
            "--execute=SHOW GLOBAL STATUS WHERE Variable_name='Com_select';" 2>&1

        if ($LASTEXITCODE -ne 0) {
            throw "MySQL 查询失败：$($result -join [Environment]::NewLine)"
        }

        $line = $result | Where-Object { "$_".Trim() -match '^Com_select\s+\d+$' } | Select-Object -First 1
        if (-not $line) {
            throw "无法从 MySQL 返回结果中解析 Com_select：$($result -join ' ')"
        }

        return [long](("$line".Trim() -split '\s+')[-1])
    }
    finally {
        $env:MYSQL_PWD = $oldPwd
    }
}

try {
    $repoRoot = (Get-Location).Path

    if (-not (Test-Path (Join-Path $repoRoot ".git"))) {
        throw "请在 Git 项目根目录运行此脚本。"
    }

    $branch = (& git branch --show-current).Trim()
    if ($LASTEXITCODE -ne 0) { throw "无法读取 Git 分支。" }

    $dirty = & git status --porcelain
    if ($dirty -and -not $AllowDirtyGit) {
        throw "Git 工作区存在未提交修改。请先提交/暂存，或确认后使用 -AllowDirtyGit。"
    }

    $commit = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw "无法读取 Git commit。" }

    Write-Host "收到的 Jmx 参数：[$Jmx]"
    Write-Host "参数长度：$($Jmx.Length)"
    Write-Host "是否绝对路径：$([IO.Path]::IsPathRooted($Jmx))"

    $jmxPath = if ([IO.Path]::IsPathRooted($Jmx)) { $Jmx } else { Join-Path $repoRoot $Jmx }
    if (-not (Test-Path $jmxPath)) { throw "找不到 JMX 文件：$jmxPath" }
    $jmxPath = (Resolve-Path $jmxPath).Path

    $jmeterPath = Resolve-Executable $JMeterBat "jmeter.bat"

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $safeScenario = ($Scenario -replace '[^a-zA-Z0-9_-]', '-')
    $runName = "$safeScenario-$timestamp"

    $root = Join-Path $repoRoot $OutputRoot
    $resultsDir = Join-Path $root "results"
    $reportsDir = Join-Path $root "reports"
    New-Item -ItemType Directory -Force -Path $resultsDir, $reportsDir | Out-Null

    $jtlPath = Join-Path $resultsDir "$runName.jtl"
    $reportPath = Join-Path $reportsDir $runName
    $logPath = Join-Path $resultsDir "$runName-console.log"
    $summaryPath = Join-Path $resultsDir "$runName-summary.md"

    if (Test-Path $reportPath) { throw "报告目录已存在，拒绝覆盖：$reportPath" }

    $before = $null
    $after = $null
    $mysqlPath = $null
    $mysqlPassword = $null

    if (-not $SkipMysql) {
        $mysqlPath = Resolve-Executable $MysqlExe "mysql.exe"
        $secure = Read-Host "请输入 MySQL 用户 $MysqlUser 的密码（输入不显示）" -AsSecureString
        $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try {
            $mysqlPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
        }
        finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
        }
        $before = Get-ComSelect $mysqlPath $mysqlPassword
        Write-Host "压测前 Com_select: $before" -ForegroundColor Cyan
    }

    Write-Host ""
    Write-Host "场景: $Scenario"
    Write-Host "分支: $branch"
    Write-Host "Commit: $commit"
    Write-Host "JMX: $jmxPath"
    Write-Host "开始执行 JMeter 非 GUI 压测..." -ForegroundColor Green

    $jmeterArgs = @(
        "-n",
        "-t", $jmxPath,
        "-l", $jtlPath,
        "-e",
        "-o", $reportPath
    )

    & $jmeterPath @jmeterArgs 2>&1 | Tee-Object -FilePath $logPath
    $jmeterExit = $LASTEXITCODE

    if (-not $SkipMysql) {
        $after = Get-ComSelect $mysqlPath $mysqlPassword
        Write-Host "压测后 Com_select: $after" -ForegroundColor Cyan
    }

    $deltaText = "未采集"
    if ($null -ne $before -and $null -ne $after) {
        $delta = $after - $before
        $deltaText = "$delta"
    }

    $endTime = Get-Date
    $beforeText = if ($null -eq $before) { "not collected" } else { "$before" }
    $afterText = if ($null -eq $after) { "not collected" } else { "$after" }
    $lines = @(
        "# JMeter Benchmark Run",
        "",
        "- Scenario: $Scenario",
        "- Start time: $timestamp",
        "- End time: $($endTime.ToString('yyyy-MM-dd HH:mm:ss'))",
        "- Git branch: $branch",
        "- Git commit: ``$commit``",
        "- JMX: ``$Jmx``",
        "- JMeter exit code: $jmeterExit",
        "- JTL: ``$jtlPath``",
        "- HTML report: ``$reportPath/index.html``",
        "- MySQL Com_select before: $beforeText",
        "- MySQL Com_select after: $afterText",
        "- MySQL Com_select delta: $deltaText",
        "",
        "> Com_select 是 MySQL 全局累计计数。压测窗口内的其他连接、定时任务和手动 SQL 都可能影响差值。",
        "> 请结合 JMeter HTML 报告、数据库业务结果和测试环境记录解读，不要把全局差值直接当成接口专属 SQL 数量。"
    )
    Set-Content -Path $summaryPath -Value $lines -Encoding UTF8

    Write-Host ""
    Write-Host "本轮记录已生成：" -ForegroundColor Green
    Write-Host "JTL:      $jtlPath"
    Write-Host "HTML:     $reportPath\index.html"
    Write-Host "Summary:  $summaryPath"

    if ($jmeterExit -ne 0) {
        throw "JMeter 执行失败，退出码：$jmeterExit。请检查控制台日志：$logPath"
    }
}
catch {
    Write-Error $_
    exit 1
}
finally {
    $mysqlPassword = $null
}
