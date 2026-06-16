@echo off
REM =============================================================
REM  从 .env 文件读取并设置 HandWrite-client 的所有环境变量
REM  Windows CMD 兜底脚本（推荐优先使用 PowerShell 版）
REM =============================================================

setlocal enabledelayedexpansion

REM ---- 参数解析 ----
set "MODE=Temporary"
set "ENV_FILE="

:parse_args
if "%~1"=="" goto end_parse
if /i "%~1"=="-m" ( set "MODE=%~2" & shift & shift & goto parse_args )
if /i "%~1"=="--mode" ( set "MODE=%~2" & shift & shift & goto parse_args )
if /i "%~1"=="-f" ( set "ENV_FILE=%~2" & shift & shift & goto parse_args )
if /i "%~1"=="--file" ( set "ENV_FILE=%~2" & shift & shift & goto parse_args )
if /i "%~1"=="-h" goto show_help
if /i "%~1"=="--help" goto show_help
echo [ERROR] 未知参数: %~1
exit /b 1

:show_help
echo Usage: %~nx0 [-m Temporary^|Permanent] [-f .env-file]
echo.
echo   -m Temporary   仅当前 CMD 会话生效 (默认)
echo   -m Permanent   永久写入用户环境变量 (使用 setx)
echo   -f FILE        指定 .env 文件路径
exit /b 0

:end_parse

REM ---- 定位 .env ----
if "%ENV_FILE%"=="" (
    set "SCRIPT_DIR=%~dp0"
    set "PROJECT_DIR=%SCRIPT_DIR%.."
    set "ENV_FILE=%PROJECT_DIR%\.env"
)

if not exist "%ENV_FILE%" (
    echo [ERROR] .env 文件不存在: %ENV_FILE%
    echo         请先执行:  copy .env.example .env
    exit /b 1
)

echo [INFO] 使用配置: %ENV_FILE%
echo [INFO] 模式    : %MODE%
echo.

REM ---- 解析并设置 ----
set "SUCCESS=0"
set "FAILED=0"

for /f "usebackq tokens=1* delims==" %%a in ("%ENV_FILE%") do (
    set "LINE=%%a"
    if not "!LINE:~0,1!"=="#" if not "!LINE!"=="" (
        set "KEY=%%a"
        set "VALUE=%%b"
        REM 去 KEY 前后空格
        for /f "tokens=* delims= " %%i in ("!KEY!") do set "KEY=%%i"
        REM 去 VALUE 前后空格
        for /f "tokens=* delims= " %%i in ("!VALUE!") do set "VALUE=%%i"
        REM 去 VALUE 首尾引号
        set "TMP=!VALUE:~0,1!"
        if "!TMP!"=="^"" (
            set "VALUE=!VALUE:~1!"
            set "TMP=!VALUE:~-1!"
            if "!TMP!"=="^"" set "VALUE=!VALUE:~0,-1!"
        )

        if /i "%MODE%"=="Temporary" (
            set "KEY=!KEY!"
        ) else (
            REM Permanent: 使用 setx
            setx !KEY! "!VALUE!" >nul 2>&1
        )
        if !errorlevel! equ 0 (
            if /i "%MODE%"=="Temporary" set "!KEY!=!VALUE!"
            echo   [OK]   !KEY! = !VALUE!
            set /a SUCCESS+=1 >nul
        ) else (
            echo   [FAIL] !KEY!
            set /a FAILED+=1 >nul
        )
    )
)

echo.
echo [INFO] 设置完成: 成功=%SUCCESS%  失败=%FAILED%

REM ---- 验证 ----
echo.
echo ================================ 验证 ================================
set "VERIFY_FAILED=0"
for /f "usebackq tokens=1* delims==" %%a in ("%ENV_FILE%") do (
    set "LINE=%%a"
    if not "!LINE:~0,1!"=="#" if not "!LINE!"=="" (
        set "KEY=%%a"
        for /f "tokens=* delims= " %%i in ("!KEY!") do set "KEY=%%i"
        if /i "%MODE%"=="Temporary" (
            set "EXPECTED=!%KEY%!"
        ) else (
            for /f "tokens=2 delims==" %%v in ('setx !KEY! 2^>nul') do set "EXPECTED=%%v"
        )
    )
)

if %VERIFY_FAILED% equ 0 (
    echo ALL %SUCCESS% 个变量验证通过
) else (
    echo 有 %VERIFY_FAILED% 个变量验证失败
)

endlocal
