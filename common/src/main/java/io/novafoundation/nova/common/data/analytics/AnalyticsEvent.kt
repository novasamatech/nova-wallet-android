package io.novafoundation.nova.common.data.analytics

sealed class AnalyticsEvent(
    val name: String,
    val properties: Map<String, Any>
) {

    // App lifecycle

    class AppOpened(isFirstLaunch: Boolean) : AnalyticsEvent(
        name = "app_opened",
        properties = mapOf(
            "is_first_launch" to isFirstLaunch,
            "\$ip" to false
        )
    )

    object SessionStarted : AnalyticsEvent(
        name = "session_started",
        properties = mapOf(
            "\$ip" to false
        )
    )

    class SessionEnded(durationBucket: DurationBucket) : AnalyticsEvent(
        name = "session_ended",
        properties = mapOf(
            "duration_bucket" to durationBucket.value,
            "\$ip" to false
        )
    )

    // Onboarding

    class OnboardingStarted(source: OnboardingSource) : AnalyticsEvent(
        name = "onboarding_started",
        properties = mapOf(
            "source" to source.value,
            "\$ip" to false
        )
    )

    class WalletCreationMethodSelected(method: WalletCreationMethod) : AnalyticsEvent(
        name = "wallet_creation_method_selected",
        properties = mapOf(
            "method" to method.value,
            "\$ip" to false
        )
    )

    class WalletCreationStarted(method: WalletCreationMethod) : AnalyticsEvent(
        name = "wallet_creation_started",
        properties = mapOf(
            "method" to method.value,
            "\$ip" to false
        )
    )

    class WalletCreationCompleted(
        method: WalletCreationMethod,
        durationBucket: DurationBucket? = null
    ) : AnalyticsEvent(
        name = "wallet_creation_completed",
        properties = buildMap {
            put("method", method.value)
            if (durationBucket != null) put("duration_bucket", durationBucket.value)
            put("\$ip", false)
        }
    )

    class WalletCreationAbandoned(lastStep: WalletCreationStep) : AnalyticsEvent(
        name = "wallet_creation_abandoned",
        properties = mapOf(
            "last_step" to lastStep.value,
            "\$ip" to false
        )
    )

    class FirstActionAfterWallet(action: FirstAction) : AnalyticsEvent(
        name = "first_action_after_wallet",
        properties = mapOf(
            "action" to action.value,
            "\$ip" to false
        )
    )

    // Features

    class FeatureOpened(featureId: FeatureId) : AnalyticsEvent(
        name = "feature_opened",
        properties = mapOf(
            "feature_id" to featureId.value,
            "\$ip" to false
        )
    )

    // Swap

    class SwapScreenOpened(source: SwapSource) : AnalyticsEvent(
        name = "swap_screen_opened",
        properties = mapOf(
            "source" to source.value,
            "\$ip" to false
        )
    )

    class SwapInitiated(
        source: SwapSource,
        assetInCategory: AssetCategory,
        assetOutCategory: AssetCategory,
        assetIn: String,
        assetOut: String,
        networkIn: String,
        networkOut: String,
        amountBucket: AmountBucket
    ) : AnalyticsEvent(
        name = "swap_initiated",
        properties = mapOf(
            "source" to source.value,
            "asset_in_category" to assetInCategory.value,
            "asset_out_category" to assetOutCategory.value,
            "asset_in" to assetIn,
            "asset_out" to assetOut,
            "network_in" to networkIn,
            "network_out" to networkOut,
            "amount_bucket" to amountBucket.value,
            "\$ip" to false
        )
    )

    class SwapConfirmed(
        amountBucket: AmountBucket,
        slippageBucket: SlippageBucket,
        assetIn: String,
        assetOut: String,
        networkIn: String,
        networkOut: String
    ) : AnalyticsEvent(
        name = "swap_confirmed",
        properties = mapOf(
            "amount_bucket" to amountBucket.value,
            "slippage_bucket" to slippageBucket.value,
            "asset_in" to assetIn,
            "asset_out" to assetOut,
            "network_in" to networkIn,
            "network_out" to networkOut,
            "\$ip" to false
        )
    )

    class SwapCompleted(
        amountBucket: AmountBucket,
        durationBucket: DurationBucket,
        assetIn: String,
        assetOut: String,
        networkIn: String,
        networkOut: String
    ) : AnalyticsEvent(
        name = "swap_completed",
        properties = mapOf(
            "amount_bucket" to amountBucket.value,
            "duration_bucket" to durationBucket.value,
            "asset_in" to assetIn,
            "asset_out" to assetOut,
            "network_in" to networkIn,
            "network_out" to networkOut,
            "\$ip" to false
        )
    )

    class SwapFailed(reason: SwapFailureReason) : AnalyticsEvent(
        name = "swap_failed",
        properties = mapOf(
            "reason" to reason.value,
            "\$ip" to false
        )
    )

    class SwapAbandoned(stage: SwapStage) : AnalyticsEvent(
        name = "swap_abandoned",
        properties = mapOf(
            "stage" to stage.value,
            "\$ip" to false
        )
    )

    // Staking

    class StakingFlowOpened(network: String, source: String) : AnalyticsEvent(
        name = "staking_flow_opened",
        properties = mapOf(
            "network" to network,
            "source" to source,
            "\$ip" to false
        )
    )

    class StakingTypeSelected(stakingType: String, network: String) : AnalyticsEvent(
        name = "staking_type_selected",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "\$ip" to false
        )
    )

    class StakingInitiated(stakingType: String, network: String, amountBucket: AmountBucket) : AnalyticsEvent(
        name = "staking_initiated",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "amount_bucket" to amountBucket.value,
            "\$ip" to false
        )
    )

    class StakingConfirmed(stakingType: String, network: String, amountBucket: AmountBucket) : AnalyticsEvent(
        name = "staking_confirmed",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "amount_bucket" to amountBucket.value,
            "\$ip" to false
        )
    )

    class StakingCompleted(stakingType: String, network: String, amountBucket: AmountBucket) : AnalyticsEvent(
        name = "staking_completed",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "amount_bucket" to amountBucket.value,
            "\$ip" to false
        )
    )

    class StakingFailed(stakingType: String, network: String, reason: String) : AnalyticsEvent(
        name = "staking_failed",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "reason" to reason,
            "\$ip" to false
        )
    )

    class UnstakeInitiated(stakingType: String, network: String, amountBucket: AmountBucket) : AnalyticsEvent(
        name = "unstake_initiated",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "amount_bucket" to amountBucket.value,
            "\$ip" to false
        )
    )

    class UnstakeCompleted(stakingType: String, network: String, amountBucket: AmountBucket) : AnalyticsEvent(
        name = "unstake_completed",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "amount_bucket" to amountBucket.value,
            "\$ip" to false
        )
    )

    class UnstakeFailed(stakingType: String, network: String, reason: String) : AnalyticsEvent(
        name = "unstake_failed",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "reason" to reason,
            "\$ip" to false
        )
    )

    // Transfers

    class SendInitiated(
        asset: String,
        network: String,
        destinationNetwork: String? = null,
        assetCategory: AssetCategory,
        amountBucket: AmountBucket,
        isCrossChain: Boolean
    ) : AnalyticsEvent(
        name = "send_initiated",
        properties = buildMap {
            put("asset", asset)
            put("network", network)
            if (destinationNetwork != null) put("destination_network", destinationNetwork)
            put("asset_category", assetCategory.value)
            put("amount_bucket", amountBucket.value)
            put("is_cross_chain", isCrossChain)
            put("\$ip", false)
        }
    )

    class SendCompleted(asset: String, network: String, amountBucket: AmountBucket, destinationNetwork: String? = null) : AnalyticsEvent(
        name = "send_completed",
        properties = buildMap {
            put("asset", asset)
            put("network", network)
            put("amount_bucket", amountBucket.value)
            if (destinationNetwork != null) put("destination_network", destinationNetwork)
            put("\$ip", false)
        }
    )

    class SendFailed(asset: String, network: String, reason: String, destinationNetwork: String? = null) : AnalyticsEvent(
        name = "send_failed",
        properties = buildMap {
            put("asset", asset)
            put("network", network)
            put("reason", reason)
            if (destinationNetwork != null) put("destination_network", destinationNetwork)
            put("\$ip", false)
        }
    )

    // DApp

    class DappOpened(dappHost: String, source: String, isKnownDapp: Boolean) : AnalyticsEvent(
        name = "dapp_opened",
        properties = mapOf(
            "dapp_host" to dappHost,
            "source" to source,
            "is_known_dapp" to isKnownDapp,
            "\$ip" to false
        )
    )

    // Governance

    class GovernanceVoteCast(
        voteDirection: String,
        network: String,
        amountBucket: AmountBucket,
        convictionLevel: String
    ) : AnalyticsEvent(
        name = "governance_vote_cast",
        properties = mapOf(
            "vote_direction" to voteDirection,
            "network" to network,
            "amount_bucket" to amountBucket.value,
            "conviction_level" to convictionLevel,
            "\$ip" to false
        )
    )

    // General

    class TabSwitched(tab: String) : AnalyticsEvent(
        name = "tab_switched",
        properties = mapOf(
            "tab" to tab,
            "\$ip" to false
        )
    )

    class BuyInitiated(provider: String, asset: String, network: String) : AnalyticsEvent(
        name = "buy_initiated",
        properties = mapOf(
            "provider" to provider,
            "asset" to asset,
            "network" to network,
            "\$ip" to false
        )
    )

    // Banner

    class BannerClicked(bannerId: String, bannerTitle: String, screen: String) : AnalyticsEvent(
        name = "banner_clicked",
        properties = mapOf(
            "banner_id" to bannerId,
            "banner_title" to bannerTitle,
            "screen" to screen,
            "\$ip" to false
        )
    )

    // Nova Card

    object NovaCardOpened : AnalyticsEvent(
        name = "nova_card_opened",
        properties = mapOf(
            "\$ip" to false
        )
    )

    // NFT

    class NftSectionOpened(nftCount: Int) : AnalyticsEvent(
        name = "nft_section_opened",
        properties = mapOf(
            "nft_count" to nftCount,
            "\$ip" to false
        )
    )
}

