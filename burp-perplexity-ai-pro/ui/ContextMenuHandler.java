package burp.ui;

import burp.ai.AITaskManager;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import burp.testing.VulnerabilityTester;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ContextMenuHandler implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final AITaskManager taskManager;
    private final VulnerabilityTester vulnerabilityTester;
    
    public ContextMenuHandler(MontoyaApi api, AITaskManager taskManager,
                             VulnerabilityTester vulnerabilityTester) {
        this.api = api;
        this.taskManager = taskManager;
        this.vulnerabilityTester = vulnerabilityTester;
    }
    
    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();
        
        if (event.messageEditorRequestResponse().isPresent()) {
            MessageEditorHttpRequestResponse editor = event.messageEditorRequestResponse().get();
            
            // Main AI menu
            JMenu aiMenu = new JMenu("⚡ Perplexity AI Pro");
            aiMenu.setIcon(null);
            
            // Automated Testing submenu
            JMenu autoTestMenu = new JMenu("🤖 Automated Testing");
            
            JMenuItem fullScan = new JMenuItem("Full Vulnerability Scan");
            fullScan.addActionListener(e -> startTest(editor, 
                VulnerabilityTester.TestType.FULL_SCAN));
            autoTestMenu.add(fullScan);
            
            autoTestMenu.addSeparator();
            
            JMenuItem sqlTest = new JMenuItem("SQL Injection Test");
            sqlTest.addActionListener(e -> startTest(editor, 
                VulnerabilityTester.TestType.SQL_INJECTION));
            autoTestMenu.add(sqlTest);
            
            JMenuItem xssTest = new JMenuItem("XSS Test");
            xssTest.addActionListener(e -> startTest(editor, 
                VulnerabilityTester.TestType.XSS));
            autoTestMenu.add(xssTest);
            
            JMenuItem cmdTest = new JMenuItem("Command Injection Test");
            cmdTest.addActionListener(e -> startTest(editor, 
                VulnerabilityTester.TestType.COMMAND_INJECTION));
            autoTestMenu.add(cmdTest);
            
            JMenuItem pathTest = new JMenuItem("Path Traversal Test");
            pathTest.addActionListener(e -> startTest(editor, 
                VulnerabilityTester.TestType.PATH_TRAVERSAL));
            autoTestMenu.add(pathTest);
            
            JMenuItem xxeTest = new JMenuItem("XXE Test");
            xxeTest.addActionListener(e -> startTest(editor, 
                VulnerabilityTester.TestType.XXE));
            autoTestMenu.add(xxeTest);
            
            JMenuItem ssrfTest = new JMenuItem("SSRF Test");
            ssrfTest.addActionListener(e -> startTest(editor, 
                VulnerabilityTester.TestType.SSRF));
            autoTestMenu.add(ssrfTest);
            
            JMenuItem authTest = new JMenuItem("Authentication Bypass Test");
            authTest.addActionListener(e -> startTest(editor, 
                VulnerabilityTester.TestType.AUTHENTICATION_BYPASS));
            autoTestMenu.add(authTest);
            
            JMenuItem authzTest = new JMenuItem("Authorization Bypass Test");
            authzTest.addActionListener(e -> startTest(editor, 
                VulnerabilityTester.TestType.AUTHORIZATION_BYPASS));
            autoTestMenu.add(authzTest);
            
            aiMenu.add(autoTestMenu);
            aiMenu.addSeparator();
            
            // Quick analysis
            JMenuItem quickAnalysis = new JMenuItem("💡 Quick AI Analysis");
            quickAnalysis.addActionListener(e -> quickAnalyze(editor));
            aiMenu.add(quickAnalysis);
            
            JMenuItem customPrompt = new JMenuItem("✏️ Custom AI Prompt...");
            customPrompt.addActionListener(e -> showCustomPrompt(editor));
            aiMenu.add(customPrompt);
            
            menuItems.add(aiMenu);
        }
        
        return menuItems;
    }
    
    private void startTest(MessageEditorHttpRequestResponse editor, 
                          VulnerabilityTester.TestType testType) {
        // Show confirmation dialog
        int result = JOptionPane.showConfirmDialog(null,
            String.format(
                "Start automated %s testing?\n\n" +
                "This will:\n" +
                "• Generate AI-powered test payloads\n" +
                "• Send multiple HTTP requests\n" +
                "• Analyze responses for vulnerabilities\n" +
                "• Create a task in the AI Dashboard\n\n" +
                "Continue?",
                testType.getDisplayName()
            ),
            "Start Automated Testing",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            vulnerabilityTester.startAutomatedTest(editor.requestResponse(), testType);
            
            JOptionPane.showMessageDialog(null,
                "Testing started! Check the AI Dashboard for progress.",
                "Test Started",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
    
    private void quickAnalyze(MessageEditorHttpRequestResponse editor) {
        JDialog progressDialog = new JDialog((Frame) null, "Analyzing...", true);
        JLabel label = new JLabel("  AI is analyzing the request...  ", SwingConstants.CENTER);
        progressDialog.add(label);
        progressDialog.setSize(300, 100);
        progressDialog.setLocationRelativeTo(null);
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                // Quick AI analysis without full testing
                String request = editor.requestResponse().request().toString();
                String prompt = "Quickly analyze this HTTP request and identify:\n" +
                              "1. Most likely vulnerabilities to test\n" +
                              "2. Interesting parameters or headers\n" +
                              "3. Recommended testing approach\n" +
                              "Be concise.";
                
                burp.ai.PerplexityClient.AIResponse response = 
                    new burp.ai.PerplexityClient(
                        new burp.utils.ConfigManager(api).getApiKey(),
                        api.logging()
                    ).analyzeRequestWithContext(request, prompt, null);
                
                return response.isSuccess() ? response.getContent() : 
                       "Error: " + response.getError();
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    String result = get();
                    showResultDialog(result, "Quick AI Analysis");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null,
                        "Error: " + ex.getMessage(),
                        "Analysis Failed",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    private void showCustomPrompt(MessageEditorHttpRequestResponse editor) {
        String prompt = JOptionPane.showInputDialog(null,
            "Enter your custom AI analysis prompt:",
            "Custom AI Analysis",
            JOptionPane.QUESTION_MESSAGE);
        
        if (prompt != null && !prompt.trim().isEmpty()) {
            performCustomAnalysis(editor, prompt);
        }
    }
    
    private void performCustomAnalysis(MessageEditorHttpRequestResponse editor, String prompt) {
        JDialog progressDialog = new JDialog((Frame) null, "Processing...", true);
        progressDialog.add(new JLabel("  AI is processing your request...  ", 
            SwingConstants.CENTER));
        progressDialog.setSize(300, 100);
        progressDialog.setLocationRelativeTo(null);
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                String request = editor.requestResponse().request().toString();
                
                burp.ai.PerplexityClient.AIResponse response = 
                    new burp.ai.PerplexityClient(
                        new burp.utils.ConfigManager(api).getApiKey(),
                        api.logging()
                    ).analyzeRequestWithContext(request, prompt, null);
                
                return response.isSuccess() ? response.getContent() : 
                       "Error: " + response.getError();
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    String result = get();
                    showResultDialog(result, "Custom AI Analysis: " + 
                        prompt.substring(0, Math.min(50, prompt.length())) + "...");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null,
                        "Error: " + ex.getMessage(),
                        "Analysis Failed",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    private void showResultDialog(String result, String title) {
        JDialog dialog = new JDialog((Frame) null, title, false);
        dialog.setLayout(new BorderLayout());
        
        JTextArea resultArea = new JTextArea(result);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        resultArea.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(700, 500));
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        
        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e -> {
            java.awt.datatransfer.StringSelection selection = 
                new java.awt.datatransfer.StringSelection(result);
            java.awt.datatransfer.Clipboard clipboard = 
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            JOptionPane.showMessageDialog(dialog, "Copied to clipboard!");
        });
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(copyButton);
        buttonPanel.add(closeButton);
        
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
