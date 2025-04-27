package common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <h1>Logger</h1>
 * A minimal, thread‑safe logging utility implemented with the
 * <em>Singleton</em> pattern.
 *
 * <p>The logger supports five severity levels—TRACE, DEBUG, INFO, WARN,
 * and ERROR—and prints entries to {@code System.out} in a human‑friendly
 * format:</p>
 *
 * <pre>{@code
 * Logger log = Logger.getInstance();
 * log.setMinLevel(Logger.Level.DEBUG);
 *
 * log.info("Application started");
 * log.debug("Loaded config file");
 * log.error("Unable to open socket", new IOException("Bind failed"));
 * }</pre>
 *
 * <p>This example will emit all messages at {@code DEBUG} or higher.
 * Lower‑severity calls such as {@code trace(...)} are suppressed.</p>
 *
 * <p><b>Thread‑safety:</b> The class is eagerly initialised; the JVM
 * guarantees the single instance is created before any thread can
 * access it. All mutable state is stored in {@code volatile} fields,
 * so visibility across threads is ensured without additional locking.</p>
 *
 * @author ChatGPT (OpenAI)
 * @since 1.0
 */
public final class Logger {

    /** Ordered list of severities from least to most important. */
    public enum Level {
        TRACE, DEBUG, INFO, WARN, ERROR;
    }

    /* ---------------------------------------------------------------------
     * Singleton wiring
     * ------------------------------------------------------------------- */

    /** The one and only instance (eager Singleton). */
    private static final Logger INSTANCE = new Logger();

    /** Timestamp format: 2025-04-26 21:04:16.123 */
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** Current threshold below which messages are discarded. */
    private volatile Level minLevel = Level.INFO;

    /** Private constructor prevents callers from instantiating. */
    private Logger() { }

    /**
     * Returns the global {@code Logger} instance.
     *
     * @return singleton instance
     */
    public static Logger getInstance() {
        return INSTANCE;
    }

    /* ---------------------------------------------------------------------
     * Configuration
     * ------------------------------------------------------------------- */

    /**
     * Sets the minimum level that will actually be printed.
     * Messages below this threshold are ignored.
     *
     * @param level desired minimum severity (non‑null)
     * @throws NullPointerException if {@code level} is {@code null}
     */
    public void setMinLevel(Level level) {
        if (level == null) {
            throw new NullPointerException("level");
        }
        this.minLevel = level;
    }

    /* ---------------------------------------------------------------------
     * Logging API – convenience wrappers
     * ------------------------------------------------------------------- */

    public void trace(String msg)                      { log(Level.TRACE, msg, null); }
    public void debug(String msg)                      { log(Level.DEBUG, msg, null); }
    public void info(String msg)                       { log(Level.INFO,  msg, null); }
    public void warn(String msg)                       { log(Level.WARN,  msg, null); }
    public void error(String msg)                      { log(Level.ERROR, msg, null); }

    public void trace(String msg, Throwable t)         { log(Level.TRACE, msg, t); }
    public void debug(String msg, Throwable t)         { log(Level.DEBUG, msg, t); }
    public void info(String msg, Throwable t)          { log(Level.INFO,  msg, t); }
    public void warn(String msg, Throwable t)          { log(Level.WARN,  msg, t); }
    public void error(String msg, Throwable t)         { log(Level.ERROR, msg, t); }

    /**
     * Core logging method with optional {@link Throwable}.
     *
     * @param level severity of the entry
     * @param msg   message text (non‑null)
     * @param t     optional exception to print after the message (may be null)
     */
    public void log(Level level, String msg, Throwable t) {
        // Fast exit if below current threshold.
        if (level.ordinal() < minLevel.ordinal()) {
            return;
        }

        // Build the log line.
        String ts     = LocalDateTime.now().format(TS_FMT);
        String thread = Thread.currentThread().getName();

        StringBuilder sb = new StringBuilder()
                .append(ts).append(" [")
                .append(thread).append("] ")
                .append(level).append(" - ")
                .append(msg);

        // Print to standard output.
        System.out.println(sb);

        // If an exception is supplied, print its stack trace.
        if (t != null) {
            t.printStackTrace(System.out);
        }
    }

    /* ---------------------------------------------------------------------
     * Self‑test / demonstration
     * ------------------------------------------------------------------- */

    /** Quick CLI demonstration: {@code java util.Logger} */
    public static void main(String[] args) {
        Logger log = Logger.getInstance();
        log.setMinLevel(Level.WARN);

        log.info("Application started");
        log.debug("Fetching data ...");
        log.trace("This will not appear because min level is DEBUG");
        log.warn("Low memory");
        log.error("Unexpected error", new RuntimeException("Boom"));
    }
}
