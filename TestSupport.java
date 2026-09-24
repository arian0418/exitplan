import java.util.Objects;

/** Tiny shared test runner so the repository needs only a JDK. */
final class TestSupport {
    @FunctionalInterface interface Check { void run() throws Exception; }
    private static int passed;
    private static int failed;

    private TestSupport() { }

    static void run(String name, Check test) {
        try {
            test.run();
            passed++;
        } catch (Throwable problem) {
            failed++;
            System.err.println("FAIL: " + name + " — " + problem);
        }
    }

    static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected <" + expected + "> but was <" + actual + ">");
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void rejects(Class<? extends Throwable> type, Check action) throws Exception {
        try {
            action.run();
        } catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError("Expected " + type.getSimpleName() + ", received " + problem, problem);
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " but input was accepted");
    }

    static void finish(String suite) {
        System.out.println(suite + ": " + passed + " passed, " + failed + " failed");
        if (failed > 0) throw new AssertionError(suite + " test failures");
    }
}
