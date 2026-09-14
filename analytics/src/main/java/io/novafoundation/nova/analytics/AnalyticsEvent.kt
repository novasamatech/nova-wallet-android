package io.novafoundation.nova.analytics

sealed class AnalyticsEvent(
    val name: String,
    val properties: Map<String, Any>
) {

    // App lifecycle
    class AppOpened(isFirstLaunch: Boolean) : AnalyticsEvent(
        name = "app_opened",
        properties = mapOf(
            "is_first_launch" to isFirstLaunch
        )
    )

    object SessionStarted : AnalyticsEvent(
        name = "session_started",
        properties = mapOf()
    )

    class SessionEnded(durationBucket: DurationBucket) : AnalyticsEvent(
        name = "session_ended",
        properties = mapOf(
            "duration_bucket" to durationBucket.value
        )
    )

    // Onboarding
    class OnboardingStarted(source: OnboardingSource) : AnalyticsEvent(
        name = "onboarding_started",
        properties = mapOf(
            "source" to source.value
        )
    )

    class WalletImportMethodSelected(method: WalletCreationMethod) : AnalyticsEvent(
        name = "wallet_import_method_selected",
        properties = mapOf(
            "method" to method.value
        )
    )

    object WalletCreationStarted : AnalyticsEvent(
        name = "wallet_creation_started",
        properties = mapOf()
    )

    class WalletCreationCompleted(
        method: WalletCreationMethod,
        durationBucket: DurationBucket? = null
    ) : AnalyticsEvent(
        name = "wallet_creation_completed",
        properties = buildMap {
            put("method", method.value)
            if (durationBucket != null) put("duration_bucket", durationBucket.value)
        }
    )

    class WalletCreationAbandoned(lastStep: WalletCreationStep) : AnalyticsEvent(
        name = "wallet_creation_abandoned",
        properties = mapOf(
            "last_step" to lastStep.value
        )
    )

    // Features

    class FeatureOpened(featureId: FeatureId) : AnalyticsEvent(
        name = "feature_opened",
        properties = mapOf(
            "feature_id" to featureId.value
        )
    )

    // Swap

    class SwapScreenOpened(source: SwapSource) : AnalyticsEvent(
        name = "swap_screen_opened",
        properties = mapOf(
            "source" to source.value
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
            "amount_bucket" to amountBucket.value
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
            "network_out" to networkOut
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
            "network_out" to networkOut
        )
    )

    class SwapFailed(reason: SwapFailureReason) : AnalyticsEvent(
        name = "swap_failed",
        properties = mapOf(
            "reason" to reason.value
        )
    )

    class SwapAbandoned(stage: SwapStage) : AnalyticsEvent(
        name = "swap_abandoned",
        properties = mapOf(
            "stage" to stage.value
        )
    )

    // Staking

    class StakingFlowOpened(network: String, source: String) : AnalyticsEvent(
        name = "staking_flow_opened",
        properties = mapOf(
            "network" to network,
            "source" to source
        )
    )

    class StakingTypeSelected(stakingType: String, network: String) : AnalyticsEvent(
        name = "staking_type_selected",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network
        )
    )

    class StakingInitiated(stakingType: String, network: String, amountBucket: AmountBucket) : AnalyticsEvent(
        name = "staking_initiated",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "amount_bucket" to amountBucket.value
        )
    )

    class StakingConfirmed(stakingType: String, network: String, amountBucket: AmountBucket) : AnalyticsEvent(
        name = "staking_confirmed",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "amount_bucket" to amountBucket.value
        )
    )

    class StakingCompleted(stakingType: String, network: String, amountBucket: AmountBucket) : AnalyticsEvent(
        name = "staking_completed",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "amount_bucket" to amountBucket.value
        )
    )

    class StakingFailed(stakingType: String, network: String, reason: String) : AnalyticsEvent(
        name = "staking_failed",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "reason" to reason
        )
    )

    class StakingAbandoned(stage: StakingStage) : AnalyticsEvent(
        name = "staking_abandoned",
        properties = mapOf(
            "stage" to stage.value
        )
    )

    class UnstakeInitiated(stakingType: String, network: String, amountBucket: AmountBucket) : AnalyticsEvent(
        name = "unstake_initiated",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "amount_bucket" to amountBucket.value
        )
    )

    class UnstakeCompleted(stakingType: String, network: String, amountBucket: AmountBucket) : AnalyticsEvent(
        name = "unstake_completed",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "amount_bucket" to amountBucket.value
        )
    )

    class UnstakeFailed(stakingType: String, network: String, reason: String) : AnalyticsEvent(
        name = "unstake_failed",
        properties = mapOf(
            "staking_type" to stakingType,
            "network" to network,
            "reason" to reason
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
        }
    )

    class SendCompleted(asset: String, network: String, amountBucket: AmountBucket, destinationNetwork: String? = null) : AnalyticsEvent(
        name = "send_completed",
        properties = buildMap {
            put("asset", asset)
            put("network", network)
            put("amount_bucket", amountBucket.value)
            if (destinationNetwork != null) put("destination_network", destinationNetwork)
        }
    )

    class SendFailed(asset: String, network: String, reason: String, destinationNetwork: String? = null) : AnalyticsEvent(
        name = "send_failed",
        properties = buildMap {
            put("asset", asset)
            put("network", network)
            put("reason", reason)
            if (destinationNetwork != null) put("destination_network", destinationNetwork)
        }
    )

    // DApp

    class DappOpened(dappHost: String, source: String, isKnownDapp: Boolean) : AnalyticsEvent(
        name = "dapp_opened",
        properties = mapOf(
            "dapp_host" to dappHost,
            "source" to source,
            "is_known_dapp" to isKnownDapp
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
            "conviction_level" to convictionLevel
        )
    )

    // General

    class TabSwitched(tab: String) : AnalyticsEvent(
        name = "tab_switched",
        properties = mapOf(
            "tab" to tab
        )
    )

    class BuyInitiated(provider: String, asset: String, network: String) : AnalyticsEvent(
        name = "buy_initiated",
        properties = mapOf(
            "provider" to provider,
            "asset" to asset,
            "network" to network
        )
    )

    class BuyCompleted(provider: String, asset: String, network: String) : AnalyticsEvent(
        name = "buy_completed",
        properties = mapOf(
            "provider" to provider,
            "asset" to asset,
            "network" to network
        )
    )

    class SellInitiated(provider: String, asset: String, network: String) : AnalyticsEvent(
        name = "sell_initiated",
        properties = mapOf(
            "provider" to provider,
            "asset" to asset,
            "network" to network
        )
    )

    class SellCompleted(provider: String, asset: String, network: String) : AnalyticsEvent(
        name = "sell_completed",
        properties = mapOf(
            "provider" to provider,
            "asset" to asset,
            "network" to network
        )
    )

    // Banner

    class BannerClicked(bannerId: String, bannerTitle: String, screen: String) : AnalyticsEvent(
        name = "banner_clicked",
        properties = mapOf(
            "banner_id" to bannerId,
            "banner_title" to bannerTitle,
            "screen" to screen
        )
    )

    // Nova Card

    object NovaCardOpened : AnalyticsEvent(
        name = "nova_card_opened",
        properties = mapOf()
    )

    // NFT

    class NftSectionOpened(nftCount: Int) : AnalyticsEvent(
        name = "nft_section_opened",
        properties = mapOf(
            "nft_count" to nftCount
        )
    )

    // Signing

    class SignRequestShown(source: SignSource, method: String, chain: String) : AnalyticsEvent(
        name = "sign_request_shown",
        properties = mapOf(
            "source" to source.value,
            "method" to method,
            "chain" to chain
        )
    )

    class SignApproved(source: SignSource, method: String, chain: String) : AnalyticsEvent(
        name = "sign_approved",
        properties = mapOf(
            "source" to source.value,
            "method" to method,
            "chain" to chain
        )
    )

    class SignRejected(source: SignSource, method: String, chain: String) : AnalyticsEvent(
        name = "sign_rejected",
        properties = mapOf(
            "source" to source.value,
            "method" to method,
            "chain" to chain
        )
    )

    class SignFailed(source: SignSource, method: String, chain: String, reason: String) : AnalyticsEvent(
        name = "sign_failed",
        properties = mapOf(
            "source" to source.value,
            "method" to method,
            "chain" to chain,
            "reason" to reason
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
    OPERATION_DETAILS("operation_details"),
    RETRY("retry")
}

enum class SwapFailureReason(val value: String) {
    NETWORK_ERROR("network_error"),
    EXECUTION_REVERTED("execution_reverted"),
    USER_CANCELLED("user_cancelled"),
    UNKNOWN("unknown")
}

enum class StakingStage(val value: String) {
    LANDING("landing"),
    SETUP("setup"),
    TYPE_SELECTION("type_selection"),
    CONFIRM("confirm")
}

enum class SwapStage(val value: String) {
    SETUP("setup"),
    CONFIRM("confirm")
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

enum class WalletCreationStep(val value: String) {
    WELCOME("welcome"),
    BACKUP("backup"),
    CONFIRM_MNEMONIC("confirm_mnemonic"),
    PIN_SETUP("pin_setup"),
    SEED_ENTRY("seed_entry"),
    JSON_UPLOAD("json_upload"),
    LEDGER_CONNECT("ledger_connect"),
    OTHER("other")
}

enum class SignSource(val value: String) {
    DAPP_BROWSER("dapp_browser"),
    WALLET_CONNECT("walletconnect")
}
