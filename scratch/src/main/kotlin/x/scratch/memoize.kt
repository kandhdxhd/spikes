package x.scratch

import x.scratch.Memoize.Companion.memoize

fun main() {
    // See https://en.wikipedia.org/wiki/Primality_test#Pseudocode
    println("==MEMOIZATION")

    println()
    println("NO MEMOIZATION")
    println(factorial(10))

    println()
    println("MEMOIZED")
    val factorial = { n: Long -> factorial(n) }.memoize()
    println(factorial(10))
}

/** Not the Gamma or Pi functions. */
private fun factorial(n: Long): Long = factorial0(n, 1)

/**
 * @todo This does *not* actually memoize as expected.  Since it counts
 *       _down_, it does not capture smaller inputs in the memoization.
 *       Ideally, we'd capture the 1..n inputs, and not just the n<sup>th
 */
private tailrec fun factorial0(n: Long, a: Long): Long = when (n) {
    1L -> a
    else -> factorial0(n - 1, n * a)
}

private class Memoize<in T, out R>(private val f: (T) -> R) : (T) -> R {
    private val results = mutableMapOf<T, R>()

    override fun invoke(n: T): R = results.getOrPut(n) {
        f(n)
    }

    companion object {
        fun <T, R> ((T) -> R).memoize() = Memoize<T, R>(this)
    }
}
