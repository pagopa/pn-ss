package it.pagopa.pnss.common.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Component
public class DocumentDateFormatter {

    private static final String PATTERN_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneOffset.UTC);
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

    private final ZoneId referenceTimeZone;

    public DocumentDateFormatter(@Value("${pn.ss.default.time-zone}") String referenceTimeZone) {
        this.referenceTimeZone = ZoneId.of(referenceTimeZone);
    }

    public LocalDate today() {
        return LocalDate.now(referenceTimeZone);
    }

    public LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(referenceTimeZone).toLocalDate();
    }

    public Instant endOfDay(Date date) {
        return toLocalDate(date).atTime(END_OF_DAY).atZone(referenceTimeZone).toInstant();
    }

    public String format(Date date) {
        return FORMATTER.format(date.toInstant());
    }

    public String format(Instant instant) {
        return FORMATTER.format(instant);
    }

    public Instant parse(String value) {
        return Instant.from(FORMATTER.parse(value));
    }

}
