package hm.binkley.layers

import java.util.AbstractMap.SimpleEntry
import kotlin.collections.Map.Entry

class Layers(private val layers: MutableList<Layer> = mutableListOf())
    : LayersForRuleContext {
    fun asMap(): Map<String, Any> = object : AbstractMap<String, Any>() {
        override val entries: Set<Entry<String, Any>>
            get() = applied().toSet()
    }

    fun commit(): Layer {
        val layer = Layer(layers.size)
        layers.add(layer)
        return layer
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> valueFor(key: String) = layers.asReversed().flatMap {
        it.entries
    }.filter {
        it.key == key
    }.first {
        null != it.value.rule
    }.let {
        (it.value.rule!! as Rule<T>)(RuleContext(key, this))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> valuesFor(key: String) = layers.mapNotNull {
        it[key]
    }.mapNotNull {
        it.value
    } as List<T>

    @Suppress("UNCHECKED_CAST")
    private fun applied() = layers.asReversed().flatMap {
        it.entries
    }.filter {
        null != it.value.rule
    }.map {
        val key = it.key
        val value = (it.value.rule!! as Rule<Any>)(RuleContext(key, this))
        SimpleEntry(key, value)
    }

    override fun toString() = "${this::class.simpleName}$layers"
}

class Layer(val slot: Int,
        private val contents: MutableMap<String, Value<*>> = mutableMapOf())
    : Map<String, Value<*>> by contents {
    fun edit(block: MutableLayer.() -> Unit) = apply {
        val mutable = MutableLayer(contents)
        mutable.block()
    }

    override fun toString() = "${this::class.simpleName}$contents"
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
    fun <T> valueFor(key: String): T

    fun <T> valuesFor(key: String): List<T>
}

class RuleContext<T>(val myKey: String,
        private val layers: LayersForRuleContext) {
    val myValues: List<T>
        get() = layers.valuesFor(myKey)

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String) = layers.valueFor<T>(key)
}

interface Rule<T> : (RuleContext<T>) -> T {
    override fun toString(): String
}

data class Value<T>(val rule: Rule<T>?, val value: T?)

fun <T> rule(name: String, default: T, rule: (RuleContext<T>) -> T) =
        Value(object : Rule<T> {
            override fun invoke(context: RuleContext<T>) = rule(context)
            override fun toString() = "<rule: $name[=$default]>"
        }, default)

fun <T> value(context: T): Value<T> = Value(null, context)
