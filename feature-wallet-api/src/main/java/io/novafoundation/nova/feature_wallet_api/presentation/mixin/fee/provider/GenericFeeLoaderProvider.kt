package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.provider

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.firstNotNull
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.ChangeFeeTokenState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleFee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class GenericFeeLoaderProviderPresentation(
    resourceManager: ResourceManager,
    configuration: GenericFeeLoaderMixin.Configuration<SimpleFee>,
    tokenFlow: Flow<Token?>,
) : GenericFeeLoaderProvider<SimpleFee>(resourceManager, configuration, tokenFlow), FeeLoaderMixin.Presentation

internal open class GenericFeeLoaderProvider<F : GenericFee>(
    protected val resourceManager: ResourceManager,
    protected val configuration: GenericFeeLoaderMixin.Configuration<F>,
    protected val tokenFlow: Flow<Token?>,
) : GenericFeeLoaderMixin.Presentation<F> {

    final override val feeLiveData = MutableLiveData<FeeStatus<F>>()
    override val changeFeeTokenState: LiveData<ChangeFeeTokenState> = liveData { emit(ChangeFeeTokenState.NotSupported) }

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    init {
        configuration.initialState.feeStatus?.let(feeLiveData::postValue)
    }

    override suspend fun loadFeeSuspending(
        retryScope: CoroutineScope,
        feeConstructor: suspend (Token) -> F?,
        onRetryCancelled: () -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        feeLiveData.postValue(FeeStatus.Loading)

        val token = tokenFlow.firstNotNull()

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

    override fun commissionAssetFlow(): Flow<Asset> {
        throw IllegalStateException("commissionAssetFlow not supported")
    }

    override fun setCommissionAsset(chainAsset: Chain.Asset) {
        // Not supported
    }

    override suspend fun setFee(fee: F?) {
        if (fee != null) {
            val token = tokenFlow.firstNotNull()
            val feeModel = mapFeeToFeeModel(fee, token, includeZeroFiat = configuration.showZeroFiat)

            feeLiveData.postValue(FeeStatus.Loaded(feeModel))
        } else {
            feeLiveData.postValue(FeeStatus.NoFee)
        }
    }

    override suspend fun setFeeStatus(feeStatus: FeeStatus<F>) {
        feeLiveData.postValue(feeStatus)
    }

    override suspend fun setSupportCustomFee(supportCustomFee: Boolean) {
        // Not supported
    }

    override suspend fun invalidateFee() {
        feeLiveData.postValue(FeeStatus.Loading)
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
}
