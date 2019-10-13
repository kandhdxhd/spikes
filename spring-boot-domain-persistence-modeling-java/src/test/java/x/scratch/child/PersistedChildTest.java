package x.scratch.child;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import x.scratch.TestListener;
import x.scratch.UpsertableDomain.UpsertedDomainResult;
import x.scratch.parent.Parent;
import x.scratch.parent.ParentFactory;

import java.util.List;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@AutoConfigureTestDatabase(replace = NONE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@SpringBootTest
@TestInstance(PER_CLASS)
@Transactional
class PersistedChildTest {
    private static final String naturalId = "p";
    private static final String parentNaturalId = "a";

    private final ChildFactory children;
    private final ParentFactory parents;
    private final TestListener<ChildChangedEvent> testListener;

    @Test
    void shouldCreateNew() {
        final var found = children.findExistingOrCreateNew(naturalId);

        assertThat(found).isEqualTo(children.createNew(naturalId));
        assertThat(found.isExisting()).isFalse();
    }

    @Test
    void shouldFindExisting() {
        final var saved = newSavedChild();

        final var found = children.findExistingOrCreateNew(naturalId);

        assertThat(found).isEqualTo(saved);
        assertThat(found.isExisting()).isTrue();
    }

    @Test
    void shouldRoundTrip() {
        final var unsaved = children.createNew(naturalId);

        assertThat(unsaved.getVersion()).isEqualTo(0);
        assertThat(events()).isEmpty();

        final var saved = unsaved.save();

        assertThat(children.all()).hasSize(1);
        assertThat(unsaved.getVersion()).isEqualTo(1);
        assertThat(saved).isEqualTo(UpsertedDomainResult.of(unsaved, true));
        assertThat(events()).containsExactly(new ChildChangedEvent(
                null,
                new ChildResource(naturalId, null, null, emptySet(), 1)));

        assertThat(currentPersistedChild()).isEqualTo(unsaved);
    }

    @Test
    void shouldDetectNoChanges() {
        final var original = newSavedChild();
        final var resaved = original.save();

        assertThat(resaved)
                .isEqualTo(UpsertedDomainResult.of(original, false));
        assertThat(events()).isEmpty();
    }

    @Test
    void shouldMutate() {
        final var original = newSavedChild();

        final var value = "FOOBAR";
        final var modified = original.update(it -> it.setValue(value));

        assertThat(modified).isEqualTo(original);
        assertThat(original.getValue()).isEqualTo(value);
        assertThat(events()).isEmpty();

        original.save();

        assertThat(events()).containsExactly(new ChildChangedEvent(
                new ChildResource(naturalId, null, null, emptySet(), 1),
                new ChildResource(naturalId, null, value, emptySet(), 2)));
    }

    @Test
    void shouldDelete() {
        final var existing = newSavedChild();

        existing.delete();

        assertThat(children.all()).isEmpty();
        assertThatThrownBy(existing::getVersion)
                .isInstanceOf(NullPointerException.class);
        assertThat(events()).containsExactly(new ChildChangedEvent(
                new ChildResource(naturalId, null, null, emptySet(), 1),
                null));
    }

    @Test
    void shouldAssignChildAtCreation() {
        final var parent = newSavedParent();

        assertThat(parent.getVersion()).isEqualTo(1);

        final var unsaved = children.createNew(naturalId)
                .update(it -> it.assignTo(parent));

        assertThat(unsaved.getParentNaturalId()).isEqualTo(parentNaturalId);

        unsaved.save();

        assertThat(currentPersistedChild().getParentNaturalId())
                .isEqualTo(parentNaturalId);
        assertThat(currentPersistedParent().getVersion()).isEqualTo(2);
        assertThat(events()).containsExactly(new ChildChangedEvent(
                null,
                new ChildResource(naturalId, parentNaturalId, null,
                        emptySet(), 1)));
    }

    @Test
    void shouldAssignChildAtMutation() {
        final var parent = newSavedParent();
        final var child = newSavedChild();

        assertThat(parent.getVersion()).isEqualTo(1);

        final var assigned = child.update(it -> it.assignTo(parent));

        assertThat(assigned.getParentNaturalId()).isEqualTo(parentNaturalId);
        assertThat(events()).isEmpty();

        assigned.save();

        assertThat(assigned.getVersion()).isEqualTo(2);
        assertThat(currentPersistedChild().getParentNaturalId())
                .isEqualTo(parentNaturalId);
        assertThat(currentPersistedParent().getVersion()).isEqualTo(2);
        assertThat(events()).containsExactly(new ChildChangedEvent(
                new ChildResource(naturalId, null, null, emptySet(), 1),
                new ChildResource(naturalId, parentNaturalId, null,
                        emptySet(), 2)));
    }

    @Test
    void shouldUnassignChild() {
        final var parent = newSavedParent();
        final var child = children.createNew(naturalId)
                .update(it -> it.assignTo(parent))
                .save().getDomain();
        testListener.reset();

        assertThat(parent.getVersion()).isEqualTo(1);

        child.update(MutableChild::unassignFromAny).save();

        assertThat(child.getVersion()).isEqualTo(2);
        assertThat(currentPersistedChild().getParentNaturalId()).isNull();
        // Created, assigned by child, unassigned by child == version 3
        assertThat(currentPersistedParent().getVersion()).isEqualTo(3);
        assertThat(events()).containsExactly(new ChildChangedEvent(
                new ChildResource(naturalId, parentNaturalId, null,
                        emptySet(), 1),
                new ChildResource(naturalId, null, null, emptySet(), 2)));
    }

    private Child newSavedChild() {
        final var child = children.createNew(naturalId).save().getDomain();
        testListener.reset();
        return child;
    }

    private Child currentPersistedChild() {
        return children.findExisting(naturalId).orElseThrow();
    }

    private Parent newSavedParent() {
        final var parent = parents.createNew(parentNaturalId).save()
                .getDomain();
        testListener.reset();
        return parent;
    }

    private Parent currentPersistedParent() {
        return parents.findExisting(parentNaturalId).orElseThrow();
    }

    private List<ChildChangedEvent> events() {
        return testListener.events();
    }
}
