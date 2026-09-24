import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Pure local-calendar arithmetic, shared by the UI, file reader, and tests. */
public final class PlanCalculator {
    public static final int MAX_STOPS = 100;
    public static final int MAX_NAME_LENGTH = 80;
    public static final int MAX_ACTIVITY_MINUTES = 1440;
    public static final int MAX_TRAVEL_MINUTES = 1440;
    public static final int MAX_PARKING_MINUTES = 240;
    public static final int MAX_BUFFER_MINUTES = 1440;
    public static final LocalDateTime EARLIEST = LocalDateTime.of(1900, 1, 1, 0, 0);
    public static final LocalDateTime LATEST = LocalDateTime.of(9999, 12, 31, 23, 59);

    private PlanCalculator() { }

    public record Stop(String name, int minutes, int travelToNext) { }
    public enum StepType { TRAVEL, PARKING, ACTIVITY, BUFFER }
    public record Step(String label, StepType type, LocalDateTime start, LocalDateTime end, int minutes) { }
    public record Plan(LocalDateTime leaveBy, int totalMinutes, LocalDateTime homeBy, List<Step> steps) {
        public Plan { steps = List.copyOf(steps); }
    }

    public static Plan calculate(LocalDateTime deadline, int outbound, int inbound,
                                 int parkingEachWay, int buffer, List<Stop> stops) {
        if (deadline == null || deadline.isBefore(EARLIEST) || deadline.isAfter(LATEST)) {
            throw new IllegalArgumentException("Choose a return date between 1900 and 9999.");
        }
        checkMinutes("Outbound travel", outbound, 0, MAX_TRAVEL_MINUTES);
        checkMinutes("Return travel", inbound, 0, MAX_TRAVEL_MINUTES);
        checkMinutes("Parking / walking each way", parkingEachWay, 0, MAX_PARKING_MINUTES);
        checkMinutes("Safety buffer", buffer, 0, MAX_BUFFER_MINUTES);
        if (stops == null || stops.isEmpty() || stops.size() > MAX_STOPS) {
            throw new IllegalArgumentException("Add between 1 and " + MAX_STOPS + " activities.");
        }
        // All terms have small, validated upper bounds, so the sum cannot overflow.
        int total = outbound + inbound + parkingEachWay * 2 + buffer;
        for (int i = 0; i < stops.size(); i++) {
            Stop stop = stops.get(i);
            if (stop == null) throw new IllegalArgumentException("Activity " + (i + 1) + " is missing.");
            checkName(stop.name());
            checkMinutes("Activity " + (i + 1) + " duration", stop.minutes(), 1, MAX_ACTIVITY_MINUTES);
            checkMinutes("Activity " + (i + 1) + " onward travel", stop.travelToNext(), 0, MAX_TRAVEL_MINUTES);
            if (i == stops.size() - 1 && stop.travelToNext() != 0) {
                throw new IllegalArgumentException("The final activity has no next stop; set its onward travel to 0.");
            }
            total += stop.minutes() + stop.travelToNext();
        }
        LocalDateTime leaveBy = deadline.minusMinutes(total);
        if (leaveBy.isBefore(EARLIEST)) {
            throw new IllegalArgumentException("This plan would start before the supported calendar (1900).");
        }
        List<Step> steps = new ArrayList<>();
        LocalDateTime cursor = addStep(steps, "Outbound travel", StepType.TRAVEL, leaveBy, outbound);
        cursor = addStep(steps, "Park & walk in", StepType.PARKING, cursor, parkingEachWay);
        for (int i = 0; i < stops.size(); i++) {
            Stop stop = stops.get(i);
            cursor = addStep(steps, stop.name().strip(), StepType.ACTIVITY, cursor, stop.minutes());
            if (i < stops.size() - 1 && stop.travelToNext() > 0) {
                cursor = addStep(steps, "Travel to " + stops.get(i + 1).name().strip(),
                    StepType.TRAVEL, cursor, stop.travelToNext());
            }
        }
        cursor = addStep(steps, "Walk back & depart", StepType.PARKING, cursor, parkingEachWay);
        cursor = addStep(steps, "Return travel", StepType.TRAVEL, cursor, inbound);
        LocalDateTime homeBy = cursor;
        addStep(steps, "Safety buffer", StepType.BUFFER, cursor, buffer);
        return new Plan(leaveBy, total, homeBy, steps);
    }

    private static LocalDateTime addStep(List<Step> steps, String label, StepType type,
                                          LocalDateTime start, int minutes) {
        LocalDateTime end = start.plusMinutes(minutes);
        steps.add(new Step(label, type, start, end, minutes));
        return end;
    }

    public static void checkName(String name) {
        if (name == null || name.strip().isEmpty() || name.strip().length() > MAX_NAME_LENGTH
            || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Activity names need 1–" + MAX_NAME_LENGTH + " characters and no line breaks.");
        }
    }

    public static void checkMinutes(String label, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(label + " must be " + minimum + "–" + maximum + " whole minutes.");
        }
    }
}
