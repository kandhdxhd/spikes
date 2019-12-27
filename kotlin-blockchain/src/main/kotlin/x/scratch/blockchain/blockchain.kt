package x.scratch.blockchain

import java.security.MessageDigest
import java.time.Instant
import java.time.Instant.EPOCH
import java.util.Objects
import kotlin.Int.Companion.MAX_VALUE

fun main() {
    var firstBlock = Block.first("00")
    println("$firstBlock")
    var nextBlock = firstBlock.next("Hello, world!")
    println("$nextBlock")

    // Testing example
    firstBlock = Block.first(
        timestamp = EPOCH
    )
    println("$firstBlock")
    nextBlock = firstBlock.next(
        data = "Hello, world!",
        timestamp = Instant.ofEpochMilli(1L)
    )
    println("$nextBlock")
}

private val sha256 = MessageDigest.getInstance("SHA-256")

fun Block.Companion.first(
    difficulty: String = "",
    timestamp: Instant = Instant.now()
) =
    Block(
        transaction = 0,
        data = "Genesis",
        previousHash = "0".repeat(64),
        difficulty = difficulty,
        timestamp = timestamp
    )

class Block(
    val transaction: Long,
    val data: String,
    val previousHash: String,
    val difficulty: String,
    val timestamp: Instant
) {
    val hash: String = hashWithProofOfWork()

    fun next(data: String, timestamp: Instant = Instant.now()) =
        Block(transaction + 1, data, hash, difficulty, timestamp)

    private fun hashWithProofOfWork(): String {
        fun hashWithNonce(nonce: Int) = sha256
            .digest("$nonce$transaction$timestamp$previousHash$data".toByteArray())
            .joinToString("") { "%02x".format(it) }

        for (nonce in 0..MAX_VALUE) {
            val hash = hashWithNonce(nonce)
            if (hash.startsWith(difficulty)) return hash
        }

        throw IllegalStateException("Unable to complete work: $this")
    }

    override fun equals(other: Any?): Boolean {
        return this === other
                || other is Block
                && hash == other.hash
    }

    override fun hashCode() = Objects.hash(hash)

    override fun toString() =
        "${super.toString()}{transaction=$transaction, timestamp=$timestamp, data=$data, hash=$hash, previousHash=$previousHash}"

    companion object
}