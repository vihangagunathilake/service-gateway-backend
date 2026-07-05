package com.flex.common_module;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static com.flex.common_module.constants.AppConstants.ASIA_COLOMBO_TIME_ZONE;

public class CommonMethods {
    public static Duration getDuration(LocalTime start, LocalTime end) {
        Duration duration = Duration.between(start, end);

        if (duration.isNegative()) {
            duration = duration.plusHours(24);
        }

        return duration;
    }

    public static String timeFormat(LocalTime time) {
        if (time == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        return time.format(formatter);
    }

    public static String timeFormat(String stringTime) {
        LocalTime time = LocalTime.parse(stringTime);
        return timeFormat(time);
    }

    public static LocalDate getCurrentDate() {
        return LocalDate.now(ZoneId.of(ASIA_COLOMBO_TIME_ZONE));
    }

    public static LocalTime getCurrentTime() {
        return LocalTime.now(ZoneId.of(ASIA_COLOMBO_TIME_ZONE));
    }

    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now(ZoneId.of(ASIA_COLOMBO_TIME_ZONE));
    }
}
