import java.time.LocalDateTime;
import java.util.List;

/** A validated snapshot: file operations never depend on partially edited cells. */
public record PlanData(LocalDateTime deadline, int outbound, int inbound,
                       int parkingEachWay, int buffer, List<PlanCalculator.Stop> stops) {
    public PlanData {
        PlanCalculator.calculate(deadline, outbound, inbound, parkingEachWay, buffer, stops);
        stops = stops.stream().map(stop -> new PlanCalculator.Stop(
            stop.name().strip(), stop.minutes(), stop.travelToNext())).toList();
    }

    public PlanCalculator.Plan calculate() {
        return PlanCalculator.calculate(deadline, outbound, inbound, parkingEachWay, buffer, stops);
    }
}
