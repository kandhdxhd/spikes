package x.retryable

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import ch.tutteli.atrium.api.cc.en_GB.hasSize
import ch.tutteli.atrium.api.cc.en_GB.toThrow
import ch.tutteli.atrium.verbs.expect
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.retry.intercept.DefaultRetryInterceptor
import io.micronaut.test.annotation.MicronautTest
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory.getLogger
import javax.inject.Inject

@MicronautTest
internal class RetryingTest {
    @Inject
    lateinit var retrying: RetryingClient

    @Test
    fun `should retry 3 times`() {
        val retryingLogger = getLogger(DefaultRetryInterceptor::class.java)
                as ch.qos.logback.classic.Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        retryingLogger.addAppender(listAppender)

        expect {
            retrying.retryMe()
        }.toThrow<HttpClientException> { }

        expect(listAppender.list.filter {
            it.message.startsWith("Retrying")
        }).hasSize(3)
    }
}
