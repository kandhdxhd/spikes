package x.domainpersistencemodeling

import x.domainpersistencemodeling.PersistableDomain.UpsertedDomainResult
import x.domainpersistencemodeling.UpsertableRecord.UpsertedRecordResult
import java.util.Objects.hash

internal interface PersistedFactory<Snapshot,
        Record : UpsertableRecord<Record>,
        Computed : PersistedComputedDetails> {
    fun save(record: Record): UpsertedRecordResult<Record>
    fun delete(record: Record)
    fun refreshRecord(naturalId: String): Record
    fun notifyChanged(before: Snapshot?, after: Snapshot?)

    fun toSnapshot(record: Record, computed: Computed): Snapshot
}

internal interface PersistedComputedDetails {
    fun saveMutated(): Boolean
}

internal class PersistedDomain<Snapshot,
        Record : UpsertableRecord<Record>,
        Computed : PersistedComputedDetails,
        Factory : PersistedFactory<Snapshot, Record, Computed>,
        Domain : PersistableDomain<Snapshot, Domain>,
        Mutable>(
        private val factory: Factory,
        private var snapshot: Snapshot?,
        private var _record: Record?,
        internal val computed: Computed,
        private val toDomain: (PersistedDomain
        <Snapshot, Record, Computed, Factory, Domain, Mutable>) -> Domain)
    : PersistableDomain<Snapshot, Domain> {
    override val naturalId: String
        get() = record.naturalId
    override val version: Int
        get() = record.version
    override val changed
        get() = snapshot != factory.toSnapshot(record, computed)

    /** Throws [DomainException] if the domain object has been deleted. */
    internal val record: Record
        get() = _record ?: throw DomainException("Deleted: $this")

    /**
     * Notice that when **saving**, save the other _first_, so added
     * children have a valid FK reference.
     */
    @Suppress("UNCHECKED_CAST")
    override fun save(): UpsertedDomainResult<Snapshot, Domain> {
        // Save ourselves first, so children have a valid parent
        val before = snapshot
        var result =
                if (changed) factory.save(record)
                else UpsertedRecordResult(record, false)
        _record = result.record

        if (computed.saveMutated()) {
            // Refresh the version
            _record = factory.refreshRecord(naturalId)
            result = UpsertedRecordResult(record, true)
        }

        val after = factory.toSnapshot(record, computed)
        snapshot = after
        if (result.changed) // Trust the database
            factory.notifyChanged(before, after)
        return UpsertedDomainResult(toDomain(this), result.changed)
    }

    /**
     * Notice that when **deleting**, save the other _last_, so that FK
     * references get cleared.
     */
    override fun delete() {
        val before = snapshot
        computed.saveMutated()
        factory.delete(record)

        val after = null as Snapshot?
        _record = null
        snapshot = after
        factory.notifyChanged(before, after)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PersistedDomain<*, *, *, *, *, *>
        return snapshot == other.snapshot
                && _record == other._record
                && computed == other.computed
    }

    override fun hashCode() = hash(snapshot, _record, computed)

    override fun toString() =
            "${super.toString()}{snapshot=${snapshot}, record=${_record}, computed=${computed}}"
}