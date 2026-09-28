import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

/** Real Swing integration tests. Linux CI runs these with xvfb-run. */
public class ExitPlanGuiTest {
    private static Path fixture;

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("exitplan-gui-");
        fixture = directory.resolve("fixture.exitplan");
        PlanFiles.save(fixture, new PlanData(LocalDateTime.of(2026, 10, 1, 0, 30), 20, 20, 5, 10,
            List.of(new PlanCalculator.Stop("Dinner", 60, 15), new PlanCalculator.Stop("Movie", 90, 0))));
        try {
            SwingUtilities.invokeAndWait(() -> {
                run("initial results and read-only complete timeline", app -> {
                    TestSupport.equal("8:45 PM", label(app, "leaveTime").getText());
                    JTable timeline = named(app, "timeline", JTable.class);
                    TestSupport.equal(8, timeline.getRowCount());
                    for (int row = 0; row < timeline.getRowCount(); row++) {
                        for (int col = 0; col < timeline.getColumnCount(); col++) {
                            TestSupport.check(!timeline.isCellEditable(row, col), "calculated timeline is editable");
                        }
                    }
                });
                run("typing a setting immediately clears stale results", app -> {
                    var spinner = named(app, "outbound", JSpinner.class);
                    ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setText("25");
                    TestSupport.equal(0, named(app, "timeline", JTable.class).getRowCount());
                    TestSupport.equal("—", label(app, "leaveTime").getText());
                    TestSupport.check(!button(app, "copy").isEnabled(), "stale copy remains enabled");
                    button(app, "calculate").doClick();
                    TestSupport.equal("8:40 PM", label(app, "leaveTime").getText());
                });
                run("typing an activity invalidates results before commit", app -> {
                    JTable activities = named(app, "activities", JTable.class);
                    activities.editCellAt(0, 1);
                    ((JTextField) activities.getEditorComponent()).setText("75");
                    TestSupport.equal(0, named(app, "timeline", JTable.class).getRowCount());
                    button(app, "calculate").doClick();
                    TestSupport.equal("8:30 PM", label(app, "leaveTime").getText());
                });
                run("invalid activity stays editable with a visible error", app -> {
                    JTable activities = named(app, "activities", JTable.class);
                    activities.editCellAt(0, 1);
                    ((JTextField) activities.getEditorComponent()).setText("-5");
                    button(app, "calculate").doClick();
                    TestSupport.check(activities.isEditing(), "invalid edit was silently discarded");
                    TestSupport.equal(60, activities.getValueAt(0, 1));
                    TestSupport.check(label(app, "status").getText().contains("1–1440"), "range error missing");
                    TestSupport.check(!button(app, "export").isEnabled(), "invalid export remains enabled");
                    activities.getCellEditor().cancelCellEditing();
                });
                run("invalid date and oversized settings cannot calculate", app -> {
                    JFormattedTextField deadline = named(app, "deadline", JFormattedTextField.class);
                    deadline.setText("2/30/2026 10:00 AM");
                    button(app, "calculate").doClick();
                    TestSupport.equal(0, named(app, "timeline", JTable.class).getRowCount());
                    deadline.setText("10/1/2026 12:30 AM");
                    JSpinner outbound = named(app, "outbound", JSpinner.class);
                    ((JSpinner.DefaultEditor) outbound.getEditor()).getTextField().setText("99999999999999");
                    button(app, "calculate").doClick();
                    TestSupport.equal(0, named(app, "timeline", JTable.class).getRowCount());
                });
                run("reorder and removal keep final stop rule", app -> {
                    JTable activities = named(app, "activities", JTable.class);
                    activities.setRowSelectionInterval(0, 0);
                    button(app, "moveDown").doClick();
                    TestSupport.equal("Dinner", activities.getValueAt(1, 0));
                    TestSupport.equal(0, activities.getValueAt(1, 2));
                    TestSupport.check(!activities.isCellEditable(1, 2), "last onward travel is editable");
                    button(app, "removeActivity").doClick();
                    TestSupport.equal(1, activities.getRowCount());
                    button(app, "addActivity").doClick();
                    TestSupport.equal(2, activities.getRowCount());
                });
                run("empty activity state can recover", app -> {
                    JTable activities = named(app, "activities", JTable.class);
                    activities.setRowSelectionInterval(0, 0);
                    button(app, "removeActivity").doClick();
                    button(app, "removeActivity").doClick();
                    button(app, "calculate").doClick();
                    TestSupport.equal(0, named(app, "timeline", JTable.class).getRowCount());
                    button(app, "addActivity").doClick();
                    button(app, "calculate").doClick();
                    TestSupport.check(button(app, "copy").isEnabled(), "valid new activity cannot calculate");
                });
                run("example preset creates useful editable stops", app -> {
                    named(app, "preset", JComboBox.class).setSelectedItem("Errands");
                    button(app, "applyPreset").doClick();
                    TestSupport.equal(3, named(app, "activities", JTable.class).getRowCount());
                    TestSupport.check(button(app, "copy").isEnabled(), "example was not calculated");
                });
                run("copy uses full current itinerary", app -> {
                    button(app, "copy").doClick();
                    String text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                    TestSupport.check(text.contains("9/30/2026 8:45 PM") && text.contains("Safety buffer"), "clipboard itinerary incomplete");
                });
                run("minimum-size layout keeps primary controls reachable", app -> {
                    app.setSize(app.getMinimumSize());
                    app.validate();
                    for (String name : List.of("calculate", "save", "open", "addActivity", "copy")) {
                        Component component = named(app, name, Component.class);
                        TestSupport.check(component.getWidth() > 25 && component.getHeight() > 15, name + " collapsed");
                        var bounds = SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), app.getContentPane());
                        TestSupport.check(bounds.x >= 0 && bounds.y >= 0
                            && bounds.x + bounds.width <= app.getContentPane().getWidth()
                            && bounds.y + bounds.height <= app.getContentPane().getHeight(), name + " is outside the viewport");
                    }
                    JTable timeline = named(app, "timeline", JTable.class);
                    JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, timeline);
                    TestSupport.check(viewport != null && viewport.getView() == timeline, "timeline cannot scroll");
                    TestSupport.check(viewport.getExtentSize().width > 25 && viewport.getExtentSize().height > 15,
                        "timeline viewport collapsed");
                    var visible = SwingUtilities.convertRectangle(viewport.getParent(), viewport.getBounds(), app.getContentPane());
                    TestSupport.check(visible.x >= 0 && visible.y >= 0
                        && visible.x + visible.width <= app.getContentPane().getWidth()
                        && visible.y + visible.height <= app.getContentPane().getHeight(),
                        "timeline viewport is outside the window");
                });
                run("save and load restore edited settings", app -> {
                    Path saved = directory.resolve("saved.exitplan");
                    named(app, "outbound", JSpinner.class).setValue(35);
                    app.savePlan(saved);
                    named(app, "outbound", JSpinner.class).setValue(50);
                    app.loadPlan(saved);
                    TestSupport.equal(35, named(app, "outbound", JSpinner.class).getValue());
                    TestSupport.equal("8:30 PM", label(app, "leaveTime").getText());
                });
                run("failed load preserves the existing plan", app -> {
                    Path malformed = directory.resolve("bad.exitplan");
                    Files.writeString(malformed, "not a plan");
                    TestSupport.rejects(java.io.IOException.class, () -> app.loadPlan(malformed));
                    TestSupport.equal("8:45 PM", label(app, "leaveTime").getText());
                });
            });
        } finally {
            try (var files = Files.list(directory)) {
                for (Path file : files.toList()) Files.deleteIfExists(file);
            }
            Files.deleteIfExists(directory);
        }
        TestSupport.finish("ExitPlanGui");
    }

    @FunctionalInterface private interface AppCheck { void run(ExitPlan app) throws Exception; }

    private static void run(String name, AppCheck check) {
        TestSupport.run(name, () -> {
            ExitPlan app = new ExitPlan();
            try {
                app.setVisible(true);
                app.loadPlan(fixture);
                check.run(app);
            } finally {
                app.dispose();
            }
        });
    }

    private static JButton button(Container root, String name) { return named(root, name, JButton.class); }
    private static JLabel label(Container root, String name) { return named(root, name, JLabel.class); }

    private static <T extends Component> T named(Container root, String name, Class<T> type) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName()) && type.isInstance(component)) return type.cast(component);
            if (component instanceof Container container) {
                T match = find(container, name, type);
                if (match != null) return match;
            }
        }
        throw new AssertionError("Missing component: " + name);
    }

    private static <T extends Component> T find(Container root, String name, Class<T> type) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName()) && type.isInstance(component)) return type.cast(component);
            if (component instanceof Container container) {
                T match = find(container, name, type);
                if (match != null) return match;
            }
        }
        return null;
    }
}
