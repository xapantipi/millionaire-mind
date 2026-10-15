@echo off
java "%~dp0tools\Project.java" "%~dp0." test %*
exit /b %errorlevel%
