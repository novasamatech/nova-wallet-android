package io.novafoundation.nova.feature_staking_impl.data.subtensor.decoder

import java.math.BigInteger

/**
 * SCALE decoder for the `Vec<StakeInfo>` payload returned by Bittensor's
 * `state_call("StakeInfoRuntimeApi_get_stake_info_for_coldkey", coldkey)`.
 *
 * Field order matches `pallets/subtensor/src/rpc_info/stake_info.rs`. If
 * upstream adds a field before `is_registered`, update the skip count in
 * [decodeEntry] — both the multistaking sync and the detail-screen fetcher
 * route through here, so a single update fixes every call site.
 *
 * Mirrors `SubtensorStakeInfoDecoder.swift` on iOS.
 */
object SubtensorStakeInfoDecoder {

    data class Entry(
        val hotkey: ByteArray,
        val coldkey: ByteArray,
        /** 0 = root, >0 = subnet id. */
        val netuid: Int,
        /** Resolved stake amount in the subnet's smallest unit (RAO / alpha). */
        val stake: BigInteger,
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Entry) return false
            return hotkey.contentEquals(other.hotkey) &&
                coldkey.contentEquals(other.coldkey) &&
                netuid == other.netuid &&
                stake == other.stake
        }

        override fun hashCode(): Int {
            var result = hotkey.contentHashCode()
            result = 31 * result + coldkey.contentHashCode()
            result = 31 * result + netuid
            result = 31 * result + stake.hashCode()
            return result
        }
    }

    fun decode(hex: String): List<Entry> {
        val bytes = hexToBytes(hex)
        if (bytes.isEmpty()) return emptyList()

        val (count, posAfterCount) = scaleCompact(bytes, 0) ?: return emptyList()

        val result = mutableListOf<Entry>()
        var pos = posAfterCount
        val total = count.toInt()
        for (i in 0 until total) {
            val decoded = decodeEntry(bytes, pos) ?: return result
            result.add(decoded.first)
            pos = decoded.second
        }
        return result
    }

    private fun decodeEntry(bytes: ByteArray, offset: Int): Pair<Entry, Int>? {
        var pos = offset

        // hotkey: AccountId32 (32 raw bytes)
        if (pos + 32 > bytes.size) return null
        val hotkey = bytes.copyOfRange(pos, pos + 32)
        pos += 32

        // coldkey: AccountId32 (32 raw bytes)
        if (pos + 32 > bytes.size) return null
        val coldkey = bytes.copyOfRange(pos, pos + 32)
        pos += 32

        // netuid: Compact<u16>
        val (netuidBig, posAfterNetuid) = scaleCompact(bytes, pos) ?: return null
        pos = posAfterNetuid

        // stake: Compact<U128>
        val (stake, posAfterStake) = scaleCompact(bytes, pos) ?: return null
        pos = posAfterStake

        // Skip locked / emission / tao_emission / drain — four Compact<U128>
        // we don't read.
        repeat(4) {
            val (_, next) = scaleCompact(bytes, pos) ?: return null
            pos = next
        }

        // is_registered: bool (1 byte)
        if (pos + 1 > bytes.size) return null
        pos += 1

        return Entry(
            hotkey = hotkey,
            coldkey = coldkey,
            netuid = netuidBig.toInt(),
            stake = stake,
        ) to pos
    }

    /**
     * SCALE compact decoder supporting all four modes (1/2/4-byte and
     * big-integer). Returns `(value, nextOffset)` or `null` on truncation.
     */
    fun scaleCompact(bytes: ByteArray, offset: Int): Pair<BigInteger, Int>? {
        if (offset >= bytes.size) return null
        val mode = bytes[offset].toInt() and 0x03
        return when (mode) {
            0x00 -> {
                val value = (bytes[offset].toInt() and 0xFF) ushr 2
                BigInteger.valueOf(value.toLong()) to (offset + 1)
            }
            0x01 -> {
                if (offset + 2 > bytes.size) return null
                val raw = (bytes[offset].toInt() and 0xFF) or
                    ((bytes[offset + 1].toInt() and 0xFF) shl 8)
                BigInteger.valueOf((raw ushr 2).toLong()) to (offset + 2)
            }
            0x02 -> {
                if (offset + 4 > bytes.size) return null
                val raw = (bytes[offset].toInt() and 0xFF).toLong() or
                    ((bytes[offset + 1].toInt() and 0xFF).toLong() shl 8) or
                    ((bytes[offset + 2].toInt() and 0xFF).toLong() shl 16) or
                    ((bytes[offset + 3].toInt() and 0xFF).toLong() shl 24)
                BigInteger.valueOf(raw ushr 2) to (offset + 4)
            }
            0x03 -> {
                // Big-integer mode: byte0 = (N << 2) | 3, then (N+4) LE bytes.
                val extraBytes = (bytes[offset].toInt() and 0xFF) ushr 2
                val len = extraBytes + 4
                if (offset + 1 + len > bytes.size) return null
                val leSlice = bytes.copyOfRange(offset + 1, offset + 1 + len)
                val beBytes = leSlice.reversedArray()
                // Prepend 0 to keep BigInteger unsigned.
                val unsigned = ByteArray(beBytes.size + 1).also {
                    System.arraycopy(beBytes, 0, it, 1, beBytes.size)
                }
                BigInteger(unsigned) to (offset + 1 + len)
            }
            else -> null
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = if (hex.startsWith("0x")) hex.substring(2) else hex
        if (clean.length % 2 != 0) return ByteArray(0)
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) {
            val high = Character.digit(clean[i * 2], 16)
            val low = Character.digit(clean[i * 2 + 1], 16)
            if (high < 0 || low < 0) return ByteArray(0)
            out[i] = ((high shl 4) or low).toByte()
        }
        return out
    }
}
