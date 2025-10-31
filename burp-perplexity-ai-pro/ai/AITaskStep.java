package burp.ai;

import burp.api.montoya.http.message.HttpRequestResponse;
import java.time.LocalDateTime;

public class AITaskStep {
    private final int stepNumber;
    private final String action;
    private final LocalDateTime timestamp;
    private final HttpRequestResponse requestResponse;
    private final String summary;
    private final String aiReasoning;
    private final int creditsUsed;
    private final boolean successful;
    
    public AITaskStep(int stepNumber, String action, String aiReasoning,
                     HttpRequestResponse requestResponse, String summary,
                     int creditsUsed, boolean successful) {
        this.stepNumber = stepNumber;
        this.action = action;
        this.timestamp = LocalDateTime.now();
        this.requestResponse = requestResponse;
        this.summary = summary;
        this.aiReasoning = aiReasoning;
        this.creditsUsed = creditsUsed;
        this.successful = successful;
    }
    
    // Getters
    public int getStepNumber() { return stepNumber; }
    public String getAction() { return action; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public HttpRequestResponse getRequestResponse() { return requestResponse; }
    public String getSummary() { return summary; }
    public String getAiReasoning() { return aiReasoning; }
    public int getCreditsUsed() { return creditsUsed; }
    public boolean isSuccessful() { return successful; }
}
