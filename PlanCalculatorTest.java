import java.time.LocalDateTime;
import java.util.List;

public class PlanCalculatorTest {
    public static void main(String[] args) {
        var stops = List.of(
            new PlanCalculator.Stop("Dinner", 60, 15),
            new PlanCalculator.Stop("Movie", 90, 0));
        var deadline = LocalDateTime.of(2026, 10, 1, 0, 30);
        var plan = PlanCalculator.calculate(deadline, 20, 20, 5, 10, stops);
        if (plan.totalMinutes() != 225) throw new AssertionError("total includes inter-stop travel");
        if (!plan.leaveBy().equals(LocalDateTime.of(2026, 9, 30, 20, 45))) throw new AssertionError("overnight leave-by");
        try {
            PlanCalculator.calculate(deadline, -1, 20, 5, 10, stops);
            throw new AssertionError("negative travel accepted");
        } catch (IllegalArgumentException expected) {}
        try {
            PlanCalculator.calculate(deadline, 20, 20, 5, 10,
                List.of(new PlanCalculator.Stop(" ", 30, 0)));
            throw new AssertionError("empty stop accepted");
        } catch (IllegalArgumentException expected) {}
        System.out.println("PlanCalculator tests passed");
    }
}
