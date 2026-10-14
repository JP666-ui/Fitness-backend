@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title Fitness System Launcher

set "ROOT=%~dp0"
set "FRONTEND=%ROOT%fitness-frontend"

echo ============================================================
echo            Personal Fitness Management System
echo ============================================================
echo.
echo  Backend : http://localhost:8080
echo  Frontend: http://localhost:3000
echo  Accounts: test/123456   admin/123456
echo.
echo ============================================================
echo.

rem ------------------------------------------------------------
rem  [1/5] Check environment
rem ------------------------------------------------------------
echo [1/5] Checking environment...
echo.

set "ENV_OK=1"
set "MVN_CMD="

rem --- Java ---
where java >nul 2>nul
if !errorlevel! neq 0 (
    echo   [X] Java not found. Please install JDK 17+ and set JAVA_HOME.
    set "ENV_OK=0"
) else (
    for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VER=%%v"
    echo   [OK] Java: !JAVA_VER!
)

rem --- Maven ---
if exist "%ROOT%mvnw.cmd" (
    set "MVN_CMD=%ROOT%mvnw.cmd"
    echo   [OK] Maven wrapper found
) else (
    where mvn >nul 2>nul
    if !errorlevel! neq 0 (
        echo   [X] Maven not found and mvnw.cmd is missing.
        set "ENV_OK=0"
    ) else (
        set "MVN_CMD=mvn"
        echo   [OK] Maven found
    )
)

rem --- Node.js ---
where node >nul 2>nul
if !errorlevel! neq 0 (
    echo   [X] Node.js not found. Please install from https://nodejs.org
    set "ENV_OK=0"
) else (
    for /f "tokens=*" %%v in ('node -v') do set "NODE_VER=%%v"
    echo   [OK] Node.js: !NODE_VER!
)

rem --- Frontend folder ---
if exist "%FRONTEND%\package.json" (
    echo   [OK] Frontend project found
) else (
    echo   [X] Frontend project missing: %FRONTEND%
    set "ENV_OK=0"
)

echo.
if "!ENV_OK!"=="0" (
    echo ============================================================
    echo  Environment check FAILED. Fix the problems above first.
    echo ============================================================
    pause
    exit /b 1
)
echo   All environment checks passed.
echo.

rem ------------------------------------------------------------
rem  [2/5] Optional database init
rem ------------------------------------------------------------
echo [2/5] Database initialization - optional
echo.
echo   First run or need to rebuild tables? Enter Y
echo   Normal startup: just press Enter to skip
echo.
set "INIT_DB="
set /p "INIT_DB=  Initialize database? [Y/N, default N]: "

if /i not "!INIT_DB!"=="Y" goto SKIP_DB

rem --- locate mysql client ---
set "MYSQL_EXE="
where mysql >nul 2>nul
if !errorlevel! equ 0 set "MYSQL_EXE=mysql"

if not defined MYSQL_EXE (
    if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)
if not defined MYSQL_EXE (
    if exist "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
)
if not defined MYSQL_EXE (
    if exist "C:\Program Files (x86)\MySQL\MySQL Server 5.7\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files (x86)\MySQL\MySQL Server 5.7\bin\mysql.exe"
)

if not defined MYSQL_EXE goto NO_MYSQL

echo.
echo   mysql client: !MYSQL_EXE!
echo   WARNING: this will DROP and recreate all tables.
set "CONFIRM="
set /p "CONFIRM=  Continue? [Y/N, default N]: "
if /i not "!CONFIRM!"=="Y" goto DB_CANCELLED

echo.
echo   Importing fitness_db_complete.sql ...
"!MYSQL_EXE!" -uroot -p123456 --default-character-set=utf8mb4 < "%ROOT%fitness_db_complete.sql"
if !errorlevel! neq 0 goto DB_FAIL
echo   [OK] Database initialized.
echo   Note: restart the backend if it is already running.
goto AFTER_DB

:NO_MYSQL
echo.
echo   [X] mysql client not found.
echo       Add MySQL bin folder to PATH, or use Navicat to run:
echo       fitness_db_complete.sql
goto AFTER_DB

:DB_CANCELLED
echo   Initialization cancelled.
goto AFTER_DB

