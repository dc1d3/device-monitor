@echo off
chcp 65001 >nul
title Device Monitor (demo mode)
cd /d "%~dp0"

call env.cmd

if not defined MVN_CMD (
    echo [run-mock] Maven not found. Install Maven or use the bundled mvnw.cmd.
    pause
    exit /b 1
)

echo Demo mode: fake data, no phone needed.
echo http://localhost:8080
echo.

call "%MVN_CMD%" -q spring-boot:run "-Dspring-boot.run.arguments=--app.device.mock=true"

echo.
echo Server stopped. Press any key to close.
pause >nul
