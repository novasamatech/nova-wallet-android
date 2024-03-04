package io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote

import io.emeraldpay.polkaj.scale.ScaleCodecReader
import io.emeraldpay.polkaj.scale.ScaleCodecWriter
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.Primitive
import kotlin.experimental.and
import kotlin.experimental.or

data class Vote(
    val aye: Boolean,
    val conviction: Conviction
)

enum class Conviction {
    None,
    Locked1x,
    Locked2x,
    Locked3x,
    Locked4x,
    Locked5x,
    Locked6x
}

fun mapConvictionFromString(conviction: String): Conviction {
    return Conviction.values().first { it.name == conviction }
}

private const val AYE_MASK = 0b1000_0000.toByte()
private const val VOTE_MASK = 0b0111_1111.toByte()

class VoteType(name: String) : Primitive<Vote>(name) {

    override fun decode(scaleCodecReader: ScaleCodecReader, runtime: RuntimeSnapshot): Vote {
        val compactVote = scaleCodecReader.readByte()

        val isAye = compactVote and AYE_MASK == AYE_MASK
        val convictionIdx = compactVote and VOTE_MASK

        val conviction = Conviction.values()[convictionIdx.toInt()]

        return Vote(
            aye = isAye,
            conviction = conviction
        )
    }

    override fun encode(scaleCodecWriter: ScaleCodecWriter, runtime: RuntimeSnapshot, value: Vote) {
        val ayeBit = if (value.aye) AYE_MASK else 0
        val compactVote = value.conviction.ordinal.toByte() or ayeBit

        scaleCodecWriter.writeByte(compactVote)
    }

    override fun isValidInstance(instance: Any?): Boolean {
        return instance is Vote
    }
}
