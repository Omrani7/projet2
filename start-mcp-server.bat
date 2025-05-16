@echo off
SETLOCAL

REM Get the port from command line arguments or use default
SET PORT=%1
IF "%PORT%"=="" SET PORT=8931

REM Check for headless mode flag
SET HEADLESS=
IF "%2"=="--headless" SET HEADLESS=--headless

echo Starting Playwright MCP server on port %PORT% %HEADLESS%... >> mcp_server_script.log 2>&1
echo Running command: npm exec -- @playwright/mcp@latest --port %PORT% %HEADLESS% >> mcp_server_script.log 2>&1

npm exec -- @playwright/mcp@latest --port %PORT% %HEADLESS% >> mcp_server_script.log 2>&1
echo MCP server exited with code %ERRORLEVEL% >> mcp_server_script.log 2>&1

ENDLOCAL 