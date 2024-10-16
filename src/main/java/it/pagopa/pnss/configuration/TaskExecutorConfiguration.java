package it.pagopa.pnss.configuration;

import it.pagopa.pnss.configurationproperties.TaskExecutorConfigurationProperties;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * A configuration class for the Spring ThreadPoolTaskExecutor. It allows to configure threads related settings.
 */
@Configuration
@CustomLog
public class TaskExecutorConfiguration {

    private final TaskExecutorConfigurationProperties taskExecutorConfigurationProperties;

    /**
     * Instantiates a new Task executor configuration.
     *
     * @param taskExecutorConfigurationProperties the task executor configuration properties
     */
    public TaskExecutorConfiguration(TaskExecutorConfigurationProperties taskExecutorConfigurationProperties) {
        this.taskExecutorConfigurationProperties = taskExecutorConfigurationProperties;
    }

    /**
     * The taskExecutor bean.
     *
     * @return the task executor
     */
    @Bean
    ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        if (taskExecutorConfigurationProperties.coreSize() != 0) {
            taskExecutor.setCorePoolSize(taskExecutorConfigurationProperties.coreSize());
        }
        if (taskExecutorConfigurationProperties.maxSize() != 0) {
            taskExecutor.setMaxPoolSize(taskExecutorConfigurationProperties.maxSize());
        }
        if (taskExecutorConfigurationProperties.queueCapacity() != 0) {
            taskExecutor.setQueueCapacity(taskExecutorConfigurationProperties.queueCapacity());
        }
        taskExecutor.afterPropertiesSet();
        return taskExecutor;
    }

}
