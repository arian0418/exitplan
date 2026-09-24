import java.time.LocalDateTime;
import java.util.List;

public class PlanTimelineTest {
    public static void main(String[] args) {
        var deadline = LocalDateTime.of(2026, 10, 1, 0, 30);
        var plan = PlanCalculator.calculate(deadline, 20, 20, 5, 10, List.of(
            new PlanCalculator.Stop("  Dinner  ", 60, 15), new PlanCalculator.Stop("Movie", 90, 0)));
        TestSupport.run("all travel, parking, activities and buffer appear separately", () -> {
            TestSupport.equal(List.of("Outbound travel", "Park & walk in", "Dinner", "Travel to Movie",
                "Movie", "Walk back & depart", "Return travel", "Safety buffer"),
                plan.steps().stream().map(PlanCalculator.Step::label).toList());
            TestSupport.equal(List.of(20, 5, 60, 15, 90, 5, 20, 10),
                plan.steps().stream().map(PlanCalculator.Step::minutes).toList());
        });
        TestSupport.run("timeline is continuous from departure to deadline", () -> {
            TestSupport.equal(LocalDateTime.of(2026, 9, 30, 20, 45), plan.steps().get(0).start());
            for (int i = 1; i < plan.steps().size(); i++) {
                TestSupport.equal(plan.steps().get(i - 1).end(), plan.steps().get(i).start());
            }
            TestSupport.equal(deadline, plan.steps().get(7).end());
            TestSupport.equal(225, plan.steps().stream().mapToInt(PlanCalculator.Step::minutes).sum());
        });
        TestSupport.run("home arrival excludes reserved safety buffer", () ->
            TestSupport.equal(LocalDateTime.of(2026, 10, 1, 0, 20), plan.homeBy()));
        TestSupport.run("calculated timeline is immutable", () ->
            TestSupport.rejects(UnsupportedOperationException.class, () -> plan.steps().clear()));
        TestSupport.run("zero allowances remain explicit with no duration", () -> {
            var small = PlanCalculator.calculate(deadline, 0, 0, 0, 0,
                List.of(new PlanCalculator.Stop("Visit", 1, 0)));
            TestSupport.equal(6, small.steps().size());
            TestSupport.equal(0, small.steps().get(0).minutes());
            TestSupport.equal(deadline, small.homeBy());
        });
        TestSupport.finish("PlanTimeline");
    }
}
