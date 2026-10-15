@echo off
java "%~dp0tools\Project.java" "%~dp0." release %*
exit /b %errorlevel%
