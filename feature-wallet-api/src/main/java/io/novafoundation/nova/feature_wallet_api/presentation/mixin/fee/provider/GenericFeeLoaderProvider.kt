package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.provider

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.firstNotNull
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.fee.FeeInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.DefaultFeeFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.FeeFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.formatFeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Deprecated("Use ChangeableFeeLoaderProviderPresentation instead")
internal class GenericFeeLoaderProviderPresentation(
    interactor: FeeInteractor,
    resourceManager: ResourceManager,
    configuration: GenericFeeLoaderMixin.Configuration<Fee>,
    tokenFlow: Flow<Token?>,
    amountFormatter: AmountFormatter
) : GenericFeeLoaderProvider<Fee>(
    resourceManager = resourceManager,
    interactor = interactor,
    configuration = configuration,
    tokenFlow = tokenFlow,
    feeFormatter = DefaultFeeFormatter(amountFormatter)
),
    FeeLoaderMixin.Presentation

@Deprecated("Use ChangeableFeeLoaderProvider instead")
internal open class GenericFeeLoaderProvider<F>(
    private val resourceManager: ResourceManager,
    private val interactor: FeeInteractor,
    private val configuration: GenericFeeLoaderMixin.Configuration<F>,
    private val tokenFlow: Flow<Token?>,
    private val feeFormatter: FeeFormatter<F, FeeDisplay>,
) : GenericFeeLoaderMixin.Presentation<F>, FeeFormatter.Context {

    private val feeFormatterConfiguration = configuration.toFeeFormatterConfiguration()

    final override val feeLiveData = MutableLiveData<FeeStatus<F, FeeDisplay>>()

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    init {
        configuration.initialState.feeStatus?.let(feeLiveData::postValue)
    }

    override suspend fun loadFeeSuspending(
        retryScope: CoroutineScope,
        feeConstructor: suspend (Token) -> F?,
        onRetryCancelled: () -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        feeLiveData.postValue(FeeStatus.Loading(visibleDuringProgress = true))

        val token = tokenFlow.firstNotNull()

        val value = runCatching {
            feeConstructor(token)
        }.fold(
            onSuccess = { fee -> feeFormatter.formatFeeStatus(fee, feeFormatterConfiguration) },
            onFailure = { exception -> onError(exception, retryScope, feeConstructor, onRetryCancelled) }
        )

        value?.run { feeLiveData.postValue(this) }
    }

    override fun loadFee(
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

    override suspend fun setFeeOrHide(fee: F?) {
        val feeStatus = feeFormatter.formatFeeStatus(fee, feeFormatterConfiguration)
        feeLiveData.postValue(feeStatus)
    }

    override suspend fun setFee(fee: F) {
        setFeeOrHide(fee as F?)
    }

    override suspend fun setFeeStatus(feeStatus: FeeStatus<F, FeeDisplay>) {
        feeLiveData.postValue(feeStatus)
    }

    override suspend fun invalidateFee() {
        feeLiveData.postValue(FeeStatus.Loading(visibleDuringProgress = true))
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

    private fun GenericFeeLoaderMixin.Configuration<*>.toFeeFormatterConfiguration(): FeeFormatter.Configuration {
        return FeeFormatter.Configuration(showZeroFiat)
    }

    override suspend fun token(chainAsset: Chain.Asset): Token {
        return interactor.getToken(chainAsset)
    }
}
