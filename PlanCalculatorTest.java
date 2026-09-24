import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** Run with java -cp build PlanCalculatorTest; no test framework required. */
public class PlanCalculatorTest {
    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 10, 1, 0, 30);
    private static final List<PlanCalculator.Stop> DINNER =
        List.of(new PlanCalculator.Stop("Dinner", 60, 0));

    public static void main(String[] args) {
        TestSupport.run("original overnight fixture includes between-stop travel", () -> {
            var plan = PlanCalculator.calculate(DEADLINE, 20, 20, 5, 10, List.of(
                new PlanCalculator.Stop("Dinner", 60, 15),
                new PlanCalculator.Stop("Movie", 90, 0)));
            TestSupport.equal(225, plan.totalMinutes());
            TestSupport.equal(LocalDateTime.of(2026, 9, 30, 20, 45), plan.leaveBy());
        });
        TestSupport.run("past departure is still a useful calculated plan", () -> {
            var plan = PlanCalculator.calculate(LocalDateTime.of(2000, 1, 1, 10, 0), 0, 0, 0, 0, DINNER);
            TestSupport.equal(LocalDateTime.of(2000, 1, 1, 9, 0), plan.leaveBy());
        });
        TestSupport.run("leap-day arithmetic is preserved", () -> {
            var plan = PlanCalculator.calculate(LocalDateTime.of(2028, 3, 1, 0, 30), 0, 0, 0, 0, DINNER);
            TestSupport.equal(LocalDateTime.of(2028, 2, 29, 23, 30), plan.leaveBy());
        });
        TestSupport.run("zero allowances do not add phantom time", () ->
            TestSupport.equal(60, PlanCalculator.calculate(DEADLINE, 0, 0, 0, 0, DINNER).totalMinutes()));
        TestSupport.run("upper limits remain usable", () -> {
            var plan = PlanCalculator.calculate(DEADLINE, 1440, 1440, 240, 1440,
                List.of(new PlanCalculator.Stop("A full day", 1440, 0)));
            TestSupport.equal(6240, plan.totalMinutes());
        });
        rejects("negative outbound", () -> calculate(-1, 20, 5, 10, DINNER));
        rejects("negative return travel", () -> calculate(20, -1, 5, 10, DINNER));
        rejects("negative parking", () -> calculate(20, 20, -1, 10, DINNER));
        rejects("negative buffer", () -> calculate(20, 20, 5, -1, DINNER));
        rejects("excessive outbound", () -> calculate(1441, 20, 5, 10, DINNER));
        rejects("excessive return travel", () -> calculate(20, 1441, 5, 10, DINNER));
        rejects("excessive parking", () -> calculate(20, 20, 241, 10, DINNER));
        rejects("excessive buffer", () -> calculate(20, 20, 5, 1441, DINNER));
        rejects("integer overflow input", () -> calculate(Integer.MAX_VALUE, Integer.MAX_VALUE, 5, 10, DINNER));
        rejects("no stops", () -> calculate(20, 20, 5, 10, List.of()));
        rejects("null deadline", () -> PlanCalculator.calculate(null, 20, 20, 5, 10, DINNER));
        rejects("null stop list", () -> calculate(20, 20, 5, 10, null));
        rejects("null stop entry", () -> calculate(20, 20, 5, 10, Arrays.asList((PlanCalculator.Stop) null)));
        rejectsStop("blank name", "  ", 60, 0);
        rejectsStop("null name", null, 60, 0);
        rejectsStop("name with line break", "Dinner\nMovie", 60, 0);
        rejectsStop("name too long", "x".repeat(81), 60, 0);
        rejectsStop("zero activity", "Dinner", 0, 0);
        rejectsStop("negative activity", "Dinner", -1, 0);
        rejectsStop("excessive activity", "Dinner", 1441, 0);
        rejectsStop("negative onward travel", "Dinner", 60, -1);
        rejectsStop("excessive onward travel", "Dinner", 60, 1441);
        rejectsStop("last stop cannot have onward travel", "Dinner", 60, 5);
        rejects("too many stops", () -> calculate(20, 20, 5, 10,
            java.util.Collections.nCopies(101, new PlanCalculator.Stop("Stop", 1, 0))));
        rejects("unsupported early date", () -> PlanCalculator.calculate(LocalDateTime.MIN, 0, 0, 0, 0, DINNER));
        rejects("unsupported late date", () -> PlanCalculator.calculate(LocalDateTime.MAX, 0, 0, 0, 0, DINNER));
        rejects("departure before supported calendar", () -> PlanCalculator.calculate(
            LocalDateTime.of(1900, 1, 1, 0, 0), 0, 0, 0, 0, DINNER));
        TestSupport.finish("PlanCalculator");
    }

    private static void calculate(int out, int back, int parking, int buffer, List<PlanCalculator.Stop> stops) {
        PlanCalculator.calculate(DEADLINE, out, back, parking, buffer, stops);
    }

    private static void rejectsStop(String label, String name, int minutes, int travel) {
        rejects(label, () -> calculate(20, 20, 5, 10, List.of(new PlanCalculator.Stop(name, minutes, travel))));
    }

    private static void rejects(String label, TestSupport.Check action) {
        TestSupport.run("rejects " + label, () -> TestSupport.rejects(IllegalArgumentException.class, action));
    }
}
