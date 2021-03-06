package x.scratch;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static x.scratch.AsyncConfiguration.SLOW_EXECUTOR_BEAN_NAME;

@EnableAsync
@TestConfiguration
public class TestAsyncConfiguration {
    @Bean
    @Primary
    public TestingExecutor testingExecutor() {
        return new TestingExecutor("default");
    }

    @Bean(SLOW_EXECUTOR_BEAN_NAME)
    public TestingExecutor alsoTestingExecutor() {
        return new TestingExecutor(SLOW_EXECUTOR_BEAN_NAME);
    }

    @RequiredArgsConstructor
    @ToString
    public static class TestingExecutor
            implements Executor, TaskExecutor {
        private final BlockingQueue<TestingExecutor> executorsCalled
                = new ArrayBlockingQueue<>(10);
        private final BlockingQueue<Runnable> tasksRan
                = new ArrayBlockingQueue<>(10);

        @Getter
        private final String name;

        @Override
        public void execute(final Runnable command) {
            assertThat(executorsCalled.offer(this))
                    .as("Too many executors called without checking")
                    .isTrue();
            new Thread(() -> {
                command.run();
                assertThat(tasksRan.offer(command))
                        .as("Too many tasks ran without checking")
                        .isTrue();
            }).start();
        }

        public TestingExecutor awaitExecutorCalled(
                final long timeout, final TimeUnit unit)
                throws InterruptedException {
            final var executor = executorsCalled.poll(timeout, unit);
            assertThat(executor)
                    .as("No executor called in time")
                    .isNotNull();
            return executor;
        }

        public Runnable awaitTaskRan(
                final long timeout, final TimeUnit unit)
                throws InterruptedException {
            final var task = tasksRan.poll(timeout, unit);
            assertThat(task)
                    .as("No task ran in time")
                    .isNotNull();
            return task;
        }
    }
}
