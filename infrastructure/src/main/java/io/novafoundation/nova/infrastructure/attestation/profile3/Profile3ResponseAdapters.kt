package io.novafoundation.nova.infrastructure.attestation.profile3

import androidx.annotation.Keep
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

@Keep
class Profile3ChallengeResponseAdapter : TypeAdapter<Profile3ChallengeResponse>() {

    override fun read(reader: JsonReader): Profile3ChallengeResponse {
        var challenge: String? = null
        var issued: Long? = null
        var expires: Long? = null
        val fields = mutableSetOf<String>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (!fields.add(name)) throw IOException("Duplicate challenge response field")
            when (name) {
                "challenge" -> challenge = reader.strictProfile3String()
                "issued_at" -> issued = reader.strictProfile3Timestamp()
                "expires_at" -> expires = reader.strictProfile3Timestamp()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return Profile3ChallengeResponse(challenge, issued, expires)
    }

    override fun write(writer: JsonWriter, value: Profile3ChallengeResponse?) = throw UnsupportedOperationException("Response only")
}

/** Gson's default Long adapter coerces strings and fractions; protocol timestamps must be JSON integers. */
@Keep
class Profile3TokenResponseAdapter : TypeAdapter<Profile3TokenResponse>() {

    override fun read(reader: JsonReader): Profile3TokenResponse {
        var token: String? = null
        var type: String? = null
        var issued: Long? = null
        var expires: Long? = null
        val fields = mutableSetOf<String>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (!fields.add(name)) throw IOException("Duplicate token response field")
            when (name) {
                "access_token" -> token = reader.strictProfile3String()
                "token_type" -> type = reader.strictProfile3String()
                "issued_at" -> issued = reader.strictProfile3Timestamp()
                "expires_at" -> expires = reader.strictProfile3Timestamp()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        if (type != "Bearer" || token == null || issued == null || expires == null) throw IOException("Invalid token response")
        return Profile3TokenResponse(token, issued, expires)
    }

    override fun write(writer: JsonWriter, value: Profile3TokenResponse?) = throw UnsupportedOperationException("Response only")
}

private fun JsonReader.strictProfile3String(): String {
    if (peek() != JsonToken.STRING) throw IOException("Expected profile-3 response string")
    return nextString()
}

private fun JsonReader.strictProfile3Timestamp(): Long {
    if (peek() != JsonToken.NUMBER) throw IOException("Expected integer timestamp")
    val value = nextString()
    if (!value.matches(Regex("0|[1-9][0-9]*"))) throw IOException("Expected nonnegative integer timestamp")
    return value.toLongOrNull() ?: throw IOException("Timestamp overflow")
}
