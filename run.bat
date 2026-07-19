@echo off
chcp 65001 >nul 2>&1

set "SCRIPT_DIR=%~dp0"
set "JRE_DIR=%SCRIPT_DIR%jre-17-32"
set "JAVA_EXE=%JRE_DIR%\bin\java.exe"
set "GRADLEW=%SCRIPT_DIR%gradlew.bat"

:: check for Gradle Wrapper

if not exist "%GRADLEW%" (
    echo [ERROR] Gradle Wrapper not found: gradlew.bat
    echo Please make sure the 'gradle/wrapper' folder exists in the project.
    pause
    exit /b 1
)

:: download JRE if absent

if not exist "%JAVA_EXE%" (
    echo [INFO] Portable 32-bit JRE not found.
    echo [INFO] Downloading JRE... This happens only on the first run and may take 1-2 minutes.
    echo.

    call "%SCRIPT_DIR%gradlew.bat" ensurePortableJre --quiet

    if errorlevel 1 (
        echo.
        echo [ERROR] Failed to download or extract the portable JRE.
        echo Please check your internet connection and try again.
        echo.
        pause
        exit /b 1
    )

    echo [INFO] JRE downloaded successfully.
    echo.
)

:: run the application via Gradle

echo [INFO] Starting application...
echo.

if "%~1"=="" (
    call "%SCRIPT_DIR%gradlew.bat" runApp --quiet
) else (
    call "%SCRIPT_DIR%gradlew.bat" runApp --quiet -PappArgs=%1
)

echo.
pause
