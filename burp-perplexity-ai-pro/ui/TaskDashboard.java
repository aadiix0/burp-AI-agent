package burp.ui;

import burp.ai.AITask;
import burp.ai.AITaskManager;
import burp.ai.AITaskStep;
import burp.api.montoya.MontoyaApi;
import burp.testing.VulnerabilityTester;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TaskDashboard extends JPanel implements AITaskManager.TaskListener {
    private final MontoyaApi api;
    private final AITaskManager taskManager;
    private final VulnerabilityTester vulnerabilityTester;
    private JTable taskTable;
    private TaskTableModel taskTableModel;
    private JTextArea taskDetailsArea;
    private JButton cancelButton;
    private JButton refreshButton;
    
    public TaskDashboard(MontoyaApi api, AITaskManager taskManager,
                        VulnerabilityTester vulnerabilityTester) {
        this.api = api;
        this.taskManager = taskManager;
        this.vulnerabilityTester = vulnerabilityTester;
        
        initComponents();
        taskManager.addListener(this);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel - Controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshTaskList());
        controlPanel.add(refreshButton);
        
        cancelButton = new JButton("Cancel Selected");
        cancelButton.addActionListener(e -> cancelSelectedTask());
        controlPanel.add(cancelButton);
        
        JLabel titleLabel = new JLabel("AI Testing Tasks Dashboard");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(titleLabel);
        
        add(controlPanel, BorderLayout.NORTH);
        
        // Center panel - Task table and details
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        // Task table
        taskTableModel = new TaskTableModel();
        taskTable = new JTable(taskTableModel);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showTaskDetails();
            }
        });
        
        JScrollPane tableScroll = new JScrollPane(taskTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Active and Recent Tasks"));
        splitPane.setTopComponent(tableScroll);
        
        // Task details
        taskDetailsArea = new JTextArea();
        taskDetailsArea.setEditable(false);
        taskDetailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane detailsScroll = new JScrollPane(taskDetailsArea);
        detailsScroll.setBorder(BorderFactory.createTitledBorder("Task Details"));
        splitPane.setBottomComponent(detailsScroll);
        
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);
        
        // Load initial tasks
        refreshTaskList();
    }
    
    private void refreshTaskList() {
        taskTableModel.setTasks(taskManager.getAllTasks());
    }
    
    private void showTaskDetails() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        
        AITask task = taskTableModel.getTaskAt(selectedRow);
        if (task == null) {
            return;
        }
        
        StringBuilder details = new StringBuilder();
        details.append("═══════════════════════════════════════════════════\n");
        details.append("TASK DETAILS\n");
        details.append("═══════════════════════════════════════════════════\n\n");
        
        details.append("Task ID: ").append(task.getTaskId()).append("\n");
        details.append("Description: ").append(task.getDescription()).append("\n");
        details.append("Target URL: ").append(task.getTargetUrl()).append("\n");
        details.append("Status: ").append(task.getStatus()).append("\n");
        details.append("Start Time: ").append(formatDateTime(task.getStartTime())).append("\n");
        
        if (task.getEndTime() != null) {
            details.append("End Time: ").append(formatDateTime(task.getEndTime())).append("\n");
        }
        
        details.append("Steps: ").append(task.getStepCount()).append("\n");
        details.append("Credits Used: ").append(task.getCreditsUsed()).append("\n\n");
        
        if (task.getSummary() != null) {
            details.append("Summary: ").append(task.getSummary()).append("\n\n");
        }
        
        details.append("───────────────────────────────────────────────────\n");
        details.append("EXECUTION STEPS\n");
        details.append("───────────────────────────────────────────────────\n\n");
        
        for (AITaskStep step : task.getSteps()) {
            details.append(String.format("Step %d: %s\n", 
                step.getStepNumber(), step.getAction()));
            details.append(String.format("Time: %s\n", formatDateTime(step.getTimestamp())));
            details.append(String.format("Result: %s\n", step.getSummary()));
            
            if (step.getAiReasoning() != null && !step.getAiReasoning().isEmpty()) {
                details.append("AI Analysis:\n");
                details.append(wrapText(step.getAiReasoning(), 70)).append("\n");
            }
            
            details.append(String.format("Status: %s | Credits: %d\n\n",
                step.isSuccessful() ? "✓ Success" : "✗ Failed",
                step.getCreditsUsed()));
        }
        
        taskDetailsArea.setText(details.toString());
        taskDetailsArea.setCaretPosition(0);
    }
    
    private void cancelSelectedTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Please select a task to cancel",
                "No Task Selected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        AITask task = taskTableModel.getTaskAt(selectedRow);
        if (task.getStatus() == AITask.TaskStatus.RUNNING) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel this task?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                vulnerabilityTester.cancelTest(task.getTaskId());
                refreshTaskList();
            }
        } else {
            JOptionPane.showMessageDialog(this,
                "Selected task is not running",
                "Cannot Cancel",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    private String wrapText(String text, int width) {
        StringBuilder wrapped = new StringBuilder();
        String[] words = text.split("\\s+");
        int lineLength = 0;
        
        for (String word : words) {
            if (lineLength + word.length() > width) {
                wrapped.append("\n");
                lineLength = 0;
            }
            wrapped.append(word).append(" ");
            lineLength += word.length() + 1;
        }
        
        return wrapped.toString();
    }
    
    @Override
    public void onTaskUpdated(AITask task) {
        SwingUtilities.invokeLater(() -> {
            refreshTaskList();
            // If this task is selected, update details
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow >= 0) {
                AITask selectedTask = taskTableModel.getTaskAt(selectedRow);
                if (selectedTask != null && selectedTask.getTaskId().equals(task.getTaskId())) {
                    showTaskDetails();
                }
            }
        });
    }
    
    // Table model
    private static class TaskTableModel extends AbstractTableModel {
        private final String[] columnNames = {
            "Task ID", "Description", "Status", "Steps", "Credits", "Start Time"
        };
        private List<AITask> tasks = new ArrayList<>();
        
        public void setTasks(List<AITask> tasks) {
            this.tasks = new ArrayList<>(tasks);
            fireTableDataChanged();
        }
        
        public AITask getTaskAt(int row) {
            if (row >= 0 && row < tasks.size()) {
                return tasks.get(row);
            }
            return null;
        }
        
        @Override
        public int getRowCount() {
            return tasks.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AITask task = tasks.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> task.getTaskId();
                case 1 -> task.getDescription();
                case 2 -> task.getStatus();
                case 3 -> task.getStepCount();
                case 4 -> task.getCreditsUsed();
                case 5 -> formatDateTime(task.getStartTime());
                default -> null;
            };
        }
        
        private String formatDateTime(java.time.LocalDateTime dateTime) {
            if (dateTime == null) return "";
            return dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
    }
}
