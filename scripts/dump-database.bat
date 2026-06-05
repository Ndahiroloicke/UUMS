@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0dump-database.ps1" %*