// Supporting Enums

enum class AssetCategory(val value: String) {
    NATIVE_TOKEN("native_token"),
    STABLECOIN("stablecoin"),
    WRAPPED_TOKEN("wrapped_token"),
    OTHER("other")
}

enum class WalletCreationMethod(val value: String) {
    CREATE("create"),
    IMPORT_MNEMONIC("import_mnemonic"),
    IMPORT_SEED("import_seed"),
    IMPORT_JSON("import_json"),
    IMPORT_LEDGER("import_ledger"),
    IMPORT_PARITY_SIGNER("import_parity_signer"),
    IMPORT_POLKADOT_VAULT("import_polkadot_vault"),
    IMPORT_WATCH_ONLY("import_watch_only"),
    CLOUD_BACKUP("cloud_backup")
}

enum class SwapSource(val value: String) {
    ASSET_DETAILS("asset_details"),
    MAIN_SCREEN("main_screen"),
    DEEP_LINK("deep_link")
}

enum class SwapFailureReason(val value: String) {
    INSUFFICIENT_BALANCE("insufficient_balance"),
    SLIPPAGE_EXCEEDED("slippage_exceeded"),
    NETWORK_ERROR("network_error"),
    EXECUTION_REVERTED("execution_reverted"),
    USER_CANCELLED("user_cancelled"),
    SIGNING_UNAVAILABLE("signing_unavailable"),
    UNKNOWN("unknown")
}

