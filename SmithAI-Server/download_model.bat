@echo off
REM Wrapper for download_model.py on Windows.

setlocal
set SCRIPT_DIR=%~dp0
cd /d %SCRIPT_DIR%

if not exist venv (
    python -m venv venv
)
call venv\Scripts\activate.bat

python download_model.py %*

pause
