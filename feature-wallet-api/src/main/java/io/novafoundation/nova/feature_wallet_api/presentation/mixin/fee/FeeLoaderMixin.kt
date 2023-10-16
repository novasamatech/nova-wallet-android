package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.FeeModel
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

sealed class FeeStatus {
    object Loading : FeeStatus()

    class Loaded(val feeModel: FeeModel) : FeeStatus()

    object NoFee : FeeStatus()

    object Error : FeeStatus()
}

interface FeeLoaderMixin : Retriable {

    class Configuration(
        val showZeroFiat: Boolean = true
    )

    val feeLiveData: LiveData<FeeStatus>

    interface Presentation : FeeLoaderMixin {

        suspend fun loadFeeSuspending(
            retryScope: CoroutineScope,
            feeConstructor: suspend (Token) -> Fee?,
            onRetryCancelled: () -> Unit,
        )

        fun loadFee(
            coroutineScope: CoroutineScope,
            feeConstructor: suspend (Token) -> BigInteger?,
            onRetryCancelled: () -> Unit,
        )



        fun loadFeeV2(
            coroutineScope: CoroutineScope,
            feeConstructor: suspend (Token) -> Fee?,
            onRetryCancelled: () -> Unit,
        )

        suspend fun setFee(fee: Fee?)

        fun invalidateFee()

        suspend fun setFee(feeAmount: BigDecimal?)

        fun requireFee(
            block: (BigDecimal) -> Unit,
            onError: (title: String, message: String) -> Unit,
        )

        @Deprecated(
            message = "Use `awaitDecimalFee` instead since it holds more information about fee",
            replaceWith = ReplaceWith("awaitDecimalFee().decimalAmount")
        )
        suspend fun awaitFee(): BigDecimal = awaitDecimalFee().decimalAmount

        fun requireOptionalFee(
            block: (BigDecimal?) -> Unit,
            onError: (title: String, message: String) -> Unit,
        )
    }

    interface Factory {

        fun create(
            tokenFlow: Flow<Token>,
            configuration: Configuration = Configuration()
        ): Presentation
    }
}

suspend fun FeeLoaderMixin.awaitDecimalFee(): DecimalFee = feeLiveData.asFlow()
    .filterIsInstance<FeeStatus.Loaded>()
    .first().feeModel.decimalFee

suspend fun FeeLoaderMixin.awaitOptionalDecimalFee(): DecimalFee? = feeLiveData.asFlow()
    .transform { feeStatus ->
        when (feeStatus) {
            is FeeStatus.Loaded -> emit(feeStatus.feeModel.decimalFee)
            FeeStatus.NoFee -> emit(null)
            else -> {} // skip
        }
    }.first()

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

fun FeeLoaderMixin.Presentation.requireOptionalFee(
    viewModel: BaseViewModel,
    block: (BigDecimal?) -> Unit,
) {
    requireOptionalFee(block) { title, message ->
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
