package x.domainpersistencemodeling.child

import ch.tutteli.atrium.api.cc.en_GB.containsExactly
import ch.tutteli.atrium.api.cc.en_GB.hasSize
import ch.tutteli.atrium.api.cc.en_GB.isEmpty
import ch.tutteli.atrium.api.cc.en_GB.isNotEmpty
import ch.tutteli.atrium.api.cc.en_GB.toBe
import ch.tutteli.atrium.api.cc.en_GB.toThrow
import ch.tutteli.atrium.verbs.expect
import org.junit.jupiter.api.Test
import x.domainpersistencemodeling.DomainException
import x.domainpersistencemodeling.KnownState.DISABLED
import x.domainpersistencemodeling.KnownState.ENABLED
import x.domainpersistencemodeling.LiveTestBase
import x.domainpersistencemodeling.PersistableDomain.UpsertedDomainResult
import x.domainpersistencemodeling.aChildChangedEvent
import x.domainpersistencemodeling.atZero
import x.domainpersistencemodeling.childNaturalId
import x.domainpersistencemodeling.otherNaturalId
import x.domainpersistencemodeling.parentNaturalId

internal class PersistedChildrenTest
    : LiveTestBase() {
    @Test
    fun `should create new`() {
        val found = children.findExistingOrCreateNewUnassigned(childNaturalId)

        expect(found).toBe(newUnsavedUnassignedChild())
        expect(found.existing).toBe(false)

        expectSqlQueryCountsByType(select = 1)
        expectDomainChangedEvents().isEmpty()
    }

    @Test
    fun `should find existing`() {
        val saved = newSavedUnassignedChild()

        val found = children.findExistingOrCreateNewUnassigned(childNaturalId)

        expect(found).toBe(saved)
        expect(found.existing).toBe(true)

        expectSqlQueryCountsByType(select = 2) // Other, this
        expectDomainChangedEvents().isEmpty()
    }

    @Test
    fun `should round trip`() {
        val unsaved = newUnsavedUnassignedChild()

        expect(unsaved.version).toBe(0)

        expectSqlQueries().isEmpty()
        expectDomainChangedEvents().isEmpty()

        val saved = unsaved.save()

        expectSqlQueryCountsByType(upsert = 1)

        expectAllChildren().hasSize(1)
        expect(unsaved.version).toBe(1)
        expect(saved).toBe(UpsertedDomainResult(unsaved, true))
        expect(currentPersistedChild()).toBe(saved.domain)

        expectDomainChangedEvents().containsExactly(
            aChildChangedEvent(
                noBefore = true,
                afterVersion = 1
            )
        )
    }

    @Test
    fun `should detect no changes`() {
        val original = newSavedUnassignedChild()
        val resaved = original.save()

        expect(resaved).toBe(UpsertedDomainResult(original, false))

        expectSqlQueries().isEmpty()
        expectDomainChangedEvents().isEmpty()
    }

    @Test
    fun `should mutate`() {
        val original = newSavedUnassignedChild()

        expect(original.changed).toBe(false)

        // TODO: Adjusting nanos is acting flaky
        val state = DISABLED.name
        val at = atZero.plusSeconds(1L)
        val value = "FOOBAR"
        val sideValue = "ABC"
        original.update {
            this.state = state
            this.at = at
            this.value = value
            this.sideValues += "FOO"
            this.sideValues += sideValue
            this.sideValues -= "FOO"
        }

        expect(original.changed).toBe(true)
        expect(original.state).toBe(state)
        expect(original.at).toBe(at)
        expect(original.value).toBe(value)
        expect(original.sideValues).containsExactly(sideValue)

        expectSqlQueries().isEmpty()
        expectDomainChangedEvents().isEmpty()

        original.save()

        expectSqlQueryCountsByType(upsert = 1)
        expect(original.changed).toBe(false)

        expectDomainChangedEvents().containsExactly(
            aChildChangedEvent(
                beforeVersion = 1,
                beforeState = ENABLED.name,
                beforeAt = atZero,
                beforeValue = null,
                beforeSideValues = setOf(),
                afterVersion = 2,
                afterState = state,
                afterAt = at,
                afterValue = value,
                afterSideValues = setOf(sideValue)
            )
        )
    }

    @Test
    fun `should delete`() {
        val existing = newSavedUnassignedChild()

        existing.delete()

        expectSqlQueryCountsByType(delete = 1)
        expectAllChildren().isEmpty()
        expect {
            existing.version
        }.toThrow<DomainException> { }

        expectDomainChangedEvents().containsExactly(
            aChildChangedEvent(
                beforeVersion = 1,
                noAfter = true
            )
        )
    }

    @Test
    fun `should sort by natural id`() {
        val a = children.findExistingOrCreateNewUnassigned(childNaturalId)
        val b =
            children.findExistingOrCreateNewUnassigned(childNaturalId + "X")
        val set = sortedSetOf<Child<*>>()

        set += b
        set += a

        expectSqlQueries().isNotEmpty()
        expect(set).containsExactly(a, b)
    }

    @Test
    fun `should revert on failed delete`() {
        val existing = newSavedUnassignedChild()

        programmableListener.fail = true

        expect {
            existing.delete()
        }.toThrow<DomainException> { }

        expectSqlQueryCountsByType(delete = 1)

        testListener.reset() // Order of listeners not predictable
        expect(existing.changed).toBe(false)
    }

    @Test
    fun `should assign child at creation`() {
        val parent = newSavedParent()

        expect(parent.version).toBe(1)

        val unsaved = newUnsavedUnassignedChild()
        unsaved.assignTo(parent.naturalId)

        expect(unsaved.parentNaturalId).toBe(parentNaturalId)

        unsaved.save()

        expectSqlQueryCountsByType(upsert = 1)

        expect(currentPersistedChild().parentNaturalId)
            .toBe(parentNaturalId)
        expect(currentPersistedParent().version).toBe(2)

        expectDomainChangedEvents().containsExactly(
            aChildChangedEvent(
                noBefore = true,
                afterVersion = 1,
                afterParentNaturalId = parentNaturalId
            )
        )
    }

    @Test
    fun `should assign child at mutation`() {
        val parent = newSavedParent()
        val child = newSavedUnassignedChild()

        expect(parent.version).toBe(1)

        val assigned = child.assignTo(parent.naturalId)

        expect(assigned.parentNaturalId).toBe(parentNaturalId)

        expectSqlQueries().isEmpty()
        expectDomainChangedEvents().isEmpty()

        assigned.save()

        expectSqlQueryCountsByType(upsert = 1)

        expect(assigned.version).toBe(2)
        expect(currentPersistedChild().parentNaturalId)
            .toBe(parentNaturalId)
        expect(currentPersistedParent().version).toBe(2)

        expectDomainChangedEvents().containsExactly(
            aChildChangedEvent(
                beforeVersion = 1,
                beforeParentNaturalId = null,
                afterVersion = 2,
                afterParentNaturalId = parentNaturalId
            )
        )
    }

    @Test
    fun `should unassign child`() {
        val parent = newSavedParent()
        val unassigned = newUnsavedUnassignedChild()
        val assigned = unassigned.assignTo(parent.naturalId)
        assigned.save().domain

        expectSqlQueryCountsByType(upsert = 1)
        expectDomainChangedEvents().containsExactly(
            aChildChangedEvent(
                noBefore = true,
                afterVersion = 1,
                afterParentNaturalId = parentNaturalId
            )
        )

        expect(parent.version).toBe(1)

        assigned.unassignFromAnyParent()
        assigned.save()

        expectSqlQueryCountsByType(upsert = 1)

        expect(unassigned.version).toBe(2)
        expect(currentPersistedChild().parentNaturalId).toBe(null)
        // Created, assigned by the child, unassigned by the child -> 3
        expect(currentPersistedParent().version).toBe(3)

        expectDomainChangedEvents().containsExactly(
            aChildChangedEvent(
                beforeVersion = 1,
                beforeParentNaturalId = parentNaturalId,
                afterVersion = 2,
                afterParentNaturalId = null
            )
        )
    }

    @Test
    fun `should assign and unassign other`() {
        val child = newSavedUnassignedChild()
        val other = newSavedOther()

        child.assign(other)
        child.save()

        expectSqlQueryCountsByType(select = 1, upsert = 1)
        expect(currentPersistedChild().other).toBe(other)

        expectDomainChangedEvents().containsExactly(
            aChildChangedEvent(
                beforeVersion = 1,
                beforeOtherNaturalId = null,
                afterVersion = 2,
                afterOtherNaturalId = otherNaturalId
            )
        )

        child.unassignAnyOther()
        child.save()

        expectSqlQueryCountsByType(select = 1, upsert = 1)
        expect(currentPersistedChild().other).toBe(null)

        expectDomainChangedEvents().containsExactly(
            aChildChangedEvent(
                beforeVersion = 2,
                beforeOtherNaturalId = otherNaturalId,
                afterVersion = 3,
                afterOtherNaturalId = null
            )
        )
    }
}
