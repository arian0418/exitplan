import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PlanFilesTest {
    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 10, 1, 0, 30);
    private static final String VALID = "version=1\ndeadline=2026-10-01T00:30\noutbound=20\ninbound=20\nparking=5\nbuffer=10\nstop.count=1\nstop.0.name=Dinner\nstop.0.minutes=60\nstop.0.travel=0\n";

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("exitplan-test-");
        try {
            Path file = directory.resolve("evening.exitplan");
            var stops = new ArrayList<>(List.of(new PlanCalculator.Stop("  Café / 映画  ", 60, 15),
                new PlanCalculator.Stop("Movie", 90, 0)));
            var data = new PlanData(DEADLINE, 20, 20, 5, 10, stops);
            TestSupport.run("plan keeps a normalized immutable input snapshot", () -> {
                stops.clear();
                TestSupport.equal("Café / 映画", data.stops().get(0).name());
                TestSupport.equal(2, data.stops().size());
                TestSupport.rejects(UnsupportedOperationException.class, () -> data.stops().clear());
            });
            TestSupport.run("Unicode and complete plan survive save/load", () -> {
                PlanFiles.save(file, data);
                TestSupport.equal(data, PlanFiles.load(file));
                TestSupport.equal(225, PlanFiles.load(file).calculate().totalMinutes());
            });
            TestSupport.run("saving replaces a previous complete file", () -> {
                var replacement = new PlanData(DEADLINE, 0, 0, 0, 0,
                    List.of(new PlanCalculator.Stop("Walk", 15, 0)));
                PlanFiles.save(file, replacement);
                TestSupport.equal(replacement, PlanFiles.load(file));
            });
            TestSupport.run("itinerary exports every step with overnight dates", () -> {
                String text = ItineraryFormatter.format(data);
                TestSupport.check(text.contains("2026-09-30 20:45"), "departure missing");
                TestSupport.check(text.contains("2026-10-01 00:20"), "home arrival missing");
                TestSupport.check(text.contains("2026-10-01 00:30"), "deadline missing");
                TestSupport.check(text.contains("Park & walk in") && text.contains("Walk back & depart"), "parking missing");
                TestSupport.check(text.contains("Travel to Movie") && text.contains("Safety buffer"), "travel or buffer missing");
                TestSupport.check(text.contains("Café / 映画") && text.contains("3h 45m"), "name or duration missing");
                Path export = directory.resolve("itinerary.txt");
                PlanFiles.exportItinerary(export, data);
                TestSupport.equal(text, Files.readString(export, StandardCharsets.UTF_8));
            });
            malformed(directory, "missing key", VALID.replace("outbound=20\n", ""));
            malformed(directory, "missing version", VALID.replace("version=1\n", ""));
            malformed(directory, "future schema", VALID.replace("version=1", "version=2"));
            malformed(directory, "bad number", VALID.replace("outbound=20", "outbound=hello"));
            malformed(directory, "decimal number", VALID.replace("outbound=20", "outbound=20.5"));
            malformed(directory, "integer overflow", VALID.replace("outbound=20", "outbound=999999999999999"));
            malformed(directory, "negative allowance", VALID.replace("parking=5", "parking=-1"));
            malformed(directory, "impossible date", VALID.replace("2026-10-01T00:30", "2026-02-30T00:30"));
            malformed(directory, "excessive stop count", VALID.replace("stop.count=1", "stop.count=2147483647"));
            malformed(directory, "empty stop count", VALID.replace("stop.count=1", "stop.count=0"));
            malformed(directory, "last stop onward travel", VALID.replace("stop.0.travel=0", "stop.0.travel=8"));
            malformed(directory, "malformed Unicode escape", VALID + "extra=\\uNOTHEX\n");
            malformed(directory, "oversized file", "x".repeat(262145));
            TestSupport.run("missing file is reported as IO failure", () ->
                TestSupport.rejects(IOException.class, () -> PlanFiles.load(directory.resolve("missing.exitplan"))));
        } finally {
            try (var files = Files.list(directory)) {
                for (Path file : files.toList()) Files.deleteIfExists(file);
            }
            Files.deleteIfExists(directory);
        }
        TestSupport.finish("PlanFiles");
    }

    private static void malformed(Path directory, String label, String content) {
        TestSupport.run("rejects " + label, () -> {
            Path file = directory.resolve("bad.exitplan");
            Files.writeString(file, content, StandardCharsets.UTF_8);
            TestSupport.rejects(IOException.class, () -> PlanFiles.load(file));
        });
    }
}
