param(
    [string[]]$Tasks = @(':app:assembleDebug'),
    [switch]$Online
)

$ErrorActionPreference = 'Stop'
$taskRepo = Split-Path -Parent $PSScriptRoot
$taskPreviousToken = $env:WAREHOUSE_DEMO_ACCESS_TOKEN
$taskToken = $taskPreviousToken
if ([string]::IsNullOrWhiteSpace($taskToken)) {
    $taskProtectedPath = Join-Path $taskRepo 'tencent-scf/.demo-access-token.dpapi'
    if (Test-Path -LiteralPath $taskProtectedPath) {
        $taskSecure = ConvertTo-SecureString -String ((Get-Content -LiteralPath $taskProtectedPath -Raw).Trim())
        $taskToken = [System.Net.NetworkCredential]::new('', $taskSecure).Password
    } else {
        $taskGenerated = Join-Path $taskRepo 'app/build/generated/source/buildConfig/debug/com/youshu/app/BuildConfig.java'
        if (Test-Path -LiteralPath $taskGenerated) {
            $taskMatch = [regex]::Match((Get-Content -LiteralPath $taskGenerated -Raw), 'WAREHOUSE_DEMO_ACCESS_TOKEN\s*=\s*"([0-9a-f]{32})"')
            if ($taskMatch.Success) { $taskToken = $taskMatch.Groups[1].Value }
        }
    }
}
if ($taskToken -notmatch '^[0-9a-f]{32}$') {
    throw '缺少有效的本地演示服务配置。请先恢复受保护配置，或设置 WAREHOUSE_DEMO_ACCESS_TOKEN；未生成缺少配置的演示包。'
}
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'C:/Program Files/Android/Android Studio/jbr')) {
    $env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
}
try {
    $env:WAREHOUSE_DEMO_ACCESS_TOKEN = $taskToken
    Push-Location -LiteralPath $taskRepo
    try {
        $taskArguments = @($Tasks) + @('--console=plain')
        if (-not $Online) { $taskArguments += '--offline' }
        & (Join-Path $taskRepo 'gradlew.bat') @taskArguments
        if ($LASTEXITCODE -ne 0) { throw "演示包构建失败，退出码 $LASTEXITCODE" }
        Write-Output '演示包构建完成，已包含原有演示服务配置。'
    } finally { Pop-Location }
} finally { $env:WAREHOUSE_DEMO_ACCESS_TOKEN = $taskPreviousToken }
