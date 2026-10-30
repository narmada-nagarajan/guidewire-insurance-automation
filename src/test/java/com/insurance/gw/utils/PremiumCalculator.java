package com.insurance.gw.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Independent "test oracle" for time-based premium rules.
 * We deliberately re-implement the business formula (instead of reading it from the app) so a defect
 * in the application's calculation is caught rather than echoed back.
 */
public final class PremiumCalculator {

    private static final BigDecimal SHORT_RATE_FACTOR = new BigDecimal("0.90");

    private PremiumCalculator() {
    }

    /** premium x (days remaining after the change date / total days in the policy period). */
    public static BigDecimal proRata(BigDecimal premium, LocalDate effective, LocalDate expiration, LocalDate changeDate) {
        return unrounded(premium, effective, expiration, changeDate).setScale(2, RoundingMode.HALF_UP);
    }

    /** Return premium for a cancellation: Flat = 100%, Pro Rata = unearned part, Short Rate = unearned less 10% penalty. */
    public static BigDecimal returnPremium(String method, BigDecimal premium, LocalDate effective, LocalDate expiration,
                                           LocalDate cancelDate) {
        switch (method) {
            case "Flat":
                return premium.setScale(2, RoundingMode.HALF_UP);
            case "Short Rate":
                return unrounded(premium, effective, expiration, cancelDate).multiply(SHORT_RATE_FACTOR)
                        .setScale(2, RoundingMode.HALF_UP);
            case "Pro Rata":
                return proRata(premium, effective, expiration, cancelDate);
            default:
                throw new IllegalArgumentException("Unknown cancellation method: " + method);
        }
    }

    private static BigDecimal unrounded(BigDecimal premium, LocalDate effective, LocalDate expiration, LocalDate date) {
        long remaining = ChronoUnit.DAYS.between(date, expiration);
        long total = ChronoUnit.DAYS.between(effective, expiration);
        return premium.multiply(BigDecimal.valueOf(remaining)).divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP);
    }
}
