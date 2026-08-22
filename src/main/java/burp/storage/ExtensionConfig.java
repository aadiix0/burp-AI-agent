package burp.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtensionConfig {
    private String nvidiaApiKey = "";
    private boolean enableNvidia = true;

    private String openCodeZenApiKey = "";
    private String openCodeZenBaseUrl = "https://opencode.ai/zen/v1";
    private boolean enableOpenCodeZen = true;
    private boolean openCodeZenFreeOnly = true;

    private String aiHubMixApiKey = "";
    private String aiHubMixBaseUrl = "https://aihubmix.com/v1";
    private boolean enableAiHubMix = true;

    private String openRouterApiKey = "";
    private String openRouterBaseUrl = "https://openrouter.ai/api/v1";
    private boolean enableOpenRouter = true;

    private String googleApiKey = "";
    private String googleBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai";
    private boolean enableGoogleAiStudio = true;
    private boolean googleFreeOnly = true;

    private String cerebrasApiKey = "";
    private String cerebrasBaseUrl = "https://api.cerebras.ai/v1";
    private boolean enableCerebras = true;

    private String groqApiKey = "";
    private String groqBaseUrl = "https://api.groq.com/openai/v1";
    private boolean enableGroq = true;
    private boolean groqFreeOnly = true;

    private String cloudflareApiKey = "";
    private String cloudflareAccountId = "";
    private boolean enableCloudflare = true;

    private String customApiUrl = "";
    private String customApiKey = "";
    private boolean enableCustom = true;
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

    public String getNvidiaApiKey() { return nvidiaApiKey; }
    public void setNvidiaApiKey(String nvidiaApiKey) { this.nvidiaApiKey = nvidiaApiKey; }

    public boolean isEnableNvidia() { return enableNvidia; }
    public void setEnableNvidia(boolean enableNvidia) { this.enableNvidia = enableNvidia; }

    public String getOpenCodeZenApiKey() { return openCodeZenApiKey; }
    public void setOpenCodeZenApiKey(String openCodeZenApiKey) { this.openCodeZenApiKey = openCodeZenApiKey; }

    public boolean isEnableOpenCodeZen() { return enableOpenCodeZen; }
    public void setEnableOpenCodeZen(boolean enableOpenCodeZen) { this.enableOpenCodeZen = enableOpenCodeZen; }

    public boolean isOpenCodeZenFreeOnly() { return openCodeZenFreeOnly; }
    public void setOpenCodeZenFreeOnly(boolean openCodeZenFreeOnly) { this.openCodeZenFreeOnly = openCodeZenFreeOnly; }

    public String getOpenCodeZenBaseUrl() {
        if (openCodeZenBaseUrl == null || openCodeZenBaseUrl.trim().isEmpty() || openCodeZenBaseUrl.contains("opencodezen.com")) {
            return "https://opencode.ai/zen/v1";
        }
        return openCodeZenBaseUrl;
    }

    public void setOpenCodeZenBaseUrl(String openCodeZenBaseUrl) {
        this.openCodeZenBaseUrl = openCodeZenBaseUrl;
    }

    public String getAiHubMixApiKey() { return aiHubMixApiKey; }
    public void setAiHubMixApiKey(String aiHubMixApiKey) { this.aiHubMixApiKey = aiHubMixApiKey; }

    public String getAiHubMixBaseUrl() { return aiHubMixBaseUrl; }
    public void setAiHubMixBaseUrl(String aiHubMixBaseUrl) { this.aiHubMixBaseUrl = aiHubMixBaseUrl; }

    public boolean isEnableAiHubMix() { return enableAiHubMix; }
    public void setEnableAiHubMix(boolean enableAiHubMix) { this.enableAiHubMix = enableAiHubMix; }

    public String getOpenRouterApiKey() { return openRouterApiKey; }
    public void setOpenRouterApiKey(String openRouterApiKey) { this.openRouterApiKey = openRouterApiKey; }

    public String getOpenRouterBaseUrl() { return openRouterBaseUrl; }
    public void setOpenRouterBaseUrl(String openRouterBaseUrl) { this.openRouterBaseUrl = openRouterBaseUrl; }

    public boolean isEnableOpenRouter() { return enableOpenRouter; }
    public void setEnableOpenRouter(boolean enableOpenRouter) { this.enableOpenRouter = enableOpenRouter; }

    public String getGoogleApiKey() { return googleApiKey; }
    public void setGoogleApiKey(String googleApiKey) { this.googleApiKey = googleApiKey; }

    public String getGoogleBaseUrl() { return googleBaseUrl; }
    public void setGoogleBaseUrl(String googleBaseUrl) { this.googleBaseUrl = googleBaseUrl; }

    public boolean isEnableGoogleAiStudio() { return enableGoogleAiStudio; }
    public void setEnableGoogleAiStudio(boolean enableGoogleAiStudio) { this.enableGoogleAiStudio = enableGoogleAiStudio; }

    public boolean isGoogleFreeOnly() { return googleFreeOnly; }
    public void setGoogleFreeOnly(boolean googleFreeOnly) { this.googleFreeOnly = googleFreeOnly; }

    public String getCerebrasApiKey() { return cerebrasApiKey; }
    public void setCerebrasApiKey(String cerebrasApiKey) { this.cerebrasApiKey = cerebrasApiKey; }

    public String getCerebrasBaseUrl() { return cerebrasBaseUrl; }
    public void setCerebrasBaseUrl(String cerebrasBaseUrl) { this.cerebrasBaseUrl = cerebrasBaseUrl; }

    public boolean isEnableCerebras() { return enableCerebras; }
    public void setEnableCerebras(boolean enableCerebras) { this.enableCerebras = enableCerebras; }

    public String getGroqApiKey() { return groqApiKey; }
    public void setGroqApiKey(String groqApiKey) { this.groqApiKey = groqApiKey; }

    public String getGroqBaseUrl() { return groqBaseUrl; }
    public void setGroqBaseUrl(String groqBaseUrl) { this.groqBaseUrl = groqBaseUrl; }

    public boolean isEnableGroq() { return enableGroq; }
    public void setEnableGroq(boolean enableGroq) { this.enableGroq = enableGroq; }

    public boolean isGroqFreeOnly() { return groqFreeOnly; }
    public void setGroqFreeOnly(boolean groqFreeOnly) { this.groqFreeOnly = groqFreeOnly; }

    public String getCloudflareApiKey() { return cloudflareApiKey; }
    public void setCloudflareApiKey(String cloudflareApiKey) { this.cloudflareApiKey = cloudflareApiKey; }

    public String getCloudflareAccountId() { return cloudflareAccountId; }
    public void setCloudflareAccountId(String cloudflareAccountId) { this.cloudflareAccountId = cloudflareAccountId; }

    public boolean isEnableCloudflare() { return enableCloudflare; }
    public void setEnableCloudflare(boolean enableCloudflare) { this.enableCloudflare = enableCloudflare; }

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

    public boolean isEnableCustom() { return enableCustom; }
    public void setEnableCustom(boolean enableCustom) { this.enableCustom = enableCustom; }

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
