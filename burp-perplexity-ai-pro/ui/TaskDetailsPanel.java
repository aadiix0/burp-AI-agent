package burp.ui;

import burp.ai.AITask;
import burp.ai.AITaskStep;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class TaskDetailsPanel extends JPanel {
    private final MontoyaApi api;
    private AITask currentTask;
    
    // UI Components
    private JLabel taskIdLabel;
    private JLabel statusLabel;
    private JLabel durationLabel;
    private JLabel stepsLabel;
    private JLabel creditsLabel;
    private JTextArea summaryArea;
    private JTextArea reasoningArea;
    private JList<String> stepList;
    private DefaultListModel<String> stepListModel;
    private JTextArea stepDetailsArea;
    private JTextArea requestArea;
    private JTextArea responseArea;
    private JProgressBar progressBar;
    
    public TaskDetailsPanel(MontoyaApi api) {
        this.api = api;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel - Task overview
        JPanel overviewPanel = createOverviewPanel();
        add(overviewPanel, BorderLayout.NORTH);
        
        // Center panel - Split between steps and details
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // Left: Step list
        JPanel stepListPanel = createStepListPanel();
        centerSplit.setLeftComponent(stepListPanel);
        
        // Right: Step details and request/response
        JPanel detailsPanel = createDetailsPanel();
        centerSplit.setRightComponent(detailsPanel);
        
        centerSplit.setDividerLocation(250);
        add(centerSplit, BorderLayout.CENTER);
    }
    
    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Task Overview",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));
        
        // Info grid
        JPanel infoGrid = new JPanel(new GridLayout(3, 4, 10, 5));
        
        infoGrid.add(new JLabel("Task ID:"));
        taskIdLabel = new JLabel("-");
        taskIdLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        infoGrid.add(taskIdLabel);
        
        infoGrid.add(new JLabel("Status:"));
        statusLabel = new JLabel("-");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        infoGrid.add(statusLabel);
        
        infoGrid.add(new JLabel("Duration:"));
        durationLabel = new JLabel("-");
        infoGrid.add(durationLabel);
        
        infoGrid.add(new JLabel("Steps:"));
        stepsLabel = new JLabel("-");
        infoGrid.add(stepsLabel);
        
        infoGrid.add(new JLabel("Credits Used:"));
        creditsLabel = new JLabel("-");
        infoGrid.add(creditsLabel);
        
        infoGrid.add(new JLabel("Progress:"));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        infoGrid.add(progressBar);
        
        panel.add(infoGrid, BorderLayout.NORTH);
        
        // Summary area
        summaryArea = new JTextArea(3, 50);
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        summaryArea.setBorder(BorderFactory.createTitledBorder("Summary"));
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        panel.add(summaryScroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStepListPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Execution Steps"));
        
        stepListModel = new DefaultListModel<>();
        stepList = new JList<>(stepListModel);
        stepList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stepList.setFont(new Font("SansSerif", Font.PLAIN, 11));
        stepList.setCellRenderer(new StepListCellRenderer());
        stepList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showStepDetails();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(stepList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Step count label
        JLabel stepCountLabel = new JLabel("Total Steps: 0");
        stepCountLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(stepCountLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        // Top: Step details and AI reasoning
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        
        // Step details
        stepDetailsArea = new JTextArea();
        stepDetailsArea.setEditable(false);
        stepDetailsArea.setLineWrap(true);
        stepDetailsArea.setWrapStyleWord(true);
        stepDetailsArea.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JScrollPane detailsScroll = new JScrollPane(stepDetailsArea);
        detailsScroll.setBorder(BorderFactory.createTitledBorder("Step Details"));
        detailsScroll.setPreferredSize(new Dimension(400, 150));
        topPanel.add(detailsScroll, BorderLayout.NORTH);
        
        // AI reasoning
        reasoningArea = new JTextArea();
        reasoningArea.setEditable(false);
        reasoningArea.setLineWrap(true);
        reasoningArea.setWrapStyleWord(true);
        reasoningArea.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JScrollPane reasoningScroll = new JScrollPane(reasoningArea);
        reasoningScroll.setBorder(BorderFactory.createTitledBorder("AI Analysis & Reasoning"));
        reasoningScroll.setPreferredSize(new Dimension(400, 150));
        topPanel.add(reasoningScroll, BorderLayout.CENTER);
        
        panel.add(topPanel, BorderLayout.NORTH);
        
        // Bottom: Request/Response viewer
        JSplitPane requestResponseSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        requestArea = new JTextArea();
        requestArea.setEditable(false);
        requestArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        JScrollPane requestScroll = new JScrollPane(requestArea);
        requestScroll.setBorder(BorderFactory.createTitledBorder("HTTP Request"));
        requestResponseSplit.setTopComponent(requestScroll);
        
        responseArea = new JTextArea();
        responseArea.setEditable(false);
        responseArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        JScrollPane responseScroll = new JScrollPane(responseArea);
        responseScroll.setBorder(BorderFactory.createTitledBorder("HTTP Response"));
        requestResponseSplit.setBottomComponent(responseScroll);
        
        requestResponseSplit.setDividerLocation(200);
        panel.add(requestResponseSplit, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Display task details
     */
    public void showTask(AITask task) {
        this.currentTask = task;
        
        if (task == null) {
            clearDisplay();
            return;
        }
        
        // Update overview
        taskIdLabel.setText(task.getTaskId());
        statusLabel.setText(task.getStatus().toString());
        statusLabel.setForeground(getStatusColor(task.getStatus()));
        
        // Calculate duration
        if (task.getStartTime() != null) {
            Duration duration;
            if (task.getEn
