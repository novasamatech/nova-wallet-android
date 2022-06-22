package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

class FeeLoaderProviderFactory(
    private val resourceManager: ResourceManager,
) : FeeLoaderMixin.Factory {

    override fun create(tokenFlow: Flow<Token>, configuration: FeeLoaderMixin.Configuration): FeeLoaderMixin.Presentation {
        return FeeLoaderProvider(resourceManager, configuration, tokenFlow)
    }
}

class FeeLoaderProvider(
    private val resourceManager: ResourceManager,
    private val configuration: FeeLoaderMixin.Configuration,
    private val tokenFlow: Flow<Token>,
) : FeeLoaderMixin.Presentation {

    override val feeLiveData = MutableLiveData<FeeStatus>()

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    override suspend fun loadFeeSuspending(
        retryScope: CoroutineScope,
        feeConstructor: suspend (Token) -> BigInteger?,
        onRetryCancelled: () -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        feeLiveData.postValue(FeeStatus.Loading)

        val token = tokenFlow.first()

        val value = runCatching {
            feeConstructor(token)
        }.fold(
            onSuccess = { feeInPlanks -> onFeeLoaded(token, feeInPlanks) },
            onFailure = { exception -> onError(exception, retryScope, feeConstructor, onRetryCancelled) }
        )

        value?.run { feeLiveData.postValue(this) }
    }

    override fun loadFee(
        coroutineScope: CoroutineScope,
        feeConstructor: suspend (Token) -> BigInteger?,
        onRetryCancelled: () -> Unit,
    ) {
        coroutineScope.launch {
            loadFeeSuspending(coroutineScope, feeConstructor, onRetryCancelled)
        }
    }

    override suspend fun setFee(fee: BigDecimal) {
        val token = tokenFlow.first()
        val feeModel = mapFeeToFeeModel(fee, token, includeZeroFiat = configuration.showZeroFiat)

        feeLiveData.postValue(FeeStatus.Loaded(feeModel))
    }

    override fun requireFee(
        block: (BigDecimal) -> Unit,
        onError: (title: String, message: String) -> Unit,
    ) {
        val feeStatus = feeLiveData.value

        if (feeStatus is FeeStatus.Loaded) {
            block(feeStatus.feeModel.fee)
        } else {
            onError(
                resourceManager.getString(R.string.fee_not_yet_loaded_title),
                resourceManager.getString(R.string.fee_not_yet_loaded_message)
            )
        }
    }

    private fun onFeeLoaded(
        token: Token,
        feeInPlanks: BigInteger?
    ): FeeStatus = if (feeInPlanks != null) {
        val fee = token.amountFromPlanks(feeInPlanks)
        val feeModel = mapFeeToFeeModel(fee, token, includeZeroFiat = configuration.showZeroFiat)

        FeeStatus.Loaded(feeModel)
    } else {
        FeeStatus.NoFee
    }

    fun onError(
        exception: Throwable,
        retryScope: CoroutineScope,
        feeConstructor: suspend (Token) -> BigInteger?,
        onRetryCancelled: () -> Unit,
    ) = if (exception !is CancellationException) {
        retryEvent.postValue(
            Event(
                RetryPayload(
                    title = resourceManager.getString(R.string.choose_amount_network_error),
                    message = resourceManager.getString(R.string.choose_amount_error_fee),
                    onRetry = { loadFee(retryScope, feeConstructor, onRetryCancelled) },
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
