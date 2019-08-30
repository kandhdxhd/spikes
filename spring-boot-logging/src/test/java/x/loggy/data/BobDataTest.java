package x.loggy.data;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureEmbeddedDatabase
@DataJdbcTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class BobDataTest {
    private final BobRepository repository;

    @Test
    void shouldRoundTrip() {
        final var queries = new SqlQueries();

        final var unsaved = new BobRecord();
        unsaved.name = "William";
        final var saved = repository.save(unsaved);
        final var found = repository.findById(saved.id);

        assertThat(found).contains(saved);

        assertThat(queries).hasSize(2);
        assertThat(queries.get(0)).startsWith("INSERT");
        assertThat(queries.get(1)).startsWith("SELECT");
        queries.clear();
    }
}
