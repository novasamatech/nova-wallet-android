package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin.Configuration
import io.novafoundation.nova.feature_wallet_api.presentation.model.FeeModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform

sealed class FeeStatus<out F : FeeBase> {
    object Loading : FeeStatus<Nothing>()

    class Loaded<F : FeeBase>(val feeModel: FeeModel<F>) : FeeStatus<F>()

    object NoFee : FeeStatus<Nothing>()

    object Error : FeeStatus<Nothing>()
}

sealed interface ChangeFeeTokenState {

    class Editable(val selectedCommissionAsset: Chain.Asset, val availableAssets: List<Chain.Asset>) : ChangeFeeTokenState

    object NotSupported : ChangeFeeTokenState
}


interface GenericFeeLoaderMixin<F : FeeBase> : Retriable {

    class Configuration<F : FeeBase>(
        val showZeroFiat: Boolean = true,
        val initialState: InitialState<F> = InitialState()
    ) {

        class InitialState<F : FeeBase>(
            val supportCustomFee: Boolean = false,
            val feeStatus: FeeStatus<F>? = null
        )
    }

    val feeLiveData: LiveData<FeeStatus<F>>

    val changeFeeTokenState: LiveData<ChangeFeeTokenState>

    fun setCommissionAsset(chainAsset: Chain.Asset)

    interface Presentation<F : FeeBase> : GenericFeeLoaderMixin<F> {

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

        suspend fun setFee(fee: F?)

        suspend fun setFeeStatus(feeStatus: FeeStatus<F>)

        suspend fun setSupportCustomFee(supportCustomFee: Boolean)

        suspend fun invalidateFee()

        fun commissionAssetFlow(): Flow<Asset>
    }

    interface Factory {

        @Deprecated("Use createChangeableFeeGeneric instead")
        fun <F : FeeBase> createGeneric(
            tokenFlow: Flow<Token?>,
            configuration: Configuration<F> = Configuration()
        ): Presentation<F>

        fun <F : FeeBase> createChangeable(
            tokenFlow: Flow<Token?>,
            coroutineScope: CoroutineScope,
            configuration: Configuration<F> = Configuration()
        ): Presentation<F>
    }
}

@Deprecated("Use GenericFeeLoaderMixin instead")
interface FeeLoaderMixin : GenericFeeLoaderMixin<Fee> {

    interface Presentation : GenericFeeLoaderMixin.Presentation<Fee>, FeeLoaderMixin

    interface Factory : GenericFeeLoaderMixin.Factory {

        fun create(
            tokenFlow: Flow<Token?>,
            configuration: Configuration<Fee> = Configuration()
        ): Presentation
    }
}

suspend fun <F : FeeBase> GenericFeeLoaderMixin<F>.awaitFee(): F = feeLiveData.asFlow()
    .filterIsInstance<FeeStatus.Loaded<F>>()
    .first()
    .feeModel.fee

suspend fun <F : FeeBase> GenericFeeLoaderMixin<F>.awaitOptionalFee(): F? = feeLiveData.asFlow()
    .transform { feeStatus ->
        when (feeStatus) {
            is FeeStatus.Loaded -> emit(feeStatus.feeModel.fee)
            FeeStatus.NoFee -> emit(null)
            else -> {} // skip
        }
    }.first()

fun <F : Fee> GenericFeeLoaderMixin<F>.loadedFeeModelOrNullFlow(): Flow<FeeModel<F>?> {
    return feeLiveData
        .asFlow()
        .map { it.castOrNull<FeeStatus.Loaded<F>>()?.feeModel }
}

@Deprecated("Use createGenericChangeableFee instead")
fun <F : FeeBase> GenericFeeLoaderMixin.Factory.createGeneric(assetFlow: Flow<Asset>):  GenericFeeLoaderMixin.Presentation<F> = createGeneric(assetFlow.map { it.token })

fun GenericFeeLoaderMixin.Factory.createSimple(assetFlow: Flow<Asset>) = createGeneric<FeeBase>(assetFlow.map { it.token })

fun <F : Fee> GenericFeeLoaderMixin.Factory.createChangeable(
    assetFlow: Flow<Asset>,
    coroutineScope: CoroutineScope,
    configuration: Configuration<F> = Configuration()
) : GenericFeeLoaderMixin.Presentation<F> = createChangeable(assetFlow.map { it.token }, coroutineScope, configuration)

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

fun ChangeFeeTokenState.isEditable() = this is ChangeFeeTokenState.Editable

suspend fun GenericFeeLoaderMixin.Presentation<*>.commissionAsset(): Asset {
    return commissionAssetFlow().first()
}
