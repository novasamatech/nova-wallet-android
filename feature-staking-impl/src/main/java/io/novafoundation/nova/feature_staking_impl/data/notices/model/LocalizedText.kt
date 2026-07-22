package io.novafoundation.nova.feature_staking_impl.data.notices.model

/**
 * Resolves a user-facing string against a preferred locale, falling back by progressively
 * stripping trailing hyphen-segments and finally to "en". E.g.
 * "zh-Hans-CN" -> ["zh-Hans-CN", "zh-Hans", "zh", "en"]. Matches iOS behavior.
 * Used only by [StakingNoticeDto] during decoding.
 */
internal object LocaleResolver {

    fun resolve(map: Map<String, String>, preferredLocale: String): String? {
        val candidates = buildList {
            var current = preferredLocale
            add(current)
            while (current.contains('-')) {
                current = current.substringBeforeLast('-')
                add(current)
            }
            if ("en" !in this) add("en")
        }
        for (candidate in candidates) {
            map[candidate]?.let { return it }
        }
        return null
    }
}
