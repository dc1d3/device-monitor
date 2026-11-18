@echo off
chcp 65001 >nul
title Device Monitor
cd /d "%~dp0"

call env.cmd

if not defined MVN_CMD (
    echo [run] Maven not found. Install Maven or use the bundled mvnw.cmd.
    pause
    exit /b 1
)

echo Java:  %JAVA_HOME%
echo ADB:   %ADB_PATH%
echo Maven: %MVN_CMD%
echo.
echo Starting Device Monitor on http://localhost:8080 ...
echo Press Ctrl+C to stop.
echo.

call "%MVN_CMD%" -q spring-boot:run

echo.
echo Server stopped. Press any key to close.
pause >nul
