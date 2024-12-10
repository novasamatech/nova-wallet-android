package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

fun interface SetFee<F> {

    suspend fun setFee(fee: F)
}
