package x.domainpersistencemodeling

import ch.tutteli.atrium.api.cc.en_GB.containsExactly
import ch.tutteli.atrium.api.cc.en_GB.toBe
import ch.tutteli.atrium.api.cc.en_GB.toThrow
import ch.tutteli.atrium.verbs.expect
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.context.annotation.Import

@AutoConfigureTestDatabase(replace = NONE)
@DataJdbcTest
@Import(value = [
    PersistedChildFactory::class,
    PersistedParentFactory::class,
    TestListener::class])
class ChildPersistenceTest {
    companion object {
        const val naturalId = "p"
    }

    @Autowired
    lateinit var children: ChildFactory
    @Autowired
    lateinit var parents: ParentFactory
    @Autowired
    lateinit var testListener: TestListener<ChildChangedEvent>

    @AfterEach
    fun tearDown() {
        children.all().forEach {
            it.delete()
        }
        parents.all().forEach {
            it.delete()
        }
    }

    @Test
    fun shouldRoundTrip() {
        val unsaved = newUnsavedChild()
        val saved = unsaved.save()

        val found = children.findExisting(saved.naturalId)

        expect(found).toBe(saved)

        testListener.expectNext.containsExactly(
                ChildChangedEvent(null, ChildResource(
                        naturalId, null, null, sortedSetOf(), 1)))
    }

    @Test
    fun shouldRoundTripJson() {
        val unsaved = newUnsavedChild()
        unsaved.update {
            subchildren.addAll(listOf("MOAT", "BAT"))
        }
        val saved = unsaved.save()

        val found = children.findExisting(saved.naturalId)!!

        expect(found.subchildren).containsExactly("BAT", "MOAT")

        found.update {
            subchildren.clear()
            subchildren.add("NANCY")
        }
        val resaved = found.save()

        val refound = children.findExisting(resaved.naturalId)!!

        expect(refound.subchildren).containsExactly("NANCY")
    }

    @Test
    fun shouldDelete() {
        val unsaved = newUnsavedChild()
        val saved = unsaved.save()

        saved.delete()

        val found = children.findExisting(naturalId)

        expect(found).toBe(null)

        expect {
            saved.naturalId
        }.toThrow<NullPointerException> { }

        testListener.expectNext.containsExactly(
                ChildChangedEvent(null, ChildResource(
                        naturalId, null, null, sortedSetOf(), 1)),
                ChildChangedEvent(ChildResource(
                        naturalId, null, null, sortedSetOf(), 1), null))
    }

    @Test
    fun shouldAddToParent() {
        val parent = parents.findExistingOrCreateNew("a").save()

        val unsaved = newUnsavedChild()
        val saved = unsaved.update {
            addTo(parent)
        }.save()

        val found = children.findExisting(saved.naturalId)!!

        expect(found.parentNaturalId).toBe(parent.naturalId)
    }

    private fun newUnsavedChild() =
            children.findExistingOrCreateNew(naturalId)
}
