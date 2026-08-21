package burp.ui;

import burp.api.ApiClient;
import burp.api.ApiClient.ModelEntry;
import burp.api.montoya.MontoyaApi;
import burp.model.ChatMessage;
import burp.model.ChatSession;
import burp.storage.ExtensionConfig;
import burp.storage.StorageManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
    private JTextField searchSessionsField;

    private JComboBox<ModelEntry> modelComboBox;
    private JButton toggleFavoriteBtn;
    private JToggleButton favoriteFilterBtn;
    private JComboBox<String> promptCategoryComboBox;
    private JComboBox<String> vulnClassComboBox;

    private JEditorPane chatDisplayPane;
    private JTextArea promptInputArea;
    private JButton sendButton;

    private JPanel attachedTrafficBanner;
    private JLabel attachedTrafficLabel;

    private JLabel statusModelLabel;
    private JLabel statusTokensLabel;
    private JLabel statusApiConnectionLabel;

    private String pendingHttpRequest;
    private String pendingHttpResponse;
    private String pendingHttpUrl;

    private ChatSession activeSession;
    private List<ModelEntry> cachedFetchedModels = new ArrayList<>();

    private static final Color DARK_BG = new Color(24, 24, 28);
    private static final Color DARK_PANEL = new Color(31, 31, 35);
    private static final Color ORANGE_ACCENT = new Color(249, 115, 22);
    private static final Color DARK_TEXT = new Color(212, 212, 216);
    private static final Color DARK_BORDER = new Color(39, 39, 42);

    public MainTabPanel(MontoyaApi api, StorageManager storageManager) {
        this.api = api;
        this.storageManager = storageManager;
        this.apiClient = new ApiClient();

        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(DARK_PANEL);
        tabbedPane.setForeground(DARK_TEXT);

        JPanel chatMainPanel = new JPanel(new BorderLayout());
        chatMainPanel.setBackground(DARK_BG);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createSessionSidebar(), createChatMainArea());
        splitPane.setDividerLocation(260);
        splitPane.setBorder(null);
        splitPane.setBackground(DARK_BG);

        chatMainPanel.add(splitPane, BorderLayout.CENTER);
        chatMainPanel.add(createBottomStatusBar(), BorderLayout.SOUTH);

        tabbedPane.addTab("AI Assistant", chatMainPanel);
        tabbedPane.addTab("Settings & API Keys", new SettingsPanel(api, storageManager, this::refreshModelsAndConfig));

        add(tabbedPane, BorderLayout.CENTER);

        loadSessionsAndActive();
        refreshModelsAndConfig();
    }

    private JPanel createSessionSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(8, 8));
        sidebar.setBackground(DARK_BG);
        sidebar.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel topBtnBar = new JPanel(new GridLayout(1, 2, 6, 6));
        topBtnBar.setBackground(DARK_BG);

        JButton newSessionBtn = new JButton("+ New");
        newSessionBtn.setBackground(ORANGE_ACCENT);
        newSessionBtn.setForeground(Color.WHITE);
        newSessionBtn.setFont(newSessionBtn.getFont().deriveFont(Font.BOLD));
        newSessionBtn.setFocusPainted(false);
        newSessionBtn.addActionListener(e -> createNewSession());

        JButton deleteSessionBtn = new JButton("🗑 Delete");
        deleteSessionBtn.setBackground(DARK_PANEL);
        deleteSessionBtn.setForeground(DARK_TEXT);
        deleteSessionBtn.setFocusPainted(false);
        deleteSessionBtn.addActionListener(e -> deleteSelectedSession());

        topBtnBar.add(newSessionBtn);
        topBtnBar.add(deleteSessionBtn);

        searchSessionsField = new JTextField();
        searchSessionsField.setBackground(DARK_PANEL);
        searchSessionsField.setForeground(DARK_TEXT);
        searchSessionsField.setCaretColor(DARK_TEXT);
        searchSessionsField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DARK_BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        searchSessionsField.setToolTipText("Search sessions...");

        searchSessionsField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterSessions(); }
            @Override public void removeUpdate(DocumentEvent e) { filterSessions(); }
            @Override public void changedUpdate(DocumentEvent e) { filterSessions(); }
        });

        JPanel topContainer = new JPanel(new BorderLayout(6, 6));
        topContainer.setBackground(DARK_BG);
        topContainer.add(topBtnBar, BorderLayout.NORTH);
        topContainer.add(searchSessionsField, BorderLayout.SOUTH);

        sessionListModel = new DefaultListModel<>();
        sessionList = new JList<>(sessionListModel);
        sessionList.setBackground(DARK_BG);
        sessionList.setForeground(DARK_TEXT);
        sessionList.setSelectionBackground(DARK_PANEL);
        sessionList.setSelectionForeground(ORANGE_ACCENT);
        sessionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(new EmptyBorder(8, 10, 8, 10));
                label.setOpaque(true);
                if (value instanceof ChatSession) {
                    ChatSession session = (ChatSession) value;
                    label.setText("🟠 " + session.getTitle());
                }
                if (isSelected) {
                    label.setBackground(DARK_PANEL);
                    label.setForeground(ORANGE_ACCENT);
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                } else {
                    label.setBackground(DARK_BG);
                    label.setForeground(DARK_TEXT);
                    label.setFont(label.getFont().deriveFont(Font.PLAIN));
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

        JScrollPane sessionScroll = new JScrollPane(sessionList);
        sessionScroll.setBorder(BorderFactory.createLineBorder(DARK_BORDER));

        sidebar.add(topContainer, BorderLayout.NORTH);
        sidebar.add(sessionScroll, BorderLayout.CENTER);

        return sidebar;
    }

    private JPanel createChatMainArea() {
        JPanel mainArea = new JPanel(new BorderLayout(6, 6));
        mainArea.setBackground(DARK_BG);
        mainArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel controlBar = new JPanel(new GridBagLayout());
        controlBar.setBackground(DARK_PANEL);
        controlBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DARK_BORDER),
                new EmptyBorder(6, 10, 6, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        promptCategoryComboBox = new JComboBox<>();
        promptCategoryComboBox.setBackground(DARK_BG);
        promptCategoryComboBox.setForeground(DARK_TEXT);
        promptCategoryComboBox.addActionListener(e -> onPromptCategorySelected());

        vulnClassComboBox = new JComboBox<>();
        vulnClassComboBox.setBackground(DARK_BG);
        vulnClassComboBox.setForeground(DARK_TEXT);
        vulnClassComboBox.addActionListener(e -> onVulnClassSelected());

        JButton exportSessionBtn = new JButton("📥 Export");
        exportSessionBtn.setBackground(DARK_BG);
        exportSessionBtn.setForeground(DARK_TEXT);
        exportSessionBtn.setFocusPainted(false);
        exportSessionBtn.addActionListener(e -> exportCurrentSession());

        JButton clearBtn = new JButton("🗑 Clear");
        clearBtn.setBackground(DARK_BG);
        clearBtn.setForeground(DARK_TEXT);
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> clearCurrentChatSession());

        JLabel promptLabel = new JLabel("Prompt:"); promptLabel.setForeground(DARK_TEXT);
        JLabel vulnLabel = new JLabel("Vuln:"); vulnLabel.setForeground(DARK_TEXT);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; controlBar.add(promptLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.4; controlBar.add(promptCategoryComboBox, gbc);

        gbc.gridx = 2; gbc.weightx = 0.0; controlBar.add(vulnLabel, gbc);
        gbc.gridx = 3; gbc.weightx = 0.4; controlBar.add(vulnClassComboBox, gbc);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        topActions.setBackground(DARK_PANEL);
        topActions.add(exportSessionBtn);
        topActions.add(clearBtn);

        gbc.gridx = 4; gbc.weightx = 0.2; controlBar.add(topActions, gbc);

        // Attached Traffic Card Banner
        attachedTrafficBanner = new JPanel(new BorderLayout(8, 8));
        attachedTrafficBanner.setBackground(DARK_PANEL);
        attachedTrafficBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ORANGE_ACCENT, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        attachedTrafficLabel = new JLabel("📎 Attached HTTP Request & Response");
        attachedTrafficLabel.setForeground(ORANGE_ACCENT);
        attachedTrafficLabel.setFont(attachedTrafficLabel.getFont().deriveFont(Font.BOLD));

        JButton clearTrafficBtn = new JButton("Clear Traffic");
        clearTrafficBtn.setBackground(DARK_BG);
        clearTrafficBtn.setForeground(DARK_TEXT);
        clearTrafficBtn.setFocusPainted(false);
        clearTrafficBtn.addActionListener(e -> clearAttachedTraffic());

        attachedTrafficBanner.add(attachedTrafficLabel, BorderLayout.CENTER);
        attachedTrafficBanner.add(clearTrafficBtn, BorderLayout.EAST);
        attachedTrafficBanner.setVisible(false);

        JPanel headerContainer = new JPanel(new BorderLayout(4, 4));
        headerContainer.setBackground(DARK_BG);
        headerContainer.add(controlBar, BorderLayout.NORTH);
        headerContainer.add(attachedTrafficBanner, BorderLayout.SOUTH);

        // Center Chat Display
        chatDisplayPane = new JEditorPane();
        chatDisplayPane.setContentType("text/html");
        chatDisplayPane.setEditable(false);
        chatDisplayPane.setBackground(DARK_BG);

        JScrollPane chatScroll = new JScrollPane(chatDisplayPane);
        chatScroll.setBorder(BorderFactory.createLineBorder(DARK_BORDER));

        // Bottom Prompt Input & Action Buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(6, 6));
        bottomPanel.setBackground(DARK_BG);

        promptInputArea = new JTextArea(3, 40);
        promptInputArea.setLineWrap(true);
        promptInputArea.setWrapStyleWord(true);
        promptInputArea.setBackground(DARK_PANEL);
        promptInputArea.setForeground(DARK_TEXT);
        promptInputArea.setCaretColor(DARK_TEXT);
        promptInputArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane promptScroll = new JScrollPane(promptInputArea);
        promptScroll.setBorder(BorderFactory.createLineBorder(DARK_BORDER));

        JPanel actionBtnsBar = new JPanel(new GridLayout(2, 1, 4, 4));
        actionBtnsBar.setBackground(DARK_BG);

        sendButton = new JButton("➤ Send");
        sendButton.setBackground(ORANGE_ACCENT);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(sendButton.getFont().deriveFont(Font.BOLD));
        sendButton.setFocusPainted(false);
        sendButton.addActionListener(e -> sendCurrentPrompt());

        JButton copyAnalysisBtn = new JButton("📋 Copy Analysis");
        copyAnalysisBtn.setBackground(DARK_PANEL);
        copyAnalysisBtn.setForeground(DARK_TEXT);
        copyAnalysisBtn.setFocusPainted(false);
        copyAnalysisBtn.addActionListener(e -> copyCurrentAnalysis());

        actionBtnsBar.add(sendButton);
        actionBtnsBar.add(copyAnalysisBtn);

        bottomPanel.add(promptScroll, BorderLayout.CENTER);
        bottomPanel.add(actionBtnsBar, BorderLayout.EAST);

        mainArea.add(headerContainer, BorderLayout.NORTH);
        mainArea.add(chatScroll, BorderLayout.CENTER);
        mainArea.add(bottomPanel, BorderLayout.SOUTH);

        return mainArea;
    }

    private JPanel createBottomStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout(10, 0));
        statusBar.setBackground(DARK_PANEL);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DARK_BORDER),
                new EmptyBorder(4, 10, 4, 10)
        ));

        modelComboBox = new JComboBox<>();
        modelComboBox.setBackground(DARK_BG);
        modelComboBox.setForeground(DARK_TEXT);
        modelComboBox.setMaximumSize(new Dimension(300, 24));
        modelComboBox.addActionListener(e -> updateFavoriteStarButtonState());

        toggleFavoriteBtn = new JButton("☆ Star");
        toggleFavoriteBtn.setBackground(DARK_BG);
        toggleFavoriteBtn.setForeground(ORANGE_ACCENT);
        toggleFavoriteBtn.setFocusPainted(false);
        toggleFavoriteBtn.addActionListener(e -> toggleCurrentModelFavorite());

        favoriteFilterBtn = new JToggleButton("⭐ Favorites");
        favoriteFilterBtn.setBackground(DARK_BG);
        favoriteFilterBtn.setForeground(DARK_TEXT);
        favoriteFilterBtn.setFocusPainted(false);
        favoriteFilterBtn.addActionListener(e -> renderModelComboBox());

        JLabel lightningLabel = new JLabel("⚡ Model:");
        lightningLabel.setForeground(ORANGE_ACCENT);
        lightningLabel.setFont(lightningLabel.getFont().deriveFont(Font.BOLD, 12f));

        JPanel modelControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        modelControls.setBackground(DARK_PANEL);
        modelControls.add(lightningLabel);
        modelControls.add(modelComboBox);
        modelControls.add(toggleFavoriteBtn);
        modelControls.add(favoriteFilterBtn);

        statusTokensLabel = new JLabel("💾 ~2.4k tokens");
        statusTokensLabel.setForeground(DARK_TEXT);
        statusTokensLabel.setFont(statusTokensLabel.getFont().deriveFont(11f));

        statusApiConnectionLabel = new JLabel("🟢 API Connected");
        statusApiConnectionLabel.setForeground(new Color(52, 211, 153));
        statusApiConnectionLabel.setFont(statusApiConnectionLabel.getFont().deriveFont(Font.BOLD, 11f));

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightControls.setBackground(DARK_PANEL);
        rightControls.add(statusTokensLabel);
        rightControls.add(statusApiConnectionLabel);

        statusBar.add(modelControls, BorderLayout.WEST);
        statusBar.add(rightControls, BorderLayout.EAST);

        return statusBar;
    }

    private void clearCurrentChatSession() {
        if (activeSession != null) {
            int confirm = JOptionPane.showConfirmDialog(this, "Clear all messages in active session?", "Clear Chat", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                activeSession.getMessages().clear();
                storageManager.saveSession(activeSession);
                renderActiveSessionChat();
            }
        }
    }

    private void filterSessions() {
        String query = searchSessionsField.getText().trim().toLowerCase();
        sessionListModel.clear();
        for (ChatSession session : storageManager.getSessions()) {
            if (query.isEmpty() || session.getTitle().toLowerCase().contains(query)) {
                sessionListModel.addElement(session);
            }
        }
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

        attachedTrafficLabel.setText("📎 Attached HTTP Traffic (" + method + " " + url + ")");
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

        promptCategoryComboBox.removeAllItems();
        promptCategoryComboBox.addItem("-- Select Prompt Template --");
        for (String promptName : config.getCustomPrompts().keySet()) {
            promptCategoryComboBox.addItem(promptName);
        }

        vulnClassComboBox.removeAllItems();
        vulnClassComboBox.addItem("-- Select Vuln Class --");
        for (String vulnName : config.getVulnerabilityClasses().keySet()) {
            vulnClassComboBox.addItem(vulnName);
        }

        SwingWorker<List<ModelEntry>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ModelEntry> doInBackground() {
                return apiClient.fetchAvailableModels(config);
            }

            @Override
            protected void done() {
                try {
                    cachedFetchedModels = get();
                    renderModelComboBox();
                } catch (Exception ignored) {
                }
            }
        };
        worker.execute();
    }

    private void renderModelComboBox() {
        ExtensionConfig config = storageManager.getConfig();
        boolean favoritesOnly = favoriteFilterBtn.isSelected();

        modelComboBox.removeAllItems();
        ModelEntry toSelect = null;

        for (ModelEntry m : cachedFetchedModels) {
            if (!favoritesOnly || config.getFavoriteModels().contains(m.getRawModelId())) {
                modelComboBox.addItem(m);
                if (m.getRawModelId().equals(config.getSelectedModel())) {
                    toSelect = m;
                }
            }
        }

        if (toSelect != null) {
            modelComboBox.setSelectedItem(toSelect);
        }
        updateFavoriteStarButtonState();
    }

    private void updateFavoriteStarButtonState() {
        ModelEntry selected = (ModelEntry) modelComboBox.getSelectedItem();
        if (selected != null) {
            ExtensionConfig config = storageManager.getConfig();
            boolean isFav = config.getFavoriteModels().contains(selected.getRawModelId());
            toggleFavoriteBtn.setText(isFav ? "★ Favorited" : "☆ Star");
        }
    }

    private void toggleCurrentModelFavorite() {
        ModelEntry selected = (ModelEntry) modelComboBox.getSelectedItem();
        if (selected == null) return;

        ExtensionConfig config = storageManager.getConfig();
        String rawId = selected.getRawModelId();
        if (config.getFavoriteModels().contains(rawId)) {
            config.getFavoriteModels().remove(rawId);
        } else {
            config.getFavoriteModels().add(rawId);
        }
        storageManager.saveConfig(config);
        updateFavoriteStarButtonState();
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
        ModelEntry selectedModel = (ModelEntry) modelComboBox.getSelectedItem();
        if (selectedModel != null) {
            config.setSelectedModel(selectedModel.getRawModelId());
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

        apiClient.streamChatCompletion(config, selectedModel, activeSession.getMessages().subList(0, activeSession.getMessages().size() - 1), userPrompt, new ApiClient.StreamCallback() {
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
                sb.append("<div class='user-bubble'><h3>👤 You</h3>");
            } else if (msg.getRole() == ChatMessage.Role.ASSISTANT) {
                sb.append("<div class='ai-bubble'><h3>🤖 AI Assistant</h3>");
            }

            if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                sb.append(msg.getContent()).append("\n\n");
            }

            if (msg.getHttpRequest() != null && !msg.getHttpRequest().isEmpty()) {
                String method = msg.getHttpMethod() != null ? msg.getHttpMethod().toUpperCase() : "GET";
                String badgeClass = "POST".equalsIgnoreCase(method) ? "badge-post" : "badge-get";

                sb.append("<div class='traffic-card'>")
                        .append("<span class='").append(badgeClass).append("'>HTTP Request ").append(method).append("</span> ")
                        .append("<code>").append(msg.getHttpUrl() != null ? msg.getHttpUrl() : "").append("</code><br/><br/>")
                        .append("```http\n").append(msg.getHttpRequest()).append("\n```\n</div>\n\n");
            }

            if (msg.getHttpResponse() != null && !msg.getHttpResponse().isEmpty()) {
                sb.append("<div class='traffic-card'>")
                        .append("<strong>HTTP Response:</strong><br/>")
                        .append("```http\n").append(msg.getHttpResponse()).append("\n```\n</div>\n\n");
            }

            sb.append("</div><hr/>");
        }

        chatDisplayPane.setText(MarkdownUtil.toHtml(sb.toString()));
        try {
            chatDisplayPane.setCaretPosition(Math.max(0, chatDisplayPane.getDocument().getLength() - 1));
        } catch (Exception ignored) {
        }
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
