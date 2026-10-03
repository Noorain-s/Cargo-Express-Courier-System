@echo off
title Cargo Express Courier System
cd /d "%~dp0"

if not exist "target\cargo-courier-system-1.0.0.jar" (
    echo ============================================
    echo  First run detected - building the app now.
    echo  This downloads Maven automatically and can
    echo  take a few minutes. You need an internet
    echo  connection for this step only.
    echo ============================================
    call mvnw.cmd clean package -DskipTests
    if not exist "target\cargo-courier-system-1.0.0.jar" (
        echo.
        echo Build failed - scroll up to see the error.
        pause
        exit /b 1
    )
)

echo ============================================
echo  Starting Cargo Express Courier System...
echo  Once you see "Started CargoCourierApplication"
echo  open http://localhost:8081 in your browser.
echo ============================================
java -jar target\cargo-courier-system-1.0.0.jar

pause
