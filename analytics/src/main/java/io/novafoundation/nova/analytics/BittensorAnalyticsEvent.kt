package io.novafoundation.nova.analytics

object BittensorAnalyticsEvent {

    const val NETWORK = "Bittensor"

    class TypeSelected(network: String, target: StakingAnalyticsTarget, method: StakingAnalyticsSelectionMethod, netuid: Int) : AnalyticsEvent(
        name = "staking_type_selected",
        properties = mapOf(
            "staking_type" to "subtensor",
            "network" to network,
            "target" to target.value,
            "selection_method" to method.value,
            "netuid" to netuid.toString()
        )
    )

    class Initiated(
        network: String,
        target: StakingAnalyticsTarget,
        mode: StakingMode,
        source: StakingValidatorSource,
        slippage: SlippageBucket?,
        amount: AmountBucket?
    ) : AnalyticsEvent(
        name = "staking_initiated",
        properties = buildMap {
            put("staking_type", "subtensor")
            put("network", network)
            put("target", target.value)
            put("mode", mode.value)
            put("validator_source", source.value)
            if (slippage != null) put("slippage_bucket", slippage.value)
            if (amount != null) put("amount_bucket", amount.value)
        }
    )

    class Confirmed(
        network: String,
        target: StakingAnalyticsTarget,
        mode: StakingMode,
        priceMoved: Boolean,
        amount: AmountBucket?
    ) : AnalyticsEvent(
        name = "staking_confirmed",
        properties = buildMap {
            put("staking_type", "subtensor")
            put("network", network)
            put("target", target.value)
            put("mode", mode.value)
            put("price_moved", priceMoved)
            if (amount != null) put("amount_bucket", amount.value)
        }
    )

    class Completed(
        network: String,
        target: StakingAnalyticsTarget,
        mode: StakingMode,
        outcome: StakingCompletionOutcome,
        duration: DurationBucket,
        amount: AmountBucket?
    ) : AnalyticsEvent(
        name = "staking_completed",
        properties = buildMap {
            put("staking_type", "subtensor")
            put("network", network)
            put("target", target.value)
            put("mode", mode.value)
            put("outcome", outcome.value)
            put("duration_bucket", duration.value)
            if (amount != null) put("amount_bucket", amount.value)
        }
    )

    class Failed(
        flow: StakingFlow,
        network: String,
        target: StakingAnalyticsTarget,
        mode: StakingMode?,
        unstakeMode: UnstakeMode?,
        reason: BittensorTransactionFailureReason,
        stage: StakingFailureStage,
        retryable: Boolean
    ) : AnalyticsEvent(
        name = when (flow) {
            StakingFlow.STAKE -> "staking_failed"
            StakingFlow.UNSTAKE -> "unstake_failed"
            StakingFlow.CLAIM -> "rewards_claim_failed"
        },
        properties = buildMap {
            put("staking_type", "subtensor")
            put("network", network)
            put("target", target.value)
            val operationMode = mode?.value ?: unstakeMode?.value
            if (operationMode != null) put("mode", operationMode)
            put("reason", reason.value)
            put("stage", stage.value)
            put("retryable", retryable)
        }
    )

    class Abandoned(stage: BittensorScreen, target: StakingAnalyticsTarget?) : AnalyticsEvent(
        name = "staking_abandoned",
        properties = buildMap {
            put("staking_type", "subtensor")
            put("stage", stage.value)
            if (target != null) put("target", target.value)
        }
    )

    class UnstakeInitiated(
        network: String,
        target: StakingAnalyticsTarget,
        mode: UnstakeMode,
        hotkeys: Int?,
        hasLocked: Boolean,
        slippage: SlippageBucket?,
        amount: AmountBucket?
    ) : AnalyticsEvent(
        name = "unstake_initiated",
        properties = buildMap {
            put("staking_type", "subtensor")
            put("network", network)
            put("target", target.value)
            put("mode", mode.value)
            if (hotkeys != null) put("hotkey_count_bucket", countBucket(hotkeys))
            put("has_locked", hasLocked)
            if (slippage != null) put("slippage_bucket", slippage.value)
            if (amount != null) put("amount_bucket", amount.value)
        }
    )

