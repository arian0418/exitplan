import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class PlanCalculator {
    private PlanCalculator() {}

    public record Stop(String name, int minutes, int travelToNext) {}
    public record Plan(LocalDateTime leaveBy, int totalMinutes) {}

    public static Plan calculate(LocalDateTime deadline, int outbound, int inbound,
                                 int parkingEachWay, int buffer, List<Stop> stops) {
        Objects.requireNonNull(deadline, "deadline");
        Objects.requireNonNull(stops, "stops");
        if (outbound < 0 || inbound < 0 || parkingEachWay < 0 || buffer < 0)
            throw new IllegalArgumentException("Travel, parking, and buffer must be nonnegative.");
        if (stops.isEmpty()) throw new IllegalArgumentException("Add at least one activity.");
        int total = Math.addExact(Math.addExact(outbound, inbound),
                                  Math.addExact(Math.multiplyExact(parkingEachWay, 2), buffer));
        for (int i = 0; i < stops.size(); i++) {
            Stop stop = Objects.requireNonNull(stops.get(i), "stop");
            if (stop.name() == null || stop.name().trim().isEmpty()
                || stop.minutes() <= 0 || stop.minutes() > 1440
                || stop.travelToNext() < 0 || stop.travelToNext() > 300)
                throw new IllegalArgumentException("Enter a name, 1–1440 activity minutes, and 0–300 travel minutes.");
            if (i == stops.size() - 1 && stop.travelToNext() != 0)
                throw new IllegalArgumentException("The final activity has no next stop; set its travel to 0.");
            total = Math.addExact(total, Math.addExact(stop.minutes(), stop.travelToNext()));
        }
        return new Plan(deadline.minusMinutes(total), total);
    }
}
