@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0package-win.ps1"
exit /b %ERRORLEVEL%
