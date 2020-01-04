package x.scratch

import x.scratch.Rational.Companion.NEGATIVE_INFINITY
import x.scratch.Rational.Companion.NaN
import x.scratch.Rational.Companion.POSITIVE_INFINITY
import x.scratch.Rational.Companion.ZERO
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.typeOf
import kotlin.time.ExperimentalTime
import kotlin.time.MonoClock
import kotlin.time.TestClock
import kotlin.time.measureTimedValue
import kotlin.time.seconds

const val a = 1

class A(val p: Int)

@UseExperimental(
    ExperimentalStdlibApi::class,
    ExperimentalTime::class,
    ExperimentalUnsignedTypes::class
)
fun main() {
    val a: UByte = 3u
    val b: UByte = 5u
    println(a.and(b))
    println(a.or(b))
    println(a.inv())
    println(a.xor(b))

    val intType = typeOf<Int>()
    println(intType)

    accessReifiedTypeArg<String>()
    accessReifiedTypeArg<List<String>>()

    val first = InlineClass(1)
    val second = InlineClass(1)
    println(first == second)
    println(first::class)

    // TODO: Why did this stop working?
//    println(::a.get())
//    println(::a.name)
//    println(::a.javaField)
    println(A(p = 3)::p.get())
    println(A(p = 5)::p.javaField)
    println(::A.javaConstructor)

    foo(second)

    val x: UByte = 255u

    println("$x -> ${x.toByte()}")

    val keys = 'a'..'f'
    val map = keys.associateWith {
        it.toString().capitalize().repeat(3)
    }
    println(map)

    val ic = WrappedInt(3)

    println(ic * 2)

    val mappy = ViewMapAsProperties(mutableMapOf("a" to "apple"))
    println(mappy.a)
    mappy.a = "aardvark"
    println(mappy.a)

    val dotdot = 1..2
    println("$dotdot -> ${dotdot::class}")

    val xs = String::class.supertypes
    println("$xs, ie, ${xs.first()::class}")
    val xxs = String::class.superclasses
    println("$xxs, ie, ${xxs.first()::class}")

    val clock = MonoClock
    val mark1 = clock.markNow()
    val mark2 = clock.markNow()
    println("$mark1 -> ${mark1.elapsedNow()}")

    val (result, duration) = clock.measureTimedValue {
        "zippo"
    }
    println("$result took $duration")

    val tc = TestClock()
    val tm = tc.markNow()
    tc.plusAssign(1.seconds)
    println("Test clock advanced since mark by ${tm.elapsedNow()}")

    println(Bob(3).aaa(listOf("fruits")))

    println("Kotlin floating point is Java's")
    println("NaN comparison: ${Double.NaN == Double.NaN}")
    println("NaN check: ${Double.NaN.isNaN()}")

    println("ZERO is $ZERO")
    println("NaN is $NaN")
    println("POSITIVE_INFINITY is $POSITIVE_INFINITY")
    println("NEGATIVE_INFINITY is $NEGATIVE_INFINITY")
    println("1 is ${Rational.new(1)}")
    println("4/10 is ${4 over 10}")
    println("4/2 is ${4 over 2}")
    println(
        "0/0 is ${0 over 0}, and NaN is NaN is same object is ${NaN === 0 over 0}, and NaN is equal to itself ${NaN == 0 over 0}, and NaN is equal to itself also ${0 over 0 == NaN}"
    )
    println("4/0 is ${4 over 0}")
    println("-4/0 is ${-4 over 0}")
    println("-4/-4 is ${-4 over -4}")

    val ratA = 3 over 5
    val ratB = 2 over 3
    println("$ratA / $ratB is ${ratA / ratB}")

    val ratC = ZERO
    val ratD = Rational.new(7, 3)
    val ratE = Rational.new(1, 2)
    for (r in ratC..ratD step ratE) println(r)
    for (r in ratD downTo ratC) println(r)
    try {
        for (r in POSITIVE_INFINITY..NaN);
    } catch (e: IllegalStateException) {
        println("Expected: $e")
    }

    println("Compare infinities? ${POSITIVE_INFINITY > ZERO} and ${NEGATIVE_INFINITY < ZERO}")

    val toSort = listOf(
        POSITIVE_INFINITY,
        NaN,
        ZERO,
        POSITIVE_INFINITY,
        NaN,
        NEGATIVE_INFINITY,
        ZERO,
        NEGATIVE_INFINITY
    )
    println("$toSort sorted is ${toSort.sorted()}")
}

@UseExperimental(ExperimentalStdlibApi::class)
inline fun <reified T> accessReifiedTypeArg() {
    val kType = typeOf<T>()
    println(kType.toString())
}

inline class InlineClass(val value: Int)

@UseExperimental(ExperimentalContracts::class)
fun synchronize(lock: Any?, block: () -> Unit) {
    // It tells compiler:
    // "This function will invoke 'block' here and now, and exactly one time"
    contract { callsInPlace(block, EXACTLY_ONCE) }

    block()
}

fun foo(lock: Any) {
    val x: Int
    synchronize(lock) {
        x = 42 // Compiler knows that lambda passed to 'synchronize' is called
        // exactly once, so no reassignment is reported
    }
    println(x) // Compiler knows that lambda will be definitely called, performing
    // initialization, so 'x' is considered to be initialized here
}

inline class WrappedInt(val value: Int) {
    operator fun times(b: Int) = WrappedInt(value * b)
}

class ViewMapAsProperties(map: MutableMap<String, Any?>) {
    var a: String by map
}

interface Listy {
    // This extends List only within scope of implementers of the interface
    fun <T> List<T>.doodah() {
        println("Howdy, there, pardner!")
    }
}

data class Bob(val a: Int) : Listy {
    fun <T> aaa(list: List<T>) {
        list.doodah()
    }
}
