package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin.Configuration
import io.novafoundation.nova.feature_wallet_api.presentation.model.GenericDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.GenericFeeModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform

sealed class FeeStatus<out F : GenericFee> {
    object Loading : FeeStatus<Nothing>()

    class Loaded<F : GenericFee>(val feeModel: GenericFeeModel<F>) : FeeStatus<F>()

    object NoFee : FeeStatus<Nothing>()

    object Error : FeeStatus<Nothing>()
}

interface GenericFee {

    val networkFee: Fee
}

@JvmInline
value class SimpleFee(override val networkFee: Fee) : GenericFee

class SimpleGenericFee<T : Fee>(override val networkFee: T) : GenericFee

interface GenericFeeLoaderMixin<F : GenericFee> : Retriable {

    class Configuration<F : GenericFee>(
        val showZeroFiat: Boolean = true,
        val initialStatusValue: FeeStatus<F>? = null
    )

    val feeLiveData: LiveData<FeeStatus<F>>

    interface Presentation<F : GenericFee> : GenericFeeLoaderMixin<F> {

        suspend fun loadFeeSuspending(
            retryScope: CoroutineScope,
            expectedChain: ChainId? = null,
            feeConstructor: suspend (Token) -> F?,
            onRetryCancelled: () -> Unit,
        )

        /**
         * @param expectedChain - Specify to force `feeConstructor` to wait until Token corresponds to the given `expectedChain`
         * Useful when `tokenFlow` that mixin was initialized with can switch chains
         */
        fun loadFeeV2Generic(
            coroutineScope: CoroutineScope,
            expectedChain: ChainId? = null,
            feeConstructor: suspend (Token) -> F?,
            onRetryCancelled: () -> Unit,
        )

        suspend fun setFee(fee: F?)

        fun invalidateFee()
    }

    interface Factory {

        fun <F : GenericFee> createGeneric(
            tokenFlow: Flow<Token?>,
            configuration: Configuration<F> = Configuration()
        ): Presentation<F>
    }
}

interface FeeLoaderMixin : GenericFeeLoaderMixin<SimpleFee> {

    interface Presentation : GenericFeeLoaderMixin.Presentation<SimpleFee>, FeeLoaderMixin {

        fun loadFee(
            coroutineScope: CoroutineScope,
            expectedChain: ChainId? = null,
            feeConstructor: suspend (Token) -> Fee?,
            onRetryCancelled: () -> Unit,
        ) = loadFeeV2Generic(
            coroutineScope = coroutineScope,
            expectedChain = expectedChain,
            feeConstructor = { token -> feeConstructor(token)?.let(::SimpleFee) },
            onRetryCancelled = onRetryCancelled
        )
    }

    interface Factory : GenericFeeLoaderMixin.Factory {

        fun create(
            tokenFlow: Flow<Token?>,
            configuration: Configuration<SimpleFee> = Configuration()
        ): Presentation
    }
}

suspend fun <F : GenericFee> GenericFeeLoaderMixin<F>.awaitDecimalFee(): GenericDecimalFee<F> = feeLiveData.asFlow()
    .filterIsInstance<FeeStatus.Loaded<F>>()
    .first().feeModel.decimalFee

suspend fun <F : GenericFee> GenericFeeLoaderMixin<F>.awaitOptionalDecimalFee(): GenericDecimalFee<F>? = feeLiveData.asFlow()
    .transform { feeStatus ->
        when (feeStatus) {
            is FeeStatus.Loaded -> emit(feeStatus.feeModel.decimalFee)
            FeeStatus.NoFee -> emit(null)
            else -> {} // skip
        }
    }.first()

fun <F : GenericFee> GenericFeeLoaderMixin<F>.loadedFeeOrNullFlow(): Flow<F?> {
    return feeLiveData.asFlow().map {
        it.castOrNull<FeeStatus.Loaded<F>>()?.feeModel?.decimalFee?.genericFee
    }
}

fun <F : GenericFee> GenericFeeLoaderMixin<F>.loadedDecimalFeeOrNullFlow(): Flow<GenericDecimalFee<F>?> {
    return feeLiveData.asFlow().map {
        it.castOrNull<FeeStatus.Loaded<F>>()?.feeModel?.decimalFee
    }
}

fun <F : GenericFee> GenericFeeLoaderMixin<F>.loadedFeeModelOrNullFlow(): Flow<GenericFeeModel<F>?> {
    return feeLiveData
        .asFlow()
        .map { it.castOrNull<FeeStatus.Loaded<F>>()?.feeModel }
}

fun <F : GenericFee> GenericFeeLoaderMixin<F>.getDecimalFeeOrNull(): GenericDecimalFee<F>? {
    return feeLiveData.value
        .castOrNull<FeeStatus.Loaded<F>>()
        ?.feeModel
        ?.decimalFee
}

fun <T : GenericFee> FeeLoaderMixin.Factory.createGeneric(assetFlow: Flow<Asset>) = createGeneric<T>(assetFlow.map { it.token })
fun FeeLoaderMixin.Factory.create(assetFlow: Flow<Asset>) = create(assetFlow.map { it.token })
fun FeeLoaderMixin.Factory.create(tokenUseCase: TokenUseCase) = create(tokenUseCase.currentTokenFlow())

fun <I1, I2, I3, I4, TFee : GenericFee> GenericFeeLoaderMixin.Presentation<TFee>.connectWith(
    inputSource1: Flow<I1>,
    inputSource2: Flow<I2>,
    inputSource3: Flow<I3>,
    inputSource4: Flow<I4>,
    scope: CoroutineScope,
    expectedChain: ((I1, I2, I3, I4) -> ChainId)? = null,
    feeConstructor: suspend Token.(input1: I1, input2: I2, input3: I3, input4: I4) -> TFee?,
    onRetryCancelled: () -> Unit = {}
) {
    combine(
        inputSource1,
        inputSource2,
        inputSource3,
        inputSource4
    ) { input1, input2, input3, input4 ->
        loadFeeV2Generic(
            coroutineScope = scope,
            expectedChain = expectedChain?.invoke(input1, input2, input3, input4),
            feeConstructor = { feeConstructor(it, input1, input2, input3, input4) },
            onRetryCancelled = onRetryCancelled
        )
    }
        .inBackground()
        .launchIn(scope)
}

fun <I> FeeLoaderMixin.Presentation.connectWith(
    inputSource: Flow<I>,
    scope: CoroutineScope,
    feeConstructor: suspend Token.(input: I) -> Fee,
    onRetryCancelled: () -> Unit = {}
) {
    inputSource.onEach { input ->
        this.loadFee(
            coroutineScope = scope,
            feeConstructor = { feeConstructor(it, input) },
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
            feeConstructor = { feeConstructor(it, input1, input2) },
            onRetryCancelled = onRetryCancelled
        )
    }
        .inBackground()
        .launchIn(scope)
}