:DB_FAIL
echo.
echo   [X] Import failed.
echo       Check that MySQL is running and the password is 123456,
echo       or use Navicat to run fitness_db_complete.sql manually.
goto AFTER_DB

:SKIP_DB
echo   Skipped database initialization.

:AFTER_DB
echo.

rem ------------------------------------------------------------
rem  [3/5] Frontend dependencies
rem ------------------------------------------------------------
echo [3/5] Checking frontend dependencies...
echo.
if exist "%FRONTEND%\node_modules" goto DEPS_OK

echo   First run: installing dependencies, this may take a few minutes...
pushd "%FRONTEND%"
call npm install
if !errorlevel! neq 0 (
    echo.
    echo   [!] Install failed, retrying with npmmirror registry...
    call npm config set registry https://registry.npmmirror.com
    call npm install
    if !errorlevel! neq 0 (
        popd
        echo   [X] Dependency install failed. Check your network.
        pause
        exit /b 1
    )
)
popd
echo   [OK] Dependencies installed.
goto AFTER_DEPS

:DEPS_OK
echo   [OK] Dependencies already present, skipping install.

:AFTER_DEPS
echo.

rem ------------------------------------------------------------
rem  [4/5] Start backend
rem ------------------------------------------------------------
echo [4/5] Starting backend...
echo.

netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>nul
if !errorlevel! equ 0 goto BACKEND_RUNNING

set "JAR_FILE="
for %%f in ("%ROOT%target\Fitness-backend-*.jar") do (
    if not defined JAR_FILE set "JAR_FILE=%%f"
)

if defined JAR_FILE goto START_JAR

start "Fitness-Backend" cmd /k "cd /d "%ROOT%" && echo Compiling and starting Spring Boot... && "!MVN_CMD!" spring-boot:run"
echo   [OK] Backend window opened - first build takes 1-2 minutes.
goto WAIT_BACKEND

:START_JAR
start "Fitness-Backend" cmd /k "cd /d "%ROOT%" && echo Starting Spring Boot backend... && java -jar "!JAR_FILE!""
echo   [OK] Backend window opened - jar mode starts in about 5 seconds.
goto WAIT_BACKEND

:BACKEND_RUNNING
echo   [!] Port 8080 already in use - backend probably already running.

:WAIT_BACKEND
echo.
echo   Waiting for backend to be ready (up to 90 seconds)...
set "BACKEND_READY=0"
for /l %%i in (1,1,45) do (
    if "!BACKEND_READY!"=="0" (
        netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>nul
        if !errorlevel! equ 0 (
            set "BACKEND_READY=1"
        ) else (
            timeout /t 2 /nobreak >nul
        )
    )
)

if "!BACKEND_READY!"=="1" (
    echo   [OK] Backend ready at http://localhost:8080
) else (
    echo   [!] Backend not ready yet - check the Fitness-Backend window.
    echo       If it says "Table doesn't exist", rerun this script and init the database.
)
echo.

rem ------------------------------------------------------------
rem  [5/5] Start frontend
rem ------------------------------------------------------------
echo [5/5] Starting frontend...
echo.

netstat -ano | findstr ":3000" | findstr "LISTENING" >nul 2>nul
if !errorlevel! equ 0 goto FRONTEND_RUNNING

start "Fitness-Frontend" cmd /k "cd /d "%FRONTEND%" && echo Starting Vite dev server... && npm run dev"
echo   [OK] Frontend window opened.
goto AFTER_FRONTEND

:FRONTEND_RUNNING
echo   [!] Port 3000 already in use - frontend probably already running.

:AFTER_FRONTEND
echo.
echo ============================================================
echo                    Startup complete
echo ============================================================
echo.
echo  Open in browser: http://localhost:3000
echo.
echo  Login accounts:
echo    - User : test  / 123456
echo    - Admin: admin / 123456
echo.
echo  Notes:
echo    - Services run in separate windows; close them to stop.
echo    - First backend build may take 1-2 minutes.
echo    - If the page shows API errors, wait for "Started" in the backend window.
echo.
echo ============================================================
echo.

timeout /t 5 /nobreak >nul
start http://localhost:3000

echo Press any key to close this window - services keep running...
pause >nul
endlocal