    class UnstakeCompleted(
        network: String,
        target: StakingAnalyticsTarget,
        mode: UnstakeMode,
        outcome: StakingCompletionOutcome,
        duration: DurationBucket,
        amount: AmountBucket?,
        hotkeys: Int?,
        hasLocked: Boolean,
        slippage: SlippageBucket?
    ) : AnalyticsEvent(
        name = "unstake_completed",
        properties = buildMap {
            put("staking_type", "subtensor")
            put("network", network)
            put("target", target.value)
            put("mode", mode.value)
            put("outcome", outcome.value)
            put("duration_bucket", duration.value)
            if (amount != null) put("amount_bucket", amount.value)
            if (hotkeys != null) put("hotkey_count_bucket", countBucket(hotkeys))
            put("has_locked", hasLocked)
            if (slippage != null) put("slippage_bucket", slippage.value)
        }
    )

    class ClaimInitiated(network: String, amount: AmountBucket?, validators: Int) : AnalyticsEvent(
        name = "rewards_claim_initiated",
        properties = buildMap {
            put("staking_type", "subtensor")
            put("network", network)
            put("target", "root")
            if (amount != null) put("amount_bucket", amount.value)
            put("validators_count_bucket", countBucket(validators))
        }
    )

    class ClaimCompleted(network: String, amount: AmountBucket?, duration: DurationBucket, claimedZero: Boolean?) : AnalyticsEvent(
        name = "rewards_claim_completed",
        properties = buildMap {
            put("staking_type", "subtensor")
            put("network", network)
            put("target", "root")
            if (amount != null) put("amount_bucket", amount.value)
            put("duration_bucket", duration.value)
            if (claimedZero != null) put("claimed_zero", claimedZero)
        }
    )

    class Blocked(
        flow: StakingFlow,
        reason: StakingBlockedReason,
        wallet: AnalyticsWalletType?,
        target: StakingAnalyticsTarget?
    ) : AnalyticsEvent(
        name = "staking_blocked",
        properties = buildMap {
            put("staking_type", "subtensor")
            put("flow", flow.value)
            put("reason", reason.value)
            if (wallet != null) put("wallet_type", wallet.value)
            if (target != null) put("target", target.value)
        }
    )

    class ValidationShown(
        flow: StakingFlow,
        kind: StakingValidationKind,
        stage: StakingValidationStage,
        outcome: StakingValidationOutcome,
        target: StakingAnalyticsTarget?
    ) : AnalyticsEvent(
        name = "staking_validation_shown",
        properties = buildMap {
            put("staking_type", "subtensor")
            put("flow", flow.value)
            put("kind", kind.value)
            put("stage", stage.value)
            put("outcome", outcome.value)
            if (target != null) put("target", target.value)
        }
    )

    class SlippageChanged(method: SlippageChangeMethod, slippage: SlippageBucket, warning: SlippageWarning) : AnalyticsEvent(
        name = "slippage_changed",
        properties = mapOf(
            "method" to method.value,
            "slippage_bucket" to slippage.value,
            "warning" to warning.value
        )
    )

    class SubnetListOpened(source: SubnetListSource, sort: SubnetSortKey, hideThinPools: Boolean) : AnalyticsEvent(
        name = "subnet_list_opened",
        properties = mapOf(
            "source" to source.value,
            "sort" to sort.value,
            "hide_thin_pools" to hideThinPools
        )
    )

    class SubnetSearch : AnalyticsEvent(
        name = "subnet_list_interacted",
        properties = mapOf(
            "action" to "search"
        )
    )

    class SubnetSort(sort: SubnetSortKey) : AnalyticsEvent(
        name = "subnet_list_interacted",
        properties = mapOf(
            "action" to "sort",
            "value" to sort.value
        )
    )

