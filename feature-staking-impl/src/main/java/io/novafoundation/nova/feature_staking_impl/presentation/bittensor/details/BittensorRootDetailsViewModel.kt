package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.StakingRewardsState
import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.analytics.AnalyticsService

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.utils.flowOf as flowOfSuspend
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootValidatorSource
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPositionWithdrawalInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorWithdrawalRequest
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount.BittensorStakeAmountPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim.BittensorRootClaimPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorWalletGate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorSlippageSettings
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorYieldColor
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitPositionPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.BittensorValidatorInfoPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class BittensorRootDetailsViewModel(
    private val analytics: AnalyticsService,
    private val bittensorPortfolioInteractor: BittensorPortfolioInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val assetIconProvider: AssetIconProvider,
    private val iconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val amountFormatter: BittensorAmountFormatter,
    private val retryController: BittensorRetryController,
    private val rootClaimInteractor: BittensorRootClaimInteractor,
    private val accountRepository: AccountRepository,
    private val withdrawalInteractor: BittensorPositionWithdrawalInteractor,
    slippageSettings: BittensorSlippageSettings,
    private val rootValidatorSource: BittensorRootValidatorSource,
    private val rootHoldFormatter: BittensorRootHoldFormatter,
    private val debugSettings: io.novafoundation.nova.common.data.storage.DebugSettings,
    val walletGate: BittensorWalletGate
) : BaseViewModel(), ExternalActions by externalActions {

    private var isOpenedTracked = false

    private val rootOverviewReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.ROOT_OVERVIEW, BittensorScreen.POSITION)
    private val claimPreviewsReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.CLAIM_PREVIEWS, BittensorScreen.POSITION)
    private val chainRpcReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.CHAIN_RPC, BittensorScreen.POSITION)

    val retryEvents = retryController.events

    private val portfolioState = retryController.observe(
        step = "root positions",
        analytics = rootOverviewReads,
        transform = { it.withSafeLoading() },
        source = bittensorPortfolioInteractor::observeRootPortfolio
    )
        .shareInBackground()

    private val portfolio = portfolioState.filterLoaded()
        .shareInBackground()

    val isLoading = portfolioState.map { it.isLoading() || it.dataOrNull?.dataStatus is BittensorDataStatus.Loading }
        .shareInBackground()

    private val selectedMetaId = accountRepository.selectedMetaAccountFlow()
        .map { it.id }
        .distinctUntilChanged()

    private val claimState = selectedMetaId.flatMapLatest { metaId ->
        retryController.observe(
            step = "claimable root rewards",
            analytics = claimPreviewsReads,
            transform = { it.withSafeLoading() }
        ) {
            rootClaimInteractor.observe(metaId)
        }
    }
        .shareInBackground(SharingStarted.WhileSubscribed(5_000))

    val redeemableRewards = claimState.mapLoading { claim ->
        val tokenAmount = claim.asset.token.amountFromPlanks(claim.claims.totalRedeemable)
        amountFormatter.amount(
            tokenAmount,
            claim.asset.token.configuration.symbol,
            claim.asset.token.toFiatOrNull(tokenAmount),
            claim.asset.token.currency
        )
    }
        .shareInBackground(SharingStarted.WhileSubscribed(5_000))

    val canClaim = combine(claimState, debugSettings.observeBittensorClaimButton()) { state, debugEnabled ->
        state.dataOrNull?.let {
            val hasPreview = debugEnabled && it.copy(previewOnly = true).selectedPreview != null
            it.dataStatus is BittensorDataStatus.Fresh && (it.availableClaims.isNotEmpty() || hasPreview)
        } == true
    }
        .shareInBackground(SharingStarted.WhileSubscribed(5_000))

    // As on iOS, the actions are never disabled by the wallet type: an unsupported wallet gets the sheet on tap.
    val canAddStake = portfolioState.map { portfolio ->
        portfolio.dataOrNull?.dataStatus is BittensorDataStatus.Fresh
    }
        .shareInBackground()

    private val withdrawalState = combine(portfolioState, slippageSettings.observe()) { state, slippage ->
        val data = state.dataOrNull
        val primary = data?.positions?.root.primaryRootPosition()
        if (data == null || data.dataStatus !is BittensorDataStatus.Fresh || primary == null) {
            null
        } else {
            BittensorWithdrawalRequest(data.metaId, data.chain.id, BittensorNetuid.Root, primary.validatorHotkey, slippage)
        }
    }.distinctUntilChanged().flatMapLatest { request ->
        retryController.observe(
            step = "root withdrawal availability",
            analytics = chainRpcReads,
            transform = { it.withSafeLoading() }
        ) {
            if (request == null) flowOf(null) else withdrawalInteractor.observe(request)
        }
    }.shareInBackground()

    val rootHold = combine(withdrawalState, claimState) { state, claims ->
        val withdrawal = state.dataOrNull
        if (!isOpenedTracked && withdrawal != null && !claims.isLoading()) {
            val claim = claims.dataOrNull
            val rewards = when {
                claim == null -> StakingRewardsState.UNAVAILABLE
                claim.availableClaims.isNotEmpty() -> StakingRewardsState.CLAIMABLE
                claim.claims.totalRedeemable.isPositive() -> StakingRewardsState.BELOW_MINIMUM
                else -> StakingRewardsState.NONE
            }
            isOpenedTracked = true
            analytics.track(
                BittensorAnalyticsEvent.PositionOpened(
                    StakingAnalyticsTarget.ROOT,
                    0,
                    withdrawal.holdDurationMillis.isPositive(),
                    withdrawal.locked.isPositive(),
                    rewards
                )
            )
        }
        state.dataOrNull?.rootHold?.let(rootHoldFormatter::format)
    }
        .shareInBackground()

    val canUnstake = combine(canAddStake, withdrawalState) { allowed, state -> allowed && state.dataOrNull?.canWithdraw == true }
        .shareInBackground()

    val rootStake = portfolio.map { portfolio ->
        val stake = portfolio.positions?.rootStake ?: return@map AmountModel("—", null)
        val token = portfolio.asset.token
        val amount = token.amountFromPlanks(stake)
        amountFormatter.amount(
            amount,
            token.configuration.symbol,
            token.toFiatOrNull(amount),
            token.currency
        )
    }
        .shareInBackground()

    val assetIcon = portfolio.map { assetIconProvider.getAssetIconOrFallback(it.asset.token.configuration) }
        .shareInBackground()

    val isActive = portfolio.map { it.positions?.root.primaryRootRegistration() == true }
        .shareInBackground()

    private val validatorDetailsState = portfolio.map { it.chain }
        .distinctUntilChanged()
        .flatMapLatest { chain ->
            flowOfSuspend { rootValidatorSource.validatorDetails(chain) }
                .withSafeLoading()
        }
        .shareInBackground()

    val validators = combine(
        portfolio,
        validatorDetailsState
    ) { portfolio, details ->
        portfolio.positions?.root.orEmpty().map { position ->
            val address = portfolio.chain.addressOf(position.validatorHotkey.value)
            val validator = details.dataOrNull?.get(position.validatorHotkey)
            val rate = validator?.annualPercentageYield
            BittensorPositionUi(
                id = address,
                title = validator?.validator?.name?.takeIf { it.isNotBlank() } ?: address.ellipsizeAddress(),
                subtitle = resources.getString(R.string.bittensor_validator),
                amount = AmountModel(
                    rate?.formatAsPercentage() ?: resources.getString(R.string.common_not_available),
                    resources.getString(R.string.bittensor_estimated_apy)
                ),
                icon = Icon.FromDrawable(iconGenerator.createAddressIcon(position.validatorHotkey.value, 32)),
                action = BittensorPositionAction.ValidatorDetails(address),
                iconSizeDp = 32,
                isAmountLoading = details.isLoading(),
                amountColorRes = bittensorYieldColor(rate)
            )
        }
    }
        .shareInBackground()

    val warning = combine(portfolioState, claimState) { portfolio, claim ->
        when {
            portfolio.dataOrNull?.dataStatus is BittensorDataStatus.Stale -> resources.getString(R.string.bittensor_positions_stale)
            claim.dataOrNull?.dataStatus is BittensorDataStatus.Stale -> resources.getString(R.string.bittensor_claim_stale)
            else -> null
        }
    }
        .shareInBackground()

    fun claimClicked() = launchUnit {
        if (!canClaim.first()) return@launchUnit
        val claim = claimState.first().dataOrNull ?: return@launchUnit
        walletGate.checkingSupport(claim.metaId, StakingFlow.CLAIM, StakingAnalyticsTarget.ROOT) {
            router.openBittensorRootClaim(BittensorRootClaimPayload(claim.metaId, previewOnly = debugSettings.bittensorClaimEnabled()))
        }
    }

    fun unstakeRoot() = launchUnit {
        if (!canUnstake.first()) return@launchUnit
        val data = portfolio.first()
        val primary = data.positions?.root.orEmpty()
            .filter { it.stakeInPlanks.isPositive() }
            .sortedBy { it.validatorHotkey.value.toHexString() }
            .sortedByDescending { it.stakeInPlanks }
            .firstOrNull() ?: return@launchUnit
        val address = data.chain.addressOf(primary.validatorHotkey.value)
        selectExit(BittensorExitPositionPayload(data.metaId, 0, address))
    }

    private fun selectExit(position: BittensorExitPositionPayload) = launchUnit {
        val data = portfolio.first()
        if (!canUnstake.first() || data.metaId != position.metaId) return@launchUnit

        val containsPosition = data.positions?.root.orEmpty().any {
            data.chain.addressOf(it.validatorHotkey.value) == position.hotkeyAddress && it.stakeInPlanks.isPositive()
        }

        if (containsPosition) {
            walletGate.checkingSupport(position.metaId, StakingFlow.UNSTAKE, StakingAnalyticsTarget.ROOT) {
                router.openBittensorExitAmount(position)
            }
        }
    }

    fun addStake() = launchUnit {
        if (!canAddStake.first()) return@launchUnit
        val currentPortfolio = portfolio.first()
        val payload = rootStakeAmountPayload(currentPortfolio)
        walletGate.checkingSupport(currentPortfolio.metaId, StakingFlow.STAKE, StakingAnalyticsTarget.ROOT) {
            router.openBittensorRootStaking(payload)
        }
    }

    fun back() = router.back()

    fun positionClicked(position: BittensorPositionUi) {
        val action = position.action as? BittensorPositionAction.ValidatorDetails ?: return
        router.openBittensorValidatorInfo(BittensorValidatorInfoPayload(BittensorNetuid.Root.value, action.address))
    }
}

internal fun List<BittensorRootPosition>?.primaryRootRegistration(): Boolean? {
    return primaryRootPosition()?.isRegistered
}

internal fun rootStakeAmountPayload(portfolio: BittensorPortfolio): BittensorStakeAmountPayload {
    val primary = portfolio.positions?.root.primaryRootPosition()
    val address = primary?.let { portfolio.chain.addressOf(it.validatorHotkey.value) }
    return BittensorStakeAmountPayload(portfolio.metaId, address)
}

internal fun List<BittensorRootPosition>?.primaryRootPosition(): BittensorRootPosition? {
    return this.orEmpty()
        .sortedBy { it.validatorHotkey.value.toHexString() }
        .maxByOrNull { it.stakeInPlanks }
}
