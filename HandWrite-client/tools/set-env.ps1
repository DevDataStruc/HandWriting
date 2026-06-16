<#
.SYNOPSIS
  从 .env 文件读取并设置 HandWrite-client 的所有环境变量。

.DESCRIPTION
  支持 Windows PowerShell 5.1+ / PowerShell Core (pwsh)。
  - 自动检测 .env 文件位置（项目根目录）
  - 支持临时设置（仅当前会话）和永久设置（写入用户/系统环境变量）
  - 内置 .env 解析（KEY=VALUE 格式，支持 # 注释、空行、引号包裹）
  - 执行后逐个验证，报告成功/失败/未设置

.PARAMETER Mode
  Temporary (默认)  - 仅当前 PowerShell 会话生效
  Permanent        - 永久写入 [System.Environment]::SetEnvironmentVariable Target=User
  System           - 永久写入系统级（需管理员权限）

.PARAMETER EnvFile
  .env 文件路径，默认：脚本所在目录的上一级/.env

.EXAMPLE
  .\tools\set-env.ps1
  .\tools\set-env.ps1 -Mode Permanent
  .\tools\set-env.ps1 -EnvFile "D:\my\.env"

.NOTES
  Author : DevDataStruc
  Tested : Windows 10 / 11 + PowerShell 5.1
#>

[CmdletBinding()]
param(
    [ValidateSet('Temporary', 'Permanent', 'System')]
    [string]$Mode = 'Temporary',

    [string]$EnvFile
)

$ErrorActionPreference = 'Stop'

# ---------- 1. 定位 .env ----------
if (-not $EnvFile) {
    $scriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
    $projectDir = Split-Path -Parent $scriptDir
    $EnvFile    = Join-Path $projectDir '.env'
}

if (-not (Test-Path $EnvFile)) {
    Write-Host "[ERROR] .env 文件不存在: $EnvFile" -ForegroundColor Red
    Write-Host "        请先执行:  cp .env.example .env" -ForegroundColor Yellow
    exit 1
}

Write-Host "[INFO] 使用配置: $EnvFile" -ForegroundColor Cyan
Write-Host "[INFO] 模式    : $Mode" -ForegroundColor Cyan
Write-Host ""

# ---------- 2. 解析 .env ----------
function Parse-EnvFile {
    param([string]$Path)

    $envMap = [ordered]@{}
    Get-Content -Path $Path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        # 跳过空行 / 注释
        if ($line -eq '' -or $line -like '#*') { return }
        # 跳过没有 = 的行
        if ($line -notmatch '^([^=]+)=(.*)$') { return }

        $key   = $matches[1].Trim()
        $value = $matches[2].Trim()
        # 去除首尾引号
        if ($value -match '^"(.*)"$' -or $value -match "^'(.*)'$") { $value = $matches[1] }

        $envMap[$key] = $value
    }
    return $envMap
}

try {
    $envMap = Parse-EnvFile -Path $EnvFile
} catch {
    Write-Host "[ERROR] 解析 .env 失败: $_" -ForegroundColor Red
    exit 1
}

if ($envMap.Count -eq 0) {
    Write-Host "[ERROR] .env 文件为空或没有有效键值对" -ForegroundColor Red
    exit 1
}

Write-Host "[INFO] 共读取 $($envMap.Count) 个变量" -ForegroundColor Cyan
Write-Host ""

# ---------- 3. 设置环境变量 ----------
$success = 0; $failed = 0
$target  = switch ($Mode) {
    'Temporary' { 'Process' }
    'Permanent' { 'User'    }
    'System'    { 'Machine' }
}

foreach ($k in $envMap.Keys) {
    $v = $envMap[$k]
    try {
        if ($Mode -eq 'Temporary') {
            Set-Item -Path "Env:$k" -Value $v -ErrorAction Stop
        } else {
            [System.Environment]::SetEnvironmentVariable($k, $v, $target)
        }
        $success++
        Write-Host "  [OK]   $k = $v" -ForegroundColor Green
    } catch {
        $failed++
        Write-Host "  [FAIL] $k : $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "[INFO] 设置完成: 成功=$success  失败=$failed" -ForegroundColor Cyan
if ($Mode -eq 'Temporary') {
    Write-Host "        ⚠️  临时模式：变量仅在当前 PowerShell 窗口有效，关闭后失效" -ForegroundColor Yellow
} else {
    Write-Host "        ✅ 永久模式：变量已写入 [$target] 环境，新打开的窗口自动生效" -ForegroundColor Green
}

# ---------- 4. 验证 ----------
Write-Host ""
Write-Host "================================ 验证 ================================" -ForegroundColor Cyan

$verifyFailed = 0
foreach ($k in $envMap.Keys) {
    $expected = $envMap[$k]
    $actual   = [System.Environment]::GetEnvironmentVariable($k, $target)
    # Permanent/System 验证写到哪就读哪；Temporary 从 Process 读
    if ($Mode -eq 'Temporary') {
        $actual = [System.Environment]::GetEnvironmentVariable($k, 'Process')
    }

    if ($actual -eq $expected) {
        Write-Host "  [✓] $k = $actual" -ForegroundColor Green
    } else {
        $verifyFailed++
        Write-Host "  [✗] $k  期望: $expected  实际: $actual" -ForegroundColor Red
    }
}

Write-Host ""
if ($verifyFailed -eq 0) {
    Write-Host "✅ 全部 $($envMap.Count) 个变量验证通过" -ForegroundColor Green
    if ($Mode -ne 'Temporary') {
        Write-Host "   请新打开一个 PowerShell 窗口运行 mvn / java 命令" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ 有 $verifyFailed 个变量验证失败" -ForegroundColor Red
    exit 1
}
