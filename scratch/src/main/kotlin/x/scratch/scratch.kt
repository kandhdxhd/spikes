package x.scratch

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.typeOf

val a = 1

class A(val p: Int)

@UseExperimental(
    ExperimentalStdlibApi::class,
    ExperimentalUnsignedTypes::class
)
fun main() {
    val intType = typeOf<Int>()
    println(intType)

    accessReifiedTypeArg<String>()
    accessReifiedTypeArg<List<String>>()

    val first = InlineClass(1)
    val second = InlineClass(1)
    println(first == second)
    println(first::class)

    println(::a.get())
    println(::a.name)
    println(::a.javaField)
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