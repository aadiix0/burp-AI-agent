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
    private JTextField openCodeZenUrlField;
    private JCheckBox openCodeZenFreeOnlyCheckBox;

    private JCheckBox enableAiHubMixCheckBox;
    private JTextField aiHubMixKeyField;
    private JTextField aiHubMixUrlField;

    private JCheckBox enableOpenRouterCheckBox;
    private JTextField openRouterKeyField;
    private JTextField openRouterUrlField;

    private JCheckBox enableGoogleCheckBox;
    private JTextField googleKeyField;
    private JTextField googleUrlField;
    private JCheckBox googleFreeOnlyCheckBox;

    private JCheckBox enableCerebrasCheckBox;
    private JTextField cerebrasKeyField;
    private JTextField cerebrasUrlField;

    private JCheckBox enableGroqCheckBox;
    private JTextField groqKeyField;
    private JTextField groqUrlField;
    private JCheckBox groqFreeOnlyCheckBox;

    private JCheckBox enableCloudflareCheckBox;
    private JTextField cloudflareKeyField;
    private JTextField cloudflareAccountIdField;

    private JCheckBox enableCustomCheckBox;
    private JTextField customUrlField;
    private JTextField customKeyField;

    private JCheckBox enableInspectorCheckBox;
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
        openCodeZenUrlField = new JTextField(30);
        openCodeZenFreeOnlyCheckBox = new JCheckBox("Show Only Free Models for OpenCode Zen");

        enableAiHubMixCheckBox = new JCheckBox("Enable AIHubMix");
        aiHubMixKeyField = new JTextField(30);
        aiHubMixUrlField = new JTextField(30);

        enableOpenRouterCheckBox = new JCheckBox("Enable OpenRouter");
        openRouterKeyField = new JTextField(30);
        openRouterUrlField = new JTextField(30);

        enableGoogleCheckBox = new JCheckBox("Enable Google AI Studio (Gemini)");
        googleKeyField = new JTextField(30);
        googleUrlField = new JTextField(30);
        googleFreeOnlyCheckBox = new JCheckBox("Show Only Free Tier Models for Google AI Studio");

        enableCerebrasCheckBox = new JCheckBox("Enable Cerebras");
        cerebrasKeyField = new JTextField(30);
        cerebrasUrlField = new JTextField(30);

        enableGroqCheckBox = new JCheckBox("Enable Groq");
        groqKeyField = new JTextField(30);
        groqUrlField = new JTextField(30);
        groqFreeOnlyCheckBox = new JCheckBox("Show Only Free Models for Groq");

        enableCloudflareCheckBox = new JCheckBox("Enable Cloudflare Workers AI");
        cloudflareKeyField = new JTextField(30);
        cloudflareAccountIdField = new JTextField(30);

        enableCustomCheckBox = new JCheckBox("Enable Custom OpenAI-Compatible Endpoint");
        customUrlField = new JTextField(30);
        customKeyField = new JTextField(30);

        int row = 0;
        row = addProviderHeader(keysPanel, gbc, row, enableNvidiaCheckBox);
        row = addGridRow(keysPanel, gbc, row, "NVIDIA API Key:", nvidiaKeyField);

        row = addProviderHeader(keysPanel, gbc, row, enableOpenCodeZenCheckBox);
        row = addGridRow(keysPanel, gbc, row, "OpenCode Zen API Key:", openCodeZenKeyField);
        row = addGridRow(keysPanel, gbc, row, "OpenCode Zen Base URL:", openCodeZenUrlField);
        row = addCheckboxRow(keysPanel, gbc, row, openCodeZenFreeOnlyCheckBox);

        row = addProviderHeader(keysPanel, gbc, row, enableAiHubMixCheckBox);
        row = addGridRow(keysPanel, gbc, row, "AIHubMix API Key:", aiHubMixKeyField);
        row = addGridRow(keysPanel, gbc, row, "AIHubMix Base URL:", aiHubMixUrlField);

        row = addProviderHeader(keysPanel, gbc, row, enableOpenRouterCheckBox);
        row = addGridRow(keysPanel, gbc, row, "OpenRouter API Key:", openRouterKeyField);
        row = addGridRow(keysPanel, gbc, row, "OpenRouter Base URL:", openRouterUrlField);

        row = addProviderHeader(keysPanel, gbc, row, enableGoogleCheckBox);
        row = addGridRow(keysPanel, gbc, row, "Google AI API Key:", googleKeyField);
        row = addGridRow(keysPanel, gbc, row, "Google AI Base URL:", googleUrlField);
        row = addCheckboxRow(keysPanel, gbc, row, googleFreeOnlyCheckBox);

        row = addProviderHeader(keysPanel, gbc, row, enableCerebrasCheckBox);
        row = addGridRow(keysPanel, gbc, row, "Cerebras API Key:", cerebrasKeyField);
        row = addGridRow(keysPanel, gbc, row, "Cerebras Base URL:", cerebrasUrlField);

        row = addProviderHeader(keysPanel, gbc, row, enableGroqCheckBox);
        row = addGridRow(keysPanel, gbc, row, "Groq API Key:", groqKeyField);
        row = addGridRow(keysPanel, gbc, row, "Groq Base URL:", groqUrlField);
        row = addCheckboxRow(keysPanel, gbc, row, groqFreeOnlyCheckBox);

        row = addProviderHeader(keysPanel, gbc, row, enableCloudflareCheckBox);
        row = addGridRow(keysPanel, gbc, row, "Cloudflare API Token:", cloudflareKeyField);
        row = addGridRow(keysPanel, gbc, row, "Cloudflare Account ID:", cloudflareAccountIdField);

        row = addProviderHeader(keysPanel, gbc, row, enableCustomCheckBox);
        row = addGridRow(keysPanel, gbc, row, "Custom API Base URL:", customUrlField);
        row = addGridRow(keysPanel, gbc, row, "Custom API Key:", customKeyField);

        formPanel.add(keysPanel);
        formPanel.add(Box.createVerticalStrut(15));

        // Display Options
        JPanel displayPanel = createSectionPanel("Burp Suite UI Options");
        displayPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        enableInspectorCheckBox = new JCheckBox("Show AI Inspector Tab in Repeater / Proxy Message Editors");
        displayPanel.add(enableInspectorCheckBox);

        formPanel.add(displayPanel);
        formPanel.add(Box.createVerticalStrut(15));

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

        add(scrollPane, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private int addProviderHeader(JPanel panel, GridBagConstraints gbc, int row, JCheckBox enableBox) {
        enableBox.setFont(enableBox.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(enableBox, gbc);
        gbc.gridwidth = 1;
        return row + 1;
    }

    private int addGridRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
        return row + 1;
    }

    private int addCheckboxRow(JPanel panel, GridBagConstraints gbc, int row, JCheckBox checkbox) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        panel.add(checkbox, gbc);
        return row + 1;
    }

    private void loadData() {
        ExtensionConfig config = storageManager.getConfig();

        enableNvidiaCheckBox.setSelected(config.isEnableNvidia());
        nvidiaKeyField.setText(config.getNvidiaApiKey());

        enableOpenCodeZenCheckBox.setSelected(config.isEnableOpenCodeZen());
        openCodeZenKeyField.setText(config.getOpenCodeZenApiKey());
        openCodeZenUrlField.setText(config.getOpenCodeZenBaseUrl());
        openCodeZenFreeOnlyCheckBox.setSelected(config.isOpenCodeZenFreeOnly());

        enableAiHubMixCheckBox.setSelected(config.isEnableAiHubMix());
        aiHubMixKeyField.setText(config.getAiHubMixApiKey());
        aiHubMixUrlField.setText(config.getAiHubMixBaseUrl());

        enableOpenRouterCheckBox.setSelected(config.isEnableOpenRouter());
        openRouterKeyField.setText(config.getOpenRouterApiKey());
        openRouterUrlField.setText(config.getOpenRouterBaseUrl());

        enableGoogleCheckBox.setSelected(config.isEnableGoogleAiStudio());
        googleKeyField.setText(config.getGoogleApiKey());
        googleUrlField.setText(config.getGoogleBaseUrl());
        googleFreeOnlyCheckBox.setSelected(config.isGoogleFreeOnly());

        enableCerebrasCheckBox.setSelected(config.isEnableCerebras());
        cerebrasKeyField.setText(config.getCerebrasApiKey());
        cerebrasUrlField.setText(config.getCerebrasBaseUrl());

        enableGroqCheckBox.setSelected(config.isEnableGroq());
        groqKeyField.setText(config.getGroqApiKey());
        groqUrlField.setText(config.getGroqBaseUrl());
        groqFreeOnlyCheckBox.setSelected(config.isGroqFreeOnly());

        enableCloudflareCheckBox.setSelected(config.isEnableCloudflare());
        cloudflareKeyField.setText(config.getCloudflareApiKey());
        cloudflareAccountIdField.setText(config.getCloudflareAccountId());

        enableCustomCheckBox.setSelected(config.isEnableCustom());
        customUrlField.setText(config.getCustomApiUrl());
        customKeyField.setText(config.getCustomApiKey());

        enableInspectorCheckBox.setSelected(config.isEnableInspectorTab());
        systemPromptArea.setText(config.getSystemPrompt());
    }

    private void saveSettings() {
        ExtensionConfig config = storageManager.getConfig();

        config.setEnableNvidia(enableNvidiaCheckBox.isSelected());
        config.setNvidiaApiKey(nvidiaKeyField.getText().trim());

        config.setEnableOpenCodeZen(enableOpenCodeZenCheckBox.isSelected());
        config.setOpenCodeZenApiKey(openCodeZenKeyField.getText().trim());
        config.setOpenCodeZenBaseUrl(openCodeZenUrlField.getText().trim());
        config.setOpenCodeZenFreeOnly(openCodeZenFreeOnlyCheckBox.isSelected());

        config.setEnableAiHubMix(enableAiHubMixCheckBox.isSelected());
        config.setAiHubMixApiKey(aiHubMixKeyField.getText().trim());
        config.setAiHubMixBaseUrl(aiHubMixUrlField.getText().trim());

        config.setEnableOpenRouter(enableOpenRouterCheckBox.isSelected());
        config.setOpenRouterApiKey(openRouterKeyField.getText().trim());
        config.setOpenRouterBaseUrl(openRouterUrlField.getText().trim());

        config.setEnableGoogleAiStudio(enableGoogleCheckBox.isSelected());
        config.setGoogleApiKey(googleKeyField.getText().trim());
        config.setGoogleBaseUrl(googleUrlField.getText().trim());
        config.setGoogleFreeOnly(googleFreeOnlyCheckBox.isSelected());

        config.setEnableCerebras(enableCerebrasCheckBox.isSelected());
        config.setCerebrasApiKey(cerebrasKeyField.getText().trim());
        config.setCerebrasBaseUrl(cerebrasUrlField.getText().trim());

        config.setEnableGroq(enableGroqCheckBox.isSelected());
        config.setGroqApiKey(groqKeyField.getText().trim());
        config.setGroqBaseUrl(groqUrlField.getText().trim());
        config.setGroqFreeOnly(groqFreeOnlyCheckBox.isSelected());

        config.setEnableCloudflare(enableCloudflareCheckBox.isSelected());
        config.setCloudflareApiKey(cloudflareKeyField.getText().trim());
        config.setCloudflareAccountId(cloudflareAccountIdField.getText().trim());

        config.setEnableCustom(enableCustomCheckBox.isSelected());
        config.setCustomApiUrl(customUrlField.getText().trim());
        config.setCustomApiKey(customKeyField.getText().trim());

        config.setEnableInspectorTab(enableInspectorCheckBox.isSelected());
        config.setSystemPrompt(systemPromptArea.getText().trim());

        storageManager.saveConfig(config);
        JOptionPane.showMessageDialog(this, "Settings saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

        if (onConfigUpdated != null) {
            onConfigUpdated.run();
        }
    }
}
