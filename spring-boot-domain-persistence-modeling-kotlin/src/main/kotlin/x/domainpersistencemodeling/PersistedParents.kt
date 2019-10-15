package x.domainpersistencemodeling

import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import x.domainpersistencemodeling.UpsertableDomain.UpsertedDomainResult
import x.domainpersistencemodeling.UpsertableRecord.UpsertedRecordResult
import java.util.Objects
import java.util.Optional
import java.util.TreeSet
import java.util.stream.Collectors.toCollection

@Component
internal open class PersistedParentFactory(
        private val repository: ParentRepository,
        private val children: ChildFactory,
        private val publisher: ApplicationEventPublisher)
    : ParentFactory {
    companion object {
        internal fun toResource(record: ParentRecord) =
                ParentResource(record.naturalId, record.value,
                        record.version)
    }

    override fun all() = repository.findAll().map {
        toParent(it)
    }.asSequence()

    override fun findExisting(naturalId: String): Parent? {
        return repository.findByNaturalId(naturalId).map {
            toParent(it)
        }.orElse(null)
    }

    override fun createNew(naturalId: String) =
            PersistedParent(this, null, ParentRecord(naturalId),
                    emptySequence())

    override fun findExistingOrCreateNew(naturalId: String) =
            findExisting(naturalId) ?: createNew(naturalId)

    fun save(record: ParentRecord) =
            UpsertedRecordResult.of(record, repository.upsert(record))

    internal fun delete(record: ParentRecord) {
        repository.delete(record)
    }

    internal fun refresh(naturalId: String) =
            repository.findByNaturalId(naturalId).orElseThrow()

    internal fun notifyChanged(
            before: ParentResource?, after: ParentResource?) {
        notifyIfChanged(before, after, publisher, ::ParentChangedEvent)
    }

    private fun toParent(record: ParentRecord) =
            PersistedParent(this, toResource(record), record,
                    children.findOwned(record.naturalId))
}

internal class PersistedParent(
        private val factory: PersistedParentFactory,
        private var snapshot: ParentResource?,
        private var record: ParentRecord?,
        assigned: Sequence<Child>)
    : Parent,
        ParentDetails by record!! {
    private var snapshotChildren: Set<Child>
    private var _children: MutableSet<Child>

    override val children: Set<Child>
        get() = _children

    init {
        snapshotChildren = assigned.toSortedSet()
        _children = TreeSet(snapshotChildren)
    }

    override val changed
        get() = snapshot != toResource()

    override fun save(): UpsertedDomainResult<Parent> {
        // Save ourselves first, so children have a valid parent
        val before = snapshot
        val result =
                if (changed) factory.save(record!!)
                else UpsertedRecordResult.of(record!!, null)
        record = result.record

        assignedChildren().forEach { it.save() }
        unassignedChildren().forEach { it.save() }
        changedChildren().forEach { it.save() }
        // Update our version -- TODO: Optimize away if unneeded
        val refreshed = factory.refresh(naturalId)
        record!!.version = refreshed.version

        snapshotChildren = TreeSet(children)
        val after = toResource()
        snapshot = after
        factory.notifyChanged(before, after)
        return UpsertedDomainResult(this, result.changed)
    }

    override fun delete() {
        if (children.isNotEmpty()) throw DomainException(
                "Deleting parent with assigned children: $this")

        snapshotChildren.forEach { it.save() }

        val before = snapshot
        val after = null as ParentResource?
        factory.delete(record!!)
        record = null
        snapshot = after
        factory.notifyChanged(before, after)
    }

    private fun assignedChildren(): Set<Child> {
        val assigned = TreeSet(children)
        assigned.removeAll(snapshotChildren)
        return assigned
    }

    private fun unassignedChildren(): Set<Child> {
        val unassigned = TreeSet(snapshotChildren)
        unassigned.removeAll(children)
        return unassigned
    }

    private fun changedChildren(): Set<Child> {
        val changed = TreeSet(snapshotChildren)
        changed.retainAll(children)
        return changed.stream()
                .filter { it.changed }
                .collect(toCollection(::TreeSet))
    }

    override fun toResource() =
            PersistedParentFactory.toResource(record!!)

    override fun update(block: MutableParent.() -> Unit): Parent {
        val mutable = PersistedMutableParent(
                record!!, children, ::addChild, ::removeChild)
        block(mutable)
        return this
    }

    private fun addChild(child: Child, all: MutableSet<Child>) {
        child.update {
            assignTo(this@PersistedParent)
        }
        _children = all
    }

    private fun removeChild(child: Child, all: MutableSet<Child>) {
        child.update {
            unassignFromAny()
        }
        _children = all
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PersistedParent
        return snapshot == other.snapshot
                && snapshotChildren == other.snapshotChildren
                && record == other.record
                && children == other.children
    }

    override fun hashCode() =
            Objects.hash(snapshot, snapshotChildren, record, children)

    override fun toString() =
            "${super.toString()}{snapshot=$snapshot, snapshotChildren=$snapshotChildren, record=$record, children=$children}"
}

internal data class PersistedMutableParent(
        private val record: ParentRecord,
        private val initial: Set<Child>,
        private val added: (Child, MutableSet<Child>) -> Unit,
        private val removed: (Child, MutableSet<Child>) -> Unit)
    : MutableParent,
        MutableParentDetails by record {
    override val children = TrackedSortedSet(initial, added, removed)
}

interface ParentRepository : CrudRepository<ParentRecord, Long> {
    @Query("""
        SELECT *
        FROM parent
        WHERE natural_id = :naturalId
        """)
    fun findByNaturalId(@Param("naturalId") naturalId: String)
            : Optional<ParentRecord>

    @Query("""
        SELECT *
        FROM upsert_parent(:naturalId, :value, :version)
        """)
    fun upsert(
            @Param("naturalId") naturalId: String?,
            @Param("value") value: String?,
            @Param("version") version: Int?): ParentRecord?

    @JvmDefault
    fun upsert(entity: ParentRecord) =
            upsert(entity.naturalId, entity.value, entity.version)?.let {
                entity.updateWith(it)
            }
}

@Table("parent")
data class ParentRecord(
        @Id var id: Long?,
        override var naturalId: String,
        override var value: String?,
        override var version: Int)
    : MutableParentDetails,
        UpsertableRecord<ParentRecord> {
    internal constructor(naturalId: String)
            : this(null, naturalId, null, 0)

    override fun updateWith(upserted: ParentRecord): ParentRecord {
        id = upserted.id
        naturalId = upserted.naturalId
        value = upserted.value
        version = upserted.version
        return this
    }
}
