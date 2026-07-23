@echo off
REM SmithAI-Server startup script for Windows.
REM Creates a virtual environment if it doesn't exist, installs dependencies, and starts the server.

setlocal
set SCRIPT_DIR=%~dp0
cd /d %SCRIPT_DIR%

if not exist venv (
    echo Creating virtual environment...
    python -m venv venv
)

call venv\Scripts\activate.bat

python -c "import fastapi, uvicorn, llama_cpp" >nul 2>&1
if errorlevel 1 (
    echo Installing dependencies...
    pip install -r requirements.txt
)

echo Starting SmithAI-Server...
python app.py

pause
