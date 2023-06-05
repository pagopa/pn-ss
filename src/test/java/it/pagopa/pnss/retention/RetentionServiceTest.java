package it.pagopa.pnss.retention;

import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.common.retention.RetentionService;
import it.pagopa.pnss.common.retention.RetentionServiceImpl;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

import java.lang.annotation.Retention;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
public class RetentionServiceTest {

    @Autowired
    private RetentionServiceImpl retentionService;

    @ParameterizedTest
    @ValueSource(strings = {"10y", "5Y", "10d", "5D", "1Y 1d", "10y 5d", "1y 10D", "10Y 10D"})
    void getRetentionPeriodInDaysOk(String retentionPeriod) {
        Assertions.assertNotNull(retentionService.computeRetentionPeriodInDays(retentionPeriod));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "d"})
    void getRetentionPeriodInDaysKo(String retentionPeriod) {
        Assertions.assertThrows(RetentionException.class, () -> retentionService.computeRetentionPeriodInDays(retentionPeriod));
    }

}
