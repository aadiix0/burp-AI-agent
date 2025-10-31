package burp.testing;

import burp.ai.PerplexityClient;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.util.*;

public class TestingStrategy {
    private final MontoyaApi api;
    private final PerplexityClient perplexityClient;
    
    public TestingStrategy(MontoyaApi api, PerplexityClient perplexityClient) {
        this.api = api;
        this.perplexityClient = perplexityClient;
    }
    
    /**
     * Determine the best testing strategy based on request characteristics
     */
    public Strategy determineStrategy(HttpRequestResponse requestResponse) {
        HttpRequest request = requestResponse.request();
        HttpResponse response = requestResponse.response();
        
        Strategy strategy = new Strategy();
        strategy.priorityTests = new ArrayList<>();
        strategy.injectionPoints = new ArrayList<>();
        strategy.reasoning = new StringBuilder();
        
        // Analyze request method
        String method = request.method();
        strategy.reasoning.append("Request Method: ").append(method).append("\n");
        
        if (method.equals("POST")) {
            strategy.priorityTests.add(VulnerabilityTester.TestType.SQL_INJECTION);
            strategy.priorityTests.add(VulnerabilityTester.TestType.XSS);
            strategy.priorityTests.add(VulnerabilityTester.TestType.COMMAND_INJECTION);
            strategy.reasoning.append("- POST request detected: Testing for injection vulnerabilities\n");
        }
        
        if (method.equals("GET")) {
            strategy.priorityTests.add(VulnerabilityTester.TestType.XSS);
            strategy.priorityTests.add(VulnerabilityTester.TestType.SQL_INJECTION);
            strategy.priorityTests.add(VulnerabilityTester.TestType.PATH_TRAVERSAL);
            strategy.reasoning.append("- GET request detected: Testing for reflected vulnerabilities\n");
        }
        
        // Analyze URL path
        String url = request.url();
        if (url.contains("upload") || url.contains("file")) {
            strategy.priorityTests.add(VulnerabilityTester.TestType.PATH_TRAVERSAL);
            strategy.priorityTests.add(VulnerabilityTester.TestType.XXE);
            strategy.reasoning.append("- File handling endpoint detected\n");
        }
        
        if (url.contains("admin") || url.contains("user") || url.contains("account")) {
            strategy.priorityTests.add(VulnerabilityTester.TestType.AUTHENTICATION_BYPASS);
            strategy.priorityTests.add(VulnerabilityTester.TestType.AUTHORIZATION_BYPASS);
            strategy.reasoning.append("- Administrative endpoint detected\n");
        }
        
        if (url.contains("api") || url.contains("rest") || url.contains("json")) {
            strategy.priorityTests.add(VulnerabilityTester.TestType.SQL_INJECTION);
            strategy.priorityTests.add(VulnerabilityTester.TestType.AUTHORIZATION_BYPASS);
            strategy.reasoning.append("- API endpoint detected\n");
        }
        
        // Analyze content type
        String contentType = request.headerValue("Content-Type");
        if (contentType != null) {
            strategy.reasoning.append("Content-Type: ").append(contentType).append("\n");
            
            if (contentType.contains("xml")) {
                strategy.priorityTests.add(VulnerabilityTester.TestType.XXE);
                strategy.reasoning.append("- XML content detected: Testing for XXE\n");
            }
            
            if (contentType.contains("json")) {
                strategy.priorityTests.add(VulnerabilityTester.TestType.SQL_INJECTION);
                strategy.reasoning.append("- JSON content detected: Testing for injection\n");
            }
            
            if (contentType.contains("multipart")) {
                strategy.priorityTests.add(VulnerabilityTester.TestType.PATH_TRAVERSAL);
                strategy.reasoning.append("- Multipart form detected: Testing file uploads\n");
            }
        }
        
        // Analyze parameters
        int paramCount = request.parameters().size();
        if (paramCount > 0) {
            strategy.reasoning.append("Parameters found: ").append(paramCount).append("\n");
            
            request.parameters().forEach(param -> {
                String paramName = param.name().toLowerCase();
                String paramValue = param.value();
                
                // Check for URL/redirect parameters
                if (paramName.contains("url") || paramName.contains("redirect") || 
                    paramName.contains("next") || paramName.contains("goto")) {
                    if (!strategy.priorityTests.contains(VulnerabilityTester.TestType.SSRF)) {
                        strategy.priorityTests.add(VulnerabilityTester.TestType.SSRF);
                        strategy.reasoning.append("- URL parameter detected: Testing for SSRF\n");
                    }
                }
                
                // Check for file/path parameters
                if (paramName.contains("file") || paramName.contains("path") || 
                    paramName.contains("dir") || paramName.contains("folder")) {
                    if (!strategy.priorityTests.contains(VulnerabilityTester.TestType.PATH_TRAVERSAL)) {
                        strategy.priorityTests.add(VulnerabilityTester.TestType.PATH_TRAVERSAL);
                        strategy.reasoning.append("- File path parameter detected\n");
                    }
                }
                
                // Check for command parameters
                if (paramName.contains("cmd") || paramName.contains("exec") || 
                    paramName.contains("command") || paramName.contains("shell")) {
                    if (!strategy.priorityTests.contains(VulnerabilityTester.TestType.COMMAND_INJECTION)) {
                        strategy.priorityTests.add(VulnerabilityTester.TestType.COMMAND_INJECTION);
                        strategy.reasoning.append("- Command parameter detected\n");
                    }
                }
                
                // Check for SQL-related parameters
                if (paramName.contains("id") || paramName.contains("user") || 
                    paramName.contains("search") || paramName.contains("query")) {
                    if (!strategy.priorityTests.contains(VulnerabilityTester.TestType.SQL_INJECTION)) {
                        strategy.priorityTests.add(VulnerabilityTester.TestType.SQL_INJECTION);
                    }
                }
                
                // Add as injection point
                strategy.injectionPoints.add(new VulnerabilityTester.InjectionPoint(
                    param.name(),
                    VulnerabilityTester.InjectionPoint.Type.PARAMETER,
                    paramValue
                ));
            });
        }
        
        // Analyze response for technologies
        if (response != null) {
            String responseBody = response.bodyToString();
            String serverHeader = response.headerValue("Server");
            String xPoweredBy = response.headerValue("X-Powered-By");
            
            if (serverHeader != null) {
                strategy.reasoning.append("Server: ").append(serverHeader).append("\n");
            }
            
            if (xPoweredBy != null) {
                strategy.reasoning.append("X-Powered-By: ").append(xPoweredBy).append("\n");
                
                if (xPoweredBy.contains("PHP")) {
                    strategy.reasoning.append("- PHP detected: Prioritizing PHP-specific tests\n");
                }
            }
            
            // Check for database indicators
            if (responseBody.contains("MySQL") || responseBody.contains("PostgreSQL") || 
                responseBody.contains("Oracle") || responseBody.contains("MSSQL")) {
                if (!strategy.priorityTests.contains(VulnerabilityTester.TestType.SQL_INJECTION)) {
                    strategy.priorityTests.add(0, VulnerabilityTester.TestType.SQL_INJECTION);
                    strategy.reasoning.append("- Database indicators found in response\n");
                }
            }
            
            // Check for authentication indicators
            if (responseBody.contains("login") || responseBody.contains("password") || 
                responseBody.contains("signin") || response.statusCode() == 401 || 
                response.statusCode() == 403) {
                if (!strategy.priorityTests.contains(VulnerabilityTester.TestType.AUTHENTICATION_BYPASS)) {
                    strategy.priorityTests.add(VulnerabilityTester.TestType.AUTHENTICATION_BYPASS);
                    strategy.reasoning.append("- Authentication mechanism detected\n");
                }
            }
        }
        
        // Check for cookies (session management)
        String cookieHeader = request.headerValue("Cookie");
        if (cookieHeader != null && !cookieHeader.isEmpty()) {
            strategy.reasoning.append("- Cookies present: Session management in use\n");
            strategy.injectionPoints.add(new VulnerabilityTester.InjectionPoint(
                "Cookie",
                VulnerabilityTester.InjectionPoint.Type.COOKIE,
                cookieHeader
            ));
        }
        
        // Add important headers as injection points
        String userAgent = request.headerValue("User-Agent");
        if (userAgent != null) {
            strategy.injectionPoints.add(new VulnerabilityTester.InjectionPoint(
                "User-Agent",
                VulnerabilityTester.InjectionPoint.Type.HEADER,
                userAgent
            ));
        }
        
        String referer = request.headerValue("Referer");
        if (referer != null) {
            strategy.injectionPoints.add(new VulnerabilityTester.InjectionPoint(
                "Referer",
                VulnerabilityTester.InjectionPoint.Type.HEADER,
                referer
            ));
        }
        
        // If no specific tests identified, add comprehensive scan
        if (strategy.priorityTests.isEmpty()) {
            strategy.priorityTests.add(VulnerabilityTester.TestType.FULL_SCAN);
            strategy.reasoning.append("- No specific indicators: Running full scan\n");
        }
        
        strategy.reasoning.append("\nRecommended test order:\n");
        for (int i = 0; i < strategy.priorityTests.size(); i++) {
            strategy.reasoning.append((i + 1)).append(". ")
                .append(strategy.priorityTests.get(i).getDisplayName()).append("\n");
        }
        
        return strategy;
    }
    
