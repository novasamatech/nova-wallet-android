package io.novafoundation.nova.feature_dapp_impl.domain.search

object StakingKeywordMatcher {

    private val FALSE_POSITIVE_PATTERNS = listOf(
        "mistake", "stakeholder", "undertake", "sweepstake",
        "validate email", "validate form", "james bond"
    )

    private val LATIN_KEYWORDS = listOf(
        // English
        "staking", "stake", "staked", "unstake", "unstaking", "restake", "restaking",
        "nominate", "nominator", "nomination", "nomination pool", "nom pool",
        "validator", "validators", "validate", "collator", "collators",
        "delegate", "delegator", "pool staking", "staking pool",
        "direct staking", "liquid staking", "bond", "bonding", "unbond", "unbonding",
        "rebond", "rebonding", "apy", "apr", "yield",
        "dot staking", "ksm staking", "polkadot staking", "kusama staking",
        "staking dashboard", "staking rewards", "stake my", "start staking",
        "how to stake", "where to stake", "staking dapp", "staking app",
        "earn rewards", "earn dot", "earn ksm", "passive income",
        "stake tokens", "stake dot", "stake ksm",
        "parachain staking", "dapp staking", "manage staking",
        "my validators", "my nominations",
        // Russian
        "\u0441\u0442\u0435\u0439\u043a\u0438\u043d\u0433", "\u0441\u0442\u0435\u0439\u043a\u0430\u0442\u044c",
        "\u043d\u0430\u0433\u0440\u0430\u0434\u044b", "\u0432\u0430\u043b\u0438\u0434\u0430\u0442\u043e\u0440",
        "\u043d\u043e\u043c\u0438\u043d\u0430\u0442\u043e\u0440",
        "\u0434\u0435\u043b\u0435\u0433\u0438\u0440\u043e\u0432\u0430\u0442\u044c",
        // Spanish
        "stakear", "hacer staking", "recompensas", "validador", "nominar", "delegar",
        // French
        "staker", "faire du staking",
        "r\u00e9compenses", "validateur", "nommer",
        "d\u00e9l\u00e9guer",
        // Turkish
        "stake yapmak", "staking yapmak",
        "\u00f6d\u00fcller",
        "do\u011frulay\u0131c\u0131",
        "delege",
        // Polish
        "stakowanie",
        "nagrody",
        "walidator", "nominacja",
        "delegowa\u0107",
        // Hungarian
        "stakel\u00e9s",
        "jutalmak",
        "valid\u00e1tor",
        "nomin\u00e1l\u00e1s",
        "deleg\u00e1l\u00e1s",
        // Indonesian
        "hadiah", "imbalan",
        // Vietnamese
        "ph\u1ea7n th\u01b0\u1edfng",
        "x\u00e1c th\u1ef1c",
        "\u1ee7y quy\u1ec1n",
        // Italian
        "fare staking", "ricompense", "validatore", "nominare", "delegare",
        // Portuguese
        "fazer staking", "recompensas", "validador", "nomear", "delegar"
    )

    // CJK keywords — matched by contains (substring matching)
    private val CJK_KEYWORDS = listOf(
        // Japanese
        "\u30b9\u30c6\u30fc\u30ad\u30f3\u30b0", "\u30d0\u30ea\u30c7\u30fc\u30bf",
        "\u30ce\u30df\u30cd\u30fc\u30b7\u30e7\u30f3", "\u59d4\u4efb", "\u5831\u916c",

        // Korean
        "\uc2a4\ud14c\uc774\ud0b9", "\uac80\uc99d\uc778", "\uc9c0\uba85",
        "\uc704\uc784", "\ubcf4\uc0c1",

        // Chinese
        "\u8d28\u62bc", "\u9a8c\u8bc1\u4eba", "\u63d0\u540d", "\u59d4\u6258",
        "\u5956\u52b1", "\u6536\u76ca"
    )

    fun isStakingQuery(query: String): Boolean {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return false

        // Check false positives first
        if (FALSE_POSITIVE_PATTERNS.any { trimmed.contains(it) }) return false

        // Latin keywords: exact match or query starts with keyword + space
        if (LATIN_KEYWORDS.any { keyword -> trimmed == keyword || trimmed.startsWith("$keyword ") }) return true

        // CJK/non-Latin keywords: contains
        if (CJK_KEYWORDS.any { keyword -> trimmed.contains(keyword) }) return true

        return false
    }
}
