@echo off
java "%~dp0tools\Project.java" "%~dp0." run %*
exit /b %errorlevel%
