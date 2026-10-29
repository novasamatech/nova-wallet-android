package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import io.novafoundation.nova.common.utils.DecimalFraction

/** The config's Nova commission, not the chain swap fee. Absence is represented by a nullable field. */
sealed interface BittensorSwapFeeRemote {

    data class Value(val fraction: DecimalFraction) : BittensorSwapFeeRemote

    object Invalid : BittensorSwapFeeRemote
}

// An invalid fee field does not invalidate an otherwise valid config. Structural JSON errors still propagate.
internal class BittensorSwapFeeAdapter : TypeAdapter<BittensorSwapFeeRemote?>() {

    override fun read(reader: JsonReader): BittensorSwapFeeRemote? {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            JsonToken.NUMBER -> {
                val fraction = reader.nextString().toBigDecimalOrNull()
                if (fraction != null) BittensorSwapFeeRemote.Value(DecimalFraction(fraction)) else BittensorSwapFeeRemote.Invalid
            }
            else -> {
                reader.skipValue()
                BittensorSwapFeeRemote.Invalid
            }
        }
    }

    override fun write(writer: JsonWriter, value: BittensorSwapFeeRemote?) {
        when (value) {
            null -> writer.nullValue()
            is BittensorSwapFeeRemote.Value -> writer.value(value.fraction.inFraction)
            BittensorSwapFeeRemote.Invalid -> throw IllegalArgumentException("Cannot serialize an invalid Bittensor swap fee")
        }
    }
}