    class SubnetFilter(enabled: Boolean) : AnalyticsEvent(
        name = "subnet_list_interacted",
        properties = mapOf(
            "action" to "filter_thin_pools",
            "value" to enabled
        )
    )

    class SubnetFavorite(added: Boolean) : AnalyticsEvent(
        name = "subnet_list_interacted",
        properties = mapOf(
            "action" to if (added) "favorite_added" else "favorite_removed"
        )
    )

    class SubnetDetailsOpened(netuid: Int, source: SubnetDetailsSource, period: AnalyticsPricePeriod?) : AnalyticsEvent(
        name = "subnet_details_opened",
        properties = buildMap {
            put("netuid", netuid.toString())
            put("source", source.value)
            if (period != null) put("period", period.value)
        }
    )

    class ValidatorChanged(target: StakingAnalyticsTarget, source: ValidatorChangeSource, unusualApyFilter: Boolean) : AnalyticsEvent(
        name = "validator_changed",
        properties = mapOf(
            "target" to target.value,
            "source" to source.value,
            "unusual_apy_filter" to unusualApyFilter
        )
    )

    class PortfolioOpened(source: StakingPortfolioSource, positions: Int, hasRoot: Boolean, chart: StakingChartState?) : AnalyticsEvent(
        name = "staking_portfolio_opened",
        properties = buildMap {
            put("staking_type", "subtensor")
            put("source", source.value)
            put("positions_count_bucket", countBucket(positions))
            put("has_root", hasRoot)
            if (chart != null) put("chart_state", chart.value)
        }
    )

    class PositionOpened(
        target: StakingAnalyticsTarget,
        netuid: Int,
        hasHold: Boolean,
        hasLock: Boolean,
        rewards: StakingRewardsState
    ) : AnalyticsEvent(
        name = "staking_position_opened",
        properties = mapOf(
            "staking_type" to "subtensor",
            "target" to target.value,
            "netuid" to netuid.toString(),
            "has_hold" to hasHold,
            "has_lock" to hasLock,
            "rewards_state" to rewards.value
        )
    )

    class ResultAction(action: StakingResultAction, flow: StakingFlow, target: StakingAnalyticsTarget) : AnalyticsEvent(
        name = "staking_result_action",
        properties = mapOf(
            "staking_type" to "subtensor",
            "action" to action.value,
            "flow" to flow.value,
            "target" to target.value
        )
    )

    class DataFailed(
        endpoint: BittensorDataEndpoint,
        reason: BittensorDataFailureReason,
        screen: BittensorScreen,
        retryable: Boolean
    ) : AnalyticsEvent(
        name = "bittensor_data_failed",
        properties = mapOf(
            "endpoint" to endpoint.value,
            "reason" to reason.value,
            "screen" to screen.value,
            "retryable" to retryable
        )
    )

    class DataRetryTapped(endpoint: BittensorDataEndpoint, screen: BittensorScreen) : AnalyticsEvent(
        name = "bittensor_data_retry_tapped",
        properties = mapOf(
            "endpoint" to endpoint.value,
            "screen" to screen.value
        )
    )

    private fun countBucket(count: Int) = when {
        count <= 0 -> "0"
        count == 1 -> "1"
        count == 2 -> "2"
        else -> "3_plus"
    }
}

enum class BittensorTransactionFailureReason(val value: String) {
    USER_CANCELLED("user_cancelled"), NETWORK_ERROR("network_error"), NOT_SUBMITTED("not_submitted"), DISPATCH_OTHER("dispatch_other"),
    SLIPPAGE_EXCEEDED("slippage_exceeded"), ROOT_STAKE_LOCKED("root_stake_locked"), CHAIN_CONDITION("chain_condition"),
    TOO_HEAVY("too_heavy"), FUND_UNAVAILABLE("fund_unavailable"), BELOW_MINIMUM("below_minimum")
}
