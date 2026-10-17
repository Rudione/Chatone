package io.rudione.chatone.util.security

class AesGcm(private val encryptBlock: (ByteArray) -> ByteArray) {

    private val hashKey: LongArray = encryptBlock(ByteArray(BLOCK)).toWords()

    fun seal(nonce: ByteArray, plain: ByteArray, aad: ByteArray): ByteArray {
        require(nonce.size == AEAD_NONCE_BYTES)
        val cipher = applyKeystream(nonce, plain)
        return cipher + tag(nonce, cipher, aad)
    }

    fun open(nonce: ByteArray, sealedBox: ByteArray, aad: ByteArray): ByteArray? {
        if (nonce.size != AEAD_NONCE_BYTES || sealedBox.size < AEAD_TAG_BYTES) return null
        val cipher = sealedBox.copyOfRange(0, sealedBox.size - AEAD_TAG_BYTES)
        val received = sealedBox.copyOfRange(sealedBox.size - AEAD_TAG_BYTES, sealedBox.size)
        if (!constantTimeEquals(tag(nonce, cipher, aad), received)) return null
        return applyKeystream(nonce, cipher)
    }

    private fun applyKeystream(nonce: ByteArray, input: ByteArray): ByteArray {
        val output = ByteArray(input.size)
        var counter = 2
        var offset = 0
        while (offset < input.size) {
            val stream = encryptBlock(counterBlock(nonce, counter++))
            val length = minOf(BLOCK, input.size - offset)
            for (i in 0 until length) output[offset + i] = (input[offset + i].toInt() xor stream[i].toInt()).toByte()
            offset += length
        }
        return output
    }

    private fun tag(nonce: ByteArray, cipher: ByteArray, aad: ByteArray): ByteArray {
        val state = LongArray(2)
        absorb(state, aad)
        absorb(state, cipher)
        val lengths = ByteArray(BLOCK)
        writeLong(lengths, 0, aad.size.toLong() * 8)
        writeLong(lengths, 8, cipher.size.toLong() * 8)
        absorbBlock(state, lengths)
        val mask = encryptBlock(counterBlock(nonce, 1))
        val digest = ByteArray(BLOCK)
        writeLong(digest, 0, state[0])
        writeLong(digest, 8, state[1])
        return ByteArray(AEAD_TAG_BYTES) { (digest[it].toInt() xor mask[it].toInt()).toByte() }
    }

    private fun absorb(state: LongArray, data: ByteArray) {
        var offset = 0
        while (offset < data.size) {
            val block = ByteArray(BLOCK)
            data.copyInto(block, 0, offset, minOf(offset + BLOCK, data.size))
            absorbBlock(state, block)
            offset += BLOCK
        }
    }

    private fun absorbBlock(state: LongArray, block: ByteArray) {
        val words = block.toWords()
        state[0] = state[0] xor words[0]
        state[1] = state[1] xor words[1]
        multiply(state)
    }

    private fun multiply(state: LongArray) {
        var zHigh = 0L
        var zLow = 0L
        var vHigh = hashKey[0]
        var vLow = hashKey[1]
        for (i in 0 until 128) {
            val word = if (i < 64) state[0] else state[1]
            val bit = (word ushr (63 - (i % 64))) and 1L
            val select = -bit
            zHigh = zHigh xor (vHigh and select)
            zLow = zLow xor (vLow and select)
            val carry = vLow and 1L
            vLow = (vLow ushr 1) or (vHigh shl 63)
            vHigh = (vHigh ushr 1) xor (REDUCTION and -carry)
        }
        state[0] = zHigh
        state[1] = zLow
    }

    private fun counterBlock(nonce: ByteArray, counter: Int): ByteArray {
        val block = ByteArray(BLOCK)
        nonce.copyInto(block)
        block[12] = (counter ushr 24).toByte()
        block[13] = (counter ushr 16).toByte()
        block[14] = (counter ushr 8).toByte()
        block[15] = counter.toByte()
        return block
    }

    private fun ByteArray.toWords(): LongArray = longArrayOf(readLong(this, 0), readLong(this, 8))

    private fun readLong(source: ByteArray, offset: Int): Long {
        var value = 0L
        for (i in 0 until 8) value = (value shl 8) or (source[offset + i].toLong() and 0xFF)
        return value
    }

    private fun writeLong(target: ByteArray, offset: Int, value: Long) {
        for (i in 0 until 8) target[offset + i] = (value ushr (56 - 8 * i)).toByte()
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private companion object {
        const val BLOCK = 16
        const val REDUCTION = -0x1f00000000000000L
    }
}
