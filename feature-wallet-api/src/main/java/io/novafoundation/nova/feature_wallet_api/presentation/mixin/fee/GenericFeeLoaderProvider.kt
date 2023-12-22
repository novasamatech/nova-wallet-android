package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.firstNotNull
import io.novafoundation.nova.feature_account_api.data.model.InlineFee
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class FeeLoaderProviderFactory(
    private val resourceManager: ResourceManager,
) : FeeLoaderMixin.Factory {

    override fun create(
        tokenFlow: Flow<Token?>,
        configuration: GenericFeeLoaderMixin.Configuration<SimpleFee>
    ): FeeLoaderMixin.Presentation {
        return FeeLoaderProvider(resourceManager, configuration, tokenFlow)
    }

    override fun <F : GenericFee> createGeneric(
        tokenFlow: Flow<Token?>,
        configuration: GenericFeeLoaderMixin.Configuration<F>
    ): GenericFeeLoaderMixin.Presentation<F> {
        return GenericFeeLoaderProvider(resourceManager, configuration, tokenFlow)
    }
}

private class FeeLoaderProvider(
    resourceManager: ResourceManager,
    configuration: GenericFeeLoaderMixin.Configuration<SimpleFee>,
    tokenFlow: Flow<Token?>,
) : GenericFeeLoaderProvider<SimpleFee>(resourceManager, configuration, tokenFlow), FeeLoaderMixin.Presentation {

    override suspend fun setFee(feeAmount: BigDecimal?) {
        val fee = feeAmount?.let {
            val token = tokenFlow.firstNotNull()
            InlineFee(token.planksFromAmount(feeAmount))
        }

        setFee(fee)
    }

    override fun requireFee(
        block: (BigDecimal) -> Unit,
        onError: (title: String, message: String) -> Unit,
    ) {
        val feeStatus = feeLiveData.value

        if (feeStatus is FeeStatus.Loaded) {
            block(feeStatus.feeModel.decimalFee.decimalAmount)
        } else {
            onError(
                resourceManager.getString(R.string.fee_not_yet_loaded_title),
                resourceManager.getString(R.string.fee_not_yet_loaded_message)
            )
        }
    }
}

private open class GenericFeeLoaderProvider<F : GenericFee>(
    protected val resourceManager: ResourceManager,
    protected val configuration: GenericFeeLoaderMixin.Configuration<F>,
    protected val tokenFlow: Flow<Token?>,
) : GenericFeeLoaderMixin.Presentation<F> {

    final override val feeLiveData = MutableLiveData<FeeStatus<F>>()

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    init {
        configuration.initialStatusValue?.let(feeLiveData::postValue)
    }

    override suspend fun loadFeeSuspending(
        retryScope: CoroutineScope,
        expectedChain: ChainId?,
        feeConstructor: suspend (Token) -> F?,
        onRetryCancelled: () -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        feeLiveData.postValue(FeeStatus.Loading)

        val token = if (expectedChain != null) {
            tokenFlow.filterNotNull().first { it.configuration.chainId == expectedChain }
        } else {
            tokenFlow.firstNotNull()
        }

        val value = runCatching {
            feeConstructor(token)
        }.fold(
            onSuccess = { genericFee -> onFeeLoaded(token, genericFee) },
            onFailure = { exception -> onError(exception, retryScope, expectedChain, feeConstructor, onRetryCancelled) }
        )

        value?.run { feeLiveData.postValue(this) }
    }

    override fun loadFeeV2Generic(
        coroutineScope: CoroutineScope,
        expectedChain: ChainId?,
        feeConstructor: suspend (Token) -> F?,
        onRetryCancelled: () -> Unit
    ) {
        coroutineScope.launch {
            loadFeeSuspending(
                retryScope = coroutineScope,
                expectedChain = expectedChain,
                feeConstructor = feeConstructor,
                onRetryCancelled = onRetryCancelled
            )
        }
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

    override fun invalidateFee() {
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
        expectedChain: ChainId?,
        feeConstructor: suspend (Token) -> F?,
        onRetryCancelled: () -> Unit,
    ) = if (exception !is CancellationException) {
        retryEvent.postValue(
            Event(
                RetryPayload(
                    title = resourceManager.getString(R.string.choose_amount_network_error),
                    message = resourceManager.getString(R.string.choose_amount_error_fee),
                    onRetry = { loadFeeV2Generic(retryScope, expectedChain, feeConstructor, onRetryCancelled) },
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
