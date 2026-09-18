package ai.unifiedprocess.petclinic.core.ui;

/**
 * Raised only by {@link CrashView}, so the deliberate failure behind the
 * {@code /oups} route is distinguishable from a real one — in a stack trace,
 * in a log, and to a reader.
 */
public class DemonstrationException extends RuntimeException {

    public DemonstrationException(String message) {
        super(message);
    }
}
