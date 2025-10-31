package burp.ai;

import burp.api.montoya.logging.Logging;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PerplexityClient {
    private static final String API_URL = "https://api.perplexity.ai/chat/completions";
    private final String apiKey;
    private final Logging logging;
    private final Gson gson;
    private List<ConversationMessage> conversationHistory;
    
    public PerplexityClient(String apiKey, Logging logging) {
        this.apiKey = apiKey;
        this.logging = logging;
        this.gson = new Gson();
        this.conversationHistory = new ArrayList<>();
    }
    
    // Main analysis method with conversation context
    public AIResponse analyzeRequestWithContext(String httpRequest, String prompt, 
                                                String previousContext) {
        try {
            JsonObject payload = buildRequestPayload(httpRequest, prompt, previousContext);
            String response = makeApiCall(payload);
            
            // Parse and structure the response
            return parseAIResponse(response);
            
        } catch (Exception e) {
            logging.logToError("Error in AI analysis: " + e.getMessage());
            return AIResponse.error(e.getMessage());
        }
    }
    
    // Generate test payloads for specific vulnerability type
    public List<String> generateTestPayloads(String vulnerabilityType, 
                                            String context, 
                                            int count) {
        try {
            String prompt = String.format(
                "Generate %d creative test payloads for %s vulnerability testing. " +
                "Context: %s\n" +
                "Return ONLY the payloads, one per line, no explanations.",
                count, vulnerabilityType, context
            );
            
            JsonObject payload = buildSimplePayload(prompt);
            String response = makeApiCall(payload);
            
            // Extract payloads from response
            return extractPayloads(response);
            
        } catch (Exception e) {
            logging.logToError("Error generating payloads: " + e.getMessage());
            return getDefaultPayloads(vulnerabilityType);
        }
    }
    
    // Analyze response for vulnerability indicators
    public VulnerabilityAnalysis analyzeResponse(String originalRequest,
                                                 String testPayload,
                                                 String response,
                                                 String vulnerabilityType) {
        try {
            String prompt = String.format(
                "Analyze this HTTP response for %s vulnerability:\n\n" +
                "Original Request:\n%s\n\n" +
                "Test Payload: %s\n\n" +
                "Response:\n%s\n\n" +
                "Determine:\n" +
                "1. Is the vulnerability present? (YES/NO/MAYBE)\n" +
                "2. Confidence level (0-100)\n" +
                "3. Evidence from response\n" +
                "4. Next testing steps\n" +
                "5. Exploitation potential\n\n" +
                "Format as JSON: {\"vulnerable\": \"YES/NO/MAYBE\", \"confidence\": 85, " +
                "\"evidence\": \"...\", \"nextSteps\": [...], \"exploitable\": true/false}",
                vulnerabilityType, originalRequest, testPayload, response
            );
            
            JsonObject payload = buildSimplePayload(prompt);
            String apiResponse = makeApiCall(payload);
            
            return parseVulnerabilityAnalysis(apiResponse);
            
        } catch (Exception e) {
            logging.logToError("Error analyzing response: " + e.getMessage());
            return VulnerabilityAnalysis.unknown();
        }
    }
    
    // Generate next testing step based on current findings
    public TestingRecommendation getNextTestingStep(AITask task) {
        try {
            StringBuilder context = new StringBuilder();
            context.append("Current Task: ").append(task.getDescription()).append("\n\n");
            context.append("Steps taken so far:\n");
            
            for (AITaskStep step : task.getSteps()) {
                context.append("- ").append(step.getAction()).append("\n");
                context.append("  Result: ").append(step.getSummary()).append("\n");
            }
            
            String prompt = "Based on the testing progress above, what should be the next " +
                          "specific testing action? Provide:\n" +
                          "1. Action type (TEST_PAYLOAD, ANALYZE_RESPONSE, SEND_TO_INTRUDER, etc.)\n" +
                          "2. Specific parameters\n" +
                          "3. Expected outcome\n" +
                          "Format as JSON.";
            
            JsonObject payload = buildSimplePayload(context.toString() + "\n\n" + prompt);
            String response = makeApiCall(payload);
            
            return parseTestingRecommendation(response);
            
        } catch (Exception e) {
            logging.logToError("Error getting next step: " + e.getMessage());
            return TestingRecommendation.finish("Error occurred");
        }
    }
    
    // Build request payload with conversation history
    private JsonObject buildRequestPayload(String httpRequest, String prompt, 
                                          String previousContext) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", "sonar-pro");
        
        JsonArray messages = new JsonArray();
        
        // System message with security expertise
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", 
            "You are an expert web application security tester and penetration tester. " +
            "Your role is to:\n" +
            "1. Analyze HTTP requests and responses for vulnerabilities\n" +
            "2. Generate creative and effective test payloads\n" +
            "3. Provide step-by-step testing strategies\n" +
            "4. Identify exploitation paths\n" +
            "5. Give specific, actionable recommendations\n\n" +
            "Be thorough, creative, and think like an attacker. Consider:\n" +
            "- SQL Injection (various DBs and contexts)\n" +
            "- XSS (stored, reflected, DOM-based)\n" +
            "- Authentication/Authorization bypasses\n" +
            "- CSRF and SSRF\n" +
            "- Business logic flaws\n" +
            "- API security issues\n" +
            "- Modern frameworks and their quirks"
        );
        messages.add(systemMsg);
        
        // Add previous context if exists
        if (previousContext != null && !previousContext.isEmpty()) {
            JsonObject contextMsg = new JsonObject();
            contextMsg.addProperty("role", "assistant");
            contextMsg.addProperty("content", previousContext);
            messages.add(contextMsg);
        }
        
        // User message with current request
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", 
            "HTTP Request:\n``````\n\n" +
            "Task: " + prompt
        );
        messages.add(userMsg);
        
        payload.add("messages", messages);
        payload.addProperty("temperature", 0.4);
        payload.addProperty("max_tokens", 2000);
        
        return payload;
    }
    
    private JsonObject buildSimplePayload(String prompt) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", "sonar-pro");
        
        JsonArray messages = new JsonArray();
        
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);
        
        payload.add("messages", messages);
        payload.addProperty("temperature", 0.3);
        payload.addProperty("max_tokens", 1500);
        
        return payload;
    }
    
    private String makeApiCall(JsonObject payload) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        
        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // Read response
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String error = readErrorStream(conn);
            logging.logToError("API returned code " + responseCode + ": " + error);
            throw new IOException("API Error: " + responseCode + " - " + error);
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        
        return response.toString();
    }
    
    private String readErrorStream(HttpURLConnection conn) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                error.append(line);
            }
            return error.toString();
        } catch (Exception e) {
            return "Unknown error";
        }
    }
    
    private AIResponse parseAIResponse(String jsonResponse) {
        try {
            JsonObject root = gson.fromJson(jsonResponse, JsonObject.class);
            JsonArray choices = root.getAsJsonArray("choices");
            
            if (choices != null && choices.size() > 0) {
                String content = choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
                
                return AIResponse.success(content);
            }
            
            return AIResponse.error("No response from AI");
        } catch (Exception e) {
            logging.logToError("Error parsing AI response: " + e.getMessage());
            return AIResponse.error("Parse error: " + e.getMessage());
        }
    }
    
    private List<String> extractPayloads(String response) {
        List<String> payloads = new ArrayList<>();
        
        try {
            JsonObject root = gson.fromJson(response, JsonObject.class);
            JsonArray choices = root.getAsJsonArray("choices");
            
            if (choices != null && choices.size() > 0) {
                String content = choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
                
                // Split by newlines and clean
                String[] lines = content.split("\\n");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#") && 
                        !line.toLowerCase().contains("payload")) {
                        payloads.add(line);
                    }
                }
            }
        } catch (Exception e) {
            logging.logToError("Error extracting payloads: " + e.getMessage());
        }
        
        return payloads.isEmpty() ? getDefaultPayloads("generic") : payloads;
    }
    
    private VulnerabilityAnalysis parseVulnerabilityAnalysis(String response) {
        try {
            JsonObject root = gson.fromJson(response, JsonObject.class);
            JsonArray choices = root.getAsJsonArray("choices");
            
            if (choices != null && choices.size() > 0) {
                String content = choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
                
                // Try to extract JSON from the response
                return VulnerabilityAnalysis.fromString(content);
            }
        } catch (Exception e) {
            logging.logToError("Error parsing vulnerability analysis: " + e.getMessage());
        }
        
        return VulnerabilityAnalysis.unknown();
    }
    
    private TestingRecommendation parseTestingRecommendation(String response) {
        try {
            JsonObject root = gson.fromJson(response, JsonObject.class);
            JsonArray choices = root.getAsJsonArray("choices");
            
            if (choices != null && choices.size() > 0) {
                String content = choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
                
                return TestingRecommendation.fromString(content);
            }
        } catch (Exception e) {
            logging.logToError("Error parsing testing recommendation: " + e.getMessage());
        }
        
        return TestingRecommendation.finish("Analysis complete");
    }
    
    private List<String> getDefaultPayloads(String vulnerabilityType) {
        Map<String, List<String>> defaultPayloads = new HashMap<>();
        
        defaultPayloads.put("sqli", Arrays.asList(
            "' OR '1'='1", "' OR '1'='1' --", "' OR '1'='1' /*",
            "admin'--", "' UNION SELECT NULL--", "1' AND '1'='2"
        ));
        
        defaultPayloads.put("xss", Arrays.asList(
            "<script>alert(1)</script>", "<img src=x onerror=alert(1)>",
            "javascript:alert(1)", "<svg onload=alert(1)>",
            "'><script>alert(String.fromCharCode(88,83,83))</script>"
        ));
        
        defaultPayloads.put("generic", Arrays.asList(
            "test", "../../../etc/passwd", "${7*7}", "{{7*7}}", 
            "; ls -la", "| whoami", "../../", "%00"
        ));
        
        return defaultPayloads.getOrDefault(
            vulnerabilityType.toLowerCase(), 
            defaultPayloads.get("generic")
        );
    }
    
    // Inner classes for structured responses
    public static class AIResponse {
        private final boolean success;
        private final String content;
        private final String error;
        
        private AIResponse(boolean success, String content, String error) {
            this.success = success;
            this.content = content;
            this.error = error;
        }
        
        public static AIResponse success(String content) {
            return new AIResponse(true, content, null);
        }
        
        public static AIResponse error(String error) {
            return new AIResponse(false, null, error);
        }
        
        public boolean isSuccess() { return success; }
        public String getContent() { return content; }
        public String getError() { return error; }
    }
    
    public static class VulnerabilityAnalysis {
        private String vulnerable;  // YES, NO, MAYBE
        private int confidence;
        private String evidence;
        private List<String> nextSteps;
        private boolean exploitable;
        
        public static VulnerabilityAnalysis unknown() {
            VulnerabilityAnalysis analysis = new VulnerabilityAnalysis();
            analysis.vulnerable = "MAYBE";
            analysis.confidence = 50;
            analysis.evidence = "Unable to determine";
            analysis.nextSteps = Arrays.asList("Manual verification needed");
            analysis.exploitable = false;
            return analysis;
        }
        
        public static VulnerabilityAnalysis fromString(String content) {
            // Parse JSON from content
            VulnerabilityAnalysis analysis = new VulnerabilityAnalysis();
            
            try {
                // Extract JSON if embedded in text
                int jsonStart = content.indexOf("{");
                int jsonEnd = content.lastIndexOf("}") + 1;
                
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonStr = content.substring(jsonStart, jsonEnd);
                    JsonObject json = new Gson().fromJson(jsonStr, JsonObject.class);
                    
                    analysis.vulnerable = json.has("vulnerable") ? 
                        json.get("vulnerable").getAsString() : "MAYBE";
                    analysis.confidence = json.has("confidence") ? 
                        json.get("confidence").getAsInt() : 50;
                    analysis.evidence = json.has("evidence") ? 
                        json.get("evidence").getAsString() : "See response";
                    analysis.exploitable = json.has("exploitable") ? 
                        json.get("exploitable").getAsBoolean() : false;
                    
                    // Parse next steps array
                    analysis.nextSteps = new ArrayList<>();
                    if (json.has("nextSteps") && json.get("nextSteps").isJsonArray()) {
                        JsonArray steps = json.getAsJsonArray("nextSteps");
                        for (int i = 0; i < steps.size(); i++) {
                            analysis.nextSteps.add(steps.get(i).getAsString());
                        }
                    }
                } else {
                    // Fallback to text parsing
                    analysis.vulnerable = content.contains("vulnerable") || 
                                        content.contains("exploitable") ? "YES" : "MAYBE";
                    analysis.confidence = 60;
                    analysis.evidence = content.substring(0, Math.min(200, content.length()));
                    analysis.nextSteps = Arrays.asList("Further testing recommended");
                    analysis.exploitable = false;
                }
            } catch (Exception e) {
                return unknown();
            }
            
            return analysis;
        }
        
        public String getVulnerable() { return vulnerable; }
        public int getConfidence() { return confidence; }
        public String getEvidence() { return evidence; }
        public List<String> getNextSteps() { return nextSteps; }
        public boolean isExploitable() { return exploitable; }
    }
    
    public static class TestingRecommendation {
        private String actionType;
        private Map<String, String> parameters;
        private String expectedOutcome;
        private boolean shouldContinue;
        
        public static TestingRecommendation finish(String reason) {
            TestingRecommendation rec = new TestingRecommendation();
            rec.actionType = "FINISH";
            rec.parameters = new HashMap<>();
            rec.parameters.put("reason", reason);
            rec.expectedOutcome = reason;
            rec.shouldContinue = false;
            return rec;
        }
        
        public static TestingRecommendation fromString(String content) {
            TestingRecommendation rec = new TestingRecommendation();
            
            try {
                int jsonStart = content.indexOf("{");
                int jsonEnd = content.lastIndexOf("}") + 1;
                
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonStr = content.substring(jsonStart, jsonEnd);
                    JsonObject json = new Gson().fromJson(jsonStr, JsonObject.class);
                    
                    rec.actionType = json.has("actionType") ? 
                        json.get("actionType").getAsString() : "ANALYZE";
                    rec.expectedOutcome = json.has("expectedOutcome") ? 
                        json.get("expectedOutcome").getAsString() : "";
                    rec.shouldContinue = !rec.actionType.equals("FINISH");
                    
                    rec.parameters = new HashMap<>();
                    if (json.has("parameters") && json.get("parameters").isJsonObject()) {
                        JsonObject params = json.getAsJsonObject("parameters");
                        for (String key : params.keySet()) {
                            rec.parameters.put(key, params.get(key).getAsString());
                        }
                    }
                } else {
                    return finish("Analysis complete");
                }
            } catch (Exception e) {
                return finish("Error occurred");
            }
            
            return rec;
        }
        
        public String getActionType() { return actionType; }
        public Map<String, String> getParameters() { return parameters; }
        public String getExpectedOutcome() { return expectedOutcome; }
        public boolean shouldContinue() { return shouldContinue; }
    }
    
    // Conversation message class
    private static class ConversationMessage {
        String role;
        String content;
        
        ConversationMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
