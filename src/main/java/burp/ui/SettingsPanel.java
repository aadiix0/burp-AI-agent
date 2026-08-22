package burp.ui;

import burp.api.montoya.MontoyaApi;
import burp.storage.ExtensionConfig;
import burp.storage.StorageManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public class SettingsPanel extends JPanel {
    private final MontoyaApi api;
    private final StorageManager storageManager;
    private final Runnable onConfigUpdated;

    private JCheckBox enableNvidiaCheckBox;
    private JTextField nvidiaKeyField;

    private JCheckBox enableOpenCodeZenCheckBox;
    private JTextField openCodeZenKeyField;
    private JCheckBox openCodeZenFreeOnlyCheckBox;

    private JCheckBox enableAiHubMixCheckBox;
    private JTextField aiHubMixKeyField;

    private JCheckBox enableOpenRouterCheckBox;
    private JTextField openRouterKeyField;

    private JCheckBox enableGoogleCheckBox;
    private JTextField googleKeyField;
    private JCheckBox googleFreeOnlyCheckBox;

    private JCheckBox enableCerebrasCheckBox;
    private JTextField cerebrasKeyField;

    private JCheckBox enableGroqCheckBox;
    private JTextField groqKeyField;
    private JCheckBox groqFreeOnlyCheckBox;

    private JCheckBox enableCloudflareCheckBox;
    private JTextField cloudflareKeyField;
    private JTextField cloudflareAccountIdField;

    private JCheckBox enableCustomCheckBox;
    private JTextField customUrlField;
    private JTextField customKeyField;

    private JTextArea systemPromptArea;

    public SettingsPanel(MontoyaApi api, StorageManager storageManager, Runnable onConfigUpdated) {
        this.api = api;
        this.storageManager = storageManager;
        this.onConfigUpdated = onConfigUpdated;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initUI();
        loadData();
    }

    private void initUI() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        // API Keys Section
        JPanel keysPanel = createSectionPanel("API Credentials & Custom Endpoints");
        keysPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        enableNvidiaCheckBox = new JCheckBox("Enable NVIDIA AI");
        nvidiaKeyField = new JTextField(30);

        enableOpenCodeZenCheckBox = new JCheckBox("Enable OpenCode Zen");
        openCodeZenKeyField = new JTextField(30);
        openCodeZenFreeOnlyCheckBox = new JCheckBox("Show Only Free Models for OpenCode Zen");

        enableAiHubMixCheckBox = new JCheckBox("Enable AIHubMix");
        aiHubMixKeyField = new JTextField(30);

        enableOpenRouterCheckBox = new JCheckBox("Enable OpenRouter");
        openRouterKeyField = new JTextField(30);

        enableGoogleCheckBox = new JCheckBox("Enable Google AI Studio (Gemini)");
        googleKeyField = new JTextField(30);
        googleFreeOnlyCheckBox = new JCheckBox("Show Only Free Tier Models for Google AI Studio");

        enableCerebrasCheckBox = new JCheckBox("Enable Cerebras");
        cerebrasKeyField = new JTextField(30);

        enableGroqCheckBox = new JCheckBox("Enable Groq");
        groqKeyField = new JTextField(30);
        groqFreeOnlyCheckBox = new JCheckBox("Show Only Free Models for Groq");

        enableCloudflareCheckBox = new JCheckBox("Enable Cloudflare Workers AI");
        cloudflareKeyField = new JTextField(30);
        cloudflareAccountIdField = new JTextField(30);

        enableCustomCheckBox = new JCheckBox("Enable Custom OpenAI-Compatible Endpoint");
        customUrlField = new JTextField(30);
        customKeyField = new JTextField(30);

        int row = 0;
        row = addCompactProviderRow(keysPanel, gbc, row, enableNvidiaCheckBox, "API Key:", nvidiaKeyField, null);
        row = addCompactProviderRow(keysPanel, gbc, row, enableOpenCodeZenCheckBox, "API Key:", openCodeZenKeyField, openCodeZenFreeOnlyCheckBox);
        row = addCompactProviderRow(keysPanel, gbc, row, enableAiHubMixCheckBox, "API Key:", aiHubMixKeyField, null);
        row = addCompactProviderRow(keysPanel, gbc, row, enableOpenRouterCheckBox, "API Key:", openRouterKeyField, null);
        row = addCompactProviderRow(keysPanel, gbc, row, enableGoogleCheckBox, "API Key:", googleKeyField, googleFreeOnlyCheckBox);
        row = addCompactProviderRow(keysPanel, gbc, row, enableCerebrasCheckBox, "API Key:", cerebrasKeyField, null);
        row = addCompactProviderRow(keysPanel, gbc, row, enableGroqCheckBox, "API Key:", groqKeyField, groqFreeOnlyCheckBox);

        // Cloudflare (Key + Account ID)
        row = addCompactProviderRow(keysPanel, gbc, row, enableCloudflareCheckBox, "Token:", cloudflareKeyField, null);
        row = addGridRow(keysPanel, gbc, row, "Account ID:", cloudflareAccountIdField);

        // Custom (URL + Key)
        row = addCompactProviderRow(keysPanel, gbc, row, enableCustomCheckBox, "Base URL:", customUrlField, null);
        row = addGridRow(keysPanel, gbc, row, "API Key:", customKeyField);

        formPanel.add(keysPanel);
        formPanel.add(Box.createVerticalStrut(10));

        // Custom System Prompt
        JPanel promptPanel = createSectionPanel("Global System Prompt");
        promptPanel.setLayout(new BorderLayout(5, 5));
        systemPromptArea = new JTextArea(5, 40);
        systemPromptArea.setLineWrap(true);
        systemPromptArea.setWrapStyleWord(true);
        promptPanel.add(new JScrollPane(systemPromptArea), BorderLayout.CENTER);

        formPanel.add(promptPanel);
        formPanel.add(Box.createVerticalStrut(15));

        // Save Button Bar
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save Settings");
        saveBtn.setFont(saveBtn.getFont().deriveFont(Font.BOLD));
        saveBtn.addActionListener(e -> saveSettings());
        btnPanel.add(saveBtn);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);

        add(scrollPane, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private int addCompactProviderRow(JPanel panel, GridBagConstraints gbc, int row, JCheckBox enableBox, String labelText, JTextField keyField, JCheckBox freeOnlyBox) {
        enableBox.setFont(enableBox.getFont().deriveFont(Font.BOLD));

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(enableBox, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rightPanel.add(new JLabel(labelText));

        keyField.setPreferredSize(new Dimension(380, 26));
        rightPanel.add(keyField);

        if (freeOnlyBox != null) {
            rightPanel.add(freeOnlyBox);
        }

        panel.add(rightPanel, gbc);
        return row + 1;
    }

    private int addGridRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(""), gbc); // blank left spacer

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rightPanel.add(new JLabel(labelText));

        if (field instanceof JTextField) {
            field.setPreferredSize(new Dimension(380, 26));
        }
        rightPanel.add(field);

        panel.add(rightPanel, gbc);
        return row + 1;
    }

    private void loadData() {
        ExtensionConfig config = storageManager.getConfig();

        enableNvidiaCheckBox.setSelected(config.isEnableNvidia());
        nvidiaKeyField.setText(config.getNvidiaApiKey());

        enableOpenCodeZenCheckBox.setSelected(config.isEnableOpenCodeZen());
        openCodeZenKeyField.setText(config.getOpenCodeZenApiKey());
        openCodeZenFreeOnlyCheckBox.setSelected(config.isOpenCodeZenFreeOnly());

        enableAiHubMixCheckBox.setSelected(config.isEnableAiHubMix());
        aiHubMixKeyField.setText(config.getAiHubMixApiKey());

        enableOpenRouterCheckBox.setSelected(config.isEnableOpenRouter());
        openRouterKeyField.setText(config.getOpenRouterApiKey());

        enableGoogleCheckBox.setSelected(config.isEnableGoogleAiStudio());
        googleKeyField.setText(config.getGoogleApiKey());
        googleFreeOnlyCheckBox.setSelected(config.isGoogleFreeOnly());

        enableCerebrasCheckBox.setSelected(config.isEnableCerebras());
        cerebrasKeyField.setText(config.getCerebrasApiKey());

        enableGroqCheckBox.setSelected(config.isEnableGroq());
        groqKeyField.setText(config.getGroqApiKey());
        groqFreeOnlyCheckBox.setSelected(config.isGroqFreeOnly());

        enableCloudflareCheckBox.setSelected(config.isEnableCloudflare());
        cloudflareKeyField.setText(config.getCloudflareApiKey());
        cloudflareAccountIdField.setText(config.getCloudflareAccountId());

        enableCustomCheckBox.setSelected(config.isEnableCustom());
        customUrlField.setText(config.getCustomApiUrl());
        customKeyField.setText(config.getCustomApiKey());

        systemPromptArea.setText(config.getSystemPrompt());
    }

    private void saveSettings() {
        ExtensionConfig config = storageManager.getConfig();

        config.setEnableNvidia(enableNvidiaCheckBox.isSelected());
        config.setNvidiaApiKey(nvidiaKeyField.getText().trim());

        config.setEnableOpenCodeZen(enableOpenCodeZenCheckBox.isSelected());
        config.setOpenCodeZenApiKey(openCodeZenKeyField.getText().trim());
        config.setOpenCodeZenFreeOnly(openCodeZenFreeOnlyCheckBox.isSelected());

        config.setEnableAiHubMix(enableAiHubMixCheckBox.isSelected());
        config.setAiHubMixApiKey(aiHubMixKeyField.getText().trim());

        config.setEnableOpenRouter(enableOpenRouterCheckBox.isSelected());
        config.setOpenRouterApiKey(openRouterKeyField.getText().trim());

        config.setEnableGoogleAiStudio(enableGoogleCheckBox.isSelected());
        config.setGoogleApiKey(googleKeyField.getText().trim());
        config.setGoogleFreeOnly(googleFreeOnlyCheckBox.isSelected());

        config.setEnableCerebras(enableCerebrasCheckBox.isSelected());
        config.setCerebrasApiKey(cerebrasKeyField.getText().trim());

        config.setEnableGroq(enableGroqCheckBox.isSelected());
        config.setGroqApiKey(groqKeyField.getText().trim());
        config.setGroqFreeOnly(groqFreeOnlyCheckBox.isSelected());

        config.setEnableCloudflare(enableCloudflareCheckBox.isSelected());
        config.setCloudflareApiKey(cloudflareKeyField.getText().trim());
        config.setCloudflareAccountId(cloudflareAccountIdField.getText().trim());

        config.setEnableCustom(enableCustomCheckBox.isSelected());
        config.setCustomApiUrl(customUrlField.getText().trim());
        config.setCustomApiKey(customKeyField.getText().trim());

        config.setSystemPrompt(systemPromptArea.getText().trim());

        storageManager.saveConfig(config);
        JOptionPane.showMessageDialog(this, "Settings saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

        if (onConfigUpdated != null) {
            onConfigUpdated.run();
        }
    }
}
