package burp.ui;

import burp.api.ApiClient;
import burp.api.montoya.MontoyaApi;
import burp.model.ChatMessage;
import burp.model.ChatSession;
import burp.storage.ExtensionConfig;
import burp.storage.StorageManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MainTabPanel extends JPanel {
    private final MontoyaApi api;
    private final StorageManager storageManager;
    private final ApiClient apiClient;

    private DefaultListModel<ChatSession> sessionListModel;
    private JList<ChatSession> sessionList;

    private JComboBox<String> modelComboBox;
    private JToggleButton favoriteFilterBtn;
    private JComboBox<String> promptCategoryComboBox;
    private JComboBox<String> vulnClassComboBox;

    private JEditorPane chatDisplayPane;
    private JTextArea promptInputArea;
    private JButton sendButton;

    private JPanel attachedTrafficBanner;
    private JLabel attachedTrafficLabel;

    private String pendingHttpRequest;
    private String pendingHttpResponse;
    private String pendingHttpUrl;

    private ChatSession activeSession;

    public MainTabPanel(MontoyaApi api, StorageManager storageManager) {
        this.api = api;
        this.storageManager = storageManager;
        this.apiClient = new ApiClient();

        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel chatMainPanel = new JPanel(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createSessionSidebar(), createChatMainArea());
        splitPane.setDividerLocation(250);
        chatMainPanel.add(splitPane, BorderLayout.CENTER);

        tabbedPane.addTab("AI Assistant", chatMainPanel);
        tabbedPane.addTab("Settings & API Keys", new SettingsPanel(api, storageManager, this::refreshModelsAndConfig));

        add(tabbedPane, BorderLayout.CENTER);

        loadSessionsAndActive();
        refreshModelsAndConfig();
    }

    private JPanel createSessionSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(5, 5));
        sidebar.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel topBtnBar = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton newSessionBtn = new JButton("+ New Session");
        newSessionBtn.addActionListener(e -> createNewSession());

        JButton deleteSessionBtn = new JButton("Delete");
        deleteSessionBtn.addActionListener(e -> deleteSelectedSession());

        topBtnBar.add(newSessionBtn);
        topBtnBar.add(deleteSessionBtn);

        sessionListModel = new DefaultListModel<>();
        sessionList = new JList<>(sessionListModel);
        sessionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ChatSession) {
                    ChatSession session = (ChatSession) value;
                    label.setText(session.getTitle());
                }
                return label;
            }
        });

        sessionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ChatSession selected = sessionList.getSelectedValue();
                if (selected != null && (activeSession == null || !activeSession.getId().equals(selected.getId()))) {
                    activeSession = selected;
                    renderActiveSessionChat();
                }
            }
        });

        sidebar.add(topBtnBar, BorderLayout.NORTH);
        sidebar.add(new JScrollPane(sessionList), BorderLayout.CENTER);

        return sidebar;
    }

    private JPanel createChatMainArea() {
        JPanel mainArea = new JPanel(new BorderLayout(5, 5));
        mainArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Top Control Bar (Model dropdown, Favorites toggle, Presets)
        JPanel controlBar = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        modelComboBox = new JComboBox<>();
        modelComboBox.setEditable(true);

        favoriteFilterBtn = new JToggleButton("⭐ Favorites");
        favoriteFilterBtn.addActionListener(e -> populateModelComboBox());

        promptCategoryComboBox = new JComboBox<>();
        promptCategoryComboBox.addActionListener(e -> onPromptCategorySelected());

        vulnClassComboBox = new JComboBox<>();
        vulnClassComboBox.addActionListener(e -> onVulnClassSelected());

        // Row 0: Model & Favorites
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; controlBar.add(new JLabel("Model:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; controlBar.add(modelComboBox, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0; controlBar.add(favoriteFilterBtn, gbc);

        // Row 1: Prompt Template & Vuln Class
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; controlBar.add(new JLabel("Prompt Category:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5; controlBar.add(promptCategoryComboBox, gbc);

        JPanel vulnPanel = new JPanel(new BorderLayout(5, 0));
        vulnPanel.add(new JLabel("Vuln Class: "), BorderLayout.WEST);
        vulnPanel.add(vulnClassComboBox, BorderLayout.CENTER);

        gbc.gridx = 2; gbc.weightx = 0.5; controlBar.add(vulnPanel, gbc);

        // Attached Traffic Banner (Shows when traffic sent from Repeater/Proxy)
        attachedTrafficBanner = new JPanel(new BorderLayout(5, 5));
        attachedTrafficBanner.setBackground(new Color(230, 242, 255));
        attachedTrafficBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 210, 245)),
                new EmptyBorder(4, 8, 4, 8)
        ));
        attachedTrafficLabel = new JLabel("Attached Traffic: None");
        attachedTrafficLabel.setForeground(new Color(20, 60, 140));
        JButton clearTrafficBtn = new JButton("Clear Traffic");
        clearTrafficBtn.addActionListener(e -> clearAttachedTraffic());

        attachedTrafficBanner.add(attachedTrafficLabel, BorderLayout.CENTER);
        attachedTrafficBanner.add(clearTrafficBtn, BorderLayout.EAST);
        attachedTrafficBanner.setVisible(false);

        JPanel headerContainer = new JPanel(new BorderLayout());
        headerContainer.add(controlBar, BorderLayout.NORTH);
        headerContainer.add(attachedTrafficBanner, BorderLayout.SOUTH);

        // Center Chat Display (Rich Text / Markdown)
        chatDisplayPane = new JEditorPane();
        chatDisplayPane.setContentType("text/html");
        chatDisplayPane.setEditable(false);

        // Bottom Input Bar & Action Buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        promptInputArea = new JTextArea(3, 40);
        promptInputArea.setLineWrap(true);
        promptInputArea.setWrapStyleWord(true);

        sendButton = new JButton("Send Prompt");
        sendButton.setFont(sendButton.getFont().deriveFont(Font.BOLD));
        sendButton.addActionListener(e -> sendCurrentPrompt());

        JPanel actionBtnsBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton copyMarkdownBtn = new JButton("Copy Analysis");
        copyMarkdownBtn.addActionListener(e -> copyCurrentAnalysis());

        JButton exportSessionBtn = new JButton("Export Session");
        exportSessionBtn.addActionListener(e -> exportCurrentSession());

        actionBtnsBar.add(copyMarkdownBtn);
        actionBtnsBar.add(exportSessionBtn);
        actionBtnsBar.add(sendButton);

        bottomPanel.add(new JScrollPane(promptInputArea), BorderLayout.CENTER);
        bottomPanel.add(actionBtnsBar, BorderLayout.SOUTH);

        mainArea.add(headerContainer, BorderLayout.NORTH);
        mainArea.add(new JScrollPane(chatDisplayPane), BorderLayout.CENTER);
        mainArea.add(bottomPanel, BorderLayout.SOUTH);

        return mainArea;
    }

    private void loadSessionsAndActive() {
        sessionListModel.clear();
        List<ChatSession> sessions = storageManager.getSessions();
        if (sessions.isEmpty()) {
            createNewSession();
        } else {
            for (ChatSession session : sessions) {
                sessionListModel.addElement(session);
            }
            sessionList.setSelectedIndex(0);
            activeSession = sessions.get(0);
            renderActiveSessionChat();
        }
    }

    public void createNewSession() {
        String title = "Session " + new SimpleDateFormat("MMM dd HH:mm").format(new Date());
        ChatSession session = new ChatSession(UUID.randomUUID().toString(), title, System.currentTimeMillis());
        storageManager.saveSession(session);

        sessionListModel.insertElementAt(session, 0);
        sessionList.setSelectedValue(session, true);
        activeSession = session;
        renderActiveSessionChat();
    }

    private void deleteSelectedSession() {
        ChatSession selected = sessionList.getSelectedValue();
        if (selected != null) {
            int option = JOptionPane.showConfirmDialog(this, "Delete session '" + selected.getTitle() + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                storageManager.deleteSession(selected.getId());
                sessionListModel.removeElement(selected);
                if (!sessionListModel.isEmpty()) {
                    sessionList.setSelectedIndex(0);
                } else {
                    createNewSession();
                }
            }
        }
    }

    public void attachTrafficToSession(ChatSession targetSession, String url, String method, String request, String response) {
        this.pendingHttpUrl = url;
        this.pendingHttpRequest = request;
        this.pendingHttpResponse = response;

        if (targetSession != null) {
            this.activeSession = targetSession;
            sessionList.setSelectedValue(targetSession, true);
        }

        attachedTrafficLabel.setText("Attached Traffic: " + method + " " + url);
        attachedTrafficBanner.setVisible(true);

        revalidate();
        repaint();
    }

    private void clearAttachedTraffic() {
        this.pendingHttpUrl = null;
        this.pendingHttpRequest = null;
        this.pendingHttpResponse = null;
        attachedTrafficBanner.setVisible(false);
    }

    private void refreshModelsAndConfig() {
        ExtensionConfig config = storageManager.getConfig();

        // Populate Prompt Templates
        promptCategoryComboBox.removeAllItems();
        promptCategoryComboBox.addItem("-- Select Prompt Template --");
        for (String promptName : config.getCustomPrompts().keySet()) {
            promptCategoryComboBox.addItem(promptName);
        }

        // Populate Vuln Classes
        vulnClassComboBox.removeAllItems();
        vulnClassComboBox.addItem("-- Select Vuln Class --");
        for (String vulnName : config.getVulnerabilityClasses().keySet()) {
            vulnClassComboBox.addItem(vulnName);
        }

        // Populate Model list asynchronously
        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() {
                return apiClient.fetchAvailableModels(config);
            }

            @Override
            protected void done() {
                try {
                    List<String> models = get();
                    if (!models.isEmpty()) {
                        populateModelComboBoxWithList(models);
                    }
                } catch (Exception ignored) {
                }
            }
        };
        worker.execute();
    }

    private void populateModelComboBox() {
        refreshModelsAndConfig();
    }

    private void populateModelComboBoxWithList(List<String> models) {
        ExtensionConfig config = storageManager.getConfig();
        boolean favoritesOnly = favoriteFilterBtn.isSelected();

        modelComboBox.removeAllItems();
        for (String m : models) {
            if (!favoritesOnly || config.getFavoriteModels().contains(m)) {
                modelComboBox.addItem(m);
            }
        }

        if (config.getSelectedModel() != null) {
            modelComboBox.setSelectedItem(config.getSelectedModel());
        }
    }

    private void onPromptCategorySelected() {
        String selected = (String) promptCategoryComboBox.getSelectedItem();
        if (selected != null && !selected.startsWith("--")) {
            ExtensionConfig config = storageManager.getConfig();
            String promptText = config.getCustomPrompts().get(selected);
            if (promptText != null) {
                promptInputArea.setText(promptText);
            }
        }
    }

    private void onVulnClassSelected() {
        String selected = (String) vulnClassComboBox.getSelectedItem();
        if (selected != null && !selected.startsWith("--")) {
            ExtensionConfig config = storageManager.getConfig();
            String vulnFocus = config.getVulnerabilityClasses().get(selected);
            if (vulnFocus != null) {
                String existing = promptInputArea.getText().trim();
                if (existing.isEmpty()) {
                    promptInputArea.setText("Target Vulnerability Focus: " + selected + "\nInstructions: " + vulnFocus);
                } else {
                    promptInputArea.setText(existing + "\n\n[Target Vulnerability Focus: " + selected + " - " + vulnFocus + "]");
                }
            }
        }
    }

    private void sendCurrentPrompt() {
        String userPrompt = promptInputArea.getText().trim();
        if (userPrompt.isEmpty() && pendingHttpRequest == null) {
            JOptionPane.showMessageDialog(this, "Please enter a prompt or attach HTTP traffic first.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (activeSession == null) {
            createNewSession();
        }

        ExtensionConfig config = storageManager.getConfig();
        String selectedModel = (String) modelComboBox.getSelectedItem();
        if (selectedModel != null) {
            config.setSelectedModel(selectedModel);
            storageManager.saveConfig(config);
        }

        ChatMessage userMsg = new ChatMessage(UUID.randomUUID().toString(), ChatMessage.Role.USER, userPrompt, System.currentTimeMillis());
        if (pendingHttpRequest != null) {
            userMsg.setHttpUrl(pendingHttpUrl);
            userMsg.setHttpRequest(pendingHttpRequest);
            userMsg.setHttpResponse(pendingHttpResponse);
            clearAttachedTraffic();
        }

        activeSession.addMessage(userMsg);

        ChatMessage assistantMsg = new ChatMessage(UUID.randomUUID().toString(), ChatMessage.Role.ASSISTANT, "Thinking...", System.currentTimeMillis());
        activeSession.addMessage(assistantMsg);

        storageManager.saveSession(activeSession);
        renderActiveSessionChat();

        promptInputArea.setText("");
        sendButton.setEnabled(false);

        StringBuilder fullResponseBuffer = new StringBuilder();

        apiClient.streamChatCompletion(config, activeSession.getMessages().subList(0, activeSession.getMessages().size() - 1), userPrompt, new ApiClient.StreamCallback() {
            @Override
            public void onChunk(String chunk) {
                fullResponseBuffer.append(chunk);
                assistantMsg.setContent(fullResponseBuffer.toString());
                SwingUtilities.invokeLater(() -> renderActiveSessionChat());
            }

            @Override
            public void onError(Throwable throwable) {
                SwingUtilities.invokeLater(() -> {
                    assistantMsg.setContent("**Error:** " + throwable.getMessage());
                    storageManager.saveSession(activeSession);
                    renderActiveSessionChat();
                    sendButton.setEnabled(true);
                });
            }

            @Override
            public void onComplete() {
                SwingUtilities.invokeLater(() -> {
                    storageManager.saveSession(activeSession);
                    renderActiveSessionChat();
                    sendButton.setEnabled(true);
                });
            }
        });
    }

    private void renderActiveSessionChat() {
        if (activeSession == null || activeSession.getMessages().isEmpty()) {
            chatDisplayPane.setText(MarkdownUtil.toHtml("### Welcome to AI Assistant Pro\nSelect a model, attach requests/responses from Repeater or Proxy, choose a Vulnerability Class / Prompt, and click **Send Prompt**."));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(activeSession.getTitle()).append("\n\n");

        for (ChatMessage msg : activeSession.getMessages()) {
            if (msg.getRole() == ChatMessage.Role.USER) {
                sb.append("### 👤 You\n");
            } else if (msg.getRole() == ChatMessage.Role.ASSISTANT) {
                sb.append("### 🤖 AI Assistant\n");
            }

            if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                sb.append(msg.getContent()).append("\n\n");
            }

            if (msg.getHttpRequest() != null && !msg.getHttpRequest().isEmpty()) {
                sb.append("**Attached HTTP Request:** (`").append(msg.getHttpUrl() != null ? msg.getHttpUrl() : "").append("`)\n```http\n")
                        .append(msg.getHttpRequest()).append("\n```\n\n");
            }

            if (msg.getHttpResponse() != null && !msg.getHttpResponse().isEmpty()) {
                sb.append("**Attached HTTP Response:**\n```http\n")
                        .append(msg.getHttpResponse()).append("\n```\n\n");
            }

            sb.append("---\n\n");
        }

        chatDisplayPane.setText(MarkdownUtil.toHtml(sb.toString()));
        chatDisplayPane.setCaretPosition(chatDisplayPane.getDocument().getLength());
    }

    private void copyCurrentAnalysis() {
        if (activeSession == null || activeSession.getMessages().isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : activeSession.getMessages()) {
            sb.append("[").append(msg.getRole()).append("]:\n").append(msg.getContent()).append("\n\n");
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
        JOptionPane.showMessageDialog(this, "Session analysis copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportCurrentSession() {
        if (activeSession == null) return;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("AI_Analysis_" + activeSession.getId() + ".md"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile())) {
                for (ChatMessage msg : activeSession.getMessages()) {
                    writer.write("## " + msg.getRole() + "\n" + msg.getContent() + "\n\n");
                }
                JOptionPane.showMessageDialog(this, "Exported successfully!", "Exported", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to export file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public ChatSession getActiveSession() {
        return activeSession;
    }
}
