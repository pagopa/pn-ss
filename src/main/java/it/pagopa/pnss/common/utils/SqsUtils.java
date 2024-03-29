package it.pagopa.pnss.common.utils;

import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;

@CustomLog
public class SqsUtils {

    private SqsUtils() {
        throw new IllegalStateException("SqsUtils is utility class");
    }

    public static <T> void logIncomingMessage(String queueName, T incomingPayload) {
        log.debug("Incoming message from '{}' queue with payload ↓\n{}", queueName, incomingPayload);
    }
}
