@echo off
chcp 65001 >nul
title ADB restart
cd /d "%~dp0"

call env.cmd

if not defined ADB_PATH (
    echo [adb] adb.exe not found. Install Android platform-tools or set ADB_PATH.
    pause
    exit /b 1
)

echo Restarting adb server: %ADB_PATH%
"%ADB_PATH%" kill-server >nul 2>&1
timeout /t 2 >nul
"%ADB_PATH%" start-server
echo.
echo Devices:
"%ADB_PATH%" devices
echo.
pause
