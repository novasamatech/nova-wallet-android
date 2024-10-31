package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2

import android.util.Log
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.toChainAsset
import io.novafoundation.nova.feature_account_api.data.fee.toFeePaymentCurrency
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.fee.CustomFeeInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeInspector
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.FeeFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.formatFeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.ChooseFeeCurrencyPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.PaymentCurrencySelectionMode
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.automaticChangeEnabled
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.onLoaded
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.onlyNativeFeeEnabled
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.userCanChangeFee
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

internal class FeeLoaderV2Provider<F, D>(
    private val chainRegistry: ChainRegistry,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val resourceManager: ResourceManager,
    private val interactor: CustomFeeInteractor,

    private val feeFormatter: FeeFormatter<F, D>,
    private val configuration: FeeLoaderMixinV2.Configuration<F, D>,
    private val feeInspector: FeeInspector<F>,
    private val selectedChainAssetFlow: Flow<Chain.Asset>,
    coroutineScope: CoroutineScope
) : FeeLoaderMixinV2.Presentation<F, D>, CoroutineScope by coroutineScope, FeeFormatter.Context {

    private val feeFormatterConfiguration = configuration.toFeeFormatterConfiguration()

    private val selectedTokenInfo = selectedChainAssetFlow
        .distinctUntilChangedBy { it.fullId }
        .map(::constructSelectedTokenInfo)
        .shareInBackground()

    private val paymentCurrencySelectionModeFlow = MutableStateFlow(configuration.initialState.paymentCurrencySelectionMode)

    override val feeChainAssetFlow = singleReplaySharedFlow<Chain.Asset>()

    private val feeAsset: Flow<Asset> = feeChainAssetFlow
        .distinctUntilChangedBy { it.fullId }
        .flatMapLatest { interactor.assetFlow(it) }
        .shareInBackground()

    private val userModifiedFeeInCurrentAsset = MutableStateFlow(false)

    private val canSwitchToSelectedToken = combine(selectedTokenInfo, feeChainAssetFlow) { selectedTokenInfo, feeAsset ->
        val canSwitchToSelected = feeAsset.fullId != selectedTokenInfo.chainAsset.fullId
        val selectedCanBeUsed = selectedTokenInfo.feePaymentSupported

        canSwitchToSelected && selectedCanBeUsed
    }
        .distinctUntilChanged()
        .shareInBackground()

    private val canChangeFeeAutomatically = combine(
        userModifiedFeeInCurrentAsset,
        paymentCurrencySelectionModeFlow,
        canSwitchToSelectedToken
    ) { userModifiedFee, selectionMode, canSwitchToSelected ->
        canSwitchToSelected && selectionMode.automaticChangeEnabled() && !userModifiedFee
    }.shareInBackground()

    override val userCanChangeFeeAsset: Flow<Boolean> = combine(
        paymentCurrencySelectionModeFlow,
        selectedTokenInfo
    ) { selectionMode, tokenInfo ->
        selectionMode.userCanChangeFee() && tokenInfo.feePaymentSupported
    }.shareInBackground()

    override val chooseFeeAsset = actionAwaitableMixinFactory.create<ChooseFeeCurrencyPayload, Chain.Asset>()

    override val fee = MutableStateFlow(configuration.initialState.feeStatus)

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    private var latestLoadFeeJob: Job? = null
    private var latestFeeConstructor: FeeConstructor<F>? = null

    init {
        observeSelectedAssetChanges()
    }

    override fun loadFee(feeConstructor: FeeConstructor<F>) {
        latestLoadFeeJob?.cancel()
        latestLoadFeeJob = launch(Dispatchers.IO) {
            latestFeeConstructor = feeConstructor
            fee.emit(feeFormatter.createLoadingStatus())

            val feePaymentCurrency = feeChainAssetFlow.first().toFeePaymentCurrency()

            runCatching { feeConstructor(feePaymentCurrency) }
                .mapCatching { onFeeLoaded(it, feePaymentCurrency, feeConstructor) }
                .onFailure { onFeeError(it, feeConstructor) }
        }
    }

    private suspend fun onFeeError(error: Throwable, feeConstructor: FeeConstructor<F>) {
        if (error !is CancellationException) {
            Log.e(LOG_TAG, "Failed to sync fee", error)

            fee.emit(FeeStatus.Error)

            awaitFeeRetry()

            loadFee(feeConstructor)
        }
    }

    private suspend fun onFeeLoaded(
        newFee: F?,
        requestedFeePaymentCurrency: FeePaymentCurrency,
        feeConstructor: FeeConstructor<F>
    ) {
        if (newFee != null) {
            val actualPaymentCurrency = feeInspector.getSubmissionFeeAsset(newFee).toFeePaymentCurrency()
            require(requestedFeePaymentCurrency == actualPaymentCurrency) {
                """
                    Fee with loaded with different fee payment currency that was requested.
                    Requested: ${requestedFeePaymentCurrency}. Actual: $actualPaymentCurrency.
                    Please check you are using the passed FeePaymentCurrency to load the fee.
                """.trimIndent()
            }

            setFeeWithAutomaticChange(newFee, feeConstructor)
        } else {
            fee.emit(FeeStatus.NoFee)
        }
    }

    private suspend fun awaitFeeRetry() {
        return suspendCancellableCoroutine { continuation ->
            retryEvent.postValue(
                Event(
                    RetryPayload(
                        title = resourceManager.getString(R.string.choose_amount_network_error),
                        message = resourceManager.getString(R.string.choose_amount_error_fee),
                        onRetry = { continuation.resume(Unit) },
                        onCancel = { continuation.cancel() }
                    )
                )
            )
        }
    }

    override suspend fun feeAsset(): Asset {
        return feeAsset.first()
    }

    override suspend fun feeToken(): Token {
        return feeAsset.first().token
    }

    override suspend fun feePaymentCurrency(): FeePaymentCurrency {
        return feeChainAssetFlow.first().toFeePaymentCurrency()
    }

    override suspend fun setPaymentCurrencySelectionMode(mode: PaymentCurrencySelectionMode) {
        paymentCurrencySelectionModeFlow.value = mode

        val isCustomFee = !feeChainAssetFlow.first().isUtilityAsset

        if (isCustomFee && mode.onlyNativeFeeEnabled()) {
            val utilityAsset = selectedTokenInfo.first().chainAsset
            feeChainAssetFlow.emit(utilityAsset)

            reloadFeeWithLatestConstructor()
        }
    }

    override suspend fun setFeeLoading() {
        fee.emit(feeFormatter.createLoadingStatus())
    }

    override suspend fun setFeeStatus(feeStatus: FeeStatus<F, D>) {
        feeStatus.onLoaded { feeModel ->
            val feeAsset = feeInspector.getSubmissionFeeAsset(feeModel.fee)
            feeChainAssetFlow.emit(feeAsset)
        }

        fee.emit(feeStatus)
    }

    override suspend fun setFee(fee: F) {
        setFeeOrHide(fee as F?)
    }

    override suspend fun setFeeOrHide(fee: F?) {
        val feeStatus = feeFormatter.formatFeeStatus(fee, feeFormatterConfiguration)
        setFeeStatus(feeStatus)
    }

    private suspend fun setFeeWithAutomaticChange(
        newFee: F,
        feeConstructor: suspend (FeePaymentCurrency) -> F?
    ) {
        val feeStatus = feeFormatter.formatFeeStatus(newFee, feeFormatterConfiguration)

        val canChangeFeeAutomatically = canChangeFeeAutomatically.first()
        if (!canChangeFeeAutomatically) {
            fee.value = feeStatus
            return
        }

        val feeAsset = feeAsset.first()
        if (feeAsset.canPayFee(newFee)) {
            fee.value = feeStatus
        } else {
            val selectedChainAsset = selectedChainAssetFlow.first()
            feeChainAssetFlow.emit(selectedChainAsset)

            loadFee(feeConstructor)
        }
    }

    private fun observeSelectedAssetChanges() {
        selectedTokenInfo.distinctUntilChangedBy { it.chainAsset.fullId }
            .withIndex()
            .onEach { (index, tokenInfo) ->
                userModifiedFeeInCurrentAsset.value = false

                if (index == 0) {
                    // First emission - we have loaded initial chain
                    val initialFeeAsset = configuration.initialState.feePaymentCurrency.toChainAsset(tokenInfo.chain)
                    feeChainAssetFlow.emit(initialFeeAsset)
                } else {
                    // Subsequent emissions - chain changed, set the utility asset
                    feeChainAssetFlow.emit(tokenInfo.chain.utilityAsset)

                    reloadFeeWithLatestConstructor()
                }
            }
            .launchIn(this)
    }

    private fun reloadFeeWithLatestConstructor() {
        latestFeeConstructor?.let(::loadFee)
    }

    override fun changePaymentCurrencyClicked() {
        launch {
            val userCanChangeFee = userCanChangeFeeAsset.first()
            if (!userCanChangeFee) return@launch

            val payload = constructChooseFeeCurrencyPayload()
            val chosenFeeAsset = chooseFeeAsset.awaitAction(payload)

            feeChainAssetFlow.emit(chosenFeeAsset)
            userModifiedFeeInCurrentAsset.value = true
            reloadFeeWithLatestConstructor()
        }
    }

    private suspend fun constructChooseFeeCurrencyPayload(): ChooseFeeCurrencyPayload {
        val selectedTokenInfo = selectedTokenInfo.first()
        val feeChainAsset = feeChainAssetFlow.first()

        val availableFeeTokens = listOf(selectedTokenInfo.chain.utilityAsset, selectedTokenInfo.chainAsset)
        return ChooseFeeCurrencyPayload(
            selectedCommissionAsset = feeChainAsset,
            availableAssets = availableFeeTokens
        )
    }

    private fun Asset.canPayFee(fee: F): Boolean {
        return transferableInPlanks >= feeInspector.requiredBalanceToPayFee(fee, token.configuration)
    }

    private suspend fun constructSelectedTokenInfo(chainAsset: Chain.Asset): SelectedAssetInfo {
        val chain = chainRegistry.getChain(chainAsset.chainId)
        val canPayFee = canPayFeeIn(chainAsset)

        return SelectedAssetInfo(chainAsset, chain, canPayFee)
    }

    private suspend fun canPayFeeIn(chainAsset: Chain.Asset): Boolean {
        return chainAsset.isCommissionAsset || interactor.canPayFeeInNonUtilityAsset(chainAsset, this)
    }

    private fun FeeLoaderMixinV2.Configuration<*, *>.toFeeFormatterConfiguration(): FeeFormatter.Configuration {
        return FeeFormatter.Configuration(showZeroFiat)
    }

    private class SelectedAssetInfo(
        val chainAsset: Chain.Asset,
        val chain: Chain,
        val feePaymentSupported: Boolean
    )
}
