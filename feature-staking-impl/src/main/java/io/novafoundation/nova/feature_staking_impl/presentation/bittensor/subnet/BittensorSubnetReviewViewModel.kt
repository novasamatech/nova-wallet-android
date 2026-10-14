package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.onError
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_wallet_api.domain.model.PricedAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetView
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetsView
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakeDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class BittensorSubnetReviewViewModel(
    private val subnetId: BittensorSubnetId,
    private val totalInPlanks: BigInteger,
    private val metaId: Long,
    private val validatorAddress: String,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val router: StakingRouter,
    chainRegistry: ChainRegistry,
    walletUiUseCase: WalletUiUseCase,
    selectedAccountUseCase: SelectedAccountUseCase,
    private val bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
    private val resources: ResourceManager,
    private val icons: AssetIconProvider,
    private val tokenFormatter: TokenFormatter,
    private val amountFormatter: AmountFormatter,
    private val externalActions: ExternalActions.Presentation,
    private val logger: Logger,
    private val iconGenerator: AddressIconGenerator
) : BaseViewModel(), ExternalActions by externalActions {

    private val chain by lazyAsync { chainRegistry.getChain(ChainGeneses.BITTENSOR) }

    val wallet = walletUiUseCase.selectedWalletUiFlow().shareInBackground()

    val account = selectedAccountUseCase.selectedAddressModelFlow(chain::await).shareInBackground()

    private val executing = MutableStateFlow(false)

    private val completed = MutableStateFlow(false)

    private val result = MutableStateFlow<BittensorStakeResultUi?>(null)

    val resultState = result.asStateFlow()

    private val portfolioState = bittensorSubnetInteractor.observePortfolio()
        .withSafeLoading()
        .onError { preparationFailed(it) }
        .shareInBackground()

    private val portfolio = portfolioState.filterLoaded().shareInBackground()

    private val preparationRequest = MutableStateFlow(0L)

    private val preparationState = preparationRequest.flatMapLatest {
        flowOf { prepareStake() }.withSafeLoading()
            .onError { preparationFailed(it) }
    }.shareInBackground()

    private val draft = preparationState.map { it.dataOrNull?.draft }.shareInBackground()

    private val subnet = preparationState.map { it.dataOrNull?.subnet }.shareInBackground()

    private val quote = draft.map { it?.operation?.quote }.shareInBackground()

    val feeCaption = quote.map { quote ->
        quote?.serviceFeeRate?.let { resources.getString(R.string.bittensor_subnet_fee_caption, it.movePointRight(2).toStripTrailingZerosString()) }.orEmpty()
    }.shareInBackground()

    val hasError = combine(portfolioState, preparationState) { portfolio, preparation -> portfolio.isError() || preparation.isError() }
        .shareInBackground()

    val fee = combine(preparationState, portfolioState, ::mapFeeStatus).shareInBackground()

    val validator = draft.map { it?.validator?.name ?: resources.getString(R.string.bittensor_select_validator) }.shareInBackground()

    val walletChanged = portfolioState.map { it.dataOrNull?.metaId != metaId }.shareInBackground()

    val assets = combine(portfolio, subnet, quote, ::mapAssets).shareInBackground()

    val rate = combine(subnet, quote, portfolio, ::formatRate).shareInBackground()

    val slippage = bittensorSubnetInteractor.slippage.fraction.multiply(BigDecimal(100)).toStripTrailingZerosString() + "%"

    val validatorIcon = draft.map { prepared ->
        prepared?.validator?.let {
            iconGenerator.createAddressIcon(it.hotkey.value, AddressIconGenerator.SIZE_MEDIUM, AddressIconGenerator.BACKGROUND_TRANSPARENT)
        }
    }.shareInBackground()

    val confirmButtonState = combine(draft, walletChanged, executing, completed, hasError) { draft, changed, executing, completed, failed ->
        val label = resources.getString(R.string.common_confirm)
        if (draft == null || changed || executing || completed || failed) DescriptiveButtonState.Disabled(label) else DescriptiveButtonState.Enabled(label)
    }.shareInBackground()

    fun retryPreparation() = launchUnit {
        if (executing.value || completed.value || preparationState.first().isLoading()) return@launchUnit
        preparationRequest.update { it + 1 }
    }

    fun accountClicked() = launchUnit {
        externalActions.showAddressActions(account.first().address, chain.await())
    }

    private fun preparationFailed(cause: Throwable) {
        logger.log("[BittensorSubnetReview] Preparation failed")
        logger.log(cause)
        showError(resources.getString(R.string.common_retry_message))
    }

    fun confirmClicked() = launchUnit {
        if (executing.value || completed.value || hasError.first() || walletChanged.first()) return@launchUnit
        val prepared = draft.first() ?: return@launchUnit
        executing.value = true
        result.value = BittensorStakeResultUi.Progress(
            resources.getString(R.string.bittensor_staking_progress_title),
            resources.getString(R.string.bittensor_subnet_staking_progress, subnetId.value)
        )
        try {
            bittensorSubnetStakingInteractor.execute(prepared)
                .onSuccess {
                    completed.value = true
                    result.value = BittensorStakeResultUi.Success(
                        resources.getString(R.string.bittensor_subnet_staking_success, subnetId.value),
                        resources.getString(R.string.bittensor_subnet_staking_completed),
                        prepared.validator.name,
                        BittensorFeeUi(formatFee(prepared), "—")
                    )
                }
                .onFailure {
                    if (it is CancellationException) throw it
                    showFailure(it)
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showFailure(e)
        } finally {
            executing.value = false
        }
    }

    private fun showFailure(cause: Throwable) {
        result.value = BittensorStakeResultUi.Failure(
            resources.getString(R.string.bittensor_staking_failed_title),
            resources.getString(R.string.bittensor_staking_failed_description),
            cause.message ?: resources.getString(R.string.common_error_general_title)
        )
    }

    private fun formatFee(draft: BittensorSubnetStakeDraft) =
        tokenFormatter.formatToken(draft.fee.decimalAmount, draft.fee.asset.symbol).toString()

    private suspend fun prepareStake(): PreparedStake {
        val subnet = bittensorSubnetInteractor.subnet(subnetId)
        val selectedValidator = bittensorSubnetStakingInteractor.validators(subnetId)
            .firstOrNull { it.address == validatorAddress && it.selectable }
        requireNotNull(selectedValidator) { "Validator is no longer eligible in this subnet" }
        val draft = bittensorSubnetStakingInteractor.prepare(metaId, subnetId, totalInPlanks, selectedValidator.validator)
        return PreparedStake(subnet, draft)
    }

    private fun mapFeeStatus(
        preparationState: ExtendedLoadingState<PreparedStake>,
        portfolioState: ExtendedLoadingState<BittensorPortfolio>
    ): FeeStatus<Unit, FeeDisplay> {
        if (preparationState.isError() || portfolioState.isError()) return FeeStatus.Error
        val draft = preparationState.dataOrNull?.draft ?: return FeeStatus.Loading(true)
        val token = portfolioState.dataOrNull?.asset?.token ?: return FeeStatus.Loading(true)
        val amount = mapAmount(draft.fee.decimalAmount, draft.fee.asset.symbol, token.toFiatOrNull(draft.fee.decimalAmount), token.currency)
        return FeeStatus.Loaded(FeeModel(Unit, FeeDisplay(amount.token, amount.fiat)))
    }

    private fun mapAssets(portfolio: BittensorPortfolio, subnet: BittensorSubnet?, quote: BittensorSubnetBuyQuote?): SwapAssetsView.Model {
        val token = portfolio.asset.token
        val input = token.amountFromPlanks(totalInPlanks)
        val output = quote?.let { BigDecimal(it.alphaOutInPlanks, 9) }
        val outputFiat = output?.let { alpha -> subnet?.priceInRao?.let { alpha * token.amountFromPlanks(it) } }?.let(token::toFiatOrNull)

        return SwapAssetsView.Model(
            assetIn = SwapAssetView.Model(
                assetIcon = icons.getAssetIconOrFallback(token.configuration),
                amount = mapAmount(input, token.configuration.symbol, token.toFiatOrNull(input), token.currency),
                iconSizeDp = 48
            ),
            assetOut = SwapAssetView.Model(
                assetIcon = subnet?.logoUrl?.asUrlIcon() ?: AssetIconProvider.fallbackIcon,
                amount = output?.let { mapAmount(it, TokenSymbol(subnet?.symbol?.ifBlank { "—" } ?: "—"), outputFiat, token.currency) }
                    ?: AmountModel("—", null),
                iconSizeDp = 48,
                clipIconToOutline = true
            )
        )
    }

    private fun mapAmount(amount: BigDecimal, symbol: TokenSymbol, fiat: BigDecimal?, currency: Currency): AmountModel {
        val model = amountFormatter.formatAmountToAmountModel(PricedAmount(amount, fiat ?: BigDecimal.ZERO, currency), symbol)
        return model.copy(fiat = model.fiat.takeIf { fiat != null })
    }

    private fun formatRate(subnet: BittensorSubnet?, quote: BittensorSubnetBuyQuote?, portfolio: BittensorPortfolio): String {
        if (subnet == null || quote == null) return "—"
        val token = portfolio.asset.token
        val ratio = BigDecimal(quote.alphaOutInPlanks, 9).divide(token.amountFromPlanks(quote.totalTaoInPlanks), 9, RoundingMode.DOWN)
        return tokenFormatter.formatToken(BigDecimal.ONE, token.configuration.symbol).toString() + " ≈ \u200e" +
            tokenFormatter.formatToken(ratio, TokenSymbol(subnet.symbol.ifBlank { "—" }))
    }

    fun resultDismissed() {
        result.value = null
    }

    fun viewPositionClicked() {
        if (completed.value) router.openBittensorPortfolio()
    }

    fun back() {
        if (!executing.value) router.back()
    }

    private data class PreparedStake(val subnet: BittensorSubnet, val draft: BittensorSubnetStakeDraft)
}
