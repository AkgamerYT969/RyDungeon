@echo off
setlocal
echo ==========================================
echo RyDungeon 1.1.0 - Paper 1.21.11
echo Made By TheRynzo
echo ==========================================

where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven was not found in PATH.
    echo Install Maven and JDK 21 first.
    pause
    exit /b 1
)

call mvn clean package
if errorlevel 1 (
    echo BUILD FAILED
    pause
    exit /b 1
)

if not exist dist mkdir dist
copy /Y target\RyDungeon.jar dist\RyDungeon.jar >nul

echo.
echo BUILD SUCCESS
echo dist\RyDungeon.jar
pause
