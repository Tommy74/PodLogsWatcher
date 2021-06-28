package poc;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public enum PodColor {
    ANSI_RESET("\u001B[0m"),
    ANSI_BRIGHT_BLACK("\u001B[90m"),
    ANSI_RED1("\u001B[31m"),
    ANSI_GREEN1("\u001B[32m"),
    ANSI_YELLOW1("\u001B[33m"),
    ANSI_AZURE1("\u001B[34m"),
    ANSI_VIOLET1("\u001B[35m"),
    ANSI_WATER1("\u001B[36m"),
    ANSI_BRIGHT_RED("\u001B[91m"),
    ANSI_BRIGHT_GREEN("\u001B[92m"),
    ANSI_BRIGHT_YELLOW("\u001B[93m"),
    ANSI_BRIGHT_BLUE("\u001B[94m"),
    ANSI_BRIGHT_PURPLE("\u001B[95m"),
    ANSI_BRIGHT_CYAN("\u001B[96m"),
    ANSI_BRIGHT_WHITE("\u001B[97m");

    public final String value;
    private static final Lock lock = new ReentrantLock();
    private static int idx = 0;

    public static final PodColor[] COLORS = {
            ANSI_BRIGHT_GREEN, ANSI_BRIGHT_RED, ANSI_BRIGHT_YELLOW,
            ANSI_BRIGHT_BLUE, ANSI_BRIGHT_PURPLE, ANSI_BRIGHT_CYAN, ANSI_BRIGHT_WHITE,
            ANSI_RED1, ANSI_GREEN1, ANSI_YELLOW1, ANSI_AZURE1, ANSI_VIOLET1, ANSI_WATER1
    };

    private PodColor(String value) {
        this.value = value;
    }

    public static PodColor getNext() {
        lock.lock();
        try {
            if (++idx >= COLORS.length) {
                idx = 0;
            }
            return COLORS[idx];
        } finally {
            lock.unlock();
        }
    }
}
