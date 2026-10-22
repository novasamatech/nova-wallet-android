package io.novafoundation.nova.analytics

enum class StakingAnalyticsTarget(val value: String) {
    ROOT("root"),
    SUBNET("subnet")
}

enum class StakingMode(val value: String) {
    NEW_POSITION("new_position"),
    ADD_STAKE("add_stake"),
    BUY_MORE("buy_more")
}

enum class UnstakeMode(val value: String) {
    PARTIAL("partial"),
    FULL("full")
}

enum class StakingAnalyticsSelectionMethod(val value: String) {
    QUICK_PICK_POOL_SIZE("quick_pick_pool_size"),
    QUICK_PICK_AGE("quick_pick_age"),
    QUICK_PICK_APY("quick_pick_apy"),
    QUICK_PICK_ROOT("quick_pick_root"),
    PICKER_ROOT_ROW("picker_root_row"),
    PICKER_SUBNET("picker_subnet"),
    SUBNET_DETAILS("subnet_details")
}

enum class StakingValidatorSource(val value: String) {
    RECOMMENDED("recommended"),
    EXISTING_HOTKEY("existing_hotkey"),
    MANUAL("manual")
}

enum class StakingFailureStage(val value: String) {
    NOT_SUBMITTED("not_submitted"),
    DISPATCHED("dispatched")
}

enum class StakingCompletionOutcome(val value: String) {
    DONE("done"),
    UNCONFIRMED("unconfirmed")
}

enum class StakingFlow(val value: String) {
    STAKE("stake"),
    UNSTAKE("unstake"),
    CLAIM("claim")
}

enum class StakingBlockedReason(val value: String) {
    LEDGER("ledger"),
    PROXY("proxy"),
    MULTISIG_UNSUPPORTED("multisig_unsupported"),
    WATCH_ONLY("watch_only"),
    NO_ACCOUNT("no_account"),
    NO_BALANCE("no_balance"),
    TRADES_UNAVAILABLE("trades_unavailable"),
    ROOT_HOLD("root_hold"),
    ALL_LOCKED("all_locked"),
    CLAIM_BELOW_MINIMUM("claim_below_minimum"),
    CLAIM_PENDING("claim_pending")
}

enum class AnalyticsWalletType(val value: String) {
    SECRETS("secrets"),
    WATCH_ONLY("watch_only"),
    PARITY_SIGNER("parity_signer"),
    LEDGER("ledger"),
    POLKADOT_VAULT("polkadot_vault"),
    PROXIED("proxied"),
    GENERIC_LEDGER("generic_ledger"),
    MULTISIG("multisig")
}

enum class StakingValidationKind(val value: String) {
    MIN_STAKE("min_stake"),
    INSUFFICIENT_BALANCE("insufficient_balance"),
    FEE_RESERVE("fee_reserve"),
    PRICE_IMPACT("price_impact"),
    ORDER_TOO_LARGE("order_too_large"),
    RATE_UPDATED("rate_updated"),
    PRICE_MOVED("price_moved"),
    STALE_DATA("stale_data"),
    DUST_REMAINDER("dust_remainder"),
    HOLD_LOCKED("hold_locked"),
    FEE_IN_PROGRESS("fee_in_progress"),
    LIQUIDITY("liquidity")
}

enum class StakingValidationStage(val value: String) {
    SETUP("setup"),
    CONFIRM("confirm")
}

enum class StakingValidationOutcome(val value: String) {
    PROCEEDED("proceeded"),
    CANCELLED("cancelled"),
    BLOCKED("blocked")
}

enum class SlippageChangeMethod(val value: String) {
    PRESET("preset"),
    CUSTOM("custom"),
    RESET("reset")
}

enum class SlippageWarning(val value: String) {
    NONE("none"),
    MAY_REVERT("may_revert"),
    MAY_FRONTRUN("may_frontrun")
}

enum class SubnetListSource(val value: String) {
    SEE_ALL_OPTIONS("see_all_options"),
    SEE_ALL_SUBNETS("see_all_subnets"),
    SETUP_CARD("setup_card")
}

enum class SubnetSortKey(val value: String) {
    POOL_SIZE("pool_size"),
    AGE("age"),
    APY("apy"),
    SEVEN_DAY_CHANGE("seven_day_change"),
    NAME("name")
}

enum class SubnetListAction(val value: String) {
    SEARCH("search"),
    SORT("sort"),
    FILTER_THIN_POOLS("filter_thin_pools"),
    FAVORITE_ADDED("favorite_added"),
    FAVORITE_REMOVED("favorite_removed")
}

enum class SubnetDetailsSource(val value: String) {
    PICKER("picker"),
    SETUP("setup"),
    POSITION("position")
}

enum class AnalyticsPricePeriod(val value: String) {
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    QUARTER("quarter"),
    YEAR("year"),
    ALL("all")
}

enum class ValidatorChangeSource(val value: String) {
    LIST("list"),
    SEARCH("search")
}

enum class StakingPortfolioSource(val value: String) {
    DASHBOARD("dashboard"),
    RESULT("result"),
    WALLET_BALANCE("wallet_balance")
}

enum class StakingChartState(val value: String) {
    SHOWN("ok"),
    UNAVAILABLE("unavailable")
}

enum class StakingRewardsState(val value: String) {
    CLAIMABLE("claimable"),
    BELOW_MINIMUM("below_minimum"),
    NONE("none"),
    UNAVAILABLE("unavailable")
}

enum class StakingResultAction(val value: String) {
    DISCOVER_SUBNETS("discover_subnets"),
    VIEW_POSITION("view_position"),
    YOUR_BITTENSOR("your_bittensor"),
    BACK_TO_POSITION("back_to_position"),
    TRY_AGAIN("try_again"),
    CLOSE("close")
}

enum class BittensorDataEndpoint(val value: String) {
    SUBNETS("subnets"),
    VALIDATORS("validators"),
    RECOMMENDATIONS("recommendations"),
    PORTFOLIO_HISTORY("portfolio_history"),
    OPERATIONS("operations"),
    ROOT_OVERVIEW("root_overview"),
    CLAIM_PREVIEWS("claim_previews"),
    PRICE_SERVER("price_server"),
    COINGECKO("coingecko"),
    CHAIN_RPC("chain_rpc"),
    NOVA_UTILS_CONFIG("nova_utils_config")
}

enum class BittensorDataFailureReason(val value: String) {
    SERVER_ERROR("server_error"),
    RATE_LIMITED("rate_limited"),
    REJECTED("rejected"),
    TRANSPORT("transport"),
    CONTRACT_VIOLATION("contract_violation"),
    GENERATION_STALE("generation_stale"),
    STALE_RATES("stale_rates")
}

enum class BittensorScreen(val value: String) {
    LANDING("landing"),
    WHERE_TO_STAKE("where_to_stake"),
    SUBNET_SELECT("subnet_select"),
    SUBNET_DETAILS("subnet_details"),
    VALIDATOR_SELECT("validator_select"),
    SETUP("setup"),
    UNSTAKE_SETUP("unstake_setup"),
    CONFIRM("confirm"),
    CLAIM("claim"),
    RESULT("result"),
    PORTFOLIO("portfolio"),
    POSITION("position")
}
