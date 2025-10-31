package burp.utils;

import burp.api.montoya.MontoyaApi;

public class ConfigManager {
    private final MontoyaApi api;
    private static final String API_KEY_PREF = "perplexity_api_key";
    private static final String MAX_PAYLOADS_PREF = "max_payloads";
    private static final String RATE_LIMIT_PREF = "rate_limit_ms";
    
    public ConfigManager(MontoyaApi api) {
        this.api = api;
    }
    
    public String getApiKey() {
        return api.persistence().preferences().getString(API_KEY_PREF);
    }
    
    public void setApiKey(String apiKey) {
        api.persistence().preferences().setString(API_KEY_PREF, apiKey);
    }
    
    public int getMaxPayloads() {
        Integer value = api.persistence().preferences().getInteger(MAX_PAYLOADS_PREF);
        return value != null ? value : 20;
    }
    
    public void setMaxPayloads(int max) {
        api.persistence().preferences().setInteger(MAX_PAYLOADS_PREF, max);
    }
    
    public int getRateLimit() {
        Integer value = api.persistence().preferences().getInteger(RATE_LIMIT_PREF);
        return value != null ? value : 100;
    }
    
    public void setRateLimit(int ms) {
        api.persistence().preferences().setInteger(RATE_LIMIT_PREF, ms);
    }
}
