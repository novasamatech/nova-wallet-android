package io.novafoundation.nova.feature_staking_impl.data.notices.model

import com.google.gson.JsonElement
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.time.LocalDate
import java.time.format.DateTimeParseException

internal class StakingNoticeDto(
    val chainId: ChainId,
    val severity: String,
    val shortText: JsonElement,   // may be JsonPrimitive (string) or JsonObject (locale map)
    val longText: JsonElement,
    val endDate: String? = null
) {

    fun toDomain(preferredLocale: String, today: LocalDate = LocalDate.now()): StakingNotice? {
        val parsedSeverity = when (severity.lowercase()) {
            "info" -> StakingNotice.Severity.INFO
            "critical" -> StakingNotice.Severity.CRITICAL
            else -> return null
        }

        val resolvedShort = resolveText(shortText, preferredLocale) ?: return null
        val resolvedLong = resolveText(longText, preferredLocale) ?: return null

        val parsedEndDate: LocalDate? = endDate?.let {
            try { LocalDate.parse(it) } catch (e: DateTimeParseException) { null }
        }
        if (parsedEndDate != null && !parsedEndDate.isAfter(today)) return null

        return StakingNotice(
            chainId = chainId,
            severity = parsedSeverity,
            shortText = resolvedShort,
            longText = resolvedLong,
            endDate = parsedEndDate
        )
    }

    private fun resolveText(element: JsonElement, preferredLocale: String): String? {
        if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
            return element.asString
        }
        if (element.isJsonObject) {
            val map = element.asJsonObject.entrySet().associate { (k, v) -> k to v.asString }
            return LocaleResolver.resolve(map, preferredLocale)
        }
        return null
    }
}
