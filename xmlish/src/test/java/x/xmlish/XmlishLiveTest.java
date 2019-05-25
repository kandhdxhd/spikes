package x.xmlish;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import x.xmlish.Xmlish.Inner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;

import static java.lang.String.format;
import static java.lang.System.out;
import static java.net.http.HttpResponse.BodyHandlers.discarding;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.util.StreamUtils.copyToString;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestInstance(PER_CLASS)
class XmlishLiveTest {
    private static final ResourceLoader resourceLoader
            = new DefaultResourceLoader();
    private static final HttpClient client = HttpClient.newBuilder().build();

    private final ObjectMapper objectMapper;
    private final Validator validator;

    @LocalServerPort
    private int port;

    private static InputStream loadXml(final String name)
            throws IOException {
        return resourceLoader
                .getResource("xml/" + name + ".xml")
                .getInputStream();
    }

    @Test
    void shouldGet()
            throws IOException, InterruptedException {
        final var request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(format("http://localhost:%d", port)))
                .build();

        final var response = client.send(
                request, BodyHandlers.ofString(UTF_8));

        assertThat(response.statusCode()).isEqualTo(200);

        out.println(response.body());
    }

    @Test
    void shouldPost()
            throws IOException, InterruptedException {
        final var request = HttpRequest.newBuilder()
                .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(
                        Xmlish.builder()
                                .foo("HI, MOM!")
                                .barNone(22)
                                .when(Instant.now())
                                .inner(Inner.builder()
                                        .qux("BYE, DAD!")
                                        .quux(77)
                                        .ever(Instant.now().minus(
                                                1_000_000L, SECONDS))
                                        .build())
                                .build())))
                .uri(URI.create(format("http://localhost:%d", port)))
                .header("Content-Type", "application/xml")
                .build();

        final var response = client.send(request, discarding());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Disabled("TODO: Does not parse here, but parses elsewhere")
    @Test
    void shouldPostGoodComplex()
            throws IOException, InterruptedException {

        final var request = HttpRequest.newBuilder()
                .POST(BodyPublishers.ofString(
                        copyToString(loadXml(
                                "good-complex-example"), UTF_8)))
                .uri(URI.create(format("http://localhost:%d/complex", port)))
                .header("Content-Type", "application/xml")
                .build();

        final var response = client.send(request, discarding());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void shouldParseGoodComplexExample()
            throws IOException {
        final var complexExample = objectMapper.readValue(
                loadXml("good-complex-example"),
                ComplexExample.class);

        out.println(complexExample);
        complexExample.getBody().getBookreview().getTable().getTr()
                .forEach(out::println);
    }

    @Test
    void shouldComplainAboutBadComplexExample()
            throws IOException {
        final var complexExample = objectMapper.readValue(
                loadXml("bad-complex-example"),
                ComplexExample.class);

        final var errors = new BeanPropertyBindingResult(
                complexExample, "xml/bad-complex-example.xml");
        validator.validate(complexExample, errors);

        assertThat(errors.hasErrors())
                .withFailMessage("Failed to find validation errors")
                .isTrue();
    }
}