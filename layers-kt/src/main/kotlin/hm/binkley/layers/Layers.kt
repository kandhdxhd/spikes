package hm.binkley.layers

import java.util.Objects

interface WithDiff {
    fun forDiff(): String
}

interface Layers {
    fun asList(): List<Map<String, Any>>
    fun asMap(): Map<String, Any>
}

interface XLayer
    : Map<String, Value<*>>,
        WithDiff {
    val slot: Int
    val script: String?
    val meta: Map<String, String>
}

typealias Rule<T> = (RuleContext<T>) -> T

open class Value<T>(open val rule: Rule<T>?, val value: T?)
    : WithDiff {
    // TODO: Print "3 : Int" rather than just "3"
    //  Expression in a class literal has a nullable type 'T', use !! to make
    //  the type non-nullable
    override fun forDiff() = if (null == rule) "$value" else "$this"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Value<*>

        return rule == other.rule
                && value == other.value
    }

    override fun hashCode() =
            Objects.hash(rule, value)

    override fun toString() =
            "${this::class.simpleName}{rule=$rule, value=$value}"
}

class RuleValue<T>(
        val name: String, val default: T, override val rule: Rule<T>)
    : Value<T>(rule, default) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RuleValue<*>

        return name == other.name
                && default == other.default
                && rule == other.rule
    }

    override fun hashCode() =
            Objects.hash(name, default, rule)

    override fun toString() =
            "${this::class.simpleName}<rule: $name[=$default]>"
}

fun <T> value(context: T): Value<T> =
        Value(null, context)

fun <T> rule(name: String, default: T, rule: Rule<T>) =
        RuleValue(name, default, rule)
