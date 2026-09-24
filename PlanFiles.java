import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Properties;

/** Small, versioned Properties files; loading uses the same rules as calculating. */
public final class PlanFiles {
    private static final int MAX_FILE_BYTES = 256 * 1024;

    private PlanFiles() { }

    public static PlanData load(Path file) throws IOException {
        byte[] bytes;
        // A bounded read also protects against a file growing after a size check.
        try (InputStream stream = Files.newInputStream(file)) {
            bytes = stream.readNBytes(MAX_FILE_BYTES + 1);
        }
        if (bytes.length > MAX_FILE_BYTES) throw new IOException("Plan files must be smaller than 256 KiB.");
        Properties properties = new Properties();
        try {
            properties.load(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8));
            if (!"1".equals(required(properties, "version"))) {
                throw new IllegalArgumentException("This plan uses an unsupported file version.");
            }
            int count = number(properties, "stop.count");
            PlanCalculator.checkMinutes("Activity count", count, 1, PlanCalculator.MAX_STOPS);
            var stops = new ArrayList<PlanCalculator.Stop>();
            for (int i = 0; i < count; i++) {
                String prefix = "stop." + i + ".";
                stops.add(new PlanCalculator.Stop(required(properties, prefix + "name"),
                    number(properties, prefix + "minutes"), number(properties, prefix + "travel")));
            }
            return new PlanData(LocalDateTime.parse(required(properties, "deadline")),
                number(properties, "outbound"), number(properties, "inbound"),
                number(properties, "parking"), number(properties, "buffer"), stops);
        } catch (IllegalArgumentException | java.time.DateTimeException problem) {
            throw new IOException("This file is not a valid ExitPlan plan. " + problem.getMessage(), problem);
        }
    }

    public static void save(Path file, PlanData data) throws IOException {
        if (data == null) throw new IOException("Calculate a valid plan before saving.");
        Properties properties = new Properties();
        properties.setProperty("version", "1");
        properties.setProperty("deadline", data.deadline().toString());
        properties.setProperty("outbound", String.valueOf(data.outbound()));
        properties.setProperty("inbound", String.valueOf(data.inbound()));
        properties.setProperty("parking", String.valueOf(data.parkingEachWay()));
        properties.setProperty("buffer", String.valueOf(data.buffer()));
        properties.setProperty("stop.count", String.valueOf(data.stops().size()));
        for (int i = 0; i < data.stops().size(); i++) {
            var stop = data.stops().get(i);
            String prefix = "stop." + i + ".";
            properties.setProperty(prefix + "name", stop.name());
            properties.setProperty(prefix + "minutes", String.valueOf(stop.minutes()));
            properties.setProperty(prefix + "travel", String.valueOf(stop.travelToNext()));
        }
        StringWriter writer = new StringWriter();
        properties.store(writer, "ExitPlan version 1 — durations in minutes; local date and time");
        replaceFile(file, writer.toString());
    }

    public static void exportItinerary(Path file, PlanData data) throws IOException {
        replaceFile(file, ItineraryFormatter.format(data));
    }

    private static void replaceFile(Path file, String contents) throws IOException {
        Path target = file.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), ".exitplan-", ".tmp");
        try {
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) throw new IllegalArgumentException("Missing field: " + key + ".");
        return value;
    }

    private static int number(Properties properties, String key) {
        try {
            return Integer.parseInt(required(properties, key).strip());
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("Field " + key + " must be a whole number.", invalid);
        }
    }
}
