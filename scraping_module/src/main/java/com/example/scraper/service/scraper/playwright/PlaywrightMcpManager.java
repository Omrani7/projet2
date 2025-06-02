package com.example.scraper.service.scraper.playwright;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Manager for the Playwright MCP (Model Context Protocol) server
 * This class handles starting, stopping, and communicating with the MCP server
 */
@Component
@Slf4j
public class PlaywrightMcpManager {

    private Process mcpServerProcess;
    private int serverPort = 8931;
    private volatile boolean serverRunning = false;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${playwright.mcp.headless:true}")
    private boolean headless;
    
    @Value("${playwright.mcp.enabled:true}")
    private boolean enabled;
    
    @Value("${playwright.mcp.npx-path:}")
    private String configuredNpxPath;
    
    /**
     * Initialize the MCP server on application startup
     */
    @PostConstruct
    public void init() {
        if (enabled) {
            startMcpServer();
        } else {
            log.info("Playwright MCP server is disabled by configuration");
        }
    }
    
    /**
     * Start the Playwright MCP server process
     */
    public void startMcpServer() {
        try {
            log.info("Starting Playwright MCP server on port {}", serverPort);
            
            // Use absolute path to batch script to avoid working directory issues
            String scriptPath = System.getProperty("user.dir") + "/start-mcp-server.bat";
            File scriptFile = new File(scriptPath);
            log.info("Using script at: {}, exists: {}", scriptFile.getAbsolutePath(), scriptFile.exists());
            
            // If script doesn't exist, try alternate location
            if (!scriptFile.exists()) {
                scriptPath = System.getProperty("user.dir") + "/scraping_module/src/main/resources/start-mcp-server.bat";
                scriptFile = new File(scriptPath);
                log.info("Fallback - Using script at: {}, exists: {}", scriptFile.getAbsolutePath(), scriptFile.exists());
            }
            
            List<String> command = new ArrayList<>();
            command.add("cmd.exe");
            command.add("/c");
            command.add(scriptPath);
            command.add(String.valueOf(serverPort));
            
            if (headless) {
                command.add("--headless");
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            log.info("Starting MCP server with command: {}", String.join(" ", command));
            mcpServerProcess = processBuilder.start();
            
            // Start a thread to log the server output
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(mcpServerProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.debug("MCP Server: {}", line);
                        // Improved detection of server startup with additional patterns
                        if (line.contains("Listening on") || 
                            line.contains("Put this in your client config") || 
                            line.contains("localhost:" + serverPort) ||
                            line.contains("Server listening on")) {
                            serverRunning = true;
                            log.info("Playwright MCP server started successfully (detected by log parsing): {}", line);
                        }
                    }
                } catch (IOException e) {
                    if (!mcpServerProcess.isAlive() && e.getMessage().contains("Stream closed")) {
                        log.info("MCP server process output stream closed, likely process terminated.");
                    } else {
                        log.error("Error reading MCP server output", e);
                    }
                } finally {
                    log.info("MCP server output reader thread finished.");
                }
            }).start();
            
            // Wait for server to start
            int maxWaitSeconds = 30;
            boolean detectedServer = false;
            for (int i = 0; i < maxWaitSeconds; i++) {
                if (serverRunning) {
                    log.info("MCP Server startup successfully detected by log parsing mechanism.");
                    detectedServer = true;
                    break;
                }
                // Attempt a direct socket connection
                try (Socket socket = new Socket()) {
                    // Connect with a timeout of 1 second (1000 milliseconds)
                    socket.connect(new InetSocketAddress("127.0.0.1", serverPort), 1000);
                    log.info("MCP Server startup successfully detected by socket connection on port {}.", serverPort);
                    serverRunning = true;
                    detectedServer = true;
                    break; 
                } catch (IOException e) {
                    // Log the attempt, but this is expected if server is not yet up
                    log.info("Waiting for MCP server: attempt {}/{}. Socket check failed: {}", i + 1, maxWaitSeconds, e.getMessage());
                }
                
                if (serverRunning) {
                    log.info("MCP Server startup detected by log parsing mechanism (checked after socket attempt).");
                    detectedServer = true;
                    break;
                }

                if (i < maxWaitSeconds -1 ) {
                   TimeUnit.SECONDS.sleep(1);
                }
            }
            
