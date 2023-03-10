package io.novafoundation.nova.common.data.network.runtime.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.JsonAdapter
import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import java.lang.reflect.Type
import java.math.BigInteger

class FeeResponse(
    val partialFee: BigInteger,

    @JsonAdapter(WeightDeserizalier::class)
    val weight: Weight
)

class WeightDeserizalier : JsonDeserializer<Weight> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Weight {
        return when {
            // weight v1
            json is JsonPrimitive -> json.asLong.toBigInteger()
            // weight v2
            json is JsonObject -> json["ref_time"].asLong.toBigInteger()

            else -> error("Unsupported weight type")
        }
    }
}
