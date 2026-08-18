@echo off
chcp 65001 >nul
title SCHEM2MCWORLD PRO

cd /d "%~dp0"

set "JAVA_CMD="

if exist "%USERPROFILE%\.jdks\jbr-21.0.7\bin\java.exe" set "JAVA_CMD=%USERPROFILE%\.jdks\jbr-21.0.7\bin\java.exe"
if "%JAVA_CMD%"=="" if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
if "%JAVA_CMD%"=="" set "JAVA_CMD=java"

set "JAR_FILE=schem2mcworld.jar"
if not exist "%JAR_FILE%" if exist "build\libs\schem2mcworld-1.0.0-all.jar" set "JAR_FILE=build\libs\schem2mcworld-1.0.0-all.jar"

if not exist "%JAR_FILE%" (
    echo [INFO] Building standalone Fat JAR, please wait...
    call gradlew.bat fatJar
    if exist "build\libs\schem2mcworld-1.0.0-all.jar" (
        copy "build\libs\schem2mcworld-1.0.0-all.jar" "schem2mcworld.jar" >nul
        set "JAR_FILE=schem2mcworld.jar"
    )
)

if not exist "%JAR_FILE%" (
    echo [ERROR] Could not find or build schem2mcworld.jar.
    pause
    exit /b 1
)

if not "%~1"=="" (
    "%JAVA_CMD%" -jar "%JAR_FILE%" %*
    echo.
    echo ========================================================================
    echo  Conversion complete! Press any key to exit...
    echo ========================================================================
    pause >nul
    exit /b 0
)

"%JAVA_CMD%" -jar "%JAR_FILE%"
if errorlevel 1 (
    echo.
    echo [Process exited with error]
)
pause