    /**
     * Get AI-enhanced strategy recommendation
     */
    public Strategy getAIEnhancedStrategy(HttpRequestResponse requestResponse) {
        Strategy baseStrategy = determineStrategy(requestResponse);
        
        try {
            // Ask AI for additional insights
            String prompt = "Analyze this HTTP request and response, and recommend the best " +
                          "penetration testing strategy:\n\n" +
                          "Request:\n" + requestResponse.request().toString() + "\n\n" +
                          "Current strategy:\n" + baseStrategy.reasoning.toString() + "\n\n" +
                          "Provide:\n" +
                          "1. Confirm or adjust priority order\n" +
                          "2. Additional attack vectors to consider\n" +
                          "3. Specific parameters or headers to target\n" +
                          "4. Any technology-specific vulnerabilities\n" +
                          "Be concise and actionable.";
            
            PerplexityClient.AIResponse response = perplexityClient.analyzeRequestWithContext(
                requestResponse.request().toString(),
                prompt,
                null
            );
            
            if (response.isSuccess()) {
                baseStrategy.aiRecommendations = response.getContent();
                baseStrategy.reasoning.append("\n=== AI Recommendations ===\n")
                    .append(response.getContent()).append("\n");
            }
            
        } catch (Exception e) {
            api.logging().logToError("Error getting AI strategy: " + e.getMessage());
        }
        
        return baseStrategy;
    }
    
