package io.novafoundation.nova.feature_staking_impl.data.bittensor

import java.math.BigDecimal
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

class BittensorConfigRemote(
    @JsonAdapter(BittensorSwapFeeAdapter::class)
    val swapFee: BigDecimal?,
    val subnets: List<Subnet?>?
) {

    class Subnet(val netuid: Int?, val logo: String?)
}

// Like iOS's try? decodeIfPresent, an invalid fee field does not invalidate an otherwise valid config.
// Structural JSON errors still propagate from the reader.
internal class BittensorSwapFeeAdapter : TypeAdapter<BigDecimal?>() {

    override fun read(reader: JsonReader): BigDecimal? {
        if (reader.peek() != JsonToken.NUMBER) {
            reader.skipValue()
            return null
        }
        return reader.nextString().toBigDecimalOrNull()
    }

    override fun write(writer: JsonWriter, value: BigDecimal?) {
        writer.value(value)
    }
}
