package io.novafoundation.nova.feature_dapp_impl.domain.search

object DAppStakingDetection {

    private val STAKING_TOKEN_PREFIXES = listOf("stak", "стейк")

    private val STAKING_SUBSTRINGS = listOf("质押", "質押", "ステーキング", "스테이킹")

    private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")

    fun isStakingQuery(query: String?): Boolean {
        if (query.isNullOrEmpty()) return false

        val normalizedQuery = query.lowercase()

        val hasKeywordToken = normalizedQuery.split(NON_ALPHANUMERIC).any { token ->
            STAKING_TOKEN_PREFIXES.any(token::startsWith)
        }

        if (hasKeywordToken) return true

        return STAKING_SUBSTRINGS.any(normalizedQuery::contains)
    }

    fun isStakingHost(host: String?): Boolean {
        if (host.isNullOrEmpty()) return false

        return host.lowercase().split(".").any { label ->
            STAKING_TOKEN_PREFIXES.any(label::startsWith)
        }
    }

    fun normalizeHost(host: String): String {
        return host.lowercase().removePrefix("www.")
    }
}
