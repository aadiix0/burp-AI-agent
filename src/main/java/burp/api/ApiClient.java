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
    public static final String PROVIDER_DEEPSEEK_WEB = "DeepSeek Web";
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
            String baseUrl = normalizeEndpointUrl(config.getOpenCodeZenBaseUrl(), "/models");
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

        models.add(new ModelEntry(PROVIDER_DEEPSEEK_WEB, "deepseek-webui"));

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
            models.add(new ModelEntry(PROVIDER_OPENCODE, "deepseek-v4-flash-free"));
            models.add(new ModelEntry(PROVIDER_OPENCODE, "deepseek-v4-flash"));
            models.add(new ModelEntry(PROVIDER_OPENCODE, "mimo-v2.5-free"));
            models.add(new ModelEntry(PROVIDER_OPENCODE, "deepseek-v4-pro"));
            models.add(new ModelEntry(PROVIDER_OPENCODE, "gpt-5.6-sol"));
            models.add(new ModelEntry(PROVIDER_OPENCODE, "claude-sonnet-4-5"));
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
            burp.model.ChatSession currentSession,
            List<ChatMessage> history,
            String newPrompt,
            boolean thinkingEnabled,
            boolean searchEnabled,
            boolean expertMode,
            StreamCallback callback) {

        new Thread(() -> {
            try {
                String endpointUrl;
                String apiKey;
                String modelId = modelEntry != null ? modelEntry.getRawModelId() : config.getSelectedModel();
                String provider = modelEntry != null ? modelEntry.getProvider() : PROVIDER_NVIDIA;

                if (PROVIDER_DEEPSEEK_WEB.equalsIgnoreCase(provider)) {
                    streamDeepSeekWebCompletion(config, currentSession, history, newPrompt, thinkingEnabled, searchEnabled, expertMode, callback);
                    return;
                } else if (PROVIDER_OPENCODE.equalsIgnoreCase(provider)) {
                    endpointUrl = normalizeEndpointUrl(config.getOpenCodeZenBaseUrl(), "/chat/completions");
                    apiKey = config.getOpenCodeZenApiKey();
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

    private String createDeepSeekWebSession(ExtensionConfig config, String authToken, String cookie) throws Exception {
        URL url = new URL("https://chat.deepseek.com/api/v6/chat/session/create");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(12000);
        applyDeepSeekWebHeaders(conn, config, authToken, cookie);
        conn.setDoOutput(true);

        byte[] input = "{}".getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
        if (is != null) {
            String rawResp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (rawResp.trim().startsWith("<") || rawResp.contains("<html") || rawResp.contains("<!DOCTYPE")) {
                throw new RuntimeException("Cloudflare / Web protection returned HTML (HTTP " + code + "). Please update Auth Token / Cookies in Settings.");
            }
            if (code == 200) {
                JsonNode root = objectMapper.readTree(rawResp);
                if (root.has("data") && root.get("data").has("biz_data") && root.get("data").get("biz_data").has("chat_session_id")) {
                    return root.get("data").get("biz_data").get("chat_session_id").asText();
                } else if (root.has("data") && root.get("data").has("chat_session_id")) {
                    return root.get("data").get("chat_session_id").asText();
                }
            }
        }
        return null;
    }

    private void applyDeepSeekWebHeaders(HttpURLConnection conn, ExtensionConfig config, String authToken, String cookie) {
        if (authToken != null && !authToken.trim().isEmpty()) {
            String cleanAuth = authToken.trim();
            if (!cleanAuth.toLowerCase().startsWith("bearer ")) {
                cleanAuth = "Bearer " + cleanAuth;
            }
            conn.setRequestProperty("Authorization", cleanAuth);
        }

        if (cookie != null && !cookie.trim().isEmpty()) {
            conn.setRequestProperty("Cookie", cookie.trim());
        }

        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:155.0) Gecko/20100101 Firefox/155.0");
        conn.setRequestProperty("Referer", "https://chat.deepseek.com/");
        conn.setRequestProperty("x-client-bundle-id", "com.deepseek.chat");
        conn.setRequestProperty("x-client-platform", "web");
        conn.setRequestProperty("x-client-version", "2.4.0");
        conn.setRequestProperty("x-client-locale", "en_US");

        if (config != null && config.getDeepSeekWebCustomHeaders() != null) {
            for (java.util.Map.Entry<String, String> entry : config.getDeepSeekWebCustomHeaders().entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    private void streamDeepSeekWebCompletion(
            ExtensionConfig config,
            burp.model.ChatSession currentSession,
            List<ChatMessage> history,
            String newPrompt,
            boolean thinkingEnabled,
            boolean searchEnabled,
            boolean expertMode,
            StreamCallback callback) {

        String authToken = config.getDeepSeekWebAuthToken();
        String cookie = config.getDeepSeekWebCookie();

        if ((authToken == null || authToken.trim().isEmpty()) && (cookie == null || cookie.trim().isEmpty())) {
            callback.onError(new IllegalArgumentException("DeepSeek Web Authorization Token / Cookie is not set in Settings."));
            return;
        }

        try {
            String deepSeekSessionId = currentSession != null ? currentSession.getDeepSeekSessionId() : null;
            if (deepSeekSessionId == null || deepSeekSessionId.trim().isEmpty()) {
                if (config != null && config.getDeepSeekWebCustomHeaders() != null) {
                    for (java.util.Map.Entry<String, String> entry : config.getDeepSeekWebCustomHeaders().entrySet()) {
                        if ("referer".equalsIgnoreCase(entry.getKey())) {
                            String ref = entry.getValue();
                            if (ref != null && ref.contains("/s/")) {
                                String extracted = ref.substring(ref.lastIndexOf("/s/") + 3).trim();
                                if (!extracted.isEmpty()) {
                                    deepSeekSessionId = extracted;
                                }
                            }
                        }
                    }
                }
                if (deepSeekSessionId == null || deepSeekSessionId.trim().isEmpty()) {
                    deepSeekSessionId = createDeepSeekWebSession(config, authToken, cookie);
                }
                if (currentSession != null && deepSeekSessionId != null) {
                    currentSession.setDeepSeekSessionId(deepSeekSessionId);
                }
            }

            String endpointUrl = "https://chat.deepseek.com/api/v6/chat/completion";
            URL url = new URL(endpointUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(20000);
            applyDeepSeekWebHeaders(conn, config, authToken, cookie);
            conn.setDoOutput(true);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", "deepseek-chat");
            if (deepSeekSessionId != null) {
                body.put("chat_session_id", deepSeekSessionId);
            }
            body.put("thinking_enabled", thinkingEnabled);
            body.put("search_enabled", expertMode ? false : searchEnabled);
            body.put("mode", expertMode ? "expert" : "instant");

            String promptText = (newPrompt != null && !newPrompt.trim().isEmpty()) ? newPrompt.trim() : "";
            if (promptText.isEmpty() && !history.isEmpty()) {
                ChatMessage lastMsg = history.get(history.size() - 1);
                if (lastMsg.getRole() == ChatMessage.Role.USER) {
                    promptText = buildFormattedMessageContent(lastMsg);
                }
            }
            body.put("prompt", promptText);

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

            if (newPrompt != null && !newPrompt.trim().isEmpty()) {
                ObjectNode userMsg = objectMapper.createObjectNode();
                userMsg.put("role", "user");
                userMsg.put("content", newPrompt.trim());
                messagesNode.add(userMsg);
            }
            body.set("messages", messagesNode);

            byte[] input = objectMapper.writeValueAsBytes(body);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(input, 0, input.length);
            }

            int statusCode = conn.getResponseCode();
            if (statusCode != 200) {
                InputStream errIs = conn.getErrorStream();
                String rawErr = errIs != null ? new String(errIs.readAllBytes(), StandardCharsets.UTF_8) : "HTTP " + statusCode;
                String errText = cleanErrorMessage(rawErr, statusCode, endpointUrl);
                callback.onError(new RuntimeException("DeepSeek Web API error (" + statusCode + "): " + errText));
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }

                    String data = trimmed.startsWith("data:") ? trimmed.substring(5).trim() : trimmed;
                    if ("[DONE]".equalsIgnoreCase(data)) {
                        break;
                    }

                    if (data.startsWith("<") || data.contains("<html") || data.contains("<!DOCTYPE")) {
                        callback.onError(new RuntimeException("Cloudflare / Web protection returned HTML page. Please update Auth Token / Cookies in Settings."));
                        return;
                    }

                    try {
                        if (burp.Extension.getMontoyaApi() != null) {
                            burp.Extension.getMontoyaApi().logging().logToOutput("[DeepSeek Web SSE] Line: " + data);
                        }

                        JsonNode root = objectMapper.readTree(data);

                        // Check for server-side error responses in JSON
                        if (root.has("error_msg") && !root.get("error_msg").isNull()) {
                            callback.onError(new RuntimeException("DeepSeek Server Error: " + root.get("error_msg").asText()));
                            return;
                        }
                        if (root.has("msg") && !root.get("msg").isNull()) {
                            String msgStr = root.get("msg").asText();
                            if (!msgStr.equalsIgnoreCase("success") && !msgStr.isEmpty()) {
                                callback.onError(new RuntimeException("DeepSeek Web Message: " + msgStr));
                                return;
                            }
                        }

                        // Extract chunk content from all possible JSON structures
                        String chunk = null;
                        if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                            JsonNode choice = root.get("choices").get(0);
                            if (choice.has("delta")) {
                                JsonNode delta = choice.get("delta");
                                if (delta.has("content") && !delta.get("content").isNull()) {
                                    chunk = delta.get("content").asText();
                                } else if (delta.has("text_content") && !delta.get("text_content").isNull()) {
                                    chunk = delta.get("text_content").asText();
                                } else if (delta.has("thinking_content") && !delta.get("thinking_content").isNull()) {
                                    chunk = delta.get("thinking_content").asText();
                                } else if (delta.has("text") && !delta.get("text").isNull()) {
                                    chunk = delta.get("text").asText();
                                }
                            } else if (choice.has("text") && !choice.get("text").isNull()) {
                                chunk = choice.get("text").asText();
                            }
                        } else if (root.has("content") && !root.get("content").isNull()) {
                            chunk = root.get("content").asText();
                        } else if (root.has("text_content") && !root.get("text_content").isNull()) {
                            chunk = root.get("text_content").asText();
                        } else if (root.has("thinking_content") && !root.get("thinking_content").isNull()) {
                            chunk = root.get("thinking_content").asText();
                        } else if (root.has("text") && !root.get("text").isNull()) {
                            chunk = root.get("text").asText();
                        } else if (root.has("response") && !root.get("response").isNull()) {
                            chunk = root.get("response").asText();
                        } else if (root.has("data") && root.get("data").isObject()) {
                            JsonNode dNode = root.get("data");
                            if (dNode.has("content") && !dNode.get("content").isNull()) {
                                chunk = dNode.get("content").asText();
                            } else if (dNode.has("text_content") && !dNode.get("text_content").isNull()) {
                                chunk = dNode.get("text_content").asText();
                            } else if (dNode.has("thinking_content") && !dNode.get("thinking_content").isNull()) {
                                chunk = dNode.get("thinking_content").asText();
                            } else if (dNode.has("text") && !dNode.get("text").isNull()) {
                                chunk = dNode.get("text").asText();
                            }
                        }

                        if (chunk != null && !chunk.isEmpty()) {
                            callback.onChunk(chunk);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            callback.onComplete();

        } catch (Throwable t) {
            callback.onError(new RuntimeException("DeepSeek Web request failed: " + t.getMessage(), t));
        }
    }

    public static String normalizeEndpointUrl(String rawBaseUrl, String suffix) {
        if (rawBaseUrl == null || rawBaseUrl.trim().isEmpty() || rawBaseUrl.contains("opencodezen.com")) {
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
        if (!clean.toLowerCase().endsWith("/v1") && !clean.toLowerCase().contains("/v1/")) {
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
