package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import io.novafoundation.nova.common.base.BaseViewModel
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import java.math.BigDecimal
import java.math.BigInteger

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

interface GenericFeeLoaderMixin<F : GenericFee> : Retriable {

    class Configuration<F : GenericFee>(
        val showZeroFiat: Boolean = true,
        val initialStatusValue: FeeStatus<F>? = null
    )

    val feeLiveData: LiveData<FeeStatus<F>>

    interface Presentation<F : GenericFee> : GenericFeeLoaderMixin<F> {

        suspend fun loadFeeSuspending(
            retryScope: CoroutineScope,
            feeConstructor: suspend (Token) -> F?,
            onRetryCancelled: () -> Unit,
        )

        fun loadFeeV2Generic(
            coroutineScope: CoroutineScope,
            feeConstructor: suspend (Token) -> F?,
            onRetryCancelled: () -> Unit,
        )

        suspend fun setFee(fee: F?)

        fun invalidateFee()

        @Deprecated(
            message = "Use `awaitDecimalFee` instead since it holds more information about fee",
            replaceWith = ReplaceWith("awaitDecimalFee().decimalAmount")
        )
        suspend fun awaitFee(): BigDecimal = awaitDecimalFee().decimalAmount
    }

    interface Factory {

        fun <F : GenericFee> createGeneric(
            tokenFlow: Flow<Token?>,
            configuration: Configuration<F> = Configuration()
        ): Presentation<F>
    }
}

interface FeeLoaderMixin : GenericFeeLoaderMixin<SimpleFee> {

    // Additional methods in this interface are only for backward-compatibility to simplify migration of the old code
    interface Presentation : GenericFeeLoaderMixin.Presentation<SimpleFee>, FeeLoaderMixin {

        @Deprecated("Use loadFeeV2")
        fun loadFee(
            coroutineScope: CoroutineScope,
            feeConstructor: suspend (Token) -> BigInteger?,
            onRetryCancelled: () -> Unit,
        )

        @Deprecated("Use setFee(fee: GenericFee)")
        suspend fun setFee(feeAmount: BigDecimal?)

        @Deprecated("Use awaitFee()")
        fun requireFee(
            block: (BigDecimal) -> Unit,
            onError: (title: String, message: String) -> Unit,
        )

        suspend fun setFee(fee: Fee?) = setFee(fee?.let(::SimpleFee))

        @Deprecated("Use loadFeeV2Generic")
        fun loadFeeV2(
            coroutineScope: CoroutineScope,
            feeConstructor: suspend (Token) -> Fee?,
            onRetryCancelled: () -> Unit,
        ) = loadFeeV2Generic(
            coroutineScope = coroutineScope,
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

fun <F : GenericFee> GenericFeeLoaderMixin<F>.getFeeOrNull(): F? {
    return feeLiveData.value
        .castOrNull<FeeStatus.Loaded<F>>()
        ?.feeModel
        ?.decimalFee
        ?.genericFee
}

fun FeeLoaderMixin.Factory.create(assetFlow: Flow<Asset>) = create(assetFlow.map { it.token })
fun FeeLoaderMixin.Factory.create(tokenUseCase: TokenUseCase) = create(tokenUseCase.currentTokenFlow())

fun FeeLoaderMixin.Presentation.requireFee(
    viewModel: BaseViewModel,
    block: (BigDecimal) -> Unit,
) {
    requireFee(block) { title, message ->
        viewModel.showError(title, message)
    }
}

fun <I> FeeLoaderMixin.Presentation.connectWith(
    inputSource: Flow<I>,
    scope: CoroutineScope,
    feeConstructor: suspend Token.(input: I) -> BigInteger,
    onRetryCancelled: () -> Unit = {}
) {
    inputSource.onEach { input ->
        loadFee(
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
    feeConstructor: suspend Token.(input1: I1, input2: I2) -> BigInteger?,
    onRetryCancelled: () -> Unit = {}
) {
    combine(
        inputSource1,
        inputSource2
    ) { input1, input2 ->
        loadFee(
            coroutineScope = scope,
            feeConstructor = { feeConstructor(it, input1, input2) },
            onRetryCancelled = onRetryCancelled
        )
    }
        .inBackground()
        .launchIn(scope)
}

fun <I1, I2, I3> FeeLoaderMixin.Presentation.connectWith(
    inputSource1: Flow<I1>,
    inputSource2: Flow<I2>,
    inputSource3: Flow<I3>,
    scope: CoroutineScope,
    feeConstructor: suspend Token.(input1: I1, input2: I2, input3: I3) -> Fee?,
    onRetryCancelled: () -> Unit = {}
) {
    combine(
        inputSource1,
        inputSource2,
        inputSource3,
    ) { input1, input2, input3 ->
        loadFeeV2(
            coroutineScope = scope,
            feeConstructor = { feeConstructor(it, input1, input2, input3) },
            onRetryCancelled = onRetryCancelled
        )
    }
        .inBackground()
        .launchIn(scope)
}

fun <I> FeeLoaderMixin.Presentation.connectWithV2(
    inputSource: Flow<I>,
    scope: CoroutineScope,
    feeConstructor: suspend Token.(input: I) -> Fee,
    onRetryCancelled: () -> Unit = {}
) {
    inputSource.onEach { input ->
        loadFeeV2(
            coroutineScope = scope,
            feeConstructor = { feeConstructor(it, input) },
            onRetryCancelled = onRetryCancelled
        )
    }
        .inBackground()
        .launchIn(scope)
}
