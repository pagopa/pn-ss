package it.pagopa.pnss.testutils.annotation;

import it.pagopa.pnss.localstack.LocalStackTestConfig;
import it.pagopa.pnss.utils.PnSsTestWatcher;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(LocalStackTestConfig.class)
@ExtendWith(PnSsTestWatcher.class)
public @interface SpringBootTestWebEnv {}
