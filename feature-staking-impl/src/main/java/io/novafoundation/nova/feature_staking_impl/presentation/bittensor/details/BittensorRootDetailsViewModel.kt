package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class BittensorRootDetailsViewModel(
    private val bittensorPortfolioInteractor: BittensorPortfolioInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val assetIconProvider: AssetIconProvider,
    private val iconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val amountFormatter: BittensorAmountFormatter,
    private val retryController: BittensorRetryController
) : BaseViewModel(), ExternalActions by externalActions {

    val retryEvents = retryController.events

    private val portfolioState = retryController.observe(
        step = "root positions",
        transform = { it.withSafeLoading() },
        source = bittensorPortfolioInteractor::observeRootPortfolio
    )
        .shareInBackground()

    private val portfolio = portfolioState.filterLoaded().shareInBackground()

    val isLoading = portfolioState.map { it.isLoading() || it.dataOrNull?.dataStatus is BittensorDataStatus.Loading }.shareInBackground()

    val canAddStake = portfolioState.map { it.dataOrNull?.dataStatus is BittensorDataStatus.Fresh }.shareInBackground()

    val rootStake = portfolio.map { portfolio ->
        val stake = portfolio.positions?.rootStake ?: return@map AmountModel("—", null)
        val token = portfolio.asset.token
        val amount = token.amountFromPlanks(stake)
        amountFormatter.amount(amount, token.configuration.symbol, token.toFiatOrNull(amount), token.currency)
    }.shareInBackground()

    val assetIcon = portfolio.map { assetIconProvider.getAssetIconOrFallback(it.asset.token.configuration) }.shareInBackground()

    val hasRootStake = portfolio.map { it.positions?.rootStake?.isPositive() == true }.shareInBackground()

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
    }.shareInBackground()

    val warning = portfolio.map {
        if (it.dataStatus is BittensorDataStatus.Stale) resources.getString(R.string.bittensor_positions_stale) else null
    }.shareInBackground()

    fun addStake() = router.openBittensorRootStaking()

    fun back() = router.back()

    fun positionClicked(position: BittensorPositionUi) = launchUnit {
        val action = position.action as? BittensorPositionAction.ValidatorDetails ?: return@launchUnit
        externalActions.showAddressActions(action.address, portfolio.first().chain)
    }
}
