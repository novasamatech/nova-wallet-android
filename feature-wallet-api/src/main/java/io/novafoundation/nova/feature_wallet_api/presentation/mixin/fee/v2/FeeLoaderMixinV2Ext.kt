package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

suspend fun <F> FeeLoaderMixinV2<F, *>.awaitFee(): F = fee
    .filterIsInstance<FeeStatus.Loaded<F, *>>()
    .first()
    .feeModel.fee

suspend fun <F> FeeLoaderMixinV2<F, *>.awaitOptionalFee(): F? = fee
    .transform { feeStatus ->
        when (feeStatus) {
            is FeeStatus.Loaded -> emit(feeStatus.feeModel.fee)
            FeeStatus.NoFee -> emit(null)
            else -> {} // skip
        }
    }.first()

context(BaseViewModel)
fun <F, I1> FeeLoaderMixinV2.Presentation<F, *>.connectWith(
    inputSource1: Flow<I1>,
    feeConstructor: suspend (FeePaymentCurrency, input1: I1) -> F?
) {
    inputSource1.map { input1 ->
        loadFee(
            feeConstructor = { paymentCurrency -> feeConstructor(paymentCurrency, input1) },
        )
    }
        .inBackground()
        .launchIn(this@BaseViewModel)
}

context(BaseViewModel)
fun <F, I1, I2> FeeLoaderMixinV2.Presentation<F, *>.connectWith(
    inputSource1: Flow<I1>,
    inputSource2: Flow<I2>,
    feeConstructor: suspend (FeePaymentCurrency, input1: I1, input2: I2) -> F?
) {
    combine(
        inputSource1,
        inputSource2
    ) { input1, input2 ->
        loadFee(
            feeConstructor = { paymentCurrency -> feeConstructor(paymentCurrency, input1, input2) },
        )
    }
        .inBackground()
        .launchIn(this@BaseViewModel)
}

context(BaseViewModel)
fun <F, I1, I2, I3> FeeLoaderMixinV2.Presentation<F, *>.connectWith(
    inputSource1: Flow<I1>,
    inputSource2: Flow<I2>,
    inputSource3: Flow<I3>,
    feeConstructor: suspend (FeePaymentCurrency, input1: I1, input2: I2, input3: I3) -> F,
) {
    combine(
        inputSource1,
        inputSource2,
        inputSource3
    ) { input1, input2, input3 ->
        loadFee(
            feeConstructor = { paymentCurrency -> feeConstructor(paymentCurrency, input1, input2, input3) },
        )
    }
        .inBackground()
        .launchIn(this@BaseViewModel)
}

context(BaseViewModel)
fun <F, I1, I2, I3, I4> FeeLoaderMixinV2.Presentation<F, *>.connectWith(
    inputSource1: Flow<I1>,
    inputSource2: Flow<I2>,
    inputSource3: Flow<I3>,
    inputSource4: Flow<I4>,
    feeConstructor: suspend (FeePaymentCurrency, input1: I1, input2: I2, input3: I3, input4: I4) -> F,
) {
    combine(
        inputSource1,
        inputSource2,
        inputSource3,
        inputSource4
    ) { input1, input2, input3, input4 ->
        loadFee(
            feeConstructor = { paymentCurrency -> feeConstructor(paymentCurrency, input1, input2, input3, input4) },
        )
    }
        .inBackground()
        .launchIn(this@BaseViewModel)
}
