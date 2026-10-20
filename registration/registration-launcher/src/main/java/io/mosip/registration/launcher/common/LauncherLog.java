/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Minimal logging facade over {@link java.util.logging}, with <b>no third-party dependency</b>.
 * <p>
 * The launcher previously logged through slf4j. That is fatal here, because {@code _launcher.jar} is
 * required to run as the <i>sole</i> entry point with nothing else on the classpath — both after
 * {@code migration.exe} resets {@code lib/} to just this jar, and on the {@code <1.3.0 -> 1.3.0}
 * transition once {@code ClientPreLoader} has deleted every jar not listed in the new root manifest.
 * In that state {@code slf4j-api} is gone, and merely loading {@code Initialization} failed with
 * {@code NoClassDefFoundError: org/slf4j/LoggerFactory} — an unbootable install. This class exists so
 * the launcher depends on nothing outside the JDK.
 * <p>
 * The slf4j call style is kept deliberately ({@code "text {} more {}", a, b}, with an optional trailing
 * {@link Throwable}) so the change was confined to the logger declarations rather than ~80 call sites.
 */
public final class LauncherLog {

    /**
     * Parent of every launcher logger. Held in a static field on purpose: {@code java.util.logging}
     * keeps only weak references to loggers, so a parent that nobody references can be collected and
     * take its handlers with it, silently stopping all output.
     */
    private static final Logger ROOT = Logger.getLogger("io.mosip.registration");

    /** Written to the working directory, which is the application root — beside migration.log / rollback.log. */
    private static final String LOG_FILE = "launcher.log";

    private static final Formatter FORMATTER = new SingleLineFormatter();

    private static boolean fileLoggingEnabled;

    static {
        // Console only by default. The file handler is opt-in via enableFileLogging() so that merely
        // constructing a logger -- which every unit test does -- never creates launcher.log in the
        // module directory.
        ROOT.setUseParentHandlers(false);
        ROOT.setLevel(Level.INFO);
        ConsoleHandler console = new ConsoleHandler();
        console.setLevel(Level.INFO);
        console.setFormatter(FORMATTER);
        ROOT.addHandler(console);
    }

    private final Logger delegate;

    private LauncherLog(Logger delegate) {
        this.delegate = delegate;
    }

    public static LauncherLog get(Class<?> owner) {
        return new LauncherLog(Logger.getLogger(owner.getName()));
    }

    /**
     * Starts writing to {@value #LOG_FILE} in the working directory. Called once from
     * {@code Initialization.main}. Best effort: a read-only or full install directory must not stop the
     * launcher from running, so a failure here only warns to the console.
     */
    public static synchronized void enableFileLogging() {
        if (fileLoggingEnabled) {
            return;
        }
        try {
            FileHandler file = new FileHandler(LOG_FILE, 0, 1, true);
            // UTF-8 explicitly: the default is the platform encoding, which on a Windows console
            // codepage cannot represent the punctuation used in the operator-facing messages and
            // writes replacement characters into the log. The console handler is deliberately left
            // on the platform default, since that is what the terminal itself can render.
            file.setEncoding("UTF-8");
            file.setLevel(Level.INFO);
            file.setFormatter(FORMATTER);
            ROOT.addHandler(file);
            fileLoggingEnabled = true;
        } catch (IOException | SecurityException e) {
            ROOT.log(Level.WARNING, "Could not open " + LOG_FILE + " for writing; console only", e);
        }
    }

    public void info(String format, Object... args) {
        log(Level.INFO, format, args);
    }

    public void warn(String format, Object... args) {
        log(Level.WARNING, format, args);
    }

    public void error(String format, Object... args) {
        log(Level.SEVERE, format, args);
    }

    private void log(Level level, String format, Object[] args) {
        if (!delegate.isLoggable(level)) {
            return;
        }
        Throwable thrown = trailingThrowable(format, args);
        int consumable = (args == null) ? 0 : (thrown == null ? args.length : args.length - 1);
        String message = substitute(format, args, consumable);
        if (thrown == null) {
            delegate.log(level, message);
        } else {
            delegate.log(level, message, thrown);
        }
    }

    /**
     * Applies slf4j's rule: a trailing {@link Throwable} is the exception to log only when the format
     * string has no {@code {}} placeholder left for it. That keeps both
     * {@code error("failed", e)} and {@code error("failed to download {}", url, e)} behaving as before.
     */
    private static Throwable trailingThrowable(String format, Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        Object last = args[args.length - 1];
        if (!(last instanceof Throwable)) {
            return null;
        }
        return placeholderCount(format) < args.length ? (Throwable) last : null;
    }

    private static int placeholderCount(String format) {
        if (format == null) {
            return 0;
        }
        int count = 0;
        for (int at = format.indexOf("{}"); at >= 0; at = format.indexOf("{}", at + 2)) {
            count++;
        }
        return count;
    }

    private static String substitute(String format, Object[] args, int consumable) {
        if (format == null) {
            return "";
        }
        if (consumable <= 0) {
            return format;
        }
        StringBuilder out = new StringBuilder(format.length() + 32);
        int argIndex = 0;
        int from = 0;
        int at;
        while (argIndex < consumable && (at = format.indexOf("{}", from)) >= 0) {
            out.append(format, from, at).append(asText(args[argIndex++]));
            from = at + 2;
        }
        out.append(format, from, format.length());
        return out.toString();
    }

    /** A broken toString() in a logged value must never take down the upgrade. */
    private static String asText(Object value) {
        try {
            return String.valueOf(value);
        } catch (RuntimeException e) {
            return "<toString() failed: " + e + ">";
        }
    }

    /**
     * One line per record, matching the shape of the client's own registration.log so the two read
     * alike when diagnosing an upgrade.
     */
    private static final class SingleLineFormatter extends Formatter {

        private static final DateTimeFormatter TIMESTAMP =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS").withZone(ZoneId.systemDefault());

        @Override
        public String format(LogRecord record) {
            StringBuilder out = new StringBuilder(192);
            out.append(TIMESTAMP.format(Instant.ofEpochMilli(record.getMillis())))
                    .append(' ').append(levelName(record.getLevel()))
                    .append(" [").append(Thread.currentThread().getName()).append("] ")
                    .append(simpleName(record.getLoggerName()))
                    .append(" : ")
                    // getMessage(), not formatMessage(): the text is already substituted, and running it
                    // through MessageFormat would mangle any literal braces it happens to contain.
                    .append(record.getMessage())
                    .append(System.lineSeparator());
            Throwable thrown = record.getThrown();
            if (thrown != null) {
                StringWriter trace = new StringWriter();
                thrown.printStackTrace(new PrintWriter(trace));
                out.append(trace);
            }
            return out.toString();
        }

        private static String levelName(Level level) {
            if (Level.SEVERE.equals(level)) {
                return "ERROR";
            }
            if (Level.WARNING.equals(level)) {
                return "WARN";
            }
            return level.getName();
        }

        private static String simpleName(String loggerName) {
            if (loggerName == null) {
                return "launcher";
            }
            int lastDot = loggerName.lastIndexOf('.');
            return lastDot < 0 ? loggerName : loggerName.substring(lastDot + 1);
        }
    }
}
