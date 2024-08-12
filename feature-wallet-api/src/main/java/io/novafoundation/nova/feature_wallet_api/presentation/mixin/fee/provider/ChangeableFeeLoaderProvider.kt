package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.provider

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.asLiveData
import io.novafoundation.nova.common.utils.combineToTriple
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.presenatation.fee.select.FeeAssetSelectorBottomSheet
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.fee.CustomFeeInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.ChangeFeeTokenState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleFee
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ChangeableFeeLoaderProviderPresentation(
    customFeeInteractor: CustomFeeInteractor,
    chainRegistry: ChainRegistry,
    resourceManager: ResourceManager,
    configuration: GenericFeeLoaderMixin.Configuration<SimpleFee>,
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    tokenFlow: Flow<Token?>,
    coroutineScope: CoroutineScope
) : ChangeableFeeLoaderProvider<SimpleFee>(
    customFeeInteractor,
    chainRegistry,
    resourceManager,
    configuration,
    actionAwaitableMixinFactory,
    tokenFlow,
    coroutineScope
), FeeLoaderMixin.Presentation

internal open class ChangeableFeeLoaderProvider<F : GenericFee>(
    private val interactor: CustomFeeInteractor,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
    private val configuration: GenericFeeLoaderMixin.Configuration<F>,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val nulableSelectedTokenFlow: Flow<Token?>,
    private val coroutineScope: CoroutineScope
) : GenericFeeLoaderMixin.Presentation<F> {

    private val selectedTokenFlow = nulableSelectedTokenFlow.filterNotNull()

    private val chainFlow = selectedTokenFlow.map { it.configuration.chainId }
        .distinctUntilChanged()
        .map { chainRegistry.getChain(it) }

    private val availableFeeAssetsFlow: Flow<List<Chain.Asset>> =
        selectedTokenFlow.map { interactor.availableCommissionAssetFor(it.configuration, coroutineScope) }

    private val commissionChainAssetFlow = singleReplaySharedFlow<Chain.Asset>()

    private val commissionAssetFlow: Flow<Asset> = commissionChainAssetFlow
        .distinctUntilChanged()
        .flatMapLatest { interactor.assetFlow(it) }

    private val commissionTokenFlow: Flow<Token> = commissionAssetFlow.map { it.token }

    final override val feeLiveData = MutableLiveData<FeeStatus<F>>()

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    val changeFeeTokenEvent = actionAwaitableMixinFactory.create<FeeAssetSelectorBottomSheet.Payload, Chain.Asset>()
    private val feeTokenWasChangedManually = MutableStateFlow(false)

    override val changeFeeTokenState =
        combine(selectedTokenFlow, commissionChainAssetFlow, availableFeeAssetsFlow) { selectedToken, selectedCommissionAsset, availableFeeAssets ->
            mapChangeFeeTokenState(selectedToken, selectedCommissionAsset, availableFeeAssets)
        }.asLiveData(coroutineScope)

    init {
        configuration.initialStatusValue?.let(feeLiveData::postValue)

        setupCustomFee()
    }

    override suspend fun loadFeeSuspending(
        retryScope: CoroutineScope,
        feeConstructor: suspend (Token) -> F?,
        onRetryCancelled: () -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        feeLiveData.postValue(FeeStatus.Loading)

        commissionTokenFlow.distinctUntilChangedBy { it.configuration.fullId }
            .onEach { calculateFee(retryScope, it, feeConstructor, onRetryCancelled) }
            .launchIn(coroutineScope)
    }

    private suspend fun calculateFee(
        retryScope: CoroutineScope,
        token: Token,
        feeConstructor: suspend (Token) -> F?,
        onRetryCancelled: () -> Unit,
    ) {
        feeLiveData.postValue(FeeStatus.Loading)

        val value = runCatching {
            feeConstructor(token)
        }.fold(
            onSuccess = { genericFee -> onFeeLoaded(token, genericFee) },
            onFailure = { exception -> onError(exception, retryScope, feeConstructor, onRetryCancelled) }
        )

        value?.run { feeLiveData.postValue(this) }
    }

    override fun loadFeeV2Generic(
        coroutineScope: CoroutineScope,
        feeConstructor: suspend (Token) -> F?,
        onRetryCancelled: () -> Unit
    ) {
        coroutineScope.launch {
            loadFeeSuspending(
                retryScope = coroutineScope,
                feeConstructor = feeConstructor,
                onRetryCancelled = onRetryCancelled
            )
        }
    }

    override suspend fun setFee(fee: F?) {
        if (fee != null) {
            val commissionAsset = getCommissionAssetFor(fee.networkFee)
            changeCommissionAsset(commissionAsset)
            val token = getTokenFor(commissionAsset)

            val feeModel = mapFeeToFeeModel(fee, token, includeZeroFiat = configuration.showZeroFiat)

            feeLiveData.postValue(FeeStatus.Loaded(feeModel))
        } else {
            feeLiveData.postValue(FeeStatus.NoFee)
        }
    }

    override suspend fun setFeeStatus(feeStatus: FeeStatus<F>) {
        feeLiveData.postValue(feeStatus)
    }

    override fun invalidateFee() {
        feeLiveData.postValue(FeeStatus.Loading)
    }

    override suspend fun commissionChainAsset(): Chain.Asset {
        return commissionChainAssetFlow.first()
    }

    override suspend fun commissionAsset(): Asset {
        return commissionAssetFlow.first()
    }

    override fun setCommissionAsset(chainAsset: Chain.Asset) {
        coroutineScope.launch {
            commissionChainAssetFlow.emit(chainAsset)
        }
    }

    private fun onFeeLoaded(token: Token, fee: F?): FeeStatus<F> = if (fee != null) {
        val feeModel = mapFeeToFeeModel(fee, token, includeZeroFiat = configuration.showZeroFiat)

        FeeStatus.Loaded(feeModel)
    } else {
        FeeStatus.NoFee
    }

    private fun onError(
        exception: Throwable,
        retryScope: CoroutineScope,
        feeConstructor: suspend (Token) -> F?,
        onRetryCancelled: () -> Unit,
    ) = if (exception !is CancellationException) {
        retryEvent.postValue(
            Event(
                RetryPayload(
                    title = resourceManager.getString(R.string.choose_amount_network_error),
                    message = resourceManager.getString(R.string.choose_amount_error_fee),
                    onRetry = { loadFeeV2Generic(retryScope, feeConstructor, onRetryCancelled) },
                    onCancel = onRetryCancelled
                )
            )
        )

        exception.printStackTrace()

        FeeStatus.Error
    } else {
        null
    }

    private fun setupCustomFee() {
        val selectedChainAssetFlow = selectedTokenFlow.map { it.configuration }
        combineToTriple(feeTokenWasChangedManually, availableFeeAssetsFlow, selectedChainAssetFlow)
            .filter { (tokenWasChangedManually, _, _) -> !tokenWasChangedManually }
            .onEach { (_, availableFeeAssets, selectedChainAsset) ->
                if (selectedChainAsset in availableFeeAssets) {
                    commissionChainAssetFlow.emit(selectedChainAsset)
                } else {
                    val chain = chainFlow.first()
                    commissionChainAssetFlow.emit(chain.commissionAsset)
                }
            }
            .launchIn(coroutineScope)

        nulableSelectedTokenFlow
            .onEach { feeTokenWasChangedManually.value = false }
            .launchIn(coroutineScope)
    }

    private suspend fun getCommissionAssetFor(fee: Fee): Chain.Asset {
        val feePaymentAsset = fee.paymentAsset
        return when (feePaymentAsset) {
            is Fee.PaymentAsset.Asset -> chainRegistry.chainWithAsset(feePaymentAsset.assetId).asset
            Fee.PaymentAsset.Native -> chainFlow.first().commissionAsset
        }
    }

    private suspend fun changeCommissionAsset(asset: Chain.Asset) {
        val currentCommissionAsset = commissionChainAssetFlow.first()
        if (currentCommissionAsset.fullId != asset.fullId) {
            commissionChainAssetFlow.emit(asset)
        }
    }

    private suspend fun getTokenFor(asset: Chain.Asset): Token {
        val currentToken = commissionTokenFlow.first()
        if (currentToken.configuration.fullId == asset.fullId) {
            return currentToken
        }

        return interactor.assetFlow(asset).first().token
    }

    private suspend fun mapChangeFeeTokenState(
        selectedToken: Token,
        customFeeAsset: Chain.Asset,
        availableFeeAssets: List<Chain.Asset>
    ): ChangeFeeTokenState {
        val originChainAsset = selectedToken.configuration

        return when {
            originChainAsset.isCommissionAsset -> ChangeFeeTokenState.NotSupported

            (originChainAsset in availableFeeAssets) -> {
                val chain = chainRegistry.getChain(originChainAsset.chainId)
                val selectableAssets = listOf(chain.commissionAsset, originChainAsset)
                ChangeFeeTokenState.Editable(customFeeAsset, selectableAssets)
            }

            else -> ChangeFeeTokenState.NotSupported
        }
    }
}
