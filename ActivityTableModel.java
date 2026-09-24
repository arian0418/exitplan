import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** Owns editable stops and keeps the last-stop travel rule true after reordering. */
public final class ActivityTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private final List<PlanCalculator.Stop> rows = new ArrayList<>();
    private static final String[] COLUMNS = {"Activity", "Stay (min)", "Next stop (min)"};

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int column) { return COLUMNS[column]; }
    @Override public Class<?> getColumnClass(int column) { return column == 0 ? String.class : Integer.class; }
    @Override public boolean isCellEditable(int row, int column) { return column != 2 || row < rows.size() - 1; }

    @Override public Object getValueAt(int row, int column) {
        PlanCalculator.Stop stop = rows.get(row);
        return switch (column) {
            case 0 -> stop.name();
            case 1 -> stop.minutes();
            default -> stop.travelToNext();
        };
    }

    /** Used by the cell editor before committing, so invalid edits remain editable. */
    public Object validateValue(Object value, int row, int column) {
        if (column == 0) {
            String name = value == null ? null : value.toString().strip();
            PlanCalculator.checkName(name);
            return name;
        }
        int number;
        try {
            number = Integer.parseInt(value == null ? "" : value.toString().strip());
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("Enter a whole number of minutes.");
        }
        int minimum = column == 1 ? 1 : 0;
        int maximum = column == 1 ? PlanCalculator.MAX_ACTIVITY_MINUTES : PlanCalculator.MAX_TRAVEL_MINUTES;
        PlanCalculator.checkMinutes(column == 1 ? "Activity duration" : "Travel to next stop", number, minimum, maximum);
        if (column == 2 && row == rows.size() - 1 && number != 0) {
            throw new IllegalArgumentException("The last stop uses Return travel, so onward travel stays 0.");
        }
        return number;
    }

    @Override public void setValueAt(Object value, int row, int column) {
        Object validated = validateValue(value, row, column);
        PlanCalculator.Stop previous = rows.get(row);
        rows.set(row, new PlanCalculator.Stop(column == 0 ? (String) validated : previous.name(),
            column == 1 ? (Integer) validated : previous.minutes(),
            column == 2 ? (Integer) validated : previous.travelToNext()));
        fireTableCellUpdated(row, column);
    }

    public List<PlanCalculator.Stop> stops() { return List.copyOf(rows); }

    public void replace(List<PlanCalculator.Stop> stops) {
        rows.clear();
        rows.addAll(stops);
        clearFinalTravel();
        fireTableDataChanged();
    }

    public void add() {
        if (rows.size() >= PlanCalculator.MAX_STOPS) {
            throw new IllegalArgumentException("A plan can contain up to " + PlanCalculator.MAX_STOPS + " activities.");
        }
        rows.add(new PlanCalculator.Stop("New activity", 30, 0));
        fireTableDataChanged();
    }

    public void remove(int row) {
        rows.remove(row);
        clearFinalTravel();
        fireTableDataChanged();
    }

    public void move(int row, int destination) {
        if (row < 0 || destination < 0 || row >= rows.size() || destination >= rows.size()) return;
        PlanCalculator.Stop moved = rows.remove(row);
        rows.add(destination, moved);
        clearFinalTravel();
        fireTableDataChanged();
    }

    private void clearFinalTravel() {
        if (rows.isEmpty()) return;
        int last = rows.size() - 1;
        PlanCalculator.Stop stop = rows.get(last);
        rows.set(last, new PlanCalculator.Stop(stop.name(), stop.minutes(), 0));
    }
}
