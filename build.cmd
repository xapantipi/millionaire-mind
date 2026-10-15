@echo off
java "%~dp0tools\Project.java" "%~dp0." build %*
exit /b %errorlevel%
