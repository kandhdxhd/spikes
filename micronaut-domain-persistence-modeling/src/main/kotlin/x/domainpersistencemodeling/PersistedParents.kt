package x.domainpersistencemodeling

import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES
import io.micronaut.data.repository.CrudRepository
import java.time.Instant
import java.time.Instant.EPOCH
import java.util.*
import javax.inject.Singleton
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table
import kotlin.reflect.KMutableProperty0

@Singleton
class PersistedParentFactory(
        private val repository: ParentRepository,
        private val publisher: ApplicationEventPublisher)
    : ParentFactory {
    override fun all() =
            repository.findAll().map {
                PersistedParent(it.asResource(), it, this)
            }.asSequence()

    override fun findExisting(naturalId: String) =
            repository.findByNaturalId(naturalId).orElse(null)?.let {
                PersistedParent(it.asResource(), it, this)
            }

    override fun createNew(resource: ParentResource) =
            PersistedParent(null, ParentRecord(resource), this)

    override fun findExistingOrCreateNew(naturalId: String) =
            findExisting(naturalId) ?: createNew(
                    ParentResource(naturalId, null, 0))

    internal fun save(record: ParentRecord) =
            repository.findByNaturalId(
                    repository.save(record).naturalId).get()

    internal fun delete(record: ParentRecord) = repository.delete(record)

    internal fun notifyChanged(
            before: ParentResource?, after: ParentResource?) =
            notifyIfChanged(before, after, publisher, ::ParentChangedEvent)

    internal fun idOf(resource: ParentResource) =
            repository.findByNaturalId(resource.naturalId).orElse(null)?.id

    internal fun resourceOf(id: Long): ParentResource? =
            repository.findById(id).orElse(null)?.asResource()
}

class PersistedParent internal constructor(
        private var snapshot: ParentResource?,
        private val record: ParentRecord,
        private val factory: PersistedParentFactory)
    : Parent {
    override val naturalId: String
        get() = record.naturalId
    override val value: String?
        get() = record.value
    override val version: Int
        get() = record.version
    override val existing: Boolean
        get() = 0 < version

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PersistedParent
        return snapshot == other.snapshot
                && record == other.record
    }

    override fun update(block: MutableParent.() -> Unit) = apply {
        PersistedMutableParent(::snapshot, record, factory).block()
    }

    override fun hashCode() = Objects.hash(snapshot, record)

    override fun toString() =
            "${super.toString()}{snapshot=$snapshot, record=$record}"
}

class PersistedMutableParent internal constructor(
        private val snapshot: KMutableProperty0<ParentResource?>,
        private var record: ParentRecord?,
        private val factory: PersistedParentFactory)
    : MutableParent,
        MutableParentDetails by record!! {
    override fun save() = apply {
        val before = snapshot.get()
        record = factory.save(record!!)
        val after = record!!.asResource()
        snapshot.set(after)
        factory.notifyChanged(before, after)
    }

    override fun delete() {
        val before = snapshot.get()
        factory.delete(record!!)
        record = null
        val after = null
        snapshot.set(after)
        factory.notifyChanged(before, after)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PersistedMutableParent
        return snapshot == other.snapshot
                && record == other.record
    }

    override fun hashCode() = Objects.hash(snapshot, record)

    override fun toString() =
            "${super.toString()}{snapshot=$snapshot, record=$record}"
}

@JdbcRepository(dialect = POSTGRES)
interface ParentRepository : CrudRepository<ParentRecord, Long> {
    // TODO: Can I return Kotlin `ParentRecord?`
    fun findByNaturalId(naturalId: String): Optional<ParentRecord>
}

@Introspected
@Table(name = "parent")
data class ParentRecord(
        @Id @GeneratedValue val id: Long?,
        override val naturalId: String,
        override var value: String?,
        override val version: Int,
        val createdAt: Instant,
        val updatedAt: Instant)
    : MutableParentDetails {
    internal constructor(resource: ParentResource) : this(
            null,
            resource.naturalId,
            resource.value,
            0,
            EPOCH,
            EPOCH)

    internal fun asResource() = ParentResource(naturalId, value, version)
}