package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim.BittensorRootClaimPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitPositionPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

data class BittensorExitChoice(val payload: BittensorExitPositionPayload, val address: String, val amount: AmountModel)

class BittensorRootDetailsViewModel(
    private val bittensorPortfolioInteractor: BittensorPortfolioInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val assetIconProvider: AssetIconProvider,
    private val iconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val amountFormatter: BittensorAmountFormatter,
    private val retryController: BittensorRetryController,
    private val rootClaimInteractor: BittensorRootClaimInteractor,
    private val accountRepository: AccountRepository
) : BaseViewModel(), ExternalActions by externalActions {

    val retryEvents = retryController.events

    private val portfolioState = retryController.observe(
        step = "root positions",
        transform = { it.withSafeLoading() },
        source = bittensorPortfolioInteractor::observeRootPortfolio
    )
        .shareInBackground()

    private val portfolio = portfolioState.filterLoaded()
        .shareInBackground()

    val isLoading = portfolioState.map { it.isLoading() || it.dataOrNull?.dataStatus is BittensorDataStatus.Loading }
        .shareInBackground()

    private val claimState = retryController.observe(
        step = "claimable root rewards",
        transform = { it.withSafeLoading() }
    ) {
        accountRepository.selectedMetaAccountFlow().map { it.id }
            .distinctUntilChanged().flatMapLatest { metaId -> flowOf { rootClaimInteractor.load(metaId) } }
    }
        .shareInBackground(SharingStarted.WhileSubscribed(5_000))

    val receivedRootRewards = retryController.observe(
        step = "received root rewards",
        transform = { source ->
            source.onStart { emit("—") }
                .catch { emit("—") }
        }
    ) {
        accountRepository.selectedMetaAccountFlow().map { it.id }
            .distinctUntilChanged().flatMapLatest {
                flowOf {
                    val (asset, amount) = rootClaimInteractor.receivedRewards(it)
                    val token = asset.token
                    amountFormatter.amount(
                        amount,
                        token.configuration.symbol,
                        token.toFiatOrNull(amount),
                        token.currency
                    ).token
                }
                    .onStart { emit("—") }
            }
    }
        .shareInBackground(SharingStarted.WhileSubscribed(5_000))

    val claimableRewards = claimState.map { state ->
        state.dataOrNull?.let { claim ->
            val amount = claim.claims.claimable.fold(BigInteger.ZERO) { sum, preview -> sum + preview.redeemable }
            val tokenAmount = claim.asset.token.amountFromPlanks(amount)
            amountFormatter.amount(
                tokenAmount,
                claim.asset.token.configuration.symbol,
                claim.asset.token.toFiatOrNull(tokenAmount),
                claim.asset.token.currency
            )
        } ?: AmountModel("—", null)
    }
        .shareInBackground(SharingStarted.WhileSubscribed(5_000))

    val canClaim = claimState.map { state ->
        state.dataOrNull?.let { it.supportsImmediateOperations && it.claims.claimable.isNotEmpty() } == true
    }
        .shareInBackground(SharingStarted.WhileSubscribed(5_000))

    fun claimClicked() = launchUnit {
        if (!canClaim.first()) return@launchUnit
        val claim = claimState.first().dataOrNull ?: return@launchUnit
        router.openBittensorRootClaim(BittensorRootClaimPayload(claim.metaId))
    }

    val exitChoices = MutableLiveData<Event<List<BittensorExitChoice>>>()

    fun unstakeRoot() = launchUnit {
        if (!canAddStake.first()) return@launchUnit
        val data = portfolio.first()
        val choices = data.positions?.root.orEmpty().filter { it.stakeInPlanks.isPositive() }
            .map {
                val address = data.chain.addressOf(it.validatorHotkey.value)
                BittensorExitChoice(BittensorExitPositionPayload(data.metaId, 0, address), address, formatExitAmount(it.stakeInPlanks, data))
            }
        if (choices.size == 1) selectExit(choices.single()) else if (choices.isNotEmpty()) exitChoices.value = choices.event()
    }

    fun selectExit(choice: BittensorExitChoice) = launchUnit {
        val data = portfolio.first()
        if (!canAddStake.first() || data.metaId != choice.payload.metaId) return@launchUnit

        val containsPosition = data.positions?.root.orEmpty().any {
            data.chain.addressOf(it.validatorHotkey.value) == choice.address && it.stakeInPlanks.isPositive()
        }

        if (containsPosition) {
            router.openBittensorExitAmount(choice.payload)
        }
    }

    private fun formatExitAmount(planks: BigInteger, data: BittensorPortfolio): AmountModel {
        val token = data.asset.token
        val amount = token.amountFromPlanks(planks)
        return amountFormatter.amount(
            amount,
            token.configuration.symbol,
            token.toFiatOrNull(amount),
            token.currency
        )
    }

    val canAddStake = portfolioState.map { portfolio ->
        val data = portfolio.dataOrNull
        data?.supportsImmediateOperations == true && data.dataStatus is BittensorDataStatus.Fresh
    }
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

    val hasRootStake = portfolio.map { it.positions?.rootStake?.isPositive() == true }
        .shareInBackground()

    val validators = portfolio.map { portfolio ->
        portfolio.positions?.root.orEmpty().map { position ->
            val address = portfolio.chain.addressOf(position.validatorHotkey.value)
            BittensorPositionUi(
                id = address,
                title = address.ellipsizeAddress(),
                subtitle = resources.getString(R.string.bittensor_validator),
                amount = AmountModel("—", resources.getString(R.string.bittensor_validator_apr_period)),
                icon = Icon.FromDrawable(iconGenerator.createAddressIcon(position.validatorHotkey.value, 32)),
                action = BittensorPositionAction.ValidatorDetails(address),
                iconSizeDp = 32
            )
        }
    }
        .shareInBackground()

    val warning = portfolio.map {
        if (it.dataStatus is BittensorDataStatus.Stale) resources.getString(R.string.bittensor_positions_stale) else null
    }
        .shareInBackground()

    fun addStake() = router.openBittensorRootStaking()

    fun back() = router.back()

    fun positionClicked(position: BittensorPositionUi) = launchUnit {
        val action = position.action as? BittensorPositionAction.ValidatorDetails ?: return@launchUnit
        externalActions.showAddressActions(
            action.address,
            portfolio.first().chain
        )
    }
}
