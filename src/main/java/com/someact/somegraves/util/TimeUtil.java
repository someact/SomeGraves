package com.someact.somegraves.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing and formatting time durations.
 */
public final class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)\\s*([a-zA-Z]+)");

    private TimeUtil() {}

    public static long parseDurationSeconds(String input) throws IllegalArgumentException {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Time input cannot be empty.");
        }

        String clean = input.trim().toLowerCase();

        if (clean.equals("0") || clean.equals("infinite") || clean.equals("never") || clean.equals("none")) {
            return 0L;
        }

        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException ignored) {}

        Matcher matcher = TIME_PATTERN.matcher(clean);
        long totalSeconds = 0;
        boolean found = false;

        while (matcher.find()) {
            found = true;
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "s", "sec", "secs", "second", "seconds" -> totalSeconds += amount;
                case "m", "min", "mins", "minute", "minutes" -> totalSeconds += amount * 60;
                case "h", "hr", "hrs", "hour", "hours" -> totalSeconds += amount * 3600;
                case "d", "day", "days" -> totalSeconds += amount * 86400;
                case "w", "week", "weeks" -> totalSeconds += amount * 604800;
                default -> throw new IllegalArgumentException("Unknown time unit: '" + unit + "'");
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Could not parse time format: '" + input + "'");
        }

        return totalSeconds;
    }

    public static String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "Infinite";
        }

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(secs).append("s");

        return sb.toString().trim();
    }
}
