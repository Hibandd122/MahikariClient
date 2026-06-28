/*
 * Decompiled with CFR 0.152.
 */
package mahikariui.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.concurrent.TimeUnit;

public class TimeUtils {
    public static final String DEFAULT_ZONE = "UTC";

    public static TimeUnit getTimeUnitFrom(String name) {
        return TimeUnit.valueOf(name.toUpperCase());
    }

    public static DateTimeFormatter getFormatter(String pattern, String timeZone) {
        return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.of(timeZone));
    }

    public static <T> T toInstance(String dateString, String pattern, String timeZone, TemporalQuery<T> query) {
        return TimeUtils.getFormatter(pattern, timeZone).parse((CharSequence)dateString, query);
    }

    public static Instant toInstant(String dateString, String pattern, String timeZone) {
        return TimeUtils.toInstance(dateString, pattern, timeZone, Instant::from);
    }

    public static String toString(TemporalAccessor date, String pattern, String timeZone) {
        if (date == null) {
            return "";
        }
        return TimeUtils.getFormatter(pattern, timeZone).format(date);
    }

    public static String toString(TemporalAccessor date, String timeZone) {
        return TimeUtils.toString(date, timeZone, DEFAULT_ZONE);
    }

    public static Instant getCurrentTime() {
        return Instant.now();
    }

    public static Duration getDuration(Temporal start, Temporal end) {
        return Duration.between(start, end);
    }

    public static String epochSecondToString(long epochSecond, String pattern, String timeZone) {
        return TimeUtils.toString(TimeUtils.fromEpochSecond(epochSecond), pattern, timeZone);
    }

    public static String epochSecondToString(long epochSecond, String pattern) {
        return TimeUtils.epochSecondToString(epochSecond, pattern, DEFAULT_ZONE);
    }

    public static long stringToEpochSecond(String dateString, String pattern, String timeZone) {
        return TimeUtils.toEpochSecond(TimeUtils.toInstant(dateString, pattern, timeZone));
    }

    public static String epochMilliToString(long epochMilli, String pattern, String timeZone) {
        return TimeUtils.toString(TimeUtils.fromEpochMilli(epochMilli), pattern, timeZone);
    }

    public static long stringToEpochMilli(String dateString, String pattern, String timeZone) {
        return TimeUtils.toEpochMilli(TimeUtils.toInstant(dateString, pattern, timeZone));
    }

    public static long getElapsedNanos() {
        return System.nanoTime();
    }

    public static long getElapsedMillis() {
        return TimeUtils.getElapsedNanos() / 1000000L;
    }

    public static long getElapsedSeconds() {
        return TimeUtils.getElapsedMillis() / 1000L;
    }

    public static Instant fromEpochSecond(long epochSecond) {
        return Instant.ofEpochSecond(epochSecond);
    }

    public static Instant fromEpochMilli(long epochMilli) {
        return Instant.ofEpochMilli(epochMilli);
    }

    public static long toEpochSecond(Instant data) {
        return data != null ? data.getEpochSecond() : 0L;
    }

    public static long toEpochMilli(Instant data) {
        return data != null ? data.toEpochMilli() : 0L;
    }

    public static long toEpochMilli() {
        return TimeUtils.toEpochMilli(TimeUtils.getCurrentTime());
    }

    public static String convertTime(String dateString, String fromFormat, String fromTimeZone, String toFormat, String toTimeZone) {
        return TimeUtils.toString(TimeUtils.toInstant(dateString, fromFormat, fromTimeZone), toFormat, toTimeZone);
    }

    public static String convertFormat(String dateString, String fromFormat, String toFormat) {
        return TimeUtils.convertTime(dateString, fromFormat, DEFAULT_ZONE, toFormat, DEFAULT_ZONE);
    }

    public static String convertZone(String dateString, String fromFormat, String fromTimeZone, String toTimeZone) {
        return TimeUtils.convertTime(dateString, fromFormat, fromTimeZone, fromFormat, toTimeZone);
    }
}

