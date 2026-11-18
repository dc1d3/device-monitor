@echo off
rem ---------------------------------------------------------------------------
rem  Common environment for the helper scripts.
rem  Used via:  call env.cmd
rem
rem  Sets (only if not already defined):
rem    JAVA_HOME  - JDK 21+
rem    ADB_PATH   - full path to adb.exe
rem    MVN_CMD    - mvnw.cmd from this project, or a Maven found on the machine
rem ---------------------------------------------------------------------------

rem --- JDK: keep an existing JAVA_HOME, otherwise look in the usual places ---
if not defined JAVA_HOME if exist "D:\dev\jdk21\bin\java.exe" set "JAVA_HOME=D:\dev\jdk21"

if not defined JAVA_HOME (
    for /d %%D in ("%ProgramFiles%\Java\jdk-21*") do (
        if exist "%%~fD\bin\java.exe" set "JAVA_HOME=%%~fD"
    )
)

if not defined JAVA_HOME (
    for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do (
        if exist "%%~fD\bin\java.exe" set "JAVA_HOME=%%~fD"
    )
)

rem --- adb: ADB_PATH from the environment, D:\dev, or PATH ---
if not defined ADB_PATH if exist "D:\dev\platform-tools\adb.exe" set "ADB_PATH=D:\dev\platform-tools\adb.exe"

if not defined ADB_PATH (
    for /f "delims=" %%A in ('where adb.exe 2^>nul') do (
        if not defined ADB_PATH set "ADB_PATH=%%A"
    )
)

rem --- Maven: wrapper first, then PATH, then the portable copy on D: ---
set "MVN_CMD="
if exist "%~dp0mvnw.cmd" set "MVN_CMD=%~dp0mvnw.cmd"

if not defined MVN_CMD (
    for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do (
        if not defined MVN_CMD set "MVN_CMD=%%M"
    )
)

if not defined MVN_CMD if exist "D:\dev\maven\bin\mvn.cmd" set "MVN_CMD=D:\dev\maven\bin\mvn.cmd"

if not defined JAVA_HOME (
    echo [env] JAVA_HOME is not set and JDK 21 was not found.
    echo [env] Install JDK 21 or set JAVA_HOME manually.
)

if not defined ADB_PATH (
    echo [env] adb.exe was not found. Real mode will report "телефон не найден".
    echo [env] Install Android platform-tools or set ADB_PATH.
)
