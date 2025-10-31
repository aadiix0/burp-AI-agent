package burp.ai;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AITask {
    private final String taskId;
    private final String description;
    private final String targetUrl;
    private final String originalRequest;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private TaskStatus status;
    private List<AITaskStep> steps;
    private String summary;
    private int creditsUsed;
    
    public enum TaskStatus {
        RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
    }
    
    public AITask(String description, String targetUrl, String originalRequest) {
        this.taskId = UUID.randomUUID().toString().substring(0, 8);
        this.description = description;
        this.targetUrl = targetUrl;
        this.originalRequest = originalRequest;
        this.startTime = LocalDateTime.now();
        this.status = TaskStatus.RUNNING;
        this.steps = new ArrayList<>();
        this.creditsUsed = 0;
    }
    
    public void addStep(AITaskStep step) {
        steps.add(step);
        creditsUsed += step.getCreditsUsed();
    }
    
    public void complete(String summary) {
        this.status = TaskStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        this.summary = summary;
    }
    
    public void fail(String reason) {
        this.status = TaskStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.summary = "Failed: " + reason;
    }
    
    public void cancel() {
        this.status = TaskStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
    }
    
    // Getters
    public String getTaskId() { return taskId; }
    public String getDescription() { return description; }
    public String getTargetUrl() { return targetUrl; }
    public String getOriginalRequest() { return originalRequest; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public TaskStatus getStatus() { return status; }
    public List<AITaskStep> getSteps() { return new ArrayList<>(steps); }
    public String getSummary() { return summary; }
    public int getCreditsUsed() { return creditsUsed; }
    public int getStepCount() { return steps.size(); }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
}
