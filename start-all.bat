@echo off
chcp 936 >nul 2>&1
setlocal enabledelayedexpansion

title Fitness Project Launcher

echo ============================================================
echo    Fitness Management System - One Click Start
echo ============================================================
echo.

set ROOT=%~dp0
set FRONT=%ROOT%fitness-frontend

echo [1/3] Checking MySQL...
tasklist /FI "IMAGENAME eq mysqld.exe" 2>nul | find /I "mysqld.exe" >nul
if errorlevel 1 (
    echo    MySQL not running, trying to start...
    net start MySQL57 >nul 2>&1
    if errorlevel 1 (
        echo    [WARN] Cannot start MySQL automatically.
        echo           Please start MySQL manually, then re-run this script.
    ) else (
        echo    [OK] MySQL started
    )
) else (
    echo    [OK] MySQL is running
)
echo.

echo [2/3] Starting backend on port 8080...
netstat -ano | findstr ":8080" | findstr LISTENING >nul
if not errorlevel 1 (
    echo    [SKIP] Port 8080 already listening
) else (
    if exist "%ROOT%target\Fitness-backend-0.0.1-SNAPSHOT.jar" (
        start "Fitness-Backend" cmd /c "cd /d %ROOT% && java -jar target\Fitness-backend-0.0.1-SNAPSHOT.jar"
        echo    [OK] Backend launched
    ) else (
        echo    [WARN] JAR not found. Build first:
        echo           mvnw.cmd clean package -DskipTests
    )
)
echo.

echo [3/3] Starting frontend on port 3000...
netstat -ano | findstr ":3000" | findstr LISTENING >nul
if not errorlevel 1 (
    echo    [SKIP] Port 3000 already listening
) else (
    if not exist "%FRONT%\node_modules" (
        echo    [INFO] Installing dependencies, this may take a while...
        pushd "%FRONT%"
        call npm.cmd install
        popd
    )
    start "Fitness-Frontend" cmd /c "cd /d %FRONT% && npm.cmd run dev"
    echo    [OK] Frontend launched
)
echo.

echo ============================================================
echo    Waiting for services (max 90 seconds)...
echo ============================================================

set BACKEND_OK=0
set FRONTEND_OK=0

for /L %%i in (1,1,30) do (
    if "!BACKEND_OK!"=="0" (
        netstat -ano | findstr ":8080" | findstr LISTENING >nul
        if not errorlevel 1 set BACKEND_OK=1
    )
    if "!FRONTEND_OK!"=="0" (
        netstat -ano | findstr ":3000" | findstr LISTENING >nul
        if not errorlevel 1 set FRONTEND_OK=1
    )
    if "!BACKEND_OK!"=="1" if "!FRONTEND_OK!"=="1" goto READY
    ping -n 4 127.0.0.1 >nul
    echo    waiting...
)

:READY
echo.
echo ============================================================
echo    Status
echo ============================================================
if "!BACKEND_OK!"=="1" (
    echo    [OK]   Backend  http://localhost:8080
) else (
    echo    [FAIL] Backend not responding on 8080
    echo           Check the "Fitness-Backend" window for errors
)

if "!FRONTEND_OK!"=="1" (
    echo    [OK]   Frontend http://localhost:3000
) else (
    echo    [FAIL] Frontend not responding on 3000
    echo           Check the "Fitness-Frontend" window for errors
)

echo.
echo ============================================================
echo    Accounts
echo ============================================================
echo    Test user : test  / 123456
echo    Admin     : admin / 123456
echo.

if "!BACKEND_OK!"=="1" if "!FRONTEND_OK!"=="1" (
    echo Opening browser in 3 seconds...
    ping -n 4 127.0.0.1 >nul
    start http://localhost:3000
) else (
    echo Fix the issues above, then run this script again.
    pause
)

endlocal
