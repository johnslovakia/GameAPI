package cz.johnslovakia.gameapi.modules.stats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;

public enum StatPeriod {
    LIFETIME, DAILY, WEEKLY, MONTHLY;

    public String getCurrentPeriodKey() {
        return switch (this) {
            case LIFETIME -> "lifetime";
            case DAILY -> LocalDate.now().toString();
            case WEEKLY -> {
                LocalDate now = LocalDate.now();
                int week = now.get(WeekFields.ISO.weekOfWeekBasedYear());
                yield now.getYear() + "-W" + String.format("%02d", week);
            }
            case MONTHLY -> {
                LocalDate now = LocalDate.now();
                yield now.getYear() + "-" + String.format("%02d", now.getMonthValue());
            }
        };
    }

    public long getMillisUntilReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = switch (this) {
            case LIFETIME -> null;
            case DAILY -> now.toLocalDate().plusDays(1).atStartOfDay();
            case WEEKLY -> {
                LocalDate date = now.toLocalDate();
                int daysUntilMonday = (8 - date.getDayOfWeek().getValue()) % 7;
                yield date.plusDays(daysUntilMonday == 0 ? 7 : daysUntilMonday).atStartOfDay();
            }
            case MONTHLY -> now.toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay();
        };
        return next == null ? -1L : ChronoUnit.MILLIS.between(now, next);
    }

    public String getTranslationKey() {
        return "period." + name().toLowerCase();
    }

    public StatPeriod next() {
        StatPeriod[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}