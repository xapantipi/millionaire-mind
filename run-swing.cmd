@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-swing.ps1" %*
exit /b %errorlevel%
