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

        if (config.getNvidiaApiKey() != null && !config.getNvidiaApiKey().trim().isEmpty()) {
            List<String> list = fetchModelsFromEndpoint(NVIDIA_BASE_URL + "/models", config.getNvidiaApiKey());
            for (String m : list) {
                models.add(new ModelEntry(PROVIDER_NVIDIA, m));
            }
        }

        if (config.getOpenCodeZenApiKey() != null && !config.getOpenCodeZenApiKey().trim().isEmpty()) {
            String baseUrl = config.getOpenCodeZenBaseUrl().replaceAll("/+$", "");
            if (!baseUrl.endsWith("/models")) {
                baseUrl += "/models";
            }
            List<String> list = fetchModelsFromEndpoint(baseUrl, config.getOpenCodeZenApiKey());
            for (String m : list) {
                models.add(new ModelEntry(PROVIDER_OPENCODE, m));
            }
        }

        if (config.getCustomApiUrl() != null && !config.getCustomApiUrl().trim().isEmpty()) {
            String baseUrl = config.getCustomApiUrl().replaceAll("/+$", "");
            if (!baseUrl.endsWith("/models")) {
                baseUrl += "/models";
            }
            List<String> list = fetchModelsFromEndpoint(baseUrl, config.getCustomApiKey());
            for (String m : list) {
                models.add(new ModelEntry(PROVIDER_CUSTOM, m));
            }
        }

        // Ensure default presets are present if none were dynamically fetched or if keys are missing
        boolean hasNvidia = models.stream().anyMatch(m -> PROVIDER_NVIDIA.equalsIgnoreCase(m.getProvider()));
        boolean hasOpenCode = models.stream().anyMatch(m -> PROVIDER_OPENCODE.equalsIgnoreCase(m.getProvider()));

        if (!hasNvidia) {
            models.add(new ModelEntry(PROVIDER_NVIDIA, "deepseek-ai/deepseek-v4-flash-0731"));
            models.add(new ModelEntry(PROVIDER_NVIDIA, "deepseek-ai/deepseek-r1"));
            models.add(new ModelEntry(PROVIDER_NVIDIA, "qwen/qwen2.5-72b-instruct"));
            models.add(new ModelEntry(PROVIDER_NVIDIA, "qwen/qwen2.5-coder-32b-instruct"));
            models.add(new ModelEntry(PROVIDER_NVIDIA, "zhipuai/glm-4-9b-chat"));
            models.add(new ModelEntry(PROVIDER_NVIDIA, "meta/llama-3.3-70b-instruct"));
            models.add(new ModelEntry(PROVIDER_NVIDIA, "nvidia/llama-3.1-nemotron-70b-instruct"));
        }

        if (!hasOpenCode) {
            models.add(new ModelEntry(PROVIDER_OPENCODE, "opencode-zen/mimo-v2.5"));
            models.add(new ModelEntry(PROVIDER_OPENCODE, "opencode/deepseek-v4-flash"));
            models.add(new ModelEntry(PROVIDER_OPENCODE, "opencode-zen/coder-70b"));
            models.add(new ModelEntry(PROVIDER_OPENCODE, "opencode/claude-3-5-sonnet"));
            models.add(new ModelEntry(PROVIDER_OPENCODE, "opencode/gpt-4o"));
            models.add(new ModelEntry(PROVIDER_OPENCODE, "opencode/deepseek-r1"));
        }

        return models;
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
                    String baseUrl = config.getOpenCodeZenBaseUrl().replaceAll("/+$", "");
                    endpointUrl = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";
                    apiKey = config.getOpenCodeZenApiKey();
                } else if (PROVIDER_CUSTOM.equalsIgnoreCase(provider)) {
                    String baseUrl = config.getCustomApiUrl().replaceAll("/+$", "");
                    endpointUrl = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";
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
                    String errText = errIs != null ? new String(errIs.readAllBytes(), StandardCharsets.UTF_8) : "HTTP " + statusCode;
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
