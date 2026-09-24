import java.util.List;

public class ActivityTableModelTest {
    public static void main(String[] args) {
        TestSupport.run("valid edits normalize names and integer text", () -> {
            var model = sample();
            model.setValueAt("  Café  ", 0, 0);
            model.setValueAt(" 45 ", 0, 1);
            TestSupport.equal(new PlanCalculator.Stop("Café", 45, 15), model.stops().get(0));
        });
        TestSupport.run("invalid numeric edits do not replace the old value", () -> {
            var model = sample();
            for (Object value : List.of("text", "1.5", "-1", "0", "1441", "999999999999999")) {
                TestSupport.rejects(IllegalArgumentException.class, () -> model.setValueAt(value, 0, 1));
                TestSupport.equal(60, model.getValueAt(0, 1));
            }
        });
        TestSupport.run("blank and control-character names are rejected", () -> {
            var model = sample();
            TestSupport.rejects(IllegalArgumentException.class, () -> model.setValueAt(" ", 0, 0));
            TestSupport.rejects(IllegalArgumentException.class, () -> model.setValueAt("A\nB", 0, 0));
            TestSupport.equal("Dinner", model.getValueAt(0, 0));
        });
        TestSupport.run("final onward travel is read-only and rejects nonzero values", () -> {
            var model = sample();
            TestSupport.check(!model.isCellEditable(1, 2), "last travel cell should be read-only");
            TestSupport.check(model.isCellEditable(0, 2), "inter-stop travel should be editable");
            TestSupport.rejects(IllegalArgumentException.class, () -> model.setValueAt(5, 1, 2));
        });
        TestSupport.run("moving an activity keeps its duration and clears final travel", () -> {
            var model = sample();
            model.move(0, 1);
            TestSupport.equal(List.of(new PlanCalculator.Stop("Movie", 90, 0),
                new PlanCalculator.Stop("Dinner", 60, 0)), model.stops());
        });
        TestSupport.run("removing the last activity clears new final travel", () -> {
            var model = sample();
            model.remove(1);
            TestSupport.equal(List.of(new PlanCalculator.Stop("Dinner", 60, 0)), model.stops());
        });
        TestSupport.run("empty list can be rebuilt with add", () -> {
            var model = sample();
            model.remove(1);
            model.remove(0);
            TestSupport.equal(0, model.getRowCount());
            model.add();
            TestSupport.equal(1, model.getRowCount());
            TestSupport.equal(0, model.getValueAt(0, 2));
        });
        TestSupport.run("activity count stays bounded", () -> {
            var model = new ActivityTableModel();
            for (int i = 0; i < 100; i++) model.add();
            TestSupport.rejects(IllegalArgumentException.class, model::add);
            TestSupport.equal(100, model.getRowCount());
        });
        TestSupport.finish("ActivityTableModel");
    }

    private static ActivityTableModel sample() {
        var model = new ActivityTableModel();
        model.replace(List.of(new PlanCalculator.Stop("Dinner", 60, 15), new PlanCalculator.Stop("Movie", 90, 0)));
        return model;
    }
}
