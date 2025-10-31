package burp.ui;

import burp.ai.AITaskManager;
import burp.ai.PerplexityClient;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.testing.VulnerabilityTester;

import javax.swing.*;
import java.awt.*;

public class AIPanel extends JPanel {
    private final MontoyaApi api;
    private final PerplexityClient perplexityClient;
    private final AITaskManager taskManager;
    private final VulnerabilityTester vulnerabilityTester;
    
    private JTextArea requestArea;
    private JTextArea responseArea;
    private JTextArea promptArea;
    private JComboBox<String> quickPrompts;
    private JComboBox<VulnerabilityTester.TestType> testTypeCombo;
    private JButton analyzeButton;
    private JButton autoTestButton;
    private JButton clearButton;
    private HttpRequestResponse currentMessage;
    
    public AIPanel(MontoyaApi api, PerplexityClient perplexityClient,
                  AITaskManager taskManager, VulnerabilityTester vulnerabilityTester) {
        this.api = api;
        this.perplexityClient = perplexityClient;
        this.taskManager = taskManager;
        this.vulnerabilityTester = vulnerabilityTester;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top instructions
        JLabel instructions = new JLabel(
            "<html><b>AI-Powered Security Testing</b><br>" +
            "<i>Right-click on any request in Repeater → Perplexity AI Pro → " +
            "Choose analysis type or use this panel for custom testing</i></html>"
        );
        instructions.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        add(instructions, BorderLayout.NORTH);
        
        // Center panel - Split view
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        // Top: Request/Response viewer
        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        requestArea = new JTextArea();
        requestArea.setEditable(false);
        requestArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane requestScroll = new JScrollPane(requestArea);
        requestScroll.setBorder(BorderFactory.createTitledBorder("HTTP Request"));
        topSplit.setLeftComponent(requestScroll);
        
        responseArea = new JTextArea();
        responseArea.setEditable(false);
        responseArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane responseScroll = new JScrollPane(responseArea);
        responseScroll.setBorder(BorderFactory.createTitledBorder("AI Response"));
        topSplit.setRightComponent(responseScroll);
        
        topSplit.setDividerLocation(0.5);
        mainSplit.setTopComponent(topSplit);
        
        // Bottom: Controls and testing
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        
        // Prompt area
        JPanel promptPanel = new JPanel(new BorderLayout(5, 5));
        promptPanel.setBorder(BorderFactory.createTitledBorder("AI Prompt / Testing"));
        
        promptArea = new JTextArea(3, 50);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        JScrollPane promptScroll = new JScrollPane(promptArea);
        promptPanel.add(promptScroll, BorderLayout.CENTER);
        
        // Controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        controlPanel.add(new JLabel("Quick Prompts:"));
        quickPrompts = new JComboBox<>(new String[]{
            "Analyze for all vulnerabilities",
            "Suggest SQL injection payloads",
            "Suggest XSS test vectors",
            "Identify authentication weaknesses",
            "Analyze session management",
            "Check for business logic flaws",
            "Suggest fuzzing approach"
        });
        quickPrompts.addActionListener(e -> {
            String selected = (String) quickPrompts.getSelectedItem();
            if (selected != null) {
                promptArea.setText(selected);
            }
        });
        controlPanel.add(quickPrompts);
        
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(new JLabel("Test Type:"));
        testTypeCombo = new JComboBox<>(VulnerabilityTester.TestType.values());
        controlPanel.add(testTypeCombo);
        
        analyzeButton = new JButton("🔍 Analyze with AI");
        analyzeButton.addActionListener(e -> performAnalysis());
        controlPanel.add(analyzeButton);
        
        autoTestButton = new JButton("🤖 Start Automated Test");
        autoTestButton.addActionListener(e -> startAutomatedTest());
        controlPanel.add(autoTestButton);
        
        clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearAll());
        controlPanel.add(clearButton);
        
        promptPanel.add(controlPanel, BorderLayout.SOUTH);
        bottomPanel.add(promptPanel, BorderLayout.CENTER);
        
        mainSplit.setBottomComponent(bottomPanel);
        mainSplit.setDividerLocation(400);
        
        add(mainSplit, BorderLayout.CENTER);
    }
    
    private void performAnalysis() {
        if (currentMessage == null || requestArea.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No request loaded. Right-click on a request in Repeater and " +
                "select 'Send to AI Assistant'",
                "No Request",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String prompt = promptArea.getText();
        if (prompt.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a prompt or select from quick prompts",
                "No Prompt",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        analyzeButton.setEnabled(false);
        analyzeButton.setText("Analyzing...");
        responseArea.setText("AI is analyzing the request...\n");
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                PerplexityClient.AIResponse response = 
                    perplexityClient.analyzeRequestWithContext(
                        requestArea.getText(),
                        prompt,
                        null
                    );
                return response.isSuccess() ? response.getContent() : 
                       "Error: " + response.getError();
            }
            
            @Override
            protected void done() {
                try {
                    String result = get();
                    responseArea.setText(result);
                } catch (Exception ex) {
                    responseArea.setText("Error: " + ex.getMessage());
                } finally {
                    analyzeButton.setEnabled(true);
                    analyzeButton.setText("🔍 Analyze with AI");
                }
            }
        };
        worker.execute();
    }
    
    private void startAutomatedTest() {
        if (currentMessage == null) {
            JOptionPane.showMessageDialog(this,
                "No request loaded",
                "No Request",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        VulnerabilityTester.TestType testType = 
            (VulnerabilityTester.TestType) testTypeCombo.getSelectedItem();
        
        int confirm = JOptionPane.showConfirmDialog(this,
            String.format(
                "Start automated %s testing?\n\n" +
                "This will send multiple HTTP requests and may take several minutes.",
                testType.getDisplayName()
            ),
            "Confirm Automated Testing",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            vulnerabilityTester.startAutomatedTest(currentMessage, testType);
            JOptionPane.showMessageDialog(this,
                "Testing started! Check the AI Dashboard tab for progress.",
                "Test Started",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    public void setRequest(HttpRequestResponse requestResponse) {
        this.currentMessage = requestResponse;
        requestArea.setText(requestResponse.request().toString());
    }
    
    private void clearAll() {
        requestArea.setText("");
        responseArea.setText("");
        promptArea.setText("");
        currentMessage = null;
    }
}
