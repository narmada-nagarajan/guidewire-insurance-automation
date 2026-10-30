package com.insurance.gw.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    /** Guidewire (US locale) default date format. */
    public static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private DateUtils() {
    }

    public static String format(LocalDate date) {
        return date.format(US_DATE);
    }

    public static LocalDate parse(String text) {
        return LocalDate.parse(text.trim(), US_DATE);
    }

    public static LocalDate today() {
        return LocalDate.now();
    }
}
