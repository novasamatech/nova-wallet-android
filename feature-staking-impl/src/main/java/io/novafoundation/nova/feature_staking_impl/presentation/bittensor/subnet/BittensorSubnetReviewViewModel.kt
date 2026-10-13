package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_wallet_api.domain.model.PricedAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetView
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetsView
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.launchUnit
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
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.BigInteger

class BittensorSubnetReviewViewModel(
    private val subnetId: BittensorSubnetId,
    private val totalInPlanks: BigInteger,
    private val metaId: Long,
    private val validatorAddress: String,
    private val interactor: BittensorSubnetInteractor,
    private val router: StakingRouter,
    chainRegistry: ChainRegistry,
    walletUiUseCase: WalletUiUseCase,
    selectedAccountUseCase: SelectedAccountUseCase,
    private val staking: BittensorSubnetStakingInteractor,
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
    private val failed = MutableStateFlow(false)
    val hasError = failed.asStateFlow()
    private val draft = MutableStateFlow<BittensorSubnetStakeDraft?>(null)
    private val loading = MutableStateFlow(false)
    private val executing = MutableStateFlow(false)
    private val completed = MutableStateFlow(false)
    private val result = MutableStateFlow<BittensorStakeResultUi?>(null)
    val resultState = result.asStateFlow()
    private val feeState = MutableStateFlow<FeeStatus<Unit, FeeDisplay>>(FeeStatus.Loading(true))
    val fee = feeState.asStateFlow()
    val validator = draft.map { it?.validator?.name ?: resources.getString(R.string.bittensor_select_validator) }.shareInBackground()
    private val subnet = MutableStateFlow<BittensorSubnet?>(null)
    private val previewQuote = MutableStateFlow<BittensorSubnetBuyQuote?>(null)
    private val quote = combine(draft, previewQuote) { prepared, preview -> prepared?.operation?.quote ?: preview }.shareInBackground()
    private val portfolio = interactor.observePortfolio().catch { preparationFailed(it) }.shareInBackground()

    val walletChanged = portfolio.map { it.metaId != metaId }
        .catch { emit(true) }.shareInBackground()
    val assets = combine(portfolio, subnet, quote) { portfolio, subnet, quote ->
        val token = portfolio.asset.token
        val input = BigDecimal(totalInPlanks, 9)
        val output = quote?.let { BigDecimal(it.alphaOutInPlanks, 9) }
        val outputFiat = output?.let { alpha -> subnet?.priceInRao?.let { alpha * BigDecimal(it, 9) } }?.let(token::toFiatOrNull)
        fun amountModel(amount: BigDecimal, symbol: TokenSymbol, fiat: BigDecimal?): AmountModel {
            return amountFormatter.formatAmountToAmountModel(
                PricedAmount(amount, fiat ?: BigDecimal.ZERO, token.currency),
                symbol
            ).let { it.copy(fiat = it.fiat.takeIf { fiat != null }) }
        }
        SwapAssetsView.Model(
            assetIn = SwapAssetView.Model(
                assetIcon = icons.getAssetIconOrFallback(token.configuration),
                amount = amountModel(input, token.configuration.symbol, token.toFiatOrNull(input)),
                iconSizeDp = 48
            ),
            assetOut = SwapAssetView.Model(
                assetIcon = subnet?.logoUrl?.asUrlIcon() ?: AssetIconProvider.fallbackIcon,
                amount = output?.let { amountModel(it, TokenSymbol(subnet?.symbol?.ifBlank { "—" } ?: "—"), outputFiat) }
                    ?: AmountModel("—", null),
                iconSizeDp = 48,
                clipIconToOutline = true
            )
        )
    }.shareInBackground()
    val rate = combine(subnet, quote) { subnet, quote ->
        if (subnet == null || quote == null) {
            "—"
        } else {
            val ratio = quote.alphaOutInPlanks.toBigDecimal().divide(quote.totalTaoInPlanks.toBigDecimal(), 9, java.math.RoundingMode.DOWN)
            "1 TAO ≈ \u200e" + tokenFormatter.formatToken(ratio, TokenSymbol(subnet.symbol.ifBlank { "—" }))
        }
    }.shareInBackground()
    val slippage = interactor.slippage.fraction.multiply(BigDecimal(100)).toStripTrailingZerosString() + "%"
    val validatorIcon = draft.map { prepared ->
        prepared?.validator?.let {
            iconGenerator.createAddressIcon(it.hotkey.value, AddressIconGenerator.SIZE_MEDIUM, AddressIconGenerator.BACKGROUND_TRANSPARENT)
        }
    }.shareInBackground()

    val confirmButtonState = combine(draft, walletChanged, executing, completed, hasError) { draft, changed, executing, completed, failed ->
        val label = resources.getString(R.string.common_confirm)
        if (draft == null || changed || executing || completed || failed) DescriptiveButtonState.Disabled(label) else DescriptiveButtonState.Enabled(label)
    }.shareInBackground()

    init {
        retryPreparation()
    }

    fun retryPreparation() = launchUnit {
        if (loading.value || executing.value || completed.value) return@launchUnit
        loading.value = true
        failed.value = false
        draft.value = null
        feeState.value = FeeStatus.Loading(true)
        try {
            subnet.value = interactor.subnets().first { it.id == subnetId }
            previewQuote.value = interactor.quote(subnetId, totalInPlanks)
            val selected = staking.validators(subnetId).firstOrNull { it.address == validatorAddress && it.selectable }
            requireNotNull(selected) { "Validator is no longer eligible in this subnet" }
            val prepared = staking.prepare(metaId, subnetId, totalInPlanks, selected.validator)
            val token = portfolio.first().asset.token
            val fiat = token.toFiatOrNull(prepared.fee.decimalAmount)
            val feeAmount = amountFormatter.formatAmountToAmountModel(
                PricedAmount(
                    prepared.fee.decimalAmount,
                    fiat ?: BigDecimal.ZERO,
                    token.currency
                ),
                prepared.fee.asset.symbol
            )
            feeState.value = FeeStatus.Loaded(FeeModel(Unit, FeeDisplay(feeAmount.token, feeAmount.fiat.takeIf { fiat != null })))
            draft.value = prepared
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            preparationFailed(e)
        } catch (e: Exception) {
            preparationFailed(e)
        } finally {
            loading.value = false
        }
    }

    fun accountClicked() = launchUnit {
        externalActions.showAddressActions(account.first().address, chain.await())
    }

    private fun preparationFailed(cause: Throwable) {
        logger.log("[BittensorSubnetReview] Preparation failed")
        logger.log(cause)
        if (!failed.value) showError(resources.getString(R.string.common_retry_message))
        failed.value = true
        feeState.value = FeeStatus.Error
    }

    fun confirmClicked() = launchUnit {
        if (executing.value || completed.value || failed.value || walletChanged.first()) return@launchUnit
        val prepared = draft.value ?: return@launchUnit
        executing.value = true
        result.value = BittensorStakeResultUi.Progress(
            resources.getString(R.string.bittensor_staking_progress_title),
            resources.getString(R.string.bittensor_subnet_staking_progress, subnetId.value)
        )
        try {
            when (val execution = staking.execute(prepared)) {
                is BittensorOperationResult.Success -> {
                    completed.value = true
                    result.value = BittensorStakeResultUi.Success(
                        resources.getString(R.string.bittensor_subnet_staking_success, subnetId.value),
                        resources.getString(R.string.bittensor_subnet_staking_completed),
                        prepared.validator.name,
                        BittensorFeeUi(formatFee(prepared), "—")
                    )
                }
                is BittensorOperationResult.Failure -> showFailure(execution.cause)
                is BittensorOperationResult.Unknown -> {
                    completed.value = true
                    result.value = BittensorStakeResultUi.Failure(
                        resources.getString(R.string.bittensor_submission_unknown_title),
                        resources.getString(R.string.bittensor_submission_unknown_description),
                        execution.transactionHash,
                        canRetry = false
                    )
                }
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

    fun resultDismissed() {
        result.value = null
    }

    fun viewPositionClicked() {
        if (completed.value) router.openBittensorPortfolio()
    }

    fun back() {
        if (!executing.value) router.back()
    }
}