            if (!detectedServer) {
                log.warn("MCP server didn't start (or wasn't detected by log parsing or socket connection) within {} seconds.", maxWaitSeconds);
                if (mcpServerProcess != null && mcpServerProcess.isAlive()) {
                    log.warn("MCP server process is still alive but was not detected as listening. Check 'mcp_server_script.log'.");
                } else if (mcpServerProcess != null) {
                    log.warn("MCP server process has exited with code: {}. Check 'mcp_server_script.log'.", mcpServerProcess.exitValue());
                } else {
                    log.warn("MCP server process was not successfully started or is null.");
                }
            } else {
                log.info("Playwright MCP Server successfully started and detected.");
            }
            
        } catch (IOException e) {
            log.error("Failed to start MCP server: {}", e.getMessage(), e);
            
            // Try to run a simple "where npx" command to help diagnose the problem
            try {
                ProcessBuilder whereBuilder = new ProcessBuilder("where", "npx");
                Process whereProcess = whereBuilder.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(whereProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("Found npx at: {}", line);
                    }
                }
            } catch (Exception ex) {
                log.warn("Couldn't locate npx: {}", ex.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to start MCP server", e);
        }
    }
    
    /**
     * Stop the MCP server on application shutdown
     */
    @PreDestroy
    public void shutdown() {
        if (mcpServerProcess != null && mcpServerProcess.isAlive()) {
            log.info("Shutting down Playwright MCP server");
            mcpServerProcess.destroy();
            
            try {
                // Wait for process to terminate
                if (!mcpServerProcess.waitFor(5, TimeUnit.SECONDS)) {
                    log.warn("MCP server didn't terminate gracefully, forcing termination");
                    mcpServerProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for MCP server to terminate", e);
                Thread.currentThread().interrupt();
            }
        }
        
        serverRunning = false;
    }
    
    /**
     * Check if the MCP server is running
     * @return true if the server is running
     */
    public boolean isServerRunning() {
        return serverRunning && mcpServerProcess != null && mcpServerProcess.isAlive();
    }
    
    /**
     * Get a snapshot of the current page
     * @param navigateUrl URL to navigate to before taking the snapshot, or null to use current page
     * @return JSON object with the page snapshot
     */
    public JsonNode getPageSnapshot(String navigateUrl) throws IOException, InterruptedException, ExecutionException {
        if (!isServerRunning()) {
            log.warn("MCP server is not running, attempting to restart");
            startMcpServer();
            if (!isServerRunning()) {
                throw new IOException("Failed to start MCP server");
            }
        }
        
        // First navigate to the URL if provided
        if (navigateUrl != null && !navigateUrl.isEmpty()) {
            CompletableFuture<JsonNode> navigateFuture = sendMcpRequest("browser_navigate", 
                objectMapper.createObjectNode().put("url", navigateUrl));
            
            navigateFuture.get(); // Wait for navigation to complete
        }
        
        // Then get the page snapshot and wait for the result
        return sendMcpRequest("browser_snapshot", objectMapper.createObjectNode()).get();
    }
    
    /**
     * Click on an element on the page
     * @param elementRef Element reference from the snapshot
     * @param elementDescription Human-readable description of the element
     */
    public CompletableFuture<JsonNode> clickElement(String elementRef, String elementDescription) throws IOException {
        if (!isServerRunning()) {
            log.warn("MCP server is not running, attempting to restart");
            startMcpServer();
            if (!isServerRunning()) {
                throw new IOException("Failed to start MCP server");
            }
        }
        
        log.info("Clicking on element: {}", elementDescription);
        return sendMcpRequest("browser_click", 
            objectMapper.createObjectNode().put("element", elementRef));
    }
    
    /**
     * Type text into an input field
     * @param elementRef Element reference from the snapshot
     * @param elementDescription Human-readable description of the element
     * @param text Text to type
     * @param submit Whether to submit the form after typing
     */
    public CompletableFuture<JsonNode> typeText(String elementRef, String elementDescription, String text, boolean submit) throws IOException {
        if (!isServerRunning()) {
            log.warn("MCP server is not running, attempting to restart");
            startMcpServer();
            if (!isServerRunning()) {
                throw new IOException("Failed to start MCP server");
            }
        }
        
        log.info("Typing text into element: {}", elementDescription);
        return sendMcpRequest("browser_fill", 
            objectMapper.createObjectNode()
                .put("element", elementRef)
                .put("value", text)
                .put("noWaitAfter", !submit));
    }
    
    /**
     * Take a screenshot of the current page
     * @return Base64-encoded screenshot data
     */
    public String takeScreenshot() throws IOException, ExecutionException, InterruptedException {
        if (!isServerRunning()) {
            log.warn("MCP server is not running, attempting to restart");
            startMcpServer();
            if (!isServerRunning()) {
                throw new IOException("Failed to start MCP server");
            }
        }
        
        log.info("Taking screenshot");
        JsonNode result = sendMcpRequest("browser_screenshot", objectMapper.createObjectNode()).get();
        if (result.has("data")) {
            return result.get("data").asText();
        }
        
        return null;
    }
    
    /**
     * Send a request to the MCP server
     * @param method The method to call
     * @param params Parameters for the method
     * @return Future with the response from the server
     */
    private CompletableFuture<JsonNode> sendMcpRequest(String method, JsonNode params) throws IOException {
        String endpoint = String.format("http://localhost:%d/api/%s", serverPort, method);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(params.toString()))
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    return objectMapper.readTree(response.body());
                } catch (Exception e) {
                    log.error("Error parsing MCP server response", e);
                    return objectMapper.createObjectNode();
                }
            });
    }
    
    /**
     * Check if the MCP server is initialized
     * @return true if initialized
     */
    public boolean isInitialized() {
        return serverRunning;
    }
    
    /**
     * Get the MCP server endpoint
     * @return The server endpoint URL
     */
    public String getEndpoint() {
        return String.format("http://localhost:%d", serverPort);
    }
} 