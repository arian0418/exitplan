@echo off
cd /d "%~dp0"
if not exist build mkdir build
javac -encoding UTF-8 -d build *.java
if errorlevel 1 (
  echo Install JDK 17 or newer and ensure javac is on PATH.
  pause
  exit /b 1
)
java -cp build ExitPlan
