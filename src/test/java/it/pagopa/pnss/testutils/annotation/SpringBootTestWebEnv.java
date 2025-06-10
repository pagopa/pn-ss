package it.pagopa.pnss.testutils.annotation;

import it.pagopa.pnss.localstack.LocalStackTestConfig;
import it.pagopa.pnss.utils.PnSsTestWatcher;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(LocalStackTestConfig.class)
@ExtendWith(PnSsTestWatcher.class)
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.properties")
public @interface SpringBootTestWebEnv {}
