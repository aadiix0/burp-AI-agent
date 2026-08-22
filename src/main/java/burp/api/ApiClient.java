package burp.api;

import burp.model.ChatMessage;
import burp.storage.ExtensionConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ApiClient {
    public static final String NVIDIA_BASE_URL = "https://integrate.api.nvidia.com/v1";

    public static final String PROVIDER_NVIDIA = "NVIDIA";
    public static final String PROVIDER_OPENCODE = "OpenCode Zen";
    public static final String PROVIDER_AIHUBMIX = "AIHubMix";
    public static final String PROVIDER_OPENROUTER = "OpenRouter";
    public static final String PROVIDER_GOOGLE = "Google AI Studio";
    public static final String PROVIDER_CEREBRAS = "Cerebras";
    public static final String PROVIDER_GROQ = "Groq";
    public static final String PROVIDER_CLOUDFLARE = "Cloudflare Workers AI";
    public static final String PROVIDER_CUSTOM = "Custom";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public interface StreamCallback {
        void onChunk(String chunk);
        void onError(Throwable throwable);
        void onComplete();
    }

    public static class ModelEntry {
        private final String provider;
        private final String rawModelId;
        private final String displayName;

        public ModelEntry(String provider, String rawModelId) {
            this.provider = provider;
            this.rawModelId = rawModelId;
            this.displayName = provider + ": " + rawModelId;
        }

        public String getProvider() { return provider; }
        public String getRawModelId() { return rawModelId; }
        public String getDisplayName() { return displayName; }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public List<ModelEntry> fetchAvailableModels(ExtensionConfig config) {
        List<ModelEntry> models = new ArrayList<>();

        if (config.isEnableNvidia() && config.getNvidiaApiKey() != null && !config.getNvidiaApiKey().trim().isEmpty()) {
            List<String> list = fetchModelsFromEndpoint(NVIDIA_BASE_URL + "/models", config.getNvidiaApiKey());
            for (String m : list) {
                models.add(new ModelEntry(PROVIDER_NVIDIA, m));
            }
        }

        if (config.isEnableOpenCodeZen() && config.getOpenCodeZenApiKey() != null && !config.getOpenCodeZenApiKey().trim().isEmpty()) {
            String baseUrl = normalizeEndpointUrl(config.getOpenCodeZenBaseUrl(), "/models");
            List<String> list = fetchModelsFromEndpoint(baseUrl, config.getOpenCodeZenApiKey());
            for (String m : list) {
                if (!config.isOpenCodeZenFreeOnly() || m.toLowerCase().endsWith("-free")) {
                    models.add(new ModelEntry(PROVIDER_OPENCODE, m));
                }
            }
        }

        if (config.isEnableAiHubMix() && config.getAiHubMixApiKey() != null && !config.getAiHubMixApiKey().trim().isEmpty()) {
            String baseUrl = normalizeEndpointUrl(config.getAiHubMixBaseUrl(), "/models");
            List<String> list = fetchModelsFromEndpoint(baseUrl, config.getAiHubMixApiKey());
            for (String m : list) {
                models.add(new ModelEntry(PROVIDER_AIHUBMIX, m));
            }
        }

        if (config.isEnableOpenRouter() && config.getOpenRouterApiKey() != null && !config.getOpenRouterApiKey().trim().isEmpty()) {
            String baseUrl = normalizeEndpointUrl(config.getOpenRouterBaseUrl(), "/models");
            List<String> list = fetchModelsFromEndpoint(baseUrl, config.getOpenRouterApiKey());
            for (String m : list) {
                models.add(new ModelEntry(PROVIDER_OPENROUTER, m));
            }
        }

        if (config.isEnableGoogleAiStudio() && config.getGoogleApiKey() != null && !config.getGoogleApiKey().trim().isEmpty()) {
            String baseUrl = normalizeEndpointUrl(config.getGoogleBaseUrl(), "/models");
            List<String> list = fetchModelsFromEndpoint(baseUrl, config.getGoogleApiKey());
            for (String m : list) {
                if (!config.isGoogleFreeOnly() || m.toLowerCase().contains("flash") || m.toLowerCase().contains("free")) {
                    models.add(new ModelEntry(PROVIDER_GOOGLE, m));
                }
            }
        }

        if (config.isEnableCerebras() && config.getCerebrasApiKey() != null && !config.getCerebrasApiKey().trim().isEmpty()) {
            String baseUrl = normalizeEndpointUrl(config.getCerebrasBaseUrl(), "/models");
            List<String> list = fetchModelsFromEndpoint(baseUrl, config.getCerebrasApiKey());
            for (String m : list) {
                models.add(new ModelEntry(PROVIDER_CEREBRAS, m));
            }
        }

        if (config.isEnableGroq() && config.getGroqApiKey() != null && !config.getGroqApiKey().trim().isEmpty()) {
            String baseUrl = normalizeEndpointUrl(config.getGroqBaseUrl(), "/models");
            List<String> list = fetchModelsFromEndpoint(baseUrl, config.getGroqApiKey());
            for (String m : list) {
                if (!config.isGroqFreeOnly() || isGroqFreeModel(m)) {
                    models.add(new ModelEntry(PROVIDER_GROQ, m));
                }
            }
        }

        if (config.isEnableCloudflare() && config.getCloudflareApiKey() != null && !config.getCloudflareApiKey().trim().isEmpty()) {
            models.add(new ModelEntry(PROVIDER_CLOUDFLARE, "@cf/meta/llama-3.3-70b-instruct-fp8-fast"));
            models.add(new ModelEntry(PROVIDER_CLOUDFLARE, "@cf/deepseek-ai/deepseek-r1-distill-qwen-32b"));
            models.add(new ModelEntry(PROVIDER_CLOUDFLARE, "@cf/meta/llama-3.1-8b-instruct"));
            models.add(new ModelEntry(PROVIDER_CLOUDFLARE, "@cf/qwen/qwen1.5-14b-chat"));
        }

        if (config.isEnableCustom() && config.getCustomApiUrl() != null && !config.getCustomApiUrl().trim().isEmpty()) {
            String baseUrl = config.getCustomApiUrl().replaceAll("/+$", "");
            if (!baseUrl.endsWith("/models")) {
                baseUrl += "/models";
            }
            List<String> list = fetchModelsFromEndpoint(baseUrl, config.getCustomApiKey());
            for (String m : list) {
                models.add(new ModelEntry(PROVIDER_CUSTOM, m));
            }
        }

        // Ensure default presets are present if enabled but none were dynamically fetched
        if (config.isEnableNvidia() && models.stream().noneMatch(m -> PROVIDER_NVIDIA.equalsIgnoreCase(m.getProvider()))) {
            models.add(new ModelEntry(PROVIDER_NVIDIA, "deepseek-ai/deepseek-v4-flash-0731"));
            models.add(new ModelEntry(PROVIDER_NVIDIA, "deepseek-ai/deepseek-r1"));
            models.add(new ModelEntry(PROVIDER_NVIDIA, "qwen/qwen2.5-72b-instruct"));
            models.add(new ModelEntry(PROVIDER_NVIDIA, "qwen/qwen2.5-coder-32b-instruct"));
            models.add(new ModelEntry(PROVIDER_NVIDIA, "zhipuai/glm-4-9b-chat"));
            models.add(new ModelEntry(PROVIDER_NVIDIA, "meta/llama-3.3-70b-instruct"));
            models.add(new ModelEntry(PROVIDER_NVIDIA, "nvidia/llama-3.1-nemotron-70b-instruct"));
        }

        if (config.isEnableOpenCodeZen() && models.stream().noneMatch(m -> PROVIDER_OPENCODE.equalsIgnoreCase(m.getProvider()))) {
            models.add(new ModelEntry(PROVIDER_OPENCODE, "deepseek-v4-flash-free"));
            models.add(new ModelEntry(PROVIDER_OPENCODE, "mimo-v2.5-free"));
            if (!config.isOpenCodeZenFreeOnly()) {
                models.add(new ModelEntry(PROVIDER_OPENCODE, "deepseek-v4-flash"));
                models.add(new ModelEntry(PROVIDER_OPENCODE, "deepseek-v4-pro"));
                models.add(new ModelEntry(PROVIDER_OPENCODE, "gpt-5.6-sol"));
                models.add(new ModelEntry(PROVIDER_OPENCODE, "claude-sonnet-4-5"));
            }
        }

        if (config.isEnableAiHubMix() && models.stream().noneMatch(m -> PROVIDER_AIHUBMIX.equalsIgnoreCase(m.getProvider()))) {
            models.add(new ModelEntry(PROVIDER_AIHUBMIX, "gpt-4o"));
            models.add(new ModelEntry(PROVIDER_AIHUBMIX, "claude-3-5-sonnet"));
            models.add(new ModelEntry(PROVIDER_AIHUBMIX, "deepseek-r1"));
        }

        if (config.isEnableOpenRouter() && models.stream().noneMatch(m -> PROVIDER_OPENROUTER.equalsIgnoreCase(m.getProvider()))) {
            models.add(new ModelEntry(PROVIDER_OPENROUTER, "google/gemini-2.0-flash-001"));
            models.add(new ModelEntry(PROVIDER_OPENROUTER, "deepseek/deepseek-r1"));
            models.add(new ModelEntry(PROVIDER_OPENROUTER, "meta-llama/llama-3.3-70b-instruct"));
        }

        if (config.isEnableGoogleAiStudio() && models.stream().noneMatch(m -> PROVIDER_GOOGLE.equalsIgnoreCase(m.getProvider()))) {
            models.add(new ModelEntry(PROVIDER_GOOGLE, "gemini-2.0-flash"));
            models.add(new ModelEntry(PROVIDER_GOOGLE, "gemini-1.5-flash"));
            if (!config.isGoogleFreeOnly()) {
                models.add(new ModelEntry(PROVIDER_GOOGLE, "gemini-1.5-pro"));
            }
        }

        if (config.isEnableCerebras() && models.stream().noneMatch(m -> PROVIDER_CEREBRAS.equalsIgnoreCase(m.getProvider()))) {
            models.add(new ModelEntry(PROVIDER_CEREBRAS, "llama3.3-70b"));
            models.add(new ModelEntry(PROVIDER_CEREBRAS, "llama3.1-8b"));
        }

        if (config.isEnableGroq() && models.stream().noneMatch(m -> PROVIDER_GROQ.equalsIgnoreCase(m.getProvider()))) {
            models.add(new ModelEntry(PROVIDER_GROQ, "llama-3.3-70b-versatile"));
            models.add(new ModelEntry(PROVIDER_GROQ, "deepseek-r1-distill-llama-70b"));
            models.add(new ModelEntry(PROVIDER_GROQ, "gemma2-9b-it"));
        }

        return models;
    }

    private boolean isGroqFreeModel(String modelId) {
        String lower = modelId.toLowerCase();
        return lower.contains("versatile") || lower.contains("llama-3") || lower.contains("gemma") || lower.contains("deepseek-r1");
    }

    private List<String> fetchModelsFromEndpoint(String endpointUrl, String apiKey) {
        List<String> list = new ArrayList<>();
        try {
            URL url = new URL(endpointUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    JsonNode root = objectMapper.readTree(is);
                    if (root.has("data") && root.get("data").isArray()) {
                        for (JsonNode node : root.get("data")) {
                            if (node.has("id")) {
                                list.add(node.get("id").asText());
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public void streamChatCompletion(
            ExtensionConfig config,
            ModelEntry modelEntry,
            List<ChatMessage> history,
            String newPrompt,
            StreamCallback callback) {

        new Thread(() -> {
            try {
                String endpointUrl;
                String apiKey;
                String modelId = modelEntry != null ? modelEntry.getRawModelId() : config.getSelectedModel();
                String provider = modelEntry != null ? modelEntry.getProvider() : PROVIDER_NVIDIA;

                if (PROVIDER_OPENCODE.equalsIgnoreCase(provider)) {
                    endpointUrl = normalizeEndpointUrl(config.getOpenCodeZenBaseUrl(), "/chat/completions");
                    apiKey = config.getOpenCodeZenApiKey();
                } else if (PROVIDER_AIHUBMIX.equalsIgnoreCase(provider)) {
                    endpointUrl = normalizeEndpointUrl(config.getAiHubMixBaseUrl(), "/chat/completions");
                    apiKey = config.getAiHubMixApiKey();
                } else if (PROVIDER_OPENROUTER.equalsIgnoreCase(provider)) {
                    endpointUrl = normalizeEndpointUrl(config.getOpenRouterBaseUrl(), "/chat/completions");
                    apiKey = config.getOpenRouterApiKey();
                } else if (PROVIDER_GOOGLE.equalsIgnoreCase(provider)) {
                    endpointUrl = normalizeEndpointUrl(config.getGoogleBaseUrl(), "/chat/completions");
                    apiKey = config.getGoogleApiKey();
                } else if (PROVIDER_CEREBRAS.equalsIgnoreCase(provider)) {
                    endpointUrl = normalizeEndpointUrl(config.getCerebrasBaseUrl(), "/chat/completions");
                    apiKey = config.getCerebrasApiKey();
                } else if (PROVIDER_GROQ.equalsIgnoreCase(provider)) {
                    endpointUrl = normalizeEndpointUrl(config.getGroqBaseUrl(), "/chat/completions");
                    apiKey = config.getGroqApiKey();
                } else if (PROVIDER_CLOUDFLARE.equalsIgnoreCase(provider)) {
                    String accountId = config.getCloudflareAccountId().trim();
                    endpointUrl = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/v1/chat/completions";
                    apiKey = config.getCloudflareApiKey();
                } else if (PROVIDER_CUSTOM.equalsIgnoreCase(provider)) {
                    endpointUrl = normalizeEndpointUrl(config.getCustomApiUrl(), "/chat/completions");
                    apiKey = config.getCustomApiKey();
                } else {
                    endpointUrl = NVIDIA_BASE_URL + "/chat/completions";
                    apiKey = config.getNvidiaApiKey();
                }

                if (apiKey == null || apiKey.trim().isEmpty()) {
                    callback.onError(new IllegalArgumentException("API Key for " + provider + " is not set in Settings."));
                    return;
                }

                ObjectNode body = objectMapper.createObjectNode();
                body.put("model", modelId);
                body.put("stream", true);

                ArrayNode messagesNode = objectMapper.createArrayNode();

                if (config.getSystemPrompt() != null && !config.getSystemPrompt().trim().isEmpty()) {
                    ObjectNode sysMsg = objectMapper.createObjectNode();
                    sysMsg.put("role", "system");
                    sysMsg.put("content", config.getSystemPrompt());
                    messagesNode.add(sysMsg);
                }

                for (ChatMessage msg : history) {
                    ObjectNode mNode = objectMapper.createObjectNode();
                    mNode.put("role", msg.getRole().name().toLowerCase());
                    mNode.put("content", buildFormattedMessageContent(msg));
                    messagesNode.add(mNode);
                }

                body.set("messages", messagesNode);

                URL url = new URL(endpointUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "text/event-stream");
                conn.setDoOutput(true);

                byte[] input = objectMapper.writeValueAsBytes(body);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(input, 0, input.length);
                }

                int statusCode = conn.getResponseCode();
                if (statusCode != 200) {
                    InputStream errIs = conn.getErrorStream();
                    String rawErr = errIs != null ? new String(errIs.readAllBytes(), StandardCharsets.UTF_8) : "HTTP " + statusCode;
                    String errText = cleanErrorMessage(rawErr, statusCode, endpointUrl);
                    callback.onError(new RuntimeException("API error (" + statusCode + "): " + errText));
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equalsIgnoreCase(data)) {
                                break;
                            }
                            try {
                                JsonNode root = objectMapper.readTree(data);
                                if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                                    JsonNode choice = root.get("choices").get(0);
                                    if (choice.has("delta") && choice.get("delta").has("content")) {
                                        String chunk = choice.get("delta").get("content").asText();
                                        callback.onChunk(chunk);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
                callback.onComplete();

            } catch (Throwable t) {
                String errorMsg = t.getMessage();
                if (t instanceof java.net.UnknownHostException) {
                    errorMsg = "Unable to resolve API host: " + t.getMessage() + ". Please check your API Base URL in Settings.";
                } else if (t instanceof java.net.ConnectException) {
                    errorMsg = "Connection refused to API endpoint: " + t.getMessage() + ". Please verify host/port availability.";
                }
                callback.onError(new RuntimeException(errorMsg, t));
            }
        }).start();
    }

    public static String normalizeEndpointUrl(String rawBaseUrl, String suffix) {
        if (rawBaseUrl == null || rawBaseUrl.trim().isEmpty()) {
            return "https://opencode.ai/zen/v1" + suffix;
        }
        if (rawBaseUrl.contains("opencodezen.com")) {
            rawBaseUrl = "https://opencode.ai/zen/v1";
        }
        String clean = rawBaseUrl.trim().replaceAll("/+$", "");
        if (clean.endsWith(suffix)) {
            return clean;
        }
        if (suffix.equals("/chat/completions") && clean.endsWith("/models")) {
            clean = clean.substring(0, clean.length() - "/models".length());
        }
        if (suffix.equals("/models") && clean.endsWith("/chat/completions")) {
            clean = clean.substring(0, clean.length() - "/chat/completions".length());
        }
        if (!clean.toLowerCase().contains("/v1") && !clean.toLowerCase().contains("/v1beta")) {
            clean += "/v1";
        }
        return clean + suffix;
    }

    private String cleanErrorMessage(String rawResponse, int statusCode, String endpoint) {
        if (rawResponse == null) return "HTTP " + statusCode + " at " + endpoint;
        if (rawResponse.contains("<html") || rawResponse.contains("<!DOCTYPE") || rawResponse.contains("<title>")) {
            return "Endpoint return HTML 404 / error page at `" + endpoint + "`. Please check API Base URL in Settings.";
        }
        if (rawResponse.length() > 250) {
            return rawResponse.substring(0, 250) + "...";
        }
        return rawResponse;
    }

    private String buildFormattedMessageContent(ChatMessage msg) {
        StringBuilder sb = new StringBuilder();
        if (msg.getContent() != null && !msg.getContent().trim().isEmpty()) {
            sb.append(msg.getContent()).append("\n\n");
        }
        if (msg.getHttpRequest() != null && !msg.getHttpRequest().isEmpty()) {
            sb.append("### Attached HTTP Request\n```http\n")
                    .append(msg.getHttpRequest())
                    .append("\n```\n\n");
        }
        if (msg.getHttpResponse() != null && !msg.getHttpResponse().isEmpty()) {
            sb.append("### Attached HTTP Response\n```http\n")
                    .append(msg.getHttpResponse())
                    .append("\n```\n");
        }
        return sb.toString();
    }
}
