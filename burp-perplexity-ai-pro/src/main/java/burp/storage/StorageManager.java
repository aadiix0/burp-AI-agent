package burp.storage;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;
import burp.model.ChatSession;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class StorageManager {
    private static final String PREF_CONFIG_KEY = "ai_assistant_global_config";
    private static final String PROJECT_SESSIONS_KEY = "ai_assistant_project_sessions";

    private final MontoyaApi api;
    private final ObjectMapper objectMapper;

    private ExtensionConfig cachedConfig;
    private List<ChatSession> cachedSessions;

    public StorageManager(MontoyaApi api) {
        this.api = api;
        this.objectMapper = new ObjectMapper();
        loadConfig();
        loadSessions();
    }

    public synchronized ExtensionConfig getConfig() {
        if (cachedConfig == null) {
            loadConfig();
        }
        return cachedConfig;
    }

    public synchronized void saveConfig(ExtensionConfig config) {
        this.cachedConfig = config;
        try {
            Preferences preferences = api.persistence().preferences();
            String json = objectMapper.writeValueAsString(config);
            preferences.setString(PREF_CONFIG_KEY, json);
        } catch (Exception e) {
            api.logging().logToError("Failed to save configuration: " + e.getMessage());
        }
    }

    private void loadConfig() {
        try {
            Preferences preferences = api.persistence().preferences();
            String json = preferences.getString(PREF_CONFIG_KEY);
            if (json != null && !json.trim().isEmpty()) {
                this.cachedConfig = objectMapper.readValue(json, ExtensionConfig.class);
            } else {
                this.cachedConfig = new ExtensionConfig();
            }
        } catch (Exception e) {
            api.logging().logToError("Failed to load configuration: " + e.getMessage());
            this.cachedConfig = new ExtensionConfig();
        }
    }

    public synchronized List<ChatSession> getSessions() {
        if (cachedSessions == null) {
            loadSessions();
        }
        return cachedSessions;
    }

    public synchronized void saveSessions(List<ChatSession> sessions) {
        this.cachedSessions = new ArrayList<>(sessions);
        try {
            PersistedObject extensionData = api.persistence().extensionData();
            String json = objectMapper.writeValueAsString(this.cachedSessions);
            extensionData.setString(PROJECT_SESSIONS_KEY, json);
        } catch (Exception e) {
            api.logging().logToError("Failed to save project sessions: " + e.getMessage());
        }
    }

    private void loadSessions() {
        try {
            PersistedObject extensionData = api.persistence().extensionData();
            String json = extensionData.getString(PROJECT_SESSIONS_KEY);
            if (json != null && !json.trim().isEmpty()) {
                this.cachedSessions = objectMapper.readValue(json, new TypeReference<List<ChatSession>>() {});
            } else {
                this.cachedSessions = new ArrayList<>();
            }
        } catch (Exception e) {
            api.logging().logToError("Failed to load project sessions: " + e.getMessage());
            this.cachedSessions = new ArrayList<>();
        }
    }

    public synchronized void saveSession(ChatSession session) {
        List<ChatSession> sessions = getSessions();
        boolean found = false;
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).getId().equals(session.getId())) {
                sessions.set(i, session);
                found = true;
                break;
            }
        }
        if (!found) {
            sessions.add(0, session);
        }
        saveSessions(sessions);
    }

    public synchronized void deleteSession(String sessionId) {
        List<ChatSession> sessions = getSessions();
        sessions.removeIf(s -> s.getId().equals(sessionId));
        saveSessions(sessions);
    }
}
