package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.provider

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.asLiveData
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
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
),
    FeeLoaderMixin.Presentation

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

    private val chainFlow = selectedTokenFlow
        .distinctUntilChangedBy { it.configuration.chainId }
        .map { chainRegistry.getChain(it.configuration.chainId) }

    private val supportCustomFeeFlow = MutableStateFlow(configuration.initialState.supportCustomFee)

    private val availableFeeAssetsFlow: Flow<Map<Int, Chain.Asset>> = combine(supportCustomFeeFlow, selectedTokenFlow) { supportCustomFee, selectedToken ->
        if (!supportCustomFee) return@combine emptyMap()

        interactor.availableCommissionAssetFor(selectedToken.configuration, coroutineScope)
            .associateBy { it.id }
    }

    private val commissionChainAssetFlow = singleReplaySharedFlow<Chain.Asset>()

    private val commissionAssetFlow: Flow<Asset> = commissionChainAssetFlow
        .distinctUntilChangedBy { it.fullId }
        .flatMapLatest { interactor.assetFlow(it) }

    final override val feeLiveData = MutableLiveData<FeeStatus<F>>()

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    val changeFeeTokenEvent = actionAwaitableMixinFactory.create<FeeAssetSelectorBottomSheet.Payload, Chain.Asset>()
    private val feeMayChangeAutomaticallyFlow = MutableStateFlow(true)

    override val changeFeeTokenState = combine(
        supportCustomFeeFlow,
        selectedTokenFlow,
        commissionChainAssetFlow,
        availableFeeAssetsFlow
    ) { supportCustomFee, selectedToken, selectedCommissionAsset, availableFeeAssets ->
        mapChangeFeeTokenState(supportCustomFee, selectedToken, selectedCommissionAsset, availableFeeAssets)
    }.asLiveData(coroutineScope)

    init {
        configuration.initialState.feeStatus?.let { feeLiveData.postValue(it) }

        setupCustomFee()
    }

    override suspend fun loadFeeSuspending(
        retryScope: CoroutineScope,
        feeConstructor: suspend (Token) -> F?,
        onRetryCancelled: () -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        postFeeState(FeeStatus.Loading)

        val token = commissionAssetFlow.first().token

        val value = runCatching {
            feeConstructor(token)
        }.fold(
            onSuccess = { genericFee -> onFeeLoaded(token, genericFee) },
            onFailure = { exception -> onError(exception, retryScope, feeConstructor, onRetryCancelled) }
        )

        value?.run { postFeeState(this) }
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
            val token = commissionAssetFlow.first().token

            val feeModel = mapFeeToFeeModel(fee, token, includeZeroFiat = configuration.showZeroFiat)

            postFeeState(FeeStatus.Loaded(feeModel))
        } else {
            postFeeState(FeeStatus.NoFee)
        }
    }

    override suspend fun setFeeStatus(feeStatus: FeeStatus<F>) {
        postFeeState(feeStatus)
    }

    override suspend fun setSupportCustomFee(supportCustomFee: Boolean) {
        supportCustomFeeFlow.value = supportCustomFee
    }

    override suspend fun invalidateFee() {
        postFeeState(FeeStatus.Loading)
    }

    override suspend fun commissionChainAsset(): Chain.Asset {
        return commissionChainAssetFlow.first()
    }

    override suspend fun commissionAsset(): Asset {
        return commissionAssetFlow.first()
    }

    override fun commissionAssetFlow(): Flow<Asset> {
        return commissionAssetFlow
    }

    override fun setCommissionAsset(chainAsset: Chain.Asset) {
        coroutineScope.launch {
            feeMayChangeAutomaticallyFlow.value = false
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
        // After chain is changed make commission asset default and reset feeMayChangeAutomaticallyFlag
        chainFlow.onEach {
            feeMayChangeAutomaticallyFlow.value = true
            commissionChainAssetFlow.emit(it.commissionAsset)
        }.launchIn(coroutineScope)

        // After supportCustomFee is changed make commission asset default
        supportCustomFeeFlow.onEach {
            commissionChainAssetFlow.emit(chainFlow.first().commissionAsset)
        }.launchIn(coroutineScope)
    }

    private suspend fun postFeeState(feeStatus: FeeStatus<F>) {
        if (feeStatus !is FeeStatus.Loaded) {
            feeLiveData.postValue(feeStatus)
            return
        }

        val feeMayChangeAutomatically = feeMayChangeAutomaticallyFlow.first()
        val supportCustomFee = supportCustomFeeFlow.first()
        if (!feeMayChangeAutomatically || !supportCustomFee) {
            feeLiveData.postValue(feeStatus)
            return
        }

        val commissionAsset = commissionAssetFlow.first()
        val selectedToken = selectedTokenFlow.first()
        val availableFeeAssets = availableFeeAssetsFlow.first()

        val selectedAssetIsAvailableToPayFee = selectedToken.configuration.id in availableFeeAssets
        if (commissionAsset.isCommissionAsset() && selectedAssetIsAvailableToPayFee) {
            val feeAmount = feeStatus.feeModel.decimalFee.networkFee.amount
            if (interactor.hasEnoughBalanceToPayFee(commissionAsset, feeAmount)) {
                feeLiveData.postValue(feeStatus)
            } else {
                // Select custom fee asset
                commissionChainAssetFlow.emit(selectedToken.configuration)
            }
        } else {
            feeLiveData.postValue(feeStatus)
        }
    }

    private suspend fun mapChangeFeeTokenState(
        supportCustomFee: Boolean,
        selectedToken: Token,
        customFeeAsset: Chain.Asset,
        availableFeeAssets: Map<Int, Chain.Asset>
    ): ChangeFeeTokenState {
        val originChainAsset = selectedToken.configuration

        return when {
            !supportCustomFee -> ChangeFeeTokenState.NotSupported

            originChainAsset.isCommissionAsset -> ChangeFeeTokenState.NotSupported

            (originChainAsset.id in availableFeeAssets) -> {
                val chain = chainRegistry.getChain(originChainAsset.chainId)
                val selectableAssets = listOf(chain.commissionAsset, originChainAsset)
                ChangeFeeTokenState.Editable(customFeeAsset, selectableAssets)
            }

            else -> ChangeFeeTokenState.NotSupported
        }
    }

    private fun Asset.isCommissionAsset(): Boolean {
        return token.configuration.isCommissionAsset
    }
}
