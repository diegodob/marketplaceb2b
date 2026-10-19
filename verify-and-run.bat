@echo off
setlocal
call gradlew.bat clean test
if errorlevel 1 (
    echo Tests failed; the service was not started.
    exit /b 1
)
call gradlew.bat bootRun
