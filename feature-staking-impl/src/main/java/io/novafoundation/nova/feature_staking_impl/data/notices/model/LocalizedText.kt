package io.novafoundation.nova.feature_staking_impl.data.notices.model

/**
 * Resolves a user-facing string against a preferred locale, falling back through
 * "fr-FR" -> "fr" -> "en". Used only by [StakingNoticeDto] during decoding.
 */
internal object LocaleResolver {

    fun resolve(map: Map<String, String>, preferredLocale: String): String? {
        val candidates = buildList {
            add(preferredLocale)
            val baseLanguage = preferredLocale.substringBefore('-').takeIf { it != preferredLocale }
            if (baseLanguage != null) add(baseLanguage)
            add("en")
        }
        for (candidate in candidates) {
            map[candidate]?.let { return it }
        }
        return null
    }
}
