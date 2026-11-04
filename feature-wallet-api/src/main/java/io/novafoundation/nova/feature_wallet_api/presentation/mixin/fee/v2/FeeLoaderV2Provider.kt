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
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency.Asset.Companion.toFeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.toChainAsset
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.fee.FeeInteractor
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
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.shouldDetectFeeAssetFromFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.userCanChangeFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeContext.OperationUtilityAssetSource
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

internal class FeeLoaderV2Provider<F, D>(
    private val chainRegistry: ChainRegistry,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val resourceManager: ResourceManager,
    private val interactor: FeeInteractor,

    private val feeFormatter: FeeFormatter<F, D>,
    private val configuration: FeeLoaderMixinV2.Configuration<F, D>,
    private val feeInspector: FeeInspector<F>,
    private val feeContextFlow: Flow<FeeContext>,
    coroutineScope: CoroutineScope,
    private val feeLoadingDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FeeLoaderMixinV2.Presentation<F, D>,
    CoroutineScope by coroutineScope,
    FeeFormatter.Context {

    private val feeFormatterConfiguration = configuration.toFeeFormatterConfiguration()

    private val selectedTokenInfo = feeContextFlow
        .distinctUntilChangedBy { it.operationAsset.fullId }
        .map(::constructSelectedTokenInfo)
        .shareInBackground()

    private val paymentCurrencySelectionModeFlow = MutableStateFlow(configuration.initialState.paymentCurrencySelectionMode)

    override val feeChainAssetFlow = singleReplaySharedFlow<Chain.Asset>()

    private val feeAsset: Flow<Asset?> = feeChainAssetFlow
        .distinctUntilChangedBy { it.fullId }
        .flatMapLatest { interactor.assetFlow(it) }
        .shareInBackground()

    private val userModifiedFeeInCurrentAsset = MutableStateFlow(false)

    private val feeSwitchCapabilityFlow = combine(selectedTokenInfo, feeChainAssetFlow) { selectedTokenInfo, feeAsset ->
        val selectedSupported = selectedTokenInfo.feePaymentSupported

        val feeInNative = feeAsset.fullId == selectedTokenInfo.chainUtilityAsset.fullId
        val feeInSelected = feeAsset.fullId == selectedTokenInfo.chainAsset.fullId

        FeeSwitchCapability(
            canSwitchToSelected = selectedSupported && !feeInSelected,
            canSwitchToNative = !feeInNative
        )
    }
        .distinctUntilChanged()
        .shareInBackground()

    private val canChangeFeeToSelectedAutomatically = combine(
        userModifiedFeeInCurrentAsset,
        paymentCurrencySelectionModeFlow,
        feeSwitchCapabilityFlow
    ) { userModifiedFee, selectionMode, feeSwitchCapability ->
        feeSwitchCapability.canSwitchToSelected && selectionMode.automaticChangeEnabled() && !userModifiedFee
    }.shareInBackground()

    override val userCanChangeFeeAsset: Flow<Boolean> = combine(
        paymentCurrencySelectionModeFlow,
        feeSwitchCapabilityFlow
    ) { selectionMode, feeSwitchCapability ->
        feeSwitchCapability.canSwitch && selectionMode.userCanChangeFee()
    }.shareInBackground()

    override val chooseFeeAsset = actionAwaitableMixinFactory.create<ChooseFeeCurrencyPayload, Chain.Asset>()

    override val fee = MutableStateFlow(configuration.initialState.feeStatus)

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    private val feeRequestLock = Any()

    private var latestFeeRequest = 0L

    private var latestLoadFeeJob: Job? = null

    private var latestFeeConstructor: FeeConstructor<F>? = null

    private val retryDialogPending = AtomicBoolean(false)

    private val retryDialogSuppressed = AtomicBoolean(false)

    private var pendingRetryEvent: Event<RetryPayload>? = null

    init {
        observeSelectedAssetChanges()
    }

    override fun loadFee(feeConstructor: FeeConstructor<F>) {
        synchronized(feeRequestLock) {
            val request = invalidateFeeRequest()
            latestFeeConstructor = feeConstructor
            // Invalidate the previous estimate before dispatching any asynchronous work.
            fee.value = FeeStatus.Loading(visibleDuringProgress = true)
            latestLoadFeeJob = launch(feeLoadingDispatcher, start = CoroutineStart.LAZY) {
                try {
                    val loadingStatus = feeFormatter.createLoadingStatus()
                    publishFeeStatus(request, loadingStatus)
                    val feePaymentCurrency = feeChainAssetFlow.first().toFeePaymentCurrency()
                    ensureCurrentRequest(request)
                    val newFee = feeConstructor(feePaymentCurrency)
                    ensureCurrentRequest(request)
                    onFeeLoaded(newFee, feePaymentCurrency, feeConstructor, request)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    onFeeError(error, request)
                }
            }.also { it.start() }
        }
    }

    // Called under feeRequestLock. Input invalidation also prevents currency changes from retrying old inputs.
    private fun invalidateFeeRequest(clearConstructor: Boolean = true): Long {
        latestFeeRequest++
        latestLoadFeeJob?.cancel()
        latestLoadFeeJob = null
        if (clearConstructor) latestFeeConstructor = null
        retryDialogPending.set(false)
        pendingRetryEvent?.let { event ->
            // Suppress both a queued postValue and an event already delivered to an inactive observer.
            event.getContentIfNotHandled()
            retryEvent.postValue(null)
        }
        pendingRetryEvent = null
        return latestFeeRequest
    }

    private suspend fun ensureCurrentRequest(request: Long) {
        currentCoroutineContext().ensureActive()
        synchronized(feeRequestLock) {
            if (request != latestFeeRequest) throw CancellationException("Obsolete fee request")
        }
    }

    private suspend fun publishFeeStatus(request: Long, status: FeeStatus<F, D>) {
        ensureCurrentRequest(request)
        synchronized(feeRequestLock) {
            if (request == latestFeeRequest) fee.value = status
        }
    }

    private suspend fun onFeeError(error: Throwable, request: Long) {
        ensureCurrentRequest(request)
        synchronized(feeRequestLock) {
            if (request != latestFeeRequest) return
            Log.e(LOG_TAG, "Failed to sync fee", error)
            fee.value = FeeStatus.Error

            if (configuration.showRetryDialog && !retryDialogSuppressed.get() && retryDialogPending.compareAndSet(false, true)) {
                showFeeRetry(request)
            }
        }
    }

    private suspend fun onFeeLoaded(
        newFee: F?,
        requestedFeePaymentCurrency: FeePaymentCurrency,
        feeConstructor: FeeConstructor<F>,
        request: Long
    ) {
        ensureCurrentRequest(request)
        synchronized(feeRequestLock) {
            if (request == latestFeeRequest) retryDialogSuppressed.set(false)
        }

        if (newFee != null) {
            setLoadedFee(newFee, requestedFeePaymentCurrency, feeConstructor, request)
        } else {
            publishFeeStatus(request, FeeStatus.NoFee)
        }
    }

    private fun showFeeRetry(request: Long) {
        val event = Event(
            RetryPayload(
                title = resourceManager.getString(R.string.choose_amount_network_error),
                message = resourceManager.getString(R.string.choose_amount_error_fee),
                onRetry = {
                    synchronized(feeRequestLock) {
                        if (request != latestFeeRequest) return@synchronized
                        retryDialogPending.set(false)
                        retryDialogSuppressed.set(false)
                        reloadFeeWithLatestConstructor()
                    }
                },
                onCancel = {
                    synchronized(feeRequestLock) {
                        if (request != latestFeeRequest) return@synchronized
                        retryDialogPending.set(false)
                        retryDialogSuppressed.set(true)
                        configuration.onRetryCancelled()
                    }
                }
            )
        )
        pendingRetryEvent = event
        retryEvent.postValue(event)
    }

    override suspend fun feeAsset(): Asset {
        return feeAsset.filterNotNull().first()
    }

    override suspend fun token(chainAsset: Chain.Asset): Token {
        // Formatting may discover a custom fee asset before feeChainAssetFlow changes. Its token must
        // not wait for the previously selected asset's balance subscription to produce a value.
        return interactor.getToken(chainAsset)
    }

    override suspend fun feePaymentCurrency(): FeePaymentCurrency {
        return feeChainAssetFlow.first().toFeePaymentCurrency()
    }

    override suspend fun setPaymentCurrencySelectionMode(mode: PaymentCurrencySelectionMode) {
        paymentCurrencySelectionModeFlow.value = mode

        val isCustomFee = !feeChainAssetFlow.first().isUtilityAsset

        if (isCustomFee && mode.onlyNativeFeeEnabled()) {
            val utilityAsset = selectedTokenInfo.first().chainUtilityAsset
            feeChainAssetFlow.emit(utilityAsset)

            reloadFeeWithLatestConstructor()
        }
    }

    override suspend fun setFeeLoading() {
        val request = synchronized(feeRequestLock) {
            val request = invalidateFeeRequest()
            fee.value = FeeStatus.Loading(visibleDuringProgress = true)
            request
        }
        publishFeeStatus(request, feeFormatter.createLoadingStatus())
    }

    override suspend fun setFeeStatus(feeStatus: FeeStatus<F, D>) {
        synchronized(feeRequestLock) {
            invalidateFeeRequest(clearConstructor = feeStatus !is FeeStatus.Loaded)
            feeStatus.onLoaded { feeModel ->
                val feeAsset = feeInspector.getSubmissionFeeAsset(feeModel.fee)
                feeChainAssetFlow.tryEmit(feeAsset)
            }
            fee.value = feeStatus
        }
    }

    override suspend fun setFee(fee: F) {
        setFeeOrHide(fee as F?)
    }

    override suspend fun setFeeOrHide(fee: F?) {
        val request = synchronized(feeRequestLock) {
            // Send and gift validation can replace the fee of the same operation. Preserve its currency retry constructor.
            val request = invalidateFeeRequest(clearConstructor = fee == null)
            this.fee.value = FeeStatus.Loading(visibleDuringProgress = true)
            request
        }
        val feeStatus = feeFormatter.formatFeeStatus(fee, feeFormatterConfiguration)
        ensureCurrentRequest(request)
        synchronized(feeRequestLock) {
            if (request != latestFeeRequest) return
            feeStatus.onLoaded { feeModel ->
                feeChainAssetFlow.tryEmit(feeInspector.getSubmissionFeeAsset(feeModel.fee))
            }
            this.fee.value = feeStatus
        }
    }

    private suspend fun setLoadedFee(
        newFee: F,
        requestedFeePaymentCurrency: FeePaymentCurrency,
        feeConstructor: FeeConstructor<F>,
        request: Long
    ) {
        val selectionMode = paymentCurrencySelectionModeFlow.first()
        val actualFeePaymentAsset = feeInspector.getSubmissionFeeAsset(newFee)

        if (selectionMode.shouldDetectFeeAssetFromFee()) {
            val status = feeFormatter.formatFeeStatus(newFee, feeFormatterConfiguration)
            ensureCurrentRequest(request)
            synchronized(feeRequestLock) {
                if (request != latestFeeRequest) return
                feeChainAssetFlow.tryEmit(actualFeePaymentAsset)
                fee.value = status
            }
        } else {
            val actualPaymentCurrency = actualFeePaymentAsset.toFeePaymentCurrency()

            require(requestedFeePaymentCurrency == actualPaymentCurrency) {
                """
                    Fee with loaded with different fee payment currency that was requested.
                    Requested: $requestedFeePaymentCurrency. Actual: $actualPaymentCurrency.
                    Please check you are using the passed FeePaymentCurrency to load the fee.
                """.trimIndent()
            }

            setFeeWithAutomaticChange(newFee, feeConstructor, request)
        }
    }

    private suspend fun setFeeWithAutomaticChange(
        newFee: F,
        feeConstructor: suspend (FeePaymentCurrency) -> F?,
        request: Long
    ) {
        val feeStatus = feeFormatter.formatFeeStatus(newFee, feeFormatterConfiguration)

        val canChangeFeeAutomatically = canChangeFeeToSelectedAutomatically.first()
        if (!canChangeFeeAutomatically) {
            publishFeeStatus(request, feeStatus)
            return
        }

        val feeAsset = feeAsset.first()
        if (feeAsset == null || feeAsset.canPayFee(newFee)) {
            publishFeeStatus(request, feeStatus)
        } else {
            val selectedChainAsset = feeContextFlow.first().operationAsset
            ensureCurrentRequest(request)
            synchronized(feeRequestLock) {
                if (request != latestFeeRequest) return
                feeChainAssetFlow.tryEmit(selectedChainAsset)
                loadFee(feeConstructor)
            }
        }
    }

    private fun observeSelectedAssetChanges() {
        selectedTokenInfo.distinctUntilChangedBy { it.chainAsset.fullId }
            .withIndex()
            .onEach { (index, tokenInfo) ->
                userModifiedFeeInCurrentAsset.value = false

                if (index == 0) {
                    // First emission - we have loaded initial chain
                    val initialFeeAsset = configuration.initialState.feePaymentCurrency.toChainAsset(tokenInfo.chainUtilityAsset)
                    feeChainAssetFlow.emit(initialFeeAsset)
                } else {
                    // Subsequent emissions - chain changed, set the utility asset
                    feeChainAssetFlow.emit(tokenInfo.chainUtilityAsset)

                    reloadFeeWithLatestConstructor()
                }
            }
            .launchIn(this)
    }

    private fun reloadFeeWithLatestConstructor() {
        synchronized(feeRequestLock) {
            latestFeeConstructor?.let(::loadFee)
        }
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

        val availableFeeTokens = listOf(selectedTokenInfo.chainUtilityAsset, selectedTokenInfo.chainAsset)
        return ChooseFeeCurrencyPayload(
            selectedCommissionAsset = feeChainAsset,
            availableAssets = availableFeeTokens
        )
    }

    private suspend fun Asset.canPayFee(fee: F): Boolean {
        val inspectedFeeAmount = feeInspector.inspectFeeAmount(fee)

        return interactor.hasEnoughBalanceToPayFee(this, inspectedFeeAmount)
    }

    private suspend fun constructSelectedTokenInfo(feeContext: FeeContext): SelectedAssetInfo {
        val canPayFee = canPayFeeIn(feeContext.operationAsset)
        return SelectedAssetInfo(feeContext.operationAsset, feeContext.operationChainUtilityAsset(), canPayFee)
    }

    private suspend fun FeeContext.operationChainUtilityAsset(): Chain.Asset {
        return when (val source = operationChainUtilityAssetSource) {
            OperationUtilityAssetSource.DetectFromOperationChain -> chainRegistry.getChain(operationAsset.chainId).utilityAsset
            is OperationUtilityAssetSource.Specified -> source.operationChainUtilityAsset
        }
    }

    private suspend fun canPayFeeIn(chainAsset: Chain.Asset): Boolean {
        return interactor.canPayFeeInAsset(chainAsset)
    }

    private fun FeeLoaderMixinV2.Configuration<*, *>.toFeeFormatterConfiguration(): FeeFormatter.Configuration {
        return FeeFormatter.Configuration(showZeroFiat)
    }

    private class SelectedAssetInfo(
        val chainAsset: Chain.Asset,
        val chainUtilityAsset: Chain.Asset,
        val feePaymentSupported: Boolean
    )

    private class FeeSwitchCapability(
        val canSwitchToSelected: Boolean,
        val canSwitchToNative: Boolean
    ) {

        val canSwitch = canSwitchToSelected || canSwitchToNative
    }
}
