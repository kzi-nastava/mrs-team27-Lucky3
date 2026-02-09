@echo off
setlocal EnableDelayedExpansion

if not exist ".env" (
    echo .env file not found
    pause
    exit /b 1
)

for /f "eol=# tokens=1* delims==" %%a in (.env) do (
    set "key=%%a"
    set "val=%%b"
    for /f "tokens=*" %%k in ("!key!") do set "key=%%k"
    for /f "tokens=*" %%v in ("!val!") do set "val=%%v"
    if /i "!key!"=="DB_USERNAME" set "DB_USER=!val!"
    if /i "!key!"=="DB_PASSWORD" set "PGPASSWORD=!val!"
    if /i "!key!"=="DB_URL" set "DB_URL=!val!"
)

for /f "tokens=3 delims=/" %%a in ("!DB_URL!") do set "DB_NAME=%%a"
for /f "tokens=1 delims=?" %%a in ("!DB_NAME!") do set "DB_NAME=%%a"

if "!DB_NAME!"=="" (
    echo Could not parse DB_NAME from !DB_URL!
    pause
    exit /b 1
)

set "PG_BIN=psql"
where psql >nul 2>nul
if %errorlevel% neq 0 (
    if exist "C:\Program Files\PostgreSQL\18\bin\psql.exe" set "PG_BIN=C:\Program Files\PostgreSQL\18\bin\psql.exe"
    if exist "C:\Program Files\PostgreSQL\17\bin\psql.exe" set "PG_BIN=C:\Program Files\PostgreSQL\17\bin\psql.exe"
    if exist "C:\Program Files\PostgreSQL\16\bin\psql.exe" set "PG_BIN=C:\Program Files\PostgreSQL\16\bin\psql.exe"
    if exist "C:\Program Files\PostgreSQL\15\bin\psql.exe" set "PG_BIN=C:\Program Files\PostgreSQL\15\bin\psql.exe"
)

"!PG_BIN!" -U !DB_USER! -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '!DB_NAME!' AND pid <> pg_backend_pid();"
"!PG_BIN!" -U !DB_USER! -d postgres -c "DROP DATABASE IF EXISTS !DB_NAME!;"
"!PG_BIN!" -U !DB_USER! -d postgres -c "CREATE DATABASE !DB_NAME!;"

echo Database reset successfully.
pause
