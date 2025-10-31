package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.ai.AITaskManager;
import burp.ai.PerplexityClient;
import burp.testing.VulnerabilityTester;
import burp.ui.AIPanel;
import burp.ui.ContextMenuHandler;
import burp.ui.TaskDashboard;
import burp.utils.ConfigManager;

import javax.swing.*;

public class Extension implements BurpExtension {
    private MontoyaApi api;
    private PerplexityClient perplexityClient;
    private AITaskManager taskManager;
    private VulnerabilityTester vulnerabilityTester;
    private TaskDashboard taskDashboard;
    private AIPanel aiPanel;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Perplexity AI Pro");
        
        // Initialize configuration
        ConfigManager configManager = new ConfigManager(api);
        String apiKey = configManager.getApiKey();
        
        if (apiKey == null || apiKey.isEmpty()) {
            showApiKeyDialog(configManager);
            return;
        }
        
        initializeComponents(apiKey, configManager);
        registerComponents();
        
        api.logging().logToOutput("╔═══════════════════════════════════════════════════════╗");
        api.logging().logToOutput("║   Perplexity AI Pro Extension Loaded Successfully!   ║");
        api.logging().logToOutput("║   • Active Vulnerability Testing                     ║");
        api.logging().logToOutput("║   • Task Management System                           ║");
        api.logging().logToOutput("║   • Automated Request Generation                     ║");
        api.logging().logToOutput("║   • Multi-step Analysis                              ║");
        api.logging().logToOutput("╚═══════════════════════════════════════════════════════╝");
    }
    
    private void initializeComponents(String apiKey, ConfigManager configManager) {
        // Initialize AI client
        this.perplexityClient = new PerplexityClient(apiKey, api.logging());
        
        // Initialize task manager
        this.taskManager = new AITaskManager(api, perplexityClient);
        
        // Initialize vulnerability tester
        this.vulnerabilityTester = new VulnerabilityTester(
            api, 
            perplexityClient, 
            taskManager
        );
        
        // Initialize UI components
        this.taskDashboard = new TaskDashboard(api, taskManager, vulnerabilityTester);
        this.aiPanel = new AIPanel(
            api, 
            perplexityClient, 
            taskManager, 
            vulnerabilityTester
        );
    }
    
    private void registerComponents() {
        // Register UI tabs
        api.userInterface().registerSuiteTab("AI Dashboard", taskDashboard);
        api.userInterface().registerSuiteTab("AI Testing", aiPanel);
        
        // Register context menu
        api.userInterface().registerContextMenuItemsProvider(
            new ContextMenuHandler(api, taskManager, vulnerabilityTester)
        );
        
        api.logging().logToOutput("✓ All components registered successfully");
    }
    
    private void showApiKeyDialog(ConfigManager configManager) {
        SwingUtilities.invokeLater(() -> {
            String key = JOptionPane.showInputDialog(
                null,
                "Enter your Perplexity API Key:\n" +
                "(Get one from https://www.perplexity.ai/api-platform)",
                "Perplexity AI Pro Configuration",
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (key != null && !key.trim().isEmpty()) {
                configManager.setApiKey(key.trim());
                JOptionPane.showMessageDialog(null, 
                    "API Key saved! Please reload the extension.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
}
