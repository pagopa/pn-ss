package it.pagopa.pnss.configuration;

import it.pagopa.pnss.configurationproperties.TaskExecutorConfigurationProperties;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@CustomLog
class TaskExecutorConfigurationTest {
    TaskExecutorConfigurationProperties taskExecutorConfigurationProperties;
    ThreadPoolTaskExecutor defaultExecutor = new ThreadPoolTaskExecutor();


    @Test
    void taskExecutorWithProperties() {
        taskExecutorConfigurationProperties = new TaskExecutorConfigurationProperties(20, 10, 30);
        TaskExecutorConfiguration configuration = new TaskExecutorConfiguration(taskExecutorConfigurationProperties);
        ThreadPoolTaskExecutor executor = configuration.taskExecutor();

        assertEquals(10, executor.getCorePoolSize());
        assertEquals(20, executor.getMaxPoolSize());
        assertEquals(30, executor.getQueueCapacity());
    }

    @Test
    void taskExecutorWithDefaultProperties() {
        taskExecutorConfigurationProperties = new TaskExecutorConfigurationProperties(0, 0, 0);

        TaskExecutorConfiguration configuration = new TaskExecutorConfiguration(taskExecutorConfigurationProperties);
        ThreadPoolTaskExecutor executor = configuration.taskExecutor();

        assertEquals(defaultExecutor.getCorePoolSize(), executor.getCorePoolSize());
        assertEquals(defaultExecutor.getMaxPoolSize(), executor.getMaxPoolSize());
        assertEquals(defaultExecutor.getQueueCapacity(), executor.getQueueCapacity());
    }

    @Test
    void taskExecutorWithNullProperties() {
        taskExecutorConfigurationProperties = new TaskExecutorConfigurationProperties(null, null, null);

        TaskExecutorConfiguration configuration = new TaskExecutorConfiguration(taskExecutorConfigurationProperties);
        assertThrows(NullPointerException.class, configuration::taskExecutor);
    }
}