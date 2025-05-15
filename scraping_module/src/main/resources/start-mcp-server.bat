@echo off
echo Starting Playwright MCP server on port %1...
"C:\Program Files\nodejs\npx.cmd" @playwright/mcp --port %1 %2
echo MCP server process exited with code %ERRORLEVEL% 