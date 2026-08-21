package burp.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtensionConfig {
    private String nvidiaApiKey = "";
    private String openCodeZenApiKey = "";
    private String openCodeZenBaseUrl = "https://opencode.ai/zen/v1";
    private String deepSeekWebUserToken = "";
    private String customApiUrl = "";
    private String customApiKey = "";
    private String selectedModel = "nvidia/llama-3.1-nemotron-70b-instruct";
    private Set<String> favoriteModels = new HashSet<>(Arrays.asList(
            "nvidia/llama-3.1-nemotron-70b-instruct",
            "meta/llama-3.3-70b-instruct",
            "deepseek-ai/deepseek-r1",
            "opencode-zen/coder-70b"
    ));

    private boolean enableInspectorTab = true;
    private String systemPrompt = "You are an expert bug bounty hunter and cybersecurity analyst. "
            + "Examine the provided HTTP request and HTTP response context carefully. "
            + "Identify vulnerabilities, business logic flaws, edge cases, or potential attack vectors. "
            + "Provide clear, technical proof-of-concept ideas and remediation advice.";

    private Map<String, String> customPrompts = new LinkedHashMap<>();
    private Map<String, String> vulnerabilityClasses = new LinkedHashMap<>();

    public ExtensionConfig() {
        initDefaults();
    }

    private void initDefaults() {
        if (customPrompts.isEmpty()) {
            customPrompts.put("Vulnerability Assessment", "Perform a comprehensive security audit of this HTTP request and response. Highlight any suspicious headers, parameters, authentication mechanisms, or potential logic flaws.");
            customPrompts.put("Payload Generator", "Based on the input parameters and response structure, generate tailored payload lists for potential vulnerabilities (e.g. SQLi, XSS, Command Injection, SSRF).");
            customPrompts.put("WAF Bypass Techniques", "Analyze the request format and headers. Suggest WAF bypass techniques, encoding variations, or header mutations to test against filters.");
            customPrompts.put("Business Logic Audit", "Analyze the application state, user inputs, and response. Identify potential race conditions, IDORs, privilege escalation, or workflow bypasses.");
            customPrompts.put("JWT & Auth Inspector", "Inspect any JWT tokens, cookies, or authorization headers present. Check for weak algorithms, missing signature verification, or improper token handling.");
        }

        if (vulnerabilityClasses.isEmpty()) {
            vulnerabilityClasses.put("General / Any", "Look broadly for any OWASP Top 10 security issues.");
            vulnerabilityClasses.put("IDOR / Insecure Direct Object Reference", "Focus specifically on numerical IDs, UUIDs, or user reference parameters susceptible to unauthorized object access.");
            vulnerabilityClasses.put("SSRF / Server-Side Request Forgery", "Focus on URLs, IP addresses, callbacks, or external resource references in parameters.");
            vulnerabilityClasses.put("SQL Injection", "Focus on database queries, injectable parameters, time-based, boolean-based, or union-based vectors.");
            vulnerabilityClasses.put("XSS / Cross-Site Scripting", "Focus on reflected or stored user inputs in HTML/JS contexts, missing sanitization, and CSP headers.");
            vulnerabilityClasses.put("Broken Access Control", "Focus on authorization bypasses, multi-tenant boundaries, vertical or horizontal privilege escalation.");
            vulnerabilityClasses.put("Race Condition / Concurrency", "Focus on state-changing endpoints (e.g., transfers, redemptions) susceptible to parallel request exploitation.");
            vulnerabilityClasses.put("OAuth & JWT Misconfigurations", "Focus on token handling, state parameters, redirect_uri validation, and algorithm confusion.");
            vulnerabilityClasses.put("Command / Code Injection", "Focus on system execution points, command line parameters, or unsafe deserialization.");
        }
    }

    public String getNvidiaApiKey() {
        return nvidiaApiKey;
    }

    public void setNvidiaApiKey(String nvidiaApiKey) {
        this.nvidiaApiKey = nvidiaApiKey;
    }

    public String getOpenCodeZenApiKey() {
        return openCodeZenApiKey;
    }

    public void setOpenCodeZenApiKey(String openCodeZenApiKey) {
        this.openCodeZenApiKey = openCodeZenApiKey;
    }

    public String getOpenCodeZenBaseUrl() {
        if (openCodeZenBaseUrl == null || openCodeZenBaseUrl.trim().isEmpty() || openCodeZenBaseUrl.contains("opencodezen.com")) {
            return "https://opencode.ai/zen/v1";
        }
        return openCodeZenBaseUrl;
    }

    public void setOpenCodeZenBaseUrl(String openCodeZenBaseUrl) {
        this.openCodeZenBaseUrl = openCodeZenBaseUrl;
    }

    public String getDeepSeekWebUserToken() {
        return deepSeekWebUserToken;
    }

    public void setDeepSeekWebUserToken(String deepSeekWebUserToken) {
        this.deepSeekWebUserToken = deepSeekWebUserToken;
    }

    public String getCustomApiUrl() {
        return customApiUrl;
    }

    public void setCustomApiUrl(String customApiUrl) {
        this.customApiUrl = customApiUrl;
    }

    public String getCustomApiKey() {
        return customApiKey;
    }

    public void setCustomApiKey(String customApiKey) {
        this.customApiKey = customApiKey;
    }

    public String getSelectedModel() {
        return selectedModel;
    }

    public void setSelectedModel(String selectedModel) {
        this.selectedModel = selectedModel;
    }

    public Set<String> getFavoriteModels() {
        return favoriteModels;
    }

    public void setFavoriteModels(Set<String> favoriteModels) {
        this.favoriteModels = favoriteModels;
    }

    public boolean isEnableInspectorTab() {
        return enableInspectorTab;
    }

    public void setEnableInspectorTab(boolean enableInspectorTab) {
        this.enableInspectorTab = enableInspectorTab;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Map<String, String> getCustomPrompts() {
        return customPrompts;
    }

    public void setCustomPrompts(Map<String, String> customPrompts) {
        this.customPrompts = customPrompts;
    }

    public Map<String, String> getVulnerabilityClasses() {
        return vulnerabilityClasses;
    }

    public void setVulnerabilityClasses(Map<String, String> vulnerabilityClasses) {
        this.vulnerabilityClasses = vulnerabilityClasses;
    }
}
