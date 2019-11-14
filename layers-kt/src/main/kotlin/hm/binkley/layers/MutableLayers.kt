package hm.binkley.layers

import java.util.AbstractMap.SimpleEntry
import java.util.Objects
import java.util.TreeMap
import kotlin.collections.Map.Entry

class MutableLayers(
        private val layers: MutableList<PersistedLayer> = mutableListOf())
    : Layers,
        LayersForRuleContext {
    override fun asList(): List<Map<String, Any>> = layers

    override fun asMap(): Map<String, Any> =
            object : AbstractMap<String, Any>() {
                override val entries: Set<Entry<String, Any>>
                    get() = applied().toSortedSet(compareBy {
                        it.key
                    })
            }

    fun commit(script: String = ""): PersistedLayer {
        val layer = PersistedLayer(layers.size, script)
        layers.add(layer)
        return layer
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> appliedValueFor(key: String) = topDownLayers.flatMap {
        it.entries
    }.filter {
        it.key == key
    }.first {
        null != it.value.rule
    }.let {
        (it.value.rule!! as Rule<T>)(RuleContext(key, this))
    }

    /** All values for [key] from newest to oldest. */
    @Suppress("UNCHECKED_CAST")
    override fun <T> allValuesFor(key: String) = topDownLayers.mapNotNull {
        it[key]
    }.mapNotNull {
        it.value
    } as List<T>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MutableLayers

        return layers == other.layers
    }

    override fun hashCode() = Objects.hash(layers)

    override fun toString() = "${this::class.simpleName}$layers"

    @Suppress("UNCHECKED_CAST")
    private fun applied() = topDownLayers.flatMap {
        it.entries
    }.filter {
        null != it.value.rule
    }.map {
        val key = it.key
        val value =
                (it.value.rule!! as Rule<Any>)(RuleContext(key, this))
        SimpleEntry(key, value)
    }

    private val topDownLayers: List<PersistedLayer>
        get() = layers.filter {
            it.enabled
        }.asReversed()
}

class PersistedLayer(override val slot: Int,
        override val script: String,
        override val meta: MutableMap<String, String> = mutableMapOf(),
        private val contents: MutableMap<String, Value<*>> = TreeMap())
    : Map<String, Value<*>> by contents,
        Layer {
    override val enabled = true

    override fun toDiff() = contents.entries.joinToString("\n") {
        val (key, value) = it
        "$key: ${value.toDiff()}"
    }

    fun edit(block: MutableLayer.() -> Unit) = apply {
        val mutable = MutableLayer(contents)
        mutable.block()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PersistedLayer

        return slot == other.slot
                && script == other.script
                && contents == other.contents
                && enabled == other.enabled
    }

    override fun hashCode() = Objects.hash(slot, script, contents, enabled)

    override fun toString() =
            "${this::class.simpleName}#$slot:$contents\\$meta[${if (enabled) "enabled" else "disabled"}]"
}

class MutableLayer(private val contents: MutableMap<String, Value<*>>)
    : MutableMap<String, Value<*>> by contents {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> set(key: String, value: T) {
        if (value is Value<*>)
            contents[key] = value as Value<T>
        else
            contents[key] = value(value)
    }
}

interface LayersForRuleContext {
    fun <T> appliedValueFor(key: String): T

    fun <T> allValuesFor(key: String): List<T>
}

class RuleContext<T>(val myKey: String,
        private val layers: LayersForRuleContext) {
    val myValues: List<T>
        get() = layers.allValuesFor(myKey)

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String) = layers.appliedValueFor<T>(key)
}
