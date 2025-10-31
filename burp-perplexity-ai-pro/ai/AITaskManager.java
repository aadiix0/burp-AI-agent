package burp.ai;

import burp.api.montoya.MontoyaApi;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AITaskManager {
    private final MontoyaApi api;
    private final PerplexityClient perplexityClient;
    private final Map<String, AITask> tasks;
    private final List<TaskListener> listeners;
    
    public AITaskManager(MontoyaApi api, PerplexityClient perplexityClient) {
        this.api = api;
        this.perplexityClient = perplexityClient;
        this.tasks = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
    }
    
    public AITask createTask(String description, String targetUrl, String originalRequest) {
        AITask task = new AITask(description, targetUrl, originalRequest);
        tasks.put(task.getTaskId(), task);
        
        api.logging().logToOutput(String.format(
            "Created Task [%s]: %s", task.getTaskId(), description
        ));
        
        notifyListeners(task);
        return task;
    }
    
    public void addStep(String taskId, AITaskStep step) {
        AITask task = tasks.get(taskId);
        if (task != null) {
            task.addStep(step);
            api.logging().logToOutput(String.format(
                "Task [%s] Step %d: %s", 
                taskId, step.getStepNumber(), step.getAction()
            ));
            notifyListeners(task);
        }
    }
    
    public void completeTask(String taskId, String summary) {
        AITask task = tasks.get(taskId);
        if (task != null) {
            task.complete(summary);
            api.logging().logToOutput(String.format(
                "Task [%s] completed: %s", taskId, summary
            ));
            notifyListeners(task);
        }
    }
    
    public void failTask(String taskId, String reason) {
        AITask task = tasks.get(taskId);
        if (task != null) {
            task.fail(reason);
            api.logging().logToError(String.format(
                "Task [%s] failed: %s", taskId, reason
            ));
            notifyListeners(task);
        }
    }
    
    public void cancelTask(String taskId) {
        AITask task = tasks.get(taskId);
        if (task != null) {
            task.cancel();
            api.logging().logToOutput(String.format(
                "Task [%s] cancelled", taskId
            ));
            notifyListeners(task);
        }
    }
    
    public AITask getTask(String taskId) {
        return tasks.get(taskId);
    }
    
    public List<AITask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }
    
    public void addListener(TaskListener listener) {
        listeners.add(listener);
    }
    
    private void notifyListeners(AITask task) {
        for (TaskListener listener : listeners) {
            try {
                listener.onTaskUpdated(task);
            } catch (Exception e) {
                api.logging().logToError("Error notifying listener: " + e.getMessage());
            }
        }
    }
    
    public interface TaskListener {
        void onTaskUpdated(AITask task);
    }
}
