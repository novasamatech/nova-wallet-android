package io.novafoundation.nova.common.data.legal

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val UPDATED_AT_FORMAT = "yyyy-MM-dd"

class LegalDocumentsRemote(
    val termsOfService: LegalDocumentRemote,
    val privacyNotice: LegalDocumentRemote
)

class LegalDocumentRemote(
    val version: Int,
    @JsonAdapter(UpdatedAtAdapter::class)
    val updatedAt: Date
)

private class UpdatedAtAdapter : JsonDeserializer<Date> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Date {
        val raw = json.asString

        val dateFormat = SimpleDateFormat(UPDATED_AT_FORMAT, Locale.ROOT).apply {
            isLenient = false
        }

        return try {
            dateFormat.parse(raw)!!
        } catch (e: ParseException) {
            throw JsonParseException("Unexpected updatedAt format: $raw", e)
        }
    }
}
