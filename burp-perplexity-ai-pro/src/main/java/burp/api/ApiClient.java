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
import java.util.function.Consumer;

public class ApiClient {
    public static final String NVIDIA_BASE_URL = "https://integrate.api.nvidia.com/v1";
    public static final String OPENCODE_ZEN_BASE_URL = "https://api.opencodezen.com/v1";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public interface StreamCallback {
        void onChunk(String chunk);
        void onError(Throwable throwable);
        void onComplete();
    }

    public List<String> fetchAvailableModels(ExtensionConfig config) {
        List<String> models = new ArrayList<>();

        // Fetch NVIDIA models if API key present
        if (config.getNvidiaApiKey() != null && !config.getNvidiaApiKey().trim().isEmpty()) {
            models.addAll(fetchModelsFromEndpoint(NVIDIA_BASE_URL + "/models", config.getNvidiaApiKey()));
        }

        // Fetch OpenCode Zen models if API key present
        if (config.getOpenCodeZenApiKey() != null && !config.getOpenCodeZenApiKey().trim().isEmpty()) {
            models.addAll(fetchModelsFromEndpoint(OPENCODE_ZEN_BASE_URL + "/models", config.getOpenCodeZenApiKey()));
        }

        // Fetch Custom models if Custom API URL present
        if (config.getCustomApiUrl() != null && !config.getCustomApiUrl().trim().isEmpty()) {
            String baseUrl = config.getCustomApiUrl().replaceAll("/+$", "");
            if (!baseUrl.endsWith("/models")) {
                baseUrl += "/models";
            }
            models.addAll(fetchModelsFromEndpoint(baseUrl, config.getCustomApiKey()));
        }

        // Fallback default models if none loaded
        if (models.isEmpty()) {
            models.add("nvidia/llama-3.1-nemotron-70b-instruct");
            models.add("meta/llama-3.3-70b-instruct");
            models.add("deepseek-ai/deepseek-r1");
            models.add("opencode-zen/coder-70b");
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
            List<ChatMessage> history,
            String newPrompt,
            StreamCallback callback) {

        new Thread(() -> {
            try {
                String selectedModel = config.getSelectedModel();
                String endpointUrl;
                String apiKey;

                if (selectedModel.startsWith("opencode-zen/") || selectedModel.contains("opencode")) {
                    endpointUrl = OPENCODE_ZEN_BASE_URL + "/chat/completions";
                    apiKey = config.getOpenCodeZenApiKey();
                } else if (config.getCustomApiUrl() != null && !config.getCustomApiUrl().trim().isEmpty()) {
                    String baseUrl = config.getCustomApiUrl().replaceAll("/+$", "");
                    endpointUrl = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";
                    apiKey = config.getCustomApiKey();
                } else {
                    endpointUrl = NVIDIA_BASE_URL + "/chat/completions";
                    apiKey = config.getNvidiaApiKey();
                }

                if (apiKey == null || apiKey.trim().isEmpty()) {
                    callback.onError(new IllegalArgumentException("API Key for the selected provider/model is not set in Extension Settings."));
                    return;
                }

                ObjectNode body = objectMapper.createObjectNode();
                body.put("model", selectedModel);
                body.put("stream", true);

                ArrayNode messagesNode = objectMapper.createArrayNode();

                // System Prompt
                if (config.getSystemPrompt() != null && !config.getSystemPrompt().trim().isEmpty()) {
                    ObjectNode sysMsg = objectMapper.createObjectNode();
                    sysMsg.put("role", "system");
                    sysMsg.put("content", config.getSystemPrompt());
                    messagesNode.add(sysMsg);
                }

                // Chat History
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
                callback.onError(t);
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
