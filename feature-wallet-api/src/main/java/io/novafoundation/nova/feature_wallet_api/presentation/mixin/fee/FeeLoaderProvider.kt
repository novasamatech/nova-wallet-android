package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.InlineFee
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
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
        feeConstructor: suspend (Token) -> Fee?,
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
            loadFeeSuspending(
                retryScope = coroutineScope,
                feeConstructor = { feeConstructor(it)?.let(::InlineFee) },
                onRetryCancelled = onRetryCancelled
            )
        }
    }

    override fun loadFeeV2(
        coroutineScope: CoroutineScope,
        feeConstructor: suspend (Token) -> Fee?,
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

    override suspend fun setFee(fee: Fee?) {
        if (fee != null) {
            val token = tokenFlow.first()
            val feeModel = mapFeeToFeeModel(fee, token, includeZeroFiat = configuration.showZeroFiat)

            feeLiveData.postValue(FeeStatus.Loaded(feeModel))
        } else {
            feeLiveData.postValue(FeeStatus.NoFee)
        }
    }

    override suspend fun setFee(feeAmount: BigDecimal?) {
        val fee = feeAmount?.let {
            val token = tokenFlow.first()
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

    override fun requireOptionalFee(
        block: (BigDecimal?) -> Unit,
        onError: (title: String, message: String) -> Unit
    ) {
        when (val status = feeLiveData.value) {
            is FeeStatus.Loaded -> block(status.feeModel.decimalFee.decimalAmount)
            is FeeStatus.NoFee -> block(null)
            else -> onError(
                resourceManager.getString(R.string.fee_not_yet_loaded_title),
                resourceManager.getString(R.string.fee_not_yet_loaded_message)
            )
        }
    }

    private fun onFeeLoaded(
        token: Token,
        fee: Fee?
    ): FeeStatus = if (fee != null) {
        val feeModel = mapFeeToFeeModel(fee, token, includeZeroFiat = configuration.showZeroFiat)

        FeeStatus.Loaded(feeModel)
    } else {
        FeeStatus.NoFee
    }

    private fun onError(
        exception: Throwable,
        retryScope: CoroutineScope,
        feeConstructor: suspend (Token) -> Fee?,
        onRetryCancelled: () -> Unit,
    ) = if (exception !is CancellationException) {
        retryEvent.postValue(
            Event(
                RetryPayload(
                    title = resourceManager.getString(R.string.choose_amount_network_error),
                    message = resourceManager.getString(R.string.choose_amount_error_fee),
                    onRetry = { loadFeeV2(retryScope, feeConstructor, onRetryCancelled) },
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
