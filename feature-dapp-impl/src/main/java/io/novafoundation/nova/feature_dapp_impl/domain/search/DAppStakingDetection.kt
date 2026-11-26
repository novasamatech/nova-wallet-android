package io.novafoundation.nova.feature_dapp_impl.domain.search

enum class DAppStakingKind {
    STAKING,
    BITTENSOR
}

object DAppStakingDetection {

    private val STAKING_TOKEN_PREFIXES = listOf("stak", "стейк")

    private val STAKING_SUBSTRINGS = listOf("质押", "質押", "ステーキング", "스테이킹")

    private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")

    // The TAO expression of the product spec, shared verbatim with iOS and split here only to fit the line length:
    // a host label that starts or ends with "tao" (but is not "taobao"), or one of the known Bittensor names.
    private val BITTENSOR_EXPRESSION = Regex(
        pattern = """(?:^|\.)(?!taobao(?:\.|$))""" +
            """(?:tao[a-z0-9-]*|[a-z0-9-]*tao|bittensor|opentensor|tensorwallet|tensorplex|mentat[a-z0-9-]*|backprop[a-z0-9-]*)""" +
            """(?=\.|$)""",
        option = RegexOption.IGNORE_CASE
    )

    /**
     * A query that is both TAO-related and a staking one is [DAppStakingKind.BITTENSOR].
     */
    fun queryKind(query: String?): DAppStakingKind? {
        return when {
            isBittensorQuery(query) -> DAppStakingKind.BITTENSOR
            isStakingQuery(query) -> DAppStakingKind.STAKING
            else -> null
        }
    }

    fun isBittensorQuery(query: String?): Boolean {
        if (query.isNullOrEmpty()) return false

        return query.lowercase()
            .split(NON_ALPHANUMERIC)
            .any(BITTENSOR_EXPRESSION::containsMatchIn)
    }

    fun isBittensorHost(host: String?): Boolean {
        if (host.isNullOrEmpty()) return false

        return BITTENSOR_EXPRESSION.containsMatchIn(host)
    }

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
