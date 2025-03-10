package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin.Configuration
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform

interface GenericFeeLoaderMixin<F> : Retriable {

    class Configuration<F>(
        val showZeroFiat: Boolean = true,
        val initialState: InitialState<F> = InitialState()
    ) {

        class InitialState<F>(
            val feeStatus: FeeStatus<F, FeeDisplay>? = null
        )
    }

    val feeLiveData: LiveData<FeeStatus<F, FeeDisplay>>

    interface Presentation<F> : GenericFeeLoaderMixin<F>, SetFee<F> {

        suspend fun loadFeeSuspending(
            retryScope: CoroutineScope,
            feeConstructor: suspend (Token) -> F?,
            onRetryCancelled: () -> Unit,
        )

        fun loadFee(
            coroutineScope: CoroutineScope,
            feeConstructor: suspend (Token) -> F?,
            onRetryCancelled: () -> Unit,
        )

        suspend fun setFeeOrHide(fee: F?)
        suspend fun setFeeStatus(feeStatus: FeeStatus<F, FeeDisplay>)

        suspend fun invalidateFee()
    }
}

@Deprecated("Use FeeLoaderMixinV2 instead")
interface FeeLoaderMixin : GenericFeeLoaderMixin<Fee> {

    interface Presentation : GenericFeeLoaderMixin.Presentation<Fee>, FeeLoaderMixin

    interface Factory {

        fun create(
            tokenFlow: Flow<Token?>,
            configuration: Configuration<Fee> = Configuration()
        ): Presentation
    }
}

suspend fun <F : FeeBase> GenericFeeLoaderMixin<F>.awaitFee(): F = feeLiveData.asFlow()
    .filterIsInstance<FeeStatus.Loaded<F, FeeDisplay>>()
    .first()
    .feeModel.fee

suspend fun <F : FeeBase> GenericFeeLoaderMixin<F>.awaitOptionalFee(): F? = feeLiveData.asFlow()
    .transform { feeStatus ->
        when (feeStatus) {
            is FeeStatus.Loaded<F, FeeDisplay> -> emit(feeStatus.feeModel.fee)
            FeeStatus.NoFee -> emit(null)
            else -> {} // skip
        }
    }.first()

@Deprecated("Use createChangeableFee instead")
fun FeeLoaderMixin.Factory.create(assetFlow: Flow<Asset>) = create(assetFlow.map { it.token })

@Deprecated("Use createChangeableFee instead")
fun FeeLoaderMixin.Factory.create(tokenUseCase: TokenUseCase) = create(tokenUseCase.currentTokenFlow())

fun <I> FeeLoaderMixin.Presentation.connectWith(
    inputSource: Flow<I>,
    scope: CoroutineScope,
    feeConstructor: suspend Token.(input: I) -> Fee,
    onRetryCancelled: () -> Unit = {}
) {
    inputSource.onEach { input ->
        this.loadFee(
            coroutineScope = scope,
            feeConstructor = { token -> token.feeConstructor(input) },
            onRetryCancelled = onRetryCancelled
        )
    }
        .inBackground()
        .launchIn(scope)
}

fun <I1, I2> FeeLoaderMixin.Presentation.connectWith(
    inputSource1: Flow<I1>,
    inputSource2: Flow<I2>,
    scope: CoroutineScope,
    feeConstructor: suspend Token.(input1: I1, input2: I2) -> Fee,
    onRetryCancelled: () -> Unit = {}
) {
    combine(
        inputSource1,
        inputSource2
    ) { input1, input2 ->
        this.loadFee(
            coroutineScope = scope,
            feeConstructor = { token -> token.feeConstructor(input1, input2) },
            onRetryCancelled = onRetryCancelled
        )
    }
        .inBackground()
        .launchIn(scope)
}
