@echo off
SET "PATH=C:\Program Files\nodejs;%PATH%"
echo Starting Playwright MCP server... >> mcp_server_script.log 2>&1
echo Running command: "C:\Program Files\nodejs\npx.cmd" -y @playwright/mcp@latest --port %1 %2 >> mcp_server_script.log 2>&1
"C:\Program Files\nodejs\npx.cmd" -y @playwright/mcp@latest --port %1 %2 >> mcp_server_script.log 2>&1
echo MCP server npx command exited with code %ERRORLEVEL% >> mcp_server_script.log 2>&1 