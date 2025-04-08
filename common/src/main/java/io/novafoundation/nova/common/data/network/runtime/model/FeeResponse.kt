package io.novafoundation.nova.common.data.network.runtime.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.JsonAdapter
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import java.lang.reflect.Type
import java.math.BigInteger

class FeeResponse(
    val partialFee: BigInteger,

    @JsonAdapter(WeightDeserizalier::class)
    val weight: WeightV2
)

class WeightDeserizalier : JsonDeserializer<WeightV2> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): WeightV2 {
        return when {
            // weight v1
            json is JsonPrimitive -> WeightV2.fromV1(json.asLong.toBigInteger())
            // weight v2
            json is JsonObject -> WeightV2(
                refTime = json["ref_time"].asLong.toBigInteger(),
                proofSize = json["proof_size"].asLong.toBigInteger()
            )

            else -> error("Unsupported weight type")
        }
    }
}
