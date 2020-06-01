package x.scratch

import java.math.BigInteger
import java.util.Objects.hash
import kotlin.math.absoluteValue

fun main() {
    val fib0 = Fib(0)
    val fib1 = Fib(1)

    println("UNIT: $fib0")
    println("GENERATOR: $fib1")
    println()
    println("== SCALAR FIB: ${fib0.fib}; DET: ${fib0.det}")
    println("F0 * F0 -> ${fib0 * fib0}")
    println("F0 / F0 -> ${fib0 / fib0}")
    println("1/F0 -> ${fib0.inv()}")
    println("F0^2 -> ${fib0.pow(2)}")
    println("F0^-2 -> ${fib0.pow(-2)}")
    println()
    println("== SCALAR FIB: ${fib1.fib}; DET ${fib1.det}")
    println("F1 * F1 -> ${fib1 * fib1}")
    println("F1 / F1 -> ${fib1 / fib1}")
    println("1/F1 -> ${fib1.inv()}")
    println("F1^2 -> ${fib1.pow(2)}")
    println("F1^-2 -> ${fib1.pow(-2)}")
    println()
    println("== SCALAR INV FIB: ${fib1.inv().fib}; DET ${fib1.inv().det}")
    println("1/F1 * 1/F1 -> ${fib1.inv() * fib1.inv()}")
    println("1/F1 / 1/F1 -> ${fib1.inv() / fib1.inv()}")
    println("1/(1/F1) -> ${fib1.inv().inv()}")
    println("(1/F1)^2 -> ${fib1.inv().pow(2)}")
    println("(1/F1)^-2 -> ${fib1.inv().pow(-2)}")
    println()
    println("== SEQUENCE")
    for (n in -3..3)
        println("Fib($n) -> ${Fib(n)}; Fib($n)^1 -> ${Fib(n).inv()}")
    println()
    println("== BIG")
    println("F100 -> ${Fib(100)}")
    println("F100 -> ${Fib(100)}")
}

class Fib internal constructor(
    val n: Int,
    val a: BigInteger,
    val b: BigInteger,
    val c: BigInteger,
    val d: BigInteger
) {
    override fun equals(other: Any?) = this === other ||
            other is Fib &&
            n == other.n

    override fun hashCode() = hash(n)
    override fun toString() = "F($n)[$a, $b; $c, $d]"
}

// TODO: Replace with divide-and-conquer algo using memoization
fun Fib(n: Int) = fibN(n, if (n < 0) Fib_1 else Fib1, n.absoluteValue, Fib0)

val Fib.fib get() = b
val Fib.det get() = if (0 == n % 2) 1 else -1

fun Fib.inv() = when {
    0 == n -> this
    n < 0 -> Fib(-n, d.abs(), b.abs(), c.abs(), a.abs())
    0 == n % 2 -> Fib(-n, d, -b, -c, a)
    else -> Fib(-n, -d, b, c, -a)
}

fun Fib.pow(p: Int) = Fib(n * p)

operator fun Fib.times(multiplicand: Fib) = Fib(n + multiplicand.n)
operator fun Fib.div(divisor: Fib) = Fib(n - divisor.n)

private val Fib_1 = Fib(-1, (-1).big, 1.big, 1.big, 0.big)
private val Fib0 = Fib(0, 1.big, 0.big, 0.big, 1.big)
private val Fib1 = Fib(1, 0.big, 1.big, 1.big, 1.big)

private val CACHE = mutableMapOf(
    -1 to Fib_1,
    0 to Fib0,
    1 to Fib1
)

/**
 * @todo Note articles like
 *   <a href="https://dzone.com/articles/avoid-recursion"><cite>Avoid Recursion in ConcurrentHashMap.computeIfAbsent()</cite></a>,
 *   which remains true even now
 */
private tailrec fun fibN(
    n: Int,
    multiplicand: Fib,
    i: Int,
    fib_i: Fib
): Fib {
    return if (0 == i) fib_i
    else fibN(
        n,
        multiplicand,
        i - 1,
        multiply0(n, fib_i, multiplicand)
    )
}

private fun multiply0(n: Int, left: Fib, right: Fib) = Fib(
    n,
    left.a * right.a + left.b * right.c,
    left.a * right.b + left.b * right.d,
    left.c * right.a + left.d * right.c,
    left.c * right.b + left.d * right.d
)

private val Int.big get() = toBigInteger()
