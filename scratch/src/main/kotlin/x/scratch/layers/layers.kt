package x.scratch.layers

class Layer(
    val name: String,
    private val map: MutableMap<String, Value<*>>,
    private val layers: Layers
) : Map<String, Value<*>> by map {
    fun edit(block: MutableMap<String, Value<*>>.() -> Unit) = block(map)
    fun keepAndNext(nextLayerName: String) = layers.keepAndNext(nextLayerName)
    fun reset(renameLayer: String = name) = layers.reset(renameLayer)

    override fun toString() = "$name: $map"
}

class Layers private constructor(
    val name: String,
    initialLayerValues: MutableMap<String, Value<*>>,
    firstLayerName: String
) {
    private val layers: MutableList<Layer> = mutableListOf()

    val top get() = layers.last()
    val keys get() = layers.flatMap { it.keys }.distinct()

    init {
        val initialLayer = Layer("initial", initialLayerValues, this)
        layers += initialLayer
        initialLayer.keepAndNext(firstLayerName)
    }

    internal fun keepAndNext(
        nextLayerName: String,
        values: MutableMap<String, Value<*>> = mutableMapOf()
    ): Layer {
        val layer = Layer(nextLayerName, values, this)
        layers.add(layer)
        return layer
    }

    fun reset(name: String = top.name): Layer {
        val newLayer = Layer(name, mutableMapOf(), this)
        layers[layers.size - 1] = newLayer
        return newLayer
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> values(key: String): Sequence<Value<T>> {
        return layers.map {
            it[key]
        }.asSequence().filterNotNull() as Sequence<Value<T>>
    }

    override fun toString(): String {
        val x = StringBuilder(name)
        x.append(':')
        layers.withIndex().reversed().forEach {
            x.append("\n- [${it.index}] ${it.value}")
        }
        return x.toString()
    }

    companion object {
        fun newLayer(
            layersName: String,
            firstLayerName: String,
            vararg rules: Pair<String, RuleValue<*>>
        ) =
            Layers(layersName, mutableMapOf(*rules), firstLayerName)
    }
}

operator fun Layers.get(key: String) = value<Any>(key)

operator fun MutableMap<String, Value<*>>.set(key: String, value: Boolean) {
    this[key] = BooleanValue(value)
}

operator fun MutableMap<String, Value<*>>.set(key: String, value: Int) {
    this[key] = IntValue(value)
}

operator fun MutableMap<String, Value<*>>.set(key: String, value: String) {
    this[key] = StringValue(value)
}

fun Layers.edit(block: MutableMap<String, Value<*>>.() -> Unit) =
    top.edit(block)

fun <T> Layers.value(key: String): T {
    val allValues = values<T>(key)
    val rule = allValues.filterIsInstance<RuleValue<T>>().last()
    val values = allValues.map { it.value }

    return rule(values)
}

fun Layers.toMap() = keys.map { it to this[it] }.toMap()
