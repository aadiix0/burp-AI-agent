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

    private JTextField nvidiaKeyField;
    private JTextField openCodeZenKeyField;
    private JTextField openCodeZenUrlField;
    private JCheckBox openCodeZenFreeOnlyCheckBox;
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

        nvidiaKeyField = new JTextField(30);
        openCodeZenKeyField = new JTextField(30);
        openCodeZenUrlField = new JTextField(30);
        openCodeZenFreeOnlyCheckBox = new JCheckBox("Show Only Free Models for OpenCode Zen (Filter out paid models)");
        customUrlField = new JTextField(30);
        customKeyField = new JTextField(30);

        addGridRow(keysPanel, gbc, 0, "NVIDIA API Key:", nvidiaKeyField);
        addGridRow(keysPanel, gbc, 1, "OpenCode Zen API Key:", openCodeZenKeyField);
        addGridRow(keysPanel, gbc, 2, "OpenCode Zen Base URL:", openCodeZenUrlField);

        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        keysPanel.add(openCodeZenFreeOnlyCheckBox, gbc);

        addGridRow(keysPanel, gbc, 4, "Custom API Base URL:", customUrlField);
        addGridRow(keysPanel, gbc, 5, "Custom API Key:", customKeyField);

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

    private void addGridRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private void loadData() {
        ExtensionConfig config = storageManager.getConfig();
        nvidiaKeyField.setText(config.getNvidiaApiKey());
        openCodeZenKeyField.setText(config.getOpenCodeZenApiKey());
        openCodeZenUrlField.setText(config.getOpenCodeZenBaseUrl());
        openCodeZenFreeOnlyCheckBox.setSelected(config.isOpenCodeZenFreeOnly());
        customUrlField.setText(config.getCustomApiUrl());
        customKeyField.setText(config.getCustomApiKey());
        enableInspectorCheckBox.setSelected(config.isEnableInspectorTab());
        systemPromptArea.setText(config.getSystemPrompt());
    }

    private void saveSettings() {
        ExtensionConfig config = storageManager.getConfig();
        config.setNvidiaApiKey(nvidiaKeyField.getText().trim());
        config.setOpenCodeZenApiKey(openCodeZenKeyField.getText().trim());
        config.setOpenCodeZenBaseUrl(openCodeZenUrlField.getText().trim());
        config.setOpenCodeZenFreeOnly(openCodeZenFreeOnlyCheckBox.isSelected());
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