enum class SwapStage(val value: String) {
    SETUP("setup"),
    CONFIRM("confirm"),
    EXECUTING("executing")
}

enum class FeatureId(val value: String) {
    STAKING("staking"),
    GOVERNANCE("governance"),
    CROWDLOANS("crowdloans"),
    DAPPS("dapps"),
    NFT("nft"),
    SWAP("swap"),
    BUY("buy"),
    SEND("send"),
    RECEIVE("receive"),
    SETTINGS("settings")
}

enum class OnboardingSource(val value: String) {
    FRESH_INSTALL("fresh_install"),
    ADD_WALLET("add_wallet")
}

enum class FirstAction(val value: String) {
    SWAP("swap"),
    SEND("send"),
    RECEIVE("receive"),
    STAKING("staking"),
    DAPP("dapp"),
    BUY("buy"),
    OTHER("other")
}

enum class WalletCreationStep(val value: String) {
    WELCOME("welcome"),
    BACKUP("backup"),
    CONFIRM_MNEMONIC("confirm_mnemonic"),
    PIN_SETUP("pin_setup"),
    NETWORK_SELECTION("network_selection"),
    SEED_ENTRY("seed_entry"),
    JSON_UPLOAD("json_upload"),
    LEDGER_CONNECT("ledger_connect"),
    OTHER("other")
}
