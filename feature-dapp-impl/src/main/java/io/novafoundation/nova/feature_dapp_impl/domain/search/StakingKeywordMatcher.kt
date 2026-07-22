package io.novafoundation.nova.feature_dapp_impl.domain.search

object StakingKeywordMatcher {

    private val FALSE_POSITIVE_PATTERNS = listOf(
        "mistake", "stakeholder", "undertake", "sweepstake",
        "validate email", "validate form", "james bond"
    )

    private val ENGLISH_KEYWORDS = listOf(
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
        "my validators", "my nominations"
    )

    private val MULTILINGUAL_KEYWORDS = listOf(
        // Russian
        "стейкинг", "стейкать",
        "награды", "валидатор",
        "номинатор",
        "делегировать",
        // Spanish
        "stakear", "hacer staking", "recompensas", "validador", "nominar", "delegar",
        // French
        "staker", "faire du staking",
        "récompenses", "validateur", "nommer",
        "déléguer",
        // Turkish
        "stake yapmak", "staking yapmak",
        "ödüller",
        "doğrulayıcı",
        "delege",
        // Polish
        "stakowanie",
        "nagrody",
        "walidator", "nominacja",
        "delegować",
        // Hungarian
        "stakelés",
        "jutalmak",
        "validátor",
        "nominálás",
        "delegálás",
        // Indonesian
        "hadiah", "imbalan",
        // Vietnamese
        "phần thưởng",
        "xác thực",
        "ủy quyền",
        // Italian
        "fare staking", "ricompense", "validatore", "nominare", "delegare",
        // Portuguese
        "fazer staking", "recompensas", "validador", "nomear", "delegar"
    )

    // CJK keywords — matched by contains (substring matching)
    private val CJK_KEYWORDS = listOf(
        // Japanese
        "ステーキング", "バリデータ",
        "ノミネーション", "委任", "報酬",

        // Korean
        "스테이킹", "검증인", "지명",
        "위임", "보상",

        // Chinese
        "质押", "验证人", "提名", "委托",
        "奖励", "收益"
    )

    fun isStakingQuery(query: String): Boolean {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return false

        // Check false positives first
        if (FALSE_POSITIVE_PATTERNS.any { trimmed.contains(it) }) return false

        // English keywords: exact match or query starts with keyword + space.
        // Strict matching avoids false positives on common short English words
        // without needing every excluded form in the FALSE_POSITIVE_PATTERNS list.
        if (ENGLISH_KEYWORDS.any { keyword -> trimmed == keyword || trimmed.startsWith("$keyword ") }) return true

        // Non-English Latin and Cyrillic keywords: substring match.
        // Handles inflection (Russian declensions), agglutination (Turkish),
        // and arbitrary keyword position within a query.
        if (MULTILINGUAL_KEYWORDS.any { keyword -> trimmed.contains(keyword) }) return true

        // CJK keywords: substring match
        if (CJK_KEYWORDS.any { keyword -> trimmed.contains(keyword) }) return true

        return false
    }
}
