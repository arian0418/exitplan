import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Small Swing composition layer. Calculation and file parsing live in separate classes. */
public class ExitPlan extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Color NAVY = new Color(21, 40, 57);
    private static final Color TEAL = new Color(0, 111, 112);
    private static final Color MINT = new Color(223, 243, 239);
    private static final Color BACKGROUND = new Color(239, 244, 247);
    private static final Color INK = new Color(31, 49, 66);
    private static final Color MUTED = new Color(86, 106, 123);
    private static final Color LINE = new Color(219, 228, 233);
    private static final Color ERROR = new Color(159, 49, 38);
    private static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
        .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, d MMM uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH);

    private final JFormattedTextField deadline = named(new JFormattedTextField(), "deadline");
    private final JSpinner outbound = minutesSpinner(20, PlanCalculator.MAX_TRAVEL_MINUTES, "outbound");
    private final JSpinner inbound = minutesSpinner(20, PlanCalculator.MAX_TRAVEL_MINUTES, "inbound");
    private final JSpinner parking = minutesSpinner(5, PlanCalculator.MAX_PARKING_MINUTES, "parking");
    private final JSpinner buffer = minutesSpinner(15, PlanCalculator.MAX_BUFFER_MINUTES, "buffer");
    private final ActivityTableModel activities = new ActivityTableModel();
    private final JTable activityTable = named(new JTable(activities), "activities");
    private final DefaultTableModel timeline = new DefaultTableModel(new Object[]{"Step", "Start", "End", "Time"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable timelineTable = named(new JTable(timeline), "timeline");
    private final JLabel leaveTime = named(label("—", 42, Font.BOLD, TEAL), "leaveTime");
    private final JLabel leaveDate = named(label("Calculate your next outing", 13, Font.PLAIN, MUTED), "leaveDate");
    private final JLabel totalDuration = named(label("—", 25, Font.BOLD, INK), "totalDuration");
    private final JLabel countdown = named(label("Ready when you are", 23, Font.BOLD, INK), "countdown");
    private final JLabel countdownDetail = label("Time until you need to leave", 12, Font.PLAIN, MUTED);
    private final JLabel status = named(label("", 13, Font.PLAIN, MUTED), "status");
    private final JLabel timelineSummary = label("Calculate to see every step", 13, Font.PLAIN, MUTED);
    private final JLabel homeSummary = label("Your buffer protects the return deadline.", 13, Font.PLAIN, TEAL);
    private final JLabel activityCount = label("", 12, Font.BOLD, TEAL);
    private final JButton calculateButton = button("Calculate plan", "calculate", KeyEvent.VK_C, true);
    private final JButton addButton = button("+ Add", "addActivity", KeyEvent.VK_A, false);
    private final JButton removeButton = button("Remove", "removeActivity", KeyEvent.VK_R, false);
    private final JButton upButton = button("↑", "moveUp", 0, false);
    private final JButton downButton = button("↓", "moveDown", 0, false);
    private final JButton copyButton = button("Copy", "copy", 0, false);
    private final JButton exportButton = button("Export", "export", KeyEvent.VK_E, false);
    private final JButton saveButton = button("Save plan", "save", KeyEvent.VK_S, false);
    private final JButton openButton = button("Open plan", "open", KeyEvent.VK_O, false);
    private final JComboBox<String> preset = named(new JComboBox<>(new String[]{"Dinner", "Dinner + movie", "Errands"}), "preset");
    private final JButton presetButton = button("Use example", "applyPreset", 0, false);
    private final java.awt.CardLayout itineraryCards = new java.awt.CardLayout();
    private final JPanel itineraryContent = new JPanel(itineraryCards);
    private final Timer clock;
    private PlanData calculatedData;
    private PlanCalculator.Plan calculatedPlan;
    private boolean updating;
    private boolean unsavedChanges;
    private Path currentFile;

    public ExitPlan() {
        super("ExitPlan — Reverse planner");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 780));
        setSize(1280, 880);
        setLocationRelativeTo(null);
        configureInputs();
        setContentPane(buildWorkspace());
        setJMenuBar(buildMenu());
        wireActions();
        attachInputListeners();
        getRootPane().setDefaultButton(calculateButton);
        bindShortcuts();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                if (confirmDiscard()) dispose();
            }
        });
        applyData(example("Dinner"));
        calculate();
        unsavedChanges = false;
        updateTitle();
        clock = new Timer(1000, event -> updateCountdown());
        clock.start();
    }

    private void configureInputs() {
        deadline.setColumns(17);
        deadline.setFocusLostBehavior(JFormattedTextField.PERSIST);
        deadline.setFont(font(14, Font.PLAIN));
        deadline.setToolTipText("Local date and 24-hour time: YYYY-MM-DD HH:MM. For example, 2026-10-01 00:30.");
        deadline.getAccessibleContext().setAccessibleName("Must be back by, date and 24-hour time");
        deadline.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
            BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        styleTable(activityTable, 34);
        activityTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        activityTable.setAutoCreateRowSorter(false);
        activityTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        activityTable.getColumnModel().getColumn(1).setPreferredWidth(78);
        activityTable.getColumnModel().getColumn(2).setPreferredWidth(112);
        activityTable.setDefaultEditor(String.class, new ValidatedCellEditor());
        activityTable.setDefaultEditor(Integer.class, new ValidatedCellEditor());
        activityTable.setDefaultRenderer(Integer.class, new ActivityNumberRenderer());
        activityTable.getAccessibleContext().setAccessibleName("Editable activities in visit order");
        activityTable.setToolTipText("Double-click or press F2 to edit. Stay: 1–1440 min. Next stop: 0–1440 min.");
        styleTable(timelineTable, 50);
        timelineTable.setRowSelectionAllowed(false);
        timelineTable.setFocusable(false);
        timelineTable.setDefaultRenderer(Object.class, new TimelineRenderer());
        timelineTable.getColumnModel().getColumn(0).setPreferredWidth(230);
        timelineTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        timelineTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        timelineTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        timelineTable.getAccessibleContext().setAccessibleName("Calculated itinerary, read only");
        preset.setFont(font(12, Font.PLAIN));
        preset.getAccessibleContext().setAccessibleName("Example plan");
        upButton.setToolTipText("Move selected activity up (Alt+Up)");
        downButton.setToolTipText("Move selected activity down (Alt+Down)");
        upButton.getAccessibleContext().setAccessibleName("Move activity up");
        downButton.getAccessibleContext().setAccessibleName("Move activity down");
        addButton.setToolTipText("Add an activity (Ctrl+N)");
        removeButton.setToolTipText("Remove the selected activity (Delete when not editing)");
        calculateButton.setToolTipText("Calculate from the current inputs (Ctrl+Enter)");
        copyButton.setToolTipText("Copy the full itinerary (Ctrl+Shift+C)");
    }

    private JPanel buildWorkspace() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.add(buildHeader(), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(18, 18));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(18, 24, 14, 24));
        body.add(buildHero(), BorderLayout.NORTH);
        JPanel workspace = new JPanel(new BorderLayout(18, 0));
        workspace.setOpaque(false);
        JPanel editor = buildEditor();
        editor.setPreferredSize(new Dimension(420, 500));
        workspace.add(editor, BorderLayout.WEST);
        workspace.add(buildItinerary(), BorderLayout.CENTER);
        body.add(workspace, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(1, 4, 0, 0));
        body.add(status, BorderLayout.SOUTH);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(24, 0));
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(15, 27, 15, 27));
        JPanel brand = new JPanel(new BorderLayout(14, 0));
        brand.setOpaque(false);
        JLabel mark = new JLabel(new ClockIcon());
        brand.add(mark, BorderLayout.WEST);
        JPanel words = vertical();
        words.add(label("ExitPlan", 27, Font.BOLD, Color.WHITE));
        words.add(label("Make time for the outing. Be home on time.", 13, Font.PLAIN, new Color(190, 212, 224)));
        brand.add(words, BorderLayout.CENTER);
        header.add(brand, BorderLayout.WEST);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 6));
        tools.setOpaque(false);
        tools.add(openButton);
        tools.add(saveButton);
        header.add(tools, BorderLayout.EAST);
        return header;
    }

    private JPanel buildHero() {
        JPanel hero = card(new BorderLayout(18, 0), 17);
        hero.setPreferredSize(new Dimension(0, 132));
        JPanel leave = vertical();
        leave.add(label("YOUR LATEST DEPARTURE", 11, Font.BOLD, MUTED));
        leave.add(leaveTime);
        leave.add(leaveDate);
        hero.add(leave, BorderLayout.WEST);
        JPanel metrics = new JPanel(new GridLayout(1, 2, 25, 0));
        metrics.setOpaque(false);
        metrics.setBorder(BorderFactory.createEmptyBorder(6, 35, 5, 5));
        JPanel duration = vertical();
        duration.add(label("TOTAL OUTING", 11, Font.BOLD, MUTED));
        duration.add(Box.createVerticalStrut(11));
        duration.add(totalDuration);
        duration.add(label("Includes travel, parking & buffer", 12, Font.PLAIN, MUTED));
        JPanel remaining = vertical();
        remaining.add(label("DEPARTURE CHECK", 11, Font.BOLD, MUTED));
        remaining.add(Box.createVerticalStrut(11));
        remaining.add(countdown);
        remaining.add(countdownDetail);
        metrics.add(duration);
        metrics.add(remaining);
        hero.add(metrics, BorderLayout.CENTER);
        return hero;
    }

    private JPanel buildEditor() {
        JPanel editor = card(new BorderLayout(0, 13), 17);
        JPanel settings = vertical();
        settings.add(sectionTitle("01  PLAN DETAILS"));
        settings.add(Box.createVerticalStrut(11));
        JLabel deadlineLabel = label("Must be back by", 13, Font.BOLD, INK);
        deadlineLabel.setLabelFor(deadline);
        deadlineLabel.setDisplayedMnemonic(KeyEvent.VK_B);
        settings.add(deadlineLabel);
        settings.add(Box.createVerticalStrut(5));
        deadline.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        settings.add(deadline);
        settings.add(Box.createVerticalStrut(4));
        settings.add(label("YYYY-MM-DD   HH:MM  ·  24-hour local time", 11, Font.PLAIN, MUTED));
        settings.add(Box.createVerticalStrut(13));
        JPanel allowances = new JPanel(new GridLayout(2, 2, 14, 10));
        allowances.setOpaque(false);
        allowances.add(allowance("Outbound travel", outbound));
        allowances.add(allowance("Return travel", inbound));
        allowances.add(allowance("Park / walk · each way", parking));
        allowances.add(allowance("Safety buffer", buffer));
        allowances.setAlignmentX(Component.LEFT_ALIGNMENT);
        settings.add(allowances);
        editor.add(settings, BorderLayout.NORTH);

        JPanel stops = new JPanel(new BorderLayout(0, 8));
        stops.setOpaque(false);
        JPanel stopsHeader = vertical();
        JPanel sectionRow = new JPanel(new BorderLayout());
        sectionRow.setOpaque(false);
        sectionRow.add(sectionTitle("02  ACTIVITIES"), BorderLayout.WEST);
        sectionRow.add(activityCount, BorderLayout.EAST);
        sectionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        stopsHeader.add(sectionRow);
        stopsHeader.add(Box.createVerticalStrut(9));
        JPanel examples = new JPanel(new BorderLayout(8, 0));
        examples.setOpaque(false);
        examples.add(preset, BorderLayout.CENTER);
        examples.add(presetButton, BorderLayout.EAST);
        examples.setAlignmentX(Component.LEFT_ALIGNMENT);
        stopsHeader.add(examples);
        stops.add(stopsHeader, BorderLayout.NORTH);
        JScrollPane activityScroll = scroll(activityTable);
        activityScroll.setPreferredSize(new Dimension(360, 110));
        stops.add(activityScroll, BorderLayout.CENTER);
        JPanel controls = vertical();
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        actions.setOpaque(false);
        actions.add(addButton);
        actions.add(removeButton);
        actions.add(upButton);
        actions.add(downButton);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(actions);
        controls.add(Box.createVerticalStrut(7));
        controls.add(label("Next stop = travel between activities.", 11, Font.PLAIN, MUTED));
        controls.add(label("The last stop uses Return travel above.", 11, Font.PLAIN, MUTED));
        stops.add(controls, BorderLayout.SOUTH);
        editor.add(stops, BorderLayout.CENTER);
        calculateButton.setPreferredSize(new Dimension(0, 42));
        editor.add(calculateButton, BorderLayout.SOUTH);
        return editor;
    }

    private JPanel buildItinerary() {
        JPanel panel = card(new BorderLayout(0, 14), 18);
        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        JPanel words = vertical();
        words.add(label("Your itinerary", 23, Font.BOLD, INK));
        words.add(Box.createVerticalStrut(3));
        words.add(timelineSummary);
        heading.add(words, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 3));
        actions.setOpaque(false);
        actions.add(copyButton);
        actions.add(exportButton);
        heading.add(actions, BorderLayout.EAST);
        panel.add(heading, BorderLayout.NORTH);
        itineraryContent.setOpaque(false);
        itineraryContent.add(scroll(timelineTable), "timeline");
        JPanel empty = new JPanel(new java.awt.GridBagLayout());
        empty.setBackground(Color.WHITE);
        JLabel emptyText = label("<html><div style='text-align:center'>Your next move, made clear.<br><br>Adjust the details, then calculate your plan.</div></html>", 16, Font.PLAIN, MUTED);
        empty.add(emptyText);
        itineraryContent.add(empty, "empty");
        panel.add(itineraryContent, BorderLayout.CENTER);
        JPanel footer = vertical();
        homeSummary.setBorder(BorderFactory.createEmptyBorder(10, 11, 10, 11));
        homeSummary.setOpaque(true);
        homeSummary.setBackground(MINT);
        homeSummary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        footer.add(homeSummary);
        footer.add(Box.createVerticalStrut(9));
        footer.add(label("Estimates only · allow for traffic and local clock changes.", 11, Font.PLAIN, MUTED));
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JMenuBar buildMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Color.WHITE);
        menuBar.setBorder(BorderFactory.createEmptyBorder(1, 16, 1, 16));
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        file.add(menuItem("Open plan…", KeyEvent.VK_O, () -> chooseOpen()));
        file.add(menuItem("Save plan", KeyEvent.VK_S, () -> chooseSave(false)));
        JMenuItem saveAs = new JMenuItem("Save plan as…");
        saveAs.addActionListener(event -> chooseSave(true));
        file.add(saveAs);
        file.addSeparator();
        JMenuItem export = new JMenuItem("Export itinerary…");
        export.addActionListener(event -> chooseExport());
        file.add(export);
        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        JMenuItem guide = new JMenuItem("Quick guide & keyboard shortcuts");
        guide.addActionListener(event -> JOptionPane.showMessageDialog(this,
            "1. Set the required return date and time.\n2. Enter estimates and arrange your activities.\n"
            + "3. Calculate, then copy or export the itinerary.\n\n"
            + "Next stop is travel to the following activity. The last row\nuses Return travel instead. Reordering clears the new last leg.\n\n"
            + "Ctrl+Enter  Calculate     Ctrl+N  Add activity\nCtrl+S  Save     Ctrl+O  Open     Ctrl+Shift+C  Copy itinerary\n"
            + "F2  Edit cell     Esc  Cancel edit     Alt+Up / Down  Move activity\n\n"
            + "All times are local clock times. Travel is estimated, with no\nlive traffic or time-zone/daylight-saving adjustment.",
            "ExitPlan quick guide", JOptionPane.INFORMATION_MESSAGE));
        help.add(guide);
        menuBar.add(file);
        menuBar.add(help);
        return menuBar;
    }

    private void wireActions() {
        calculateButton.addActionListener(event -> calculate());
        addButton.addActionListener(event -> addActivity());
        removeButton.addActionListener(event -> removeActivity());
        upButton.addActionListener(event -> moveActivity(-1));
        downButton.addActionListener(event -> moveActivity(1));
        activityTable.getSelectionModel().addListSelectionListener(event -> updateButtons());
        copyButton.addActionListener(event -> copyItinerary());
        exportButton.addActionListener(event -> chooseExport());
        saveButton.addActionListener(event -> chooseSave(false));
        openButton.addActionListener(event -> chooseOpen());
        presetButton.addActionListener(event -> {
            if (unsavedChanges && !confirmDiscard()) return;
            if (activityTable.isEditing()) activityTable.getCellEditor().cancelCellEditing();
            applyData(example((String) preset.getSelectedItem()));
            currentFile = null;
            unsavedChanges = true;
            calculate();
            updateTitle();
            showStatus("Example loaded. Adjust the estimates to fit your outing.", TEAL);
        });
    }

    private void attachInputListeners() {
        listen(deadline);
        for (JSpinner spinner : List.of(outbound, inbound, parking, buffer)) {
            spinner.addChangeListener(event -> markEdited());
            listen(((JSpinner.DefaultEditor) spinner.getEditor()).getTextField());
        }
        activities.addTableModelListener(event -> {
            markEdited();
            updateButtons();
        });
    }

    private void listen(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { markEdited(); }
            @Override public void removeUpdate(DocumentEvent event) { markEdited(); }
            @Override public void changedUpdate(DocumentEvent event) { markEdited(); }
        });
    }

    private void markEdited() {
        if (updating) return;
        unsavedChanges = true;
        calculatedData = null;
        calculatedPlan = null;
        timeline.setRowCount(0);
        leaveTime.setText("—");
        leaveDate.setText("Calculate to update your departure");
        totalDuration.setText("—");
        countdown.setText("Needs calculation");
        countdown.setForeground(MUTED);
        countdownDetail.setText("Your inputs have changed");
        timelineSummary.setText("A fresh itinerary is one calculation away");
        homeSummary.setText("Calculate to see your home arrival and buffer.");
        itineraryCards.show(itineraryContent, "empty");
        copyButton.setEnabled(false);
        exportButton.setEnabled(false);
        showStatus("Plan changed. Calculate again to update the departure and itinerary.", MUTED);
        updateTitle();
    }

    private void calculate() {
        try {
            PlanData data = readInputs();
            if (data == null) return; // An invalid table edit remains focused for correction.
            displayCalculated(data);
        } catch (IllegalArgumentException problem) {
            markEdited();
            showStatus(problem.getMessage(), ERROR);
        }
    }

    private PlanData readInputs() {
        if (!finishEditing()) return null;
        LocalDateTime returnBy;
        try {
            returnBy = LocalDateTime.parse(deadline.getText().strip(), INPUT_DATE);
        } catch (java.time.DateTimeException invalid) {
            deadline.requestFocusInWindow();
            throw new IllegalArgumentException("Enter a real return date and time as YYYY-MM-DD HH:MM (24-hour time).");
        }
        return new PlanData(returnBy, readMinutes(outbound, "Outbound travel"), readMinutes(inbound, "Return travel"),
            readMinutes(parking, "Parking / walking"), readMinutes(buffer, "Safety buffer"), activities.stops());
    }

    private int readMinutes(JSpinner spinner, String label) {
        try {
            spinner.commitEdit();
            return ((Number) spinner.getValue()).intValue();
        } catch (ParseException invalid) {
            spinner.requestFocusInWindow();
            throw new IllegalArgumentException(label + ": enter whole minutes within the displayed range.");
        }
    }

    private void displayCalculated(PlanData data) {
        calculatedData = data;
        calculatedPlan = data.calculate();
        leaveTime.setText(calculatedPlan.leaveBy().format(TIME));
        leaveDate.setText(calculatedPlan.leaveBy().format(DAY));
        totalDuration.setText(ItineraryFormatter.duration(calculatedPlan.totalMinutes()));
        timeline.setRowCount(0);
        for (var step : calculatedPlan.steps()) {
            timeline.addRow(new Object[]{step, step.start(), step.end(), step.minutes()});
        }
        timelineSummary.setText(calculatedPlan.steps().size() + " steps · every minute accounted for");
        homeSummary.setText("Home by " + calculatedPlan.homeBy().format(TIME) + "  ·  " + data.buffer() + " min protected buffer");
        homeSummary.setToolTipText(calculatedPlan.homeBy().format(INPUT_DATE) + " before the safety buffer; deadline " + data.deadline().format(INPUT_DATE));
        itineraryCards.show(itineraryContent, "timeline");
        copyButton.setEnabled(true);
        exportButton.setEnabled(true);
        updateCountdown();
        if (calculatedPlan.leaveBy().isAfter(LocalDateTime.now())) {
            showStatus("Plan ready. Leave by the time above to keep your return deadline.", TEAL);
        } else {
            showStatus("Departure has arrived or passed. Adjust the deadline or shorten your outing.", ERROR);
        }
    }

    private void updateCountdown() {
        if (calculatedPlan == null) return;
        long seconds = Duration.between(LocalDateTime.now(), calculatedPlan.leaveBy()).getSeconds();
        if (seconds <= 0) {
            countdown.setForeground(ERROR);
            countdown.setText(seconds > -60 ? "Leave now" : "Departure passed");
            countdownDetail.setText(seconds > -60 ? "Your departure time is here" : "Adjust your plan before you go");
        } else {
            long minutes = (seconds + 59) / 60;
            countdown.setForeground(TEAL);
            String remaining = minutes >= 1440 ? (minutes / 1440) + "d " + ((minutes % 1440) / 60) + "h"
                : minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + "m";
            countdown.setText(remaining + " to go");
            countdownDetail.setText("Time until you need to leave");
        }
    }

    private boolean finishEditing() {
        return !activityTable.isEditing() || activityTable.getCellEditor().stopCellEditing();
    }

    private void addActivity() {
        if (!finishEditing()) return;
        try {
            activities.add();
            int row = activities.getRowCount() - 1;
            activityTable.setRowSelectionInterval(row, row);
            activityTable.scrollRectToVisible(activityTable.getCellRect(row, 0, true));
            activityTable.requestFocusInWindow();
        } catch (IllegalArgumentException problem) {
            showStatus(problem.getMessage(), ERROR);
        }
    }

    private void removeActivity() {
        if (!finishEditing()) return;
        int row = activityTable.getSelectedRow();
        if (row < 0) return;
        activities.remove(row);
        if (activities.getRowCount() > 0) {
            int next = Math.min(row, activities.getRowCount() - 1);
            activityTable.setRowSelectionInterval(next, next);
        }
        showStatus("Activity removed. Final onward travel is 0; review the remaining travel estimates.", MUTED);
        updateButtons();
    }

    private void moveActivity(int direction) {
        if (!finishEditing()) return;
        int row = activityTable.getSelectedRow();
        int destination = row + direction;
        if (row < 0 || destination < 0 || destination >= activities.getRowCount()) return;
        activities.move(row, destination);
        activityTable.setRowSelectionInterval(destination, destination);
        activityTable.scrollRectToVisible(activityTable.getCellRect(destination, 0, true));
        showStatus("Activity moved. Final onward travel is 0; review travel between the reordered stops.", MUTED);
    }

    private void updateButtons() {
        int row = activityTable.getSelectedRow();
        int count = activities.getRowCount();
        addButton.setEnabled(count < PlanCalculator.MAX_STOPS);
        removeButton.setEnabled(row >= 0);
        upButton.setEnabled(row > 0);
        downButton.setEnabled(row >= 0 && row < count - 1);
        activityCount.setText(count + (count == 1 ? " stop" : " stops"));
    }

    private PlanData example(String name) {
        LocalDateTime time = LocalDateTime.now().plusHours("Dinner + movie".equals(name) ? 5 : 3).truncatedTo(ChronoUnit.MINUTES);
        if ("Dinner + movie".equals(name)) {
            return new PlanData(time, 20, 20, 5, 15, List.of(
                new PlanCalculator.Stop("Dinner", 60, 15), new PlanCalculator.Stop("Movie", 120, 0)));
        }
        if ("Errands".equals(name)) {
            return new PlanData(time, 15, 15, 5, 15, List.of(
                new PlanCalculator.Stop("Grocery shop", 35, 10), new PlanCalculator.Stop("Pick up a parcel", 10, 5),
                new PlanCalculator.Stop("Coffee break", 20, 0)));
        }
        return new PlanData(time, 20, 20, 5, 15, List.of(new PlanCalculator.Stop("Dinner", 60, 0)));
    }

    private void applyData(PlanData data) {
        updating = true;
        try {
            deadline.setText(data.deadline().format(INPUT_DATE));
            outbound.setValue(data.outbound());
            inbound.setValue(data.inbound());
            parking.setValue(data.parkingEachWay());
            buffer.setValue(data.buffer());
            activities.replace(data.stops());
            activityTable.setRowSelectionInterval(0, 0);
        } finally {
            updating = false;
        }
        updateButtons();
    }

    void savePlan(Path file) throws IOException {
        PlanData data = readInputs();
        if (data == null) throw new IllegalArgumentException("Correct the highlighted activity before saving.");
        PlanFiles.save(file, data);
        currentFile = file.toAbsolutePath();
        unsavedChanges = false;
        displayCalculated(data);
        updateTitle();
        showStatus("Saved " + file.getFileName() + ".", TEAL);
    }

    void loadPlan(Path file) throws IOException {
        PlanData data = PlanFiles.load(file); // Validate completely before replacing the current workspace.
        if (activityTable.isEditing()) activityTable.getCellEditor().cancelCellEditing();
        applyData(data);
        currentFile = file.toAbsolutePath();
        unsavedChanges = false;
        displayCalculated(data);
        updateTitle();
        showStatus("Opened " + file.getFileName() + ".", TEAL);
    }

    private void chooseSave(boolean saveAs) {
        try {
            PlanData data = readInputs();
            if (data == null) return;
            Path target = currentFile;
            if (saveAs || target == null) {
                target = chooseFile("Save plan", "exitplan", "ExitPlan plans", true);
                if (target == null) return;
            }
            savePlan(target);
        } catch (IOException | IllegalArgumentException problem) {
            showStatus("Could not save. " + problem.getMessage(), ERROR);
        }
    }

    private void chooseOpen() {
        Path selected = chooseFile("Open plan", "exitplan", "ExitPlan plans", false);
        if (selected == null) return;
        try {
            PlanFiles.load(selected); // A malformed file must never trigger replacing a valid plan.
            if (confirmDiscard()) loadPlan(selected);
        } catch (IOException problem) {
            showStatus("Could not open. " + problem.getMessage(), ERROR);
        }
    }

    private void chooseExport() {
        if (calculatedData == null) {
            showStatus("Calculate the current plan before exporting.", ERROR);
            return;
        }
        Path selected = chooseFile("Export itinerary", "txt", "Text itineraries", true);
        if (selected == null) return;
        try {
            PlanFiles.exportItinerary(selected, calculatedData);
            showStatus("Exported " + selected.getFileName() + ".", TEAL);
        } catch (IOException problem) {
            showStatus("Could not export. " + problem.getMessage(), ERROR);
        }
    }

    private Path chooseFile(String title, String extension, String description, boolean saving) {
        JFileChooser chooser = new JFileChooser(currentFile == null ? null : currentFile.toFile().getParentFile());
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter(description + " (*." + extension + ")", extension));
        if (saving) chooser.setSelectedFile(new java.io.File("my-plan." + extension));
        int result = saving ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return null;
        Path selected = chooser.getSelectedFile().toPath();
        if (saving && !selected.getFileName().toString().toLowerCase(Locale.ROOT).endsWith("." + extension)) {
            selected = selected.resolveSibling(selected.getFileName() + "." + extension);
        }
        if (saving && Files.exists(selected) && JOptionPane.showConfirmDialog(this,
            "Replace " + selected.getFileName() + "?", "Replace file", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return null;
        return selected;
    }

    private void copyItinerary() {
        if (calculatedData == null) return;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(ItineraryFormatter.format(calculatedData)), null);
            showStatus("Itinerary copied. Paste it into a message, note, or calendar description.", TEAL);
        } catch (IllegalStateException | SecurityException unavailable) {
            showStatus("The clipboard is busy. Try again, or export a text itinerary.", ERROR);
        }
    }

    private boolean confirmDiscard() {
        if (!unsavedChanges) return true;
        int choice = JOptionPane.showConfirmDialog(this, "This plan has unsaved changes. Discard them?",
            "Unsaved plan", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private void updateTitle() {
        setTitle("ExitPlan — " + (currentFile == null ? "Untitled plan" : currentFile.getFileName())
            + (unsavedChanges ? " · unsaved changes" : ""));
    }

    private void showStatus(String message, Color color) {
        status.setText(message);
        status.setForeground(color);
        status.setToolTipText(message);
    }

    private void bindShortcuts() {
        bind(getRootPane(), "control ENTER", "calculate", () -> calculate());
        bind(getRootPane(), "control S", "save", () -> chooseSave(false));
        bind(getRootPane(), "control O", "open", () -> chooseOpen());
        bind(getRootPane(), "control N", "add", () -> addActivity());
        bind(getRootPane(), "control shift C", "copy", () -> copyItinerary());
        bind(activityTable, "alt UP", "up", () -> moveActivity(-1));
        bind(activityTable, "alt DOWN", "down", () -> moveActivity(1));
        bind(activityTable, "DELETE", "remove", () -> { if (!activityTable.isEditing()) removeActivity(); });
    }

    private static void bind(JComponent component, String shortcut, String key, Runnable action) {
        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(shortcut), key);
        component.getActionMap().put(key, new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        });
    }

    private JMenuItem menuItem(String text, int key, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.setAccelerator(KeyStroke.getKeyStroke(key, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(event -> action.run());
        return item;
    }

    @Override public void dispose() {
        if (clock != null) clock.stop();
        super.dispose();
    }

    private final class ValidatedCellEditor extends DefaultCellEditor {
        private static final long serialVersionUID = 1L;
        private int row;
        private int column;
        ValidatedCellEditor() {
            super(new JTextField());
            setClickCountToStart(2);
            JTextField field = (JTextField) getComponent();
            field.setFont(font(13, Font.PLAIN));
            listen(field);
        }
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
            this.row = row;
            this.column = column;
            updating = true;
            try {
                Component component = super.getTableCellEditorComponent(table, value, selected, row, column);
                ((JTextField) component).setBorder(BorderFactory.createLineBorder(TEAL, 2));
                return component;
            } finally {
                updating = false;
            }
        }
        @Override public boolean stopCellEditing() {
            try {
                activities.validateValue(getCellEditorValue(), row, column);
                return super.stopCellEditing();
            } catch (IllegalArgumentException problem) {
                ((JTextField) getComponent()).setBorder(BorderFactory.createLineBorder(ERROR, 2));
                showStatus(problem.getMessage(), ERROR);
                getComponent().requestFocusInWindow();
                return false;
            }
        }
    }

    private static final class ActivityNumberRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                                 boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            if (column == 2 && row == table.getRowCount() - 1) {
                setText("—");
                setToolTipText("No next stop. Use Return travel for the trip home.");
                if (!selected) setForeground(MUTED);
            } else {
                setToolTipText(null);
                if (!selected) setForeground(INK);
            }
            return this;
        }
    }

    private static final class TimelineRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                                 boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
            setBackground(row % 2 == 0 ? Color.WHITE : new Color(246, 249, 250));
            setForeground(INK);
            setHorizontalAlignment(column == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setToolTipText(null);
            if (value instanceof PlanCalculator.Step step) {
                String type = switch (step.type()) {
                    case ACTIVITY -> "ACTIVITY";
                    case TRAVEL -> "TRAVEL";
                    case PARKING -> "PARK / WALK";
                    case BUFFER -> "BUFFER";
                };
                setText("<html><b>" + escape(step.label()) + "</b><br><span style='color:#566a7b;font-size:9px'>" + type + "</span></html>");
                setToolTipText(step.label());
                if (step.type() == PlanCalculator.StepType.BUFFER) setBackground(MINT);
            } else if (value instanceof LocalDateTime date) {
                setText("<html><b>" + date.format(TIME) + "</b><br><span style='color:#566a7b;font-size:9px'>" + date.format(SHORT_DAY) + "</span></html>");
            } else {
                setText(value + " min");
            }
            return this;
        }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static JPanel allowance(String name, JSpinner spinner) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel label = label(name, 12, Font.PLAIN, INK);
        label.setLabelFor(spinner);
        panel.add(label, BorderLayout.NORTH);
        JPanel input = new JPanel(new BorderLayout(6, 0));
        input.setOpaque(false);
        spinner.setPreferredSize(new Dimension(130, 30));
        input.add(spinner, BorderLayout.CENTER);
        input.add(label("min", 11, Font.PLAIN, MUTED), BorderLayout.EAST);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    private static JSpinner minutesSpinner(int value, int maximum, String name) {
        JSpinner spinner = named(new JSpinner(new SpinnerNumberModel(value, 0, maximum, 1)), name);
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0"));
        JFormattedTextField field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        field.setFocusLostBehavior(JFormattedTextField.PERSIST);
        field.setFont(font(13, Font.PLAIN));
        spinner.setBorder(BorderFactory.createLineBorder(LINE));
        spinner.setToolTipText("0–" + maximum + " whole minutes");
        spinner.getAccessibleContext().setAccessibleName(name + ", 0 to " + maximum + " minutes");
        return spinner;
    }

    private static void styleTable(JTable table, int height) {
        table.setFont(font(13, Font.PLAIN));
        table.setRowHeight(height);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(LINE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(Color.WHITE);
        table.setForeground(INK);
        table.setSelectionBackground(MINT);
        table.setSelectionForeground(INK);
        table.getTableHeader().setFont(font(11, Font.BOLD));
        table.getTableHeader().setBackground(BACKGROUND);
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setPreferredSize(new Dimension(0, 30));
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
    }

    private static JScrollPane scroll(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createLineBorder(LINE));
        pane.getViewport().setBackground(Color.WHITE);
        return pane;
    }

    private static JButton button(String text, String name, int mnemonic, boolean primary) {
        JButton button = named(new JButton(text), name);
        button.setFont(font(12, Font.BOLD));
        button.setFocusPainted(true);
        button.setBackground(primary ? TEAL : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : INK);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(primary ? TEAL : LINE),
            BorderFactory.createEmptyBorder(8, 11, 8, 11)));
        if (mnemonic != 0) button.setMnemonic(mnemonic);
        return button;
    }

    private static JPanel card(java.awt.LayoutManager layout, int padding) {
        JPanel panel = new RoundedPanel(layout);
        panel.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return panel;
    }

    private static JPanel vertical() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static JLabel sectionTitle(String text) { return label(text, 11, Font.BOLD, TEAL); }
    private static Font font(int size, int weight) { return new Font(Font.SANS_SERIF, weight, size); }
    private static JLabel label(String text, int size, int weight, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, weight));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
    private static <T extends JComponent> T named(T component, String name) { component.setName(name); return component; }

    private static final class RoundedPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        RoundedPanel(java.awt.LayoutManager layout) { super(layout); setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D draw = (Graphics2D) graphics.create();
            draw.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            draw.setColor(Color.WHITE);
            draw.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            draw.setColor(LINE);
            draw.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            draw.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class ClockIcon implements javax.swing.Icon {
        @Override public int getIconWidth() { return 40; }
        @Override public int getIconHeight() { return 40; }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D draw = (Graphics2D) graphics.create();
            draw.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            draw.setColor(new Color(111, 216, 195));
            draw.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            draw.drawOval(x + 3, y + 3, 32, 32);
            draw.drawLine(x + 19, y + 10, x + 19, y + 20);
            draw.drawLine(x + 19, y + 20, x + 26, y + 24);
            draw.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
            new ExitPlan().setVisible(true);
        });
    }
}
