import java.time.format.DateTimeFormatter;

/** One plain-text itinerary for both the clipboard and UTF-8 export. */
public final class ItineraryFormatter {
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    private ItineraryFormatter() { }

    public static String format(PlanData data) {
        var plan = data.calculate();
        StringBuilder text = new StringBuilder("EXITPLAN · YOUR ITINERARY\n\n");
        text.append("Leave by: ").append(plan.leaveBy().format(DATE_TIME)).append('\n');
        text.append("Home by: ").append(plan.homeBy().format(DATE_TIME)).append(" (before buffer)\n");
        text.append("Return deadline: ").append(data.deadline().format(DATE_TIME)).append('\n');
        text.append("Total outing: ").append(duration(plan.totalMinutes())).append("\n\n");
        for (var step : plan.steps()) {
            text.append(step.start().format(DATE_TIME)).append(" → ")
                .append(step.end().format(DATE_TIME)).append("  |  ")
                .append(step.label()).append("  |  ").append(step.minutes()).append(" min\n");
        }
        text.append("\nTravel times are estimates. Local clock times; check daylight-saving or time-zone changes.\n");
        return text.toString();
    }

    public static String duration(int minutes) {
        if (minutes < 60) return minutes + "m";
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
}