    /**
     * Suggest payload variations based on response
     */
    public List<String> suggestPayloadVariations(String originalPayload, 
                                                 HttpResponse response,
                                                 VulnerabilityTester.TestType testType) {
        List<String> variations = new ArrayList<>();
        
        if (response == null) {
            return variations;
        }
        
        String responseBody = response.bodyToString();
        int statusCode = response.statusCode();
        
        // Analyze response characteristics
        boolean hasWAF = detectWAF(response);
        boolean hasFiltering = detectFiltering(responseBody, originalPayload);
        boolean hasErrorMessage = detectErrorMessage(responseBody);
        
        if (hasWAF) {
            // WAF evasion techniques
            variations.addAll(generateWAFEvasionPayloads(originalPayload, testType));
        }
        
        if (hasFiltering) {
            // Encoding variations
            variations.addAll(generateEncodingVariations(originalPayload, testType));
        }
        
        if (statusCode >= 400 && statusCode < 500) {
            // Try different HTTP methods or headers
            variations.add("/* Bypass attempt */ " + originalPayload);
        }
        
        if (hasErrorMessage) {
            // Database or system-specific payloads
            variations.addAll(generateTargetedPayloads(responseBody, testType));
        }
        
        return variations;
    }
    
    private boolean detectWAF(HttpResponse response) {
        String server = response.headerValue("Server");
        if (server != null) {
            String serverLower = server.toLowerCase();
            if (serverLower.contains("cloudflare") || serverLower.contains("cloudfront") ||
                serverLower.contains("akamai") || serverLower.contains("imperva")) {
                return true;
            }
        }
        
        // Check for WAF-specific response patterns
        String body = response.bodyToString();
        return body.contains("Access Denied") || body.contains("Blocked") ||
               body.contains("Security") || response.statusCode() == 403;
    }
    
    private boolean detectFiltering(String responseBody, String payload) {
        // Check if payload was filtered/sanitized
        String encoded = htmlEncode(payload);
        return responseBody.contains(encoded) || 
               (!responseBody.contains(payload) && payload.length() > 5);
    }
    
    private boolean detectErrorMessage(String responseBody) {
        return responseBody.matches("(?i).*(error|exception|warning|fatal|syntax).*");
    }
    
    private List<String> generateWAFEvasionPayloads(String original, 
                                                   VulnerabilityTester.TestType testType) {
        List<String> evasions = new ArrayList<>();
        
        switch (testType) {
            case SQL_INJECTION -> {
                evasions.add(original.replace(" ", "/**/"));
                evasions.add(original.replace(" ", "%0a"));
                evasions.add(original.replace("'", "%27"));
                evasions.add(original.replace("union", "UnIoN"));
                evasions.add(original.replace("select", "SeLeCt"));
            }
            case XSS -> {
                evasions.add(original.replace("<", "%3C").replace(">", "%3E"));
                evasions.add(original.replace("script", "scr<>ipt"));
                evasions.add(original.toUpperCase());
                evasions.add(original.replace("alert", "confirm"));
            }
            case COMMAND_INJECTION -> {
                evasions.add(original.replace(";", "%0a"));
                evasions.add(original.replace(" ", "${IFS}"));
                evasions.add(original.replace("|", "%7c"));
            }
        }
        
        return evasions;
    }
    
    private List<String> generateEncodingVariations(String original,
                                                   VulnerabilityTester.TestType testType) {
        List<String> variations = new ArrayList<>();
