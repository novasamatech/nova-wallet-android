package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.validation.notValidOrNull
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BITTENSOR_ALPHA_DECIMALS
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPreparedSubnetStake
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorWalletContext
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.message
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorSubmissionState
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetView
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetsView
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
    private val externalActions: ExternalActions.Presentation,
    private val iconGenerator: AddressIconGenerator,
    private val retryController: BittensorRetryController,
    private val bittensorAmountFormatter: BittensorAmountFormatter,
    private val stakeValidationSystem: BittensorStakeValidationSystem
) : BaseViewModel(), ExternalActions by externalActions {

    val retryEvents = retryController.events

    private val chain by lazyAsync { chainRegistry.getChain(ChainGeneses.BITTENSOR) }

    val wallet = walletUiUseCase.selectedWalletUiFlow().shareInBackground()

    val account = selectedAccountUseCase.selectedAddressModelFlow(chain::await).shareInBackground()

    private val submission = MutableStateFlow<BittensorSubmissionState>(BittensorSubmissionState.Ready)

    val resultState = submission.map { it.result }.shareInBackground()

    private val walletState = retryController.observe("staking wallet", bittensorSubnetInteractor::observeWallet)
        .shareInBackground()

    private val walletContext = walletState.filterLoaded().shareInBackground()

    private val preparationState = retryController.observe("stake preparation") {
        flowOf {
            bittensorSubnetStakingInteractor.prepare(
                metaId,
                subnetId,
                totalInPlanks,
                validatorAddress
            )
        }
    }.shareInBackground()

    private val draft = preparationState.map { it.dataOrNull?.draft }.shareInBackground()

    private val quote = draft.map { it?.operation?.quote }.shareInBackground()

    val feeCaption = quote.map { quote ->
        quote?.serviceFeeRate?.let { resources.getString(R.string.bittensor_subnet_fee_caption, it.movePointRight(2).toStripTrailingZerosString()) }.orEmpty()
    }.shareInBackground()

    private val hasError = combine(walletState, preparationState) { walletContext, preparation -> walletContext.isError() || preparation.isError() }
        .shareInBackground()

    val fee = combine(preparationState, walletState, ::mapFeeStatus).shareInBackground()

    val validator = draft.map { it?.validator?.name ?: resources.getString(R.string.bittensor_select_validator) }.shareInBackground()

    val walletChanged = walletState.map { it.dataOrNull?.metaId?.let { currentMetaId -> currentMetaId != metaId } == true }.shareInBackground()

    private val nativeToken = walletContext.map { it.asset.token }.shareInBackground()

    val assets = combine(nativeToken, preparationState) { token, preparation -> mapAssets(token, preparation.dataOrNull) }.shareInBackground()

    val rate = combine(nativeToken, preparationState) { token, preparation -> formatRate(token, preparation.dataOrNull) }.shareInBackground()

    val slippage = bittensorSubnetInteractor.slippage.fraction.multiply(BigDecimal(100)).toStripTrailingZerosString() + "%"

    val validatorIcon = draft.map { prepared ->
        prepared?.validator?.let {
            iconGenerator.createAddressIcon(it.hotkey.value, AddressIconGenerator.SIZE_MEDIUM, AddressIconGenerator.BACKGROUND_TRANSPARENT)
        }
    }.shareInBackground()

    val confirmButtonState = combine(draft, walletChanged, submission, hasError) { draft, changed, submission, failed ->
        val label = resources.getString(R.string.common_confirm)
        if (draft == null || changed || !submission.canSubmit || failed) DescriptiveButtonState.Disabled(label) else DescriptiveButtonState.Enabled(label)
    }.shareInBackground()

    fun accountClicked() = launchUnit {
        externalActions.showAddressActions(account.first().address, chain.await())
    }

    fun confirmClicked() = launchUnit {
        if (!submission.value.canSubmit || hasError.first() || walletChanged.first()) return@launchUnit
        val prepared = draft.first() ?: return@launchUnit
        val wallet = walletContext.first()
        val payload = BittensorStakeValidationPayload(metaId, wallet.metaId, wallet.asset, wallet.asset.token.amountFromPlanks(totalInPlanks), prepared.fee)
        val failure = stakeValidationSystem.validate(payload).getOrThrow().notValidOrNull()
        if (failure != null) {
            showError(failure.reason.message(resources))
            return@launchUnit
        }
        if (!submission.value.canSubmit) return@launchUnit
        submission.value = BittensorSubmissionState.Submitting(
            BittensorStakeResultUi.Progress(
                resources.getString(R.string.bittensor_staking_progress_title),
                resources.getString(R.string.bittensor_subnet_staking_progress, subnetId.value)
            )
        )
        try {
            bittensorSubnetStakingInteractor.execute(prepared)
                .onSuccess {
                    submission.value = BittensorSubmissionState.Completed(
                        BittensorStakeResultUi.Success(
                            resources.getString(R.string.bittensor_subnet_staking_success, subnetId.value),
                            resources.getString(R.string.bittensor_subnet_staking_completed),
                            prepared.validator.name,
                            bittensorAmountFormatter.fee(prepared.fee, walletContext.first().asset.token)
                        )
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
        }
    }

    private fun showFailure(cause: Throwable) {
        submission.value = BittensorSubmissionState.Failed(
            BittensorStakeResultUi.Failure(
                resources.getString(R.string.bittensor_staking_failed_title),
                resources.getString(R.string.bittensor_staking_failed_description),
                cause.message ?: resources.getString(R.string.common_error_general_title)
            )
        )
    }

    private fun mapFeeStatus(
        preparationState: ExtendedLoadingState<BittensorPreparedSubnetStake>,
        walletState: ExtendedLoadingState<BittensorWalletContext>
    ): FeeStatus<Unit, FeeDisplay> {
        if (preparationState.isError() || walletState.isError()) return FeeStatus.Error
        val draft = preparationState.dataOrNull?.draft ?: return FeeStatus.Loading(true)
        val token = walletState.dataOrNull?.asset?.token ?: return FeeStatus.Loading(true)
        val amount = bittensorAmountFormatter.amount(
            draft.fee.decimalAmount,
            draft.fee.asset.symbol,
            token.toFiatOrNull(draft.fee.decimalAmount),
            token.currency
        )
        return FeeStatus.Loaded(FeeModel(Unit, FeeDisplay(amount.token, amount.fiat)))
    }

    private fun mapAssets(token: TokenBase, prepared: BittensorPreparedSubnetStake?): SwapAssetsView.Model {
        val subnet = prepared?.subnet
        val quote = prepared?.draft?.operation?.quote
        val input = token.amountFromPlanks(totalInPlanks)
        val output = quote?.let { BigDecimal(it.alphaOutInPlanks, BITTENSOR_ALPHA_DECIMALS) }
        val alphaPrice = subnet?.priceInRao?.let(token::amountFromPlanks)
        val outputInNativeToken = if (output != null && alphaPrice != null) output * alphaPrice else null
        val outputFiat = outputInNativeToken?.let(token::toFiatOrNull)

        return SwapAssetsView.Model(
            assetIn = SwapAssetView.Model(
                assetIcon = icons.getAssetIconOrFallback(token.configuration),
                amount = bittensorAmountFormatter.amount(input, token.configuration.symbol, token.toFiatOrNull(input), token.currency),
                iconSizeDp = 48
            ),
            assetOut = SwapAssetView.Model(
                assetIcon = subnet?.logoUrl?.asUrlIcon() ?: AssetIconProvider.fallbackIcon,
                amount = output?.let { bittensorAmountFormatter.amount(it, TokenSymbol(subnet?.symbol?.ifBlank { "—" } ?: "—"), outputFiat, token.currency) }
                    ?: AmountModel("—", null),
                iconSizeDp = 48,
                clipIconToOutline = true
            )
        )
    }

    private fun formatRate(token: TokenBase, prepared: BittensorPreparedSubnetStake?): String {
        if (prepared == null) return "—"
        return bittensorAmountFormatter.exchangeRate(prepared.draft.operation.quote, prepared.subnet, token)
    }

    fun resultDismissed() {
        submission.value = submission.value.dismissResult()
    }

    fun viewPositionClicked() {
        if (submission.value is BittensorSubmissionState.Completed) router.openBittensorPortfolio()
    }

    fun back() {
        if (submission.value !is BittensorSubmissionState.Submitting) router.back()
    }
}
