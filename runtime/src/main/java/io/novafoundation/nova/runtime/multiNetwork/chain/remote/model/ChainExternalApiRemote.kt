package io.novafoundation.nova.runtime.multiNetwork.chain.remote.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

typealias ChainExternalApisRemote = Map<ExternalApiTypeRemote, List<ChainExternalApiRemote>>
typealias ExternalApiTypeRemote = String

data class ChainExternalApiRemote(
    @SerializedName("type")
    val sourceType: String,
    val url: String,
    @JsonAdapter(RawStringTypeAdapter::class)
    val parameters: String?
)

private class RawStringTypeAdapter private constructor() : JsonDeserializer<String?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type, context: JsonDeserializationContext): String? {
        return if (json == null || json is JsonNull) {
            null
        } else {
            json.toString()
        }
    }
}
