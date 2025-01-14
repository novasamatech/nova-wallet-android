package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model

sealed class FeeStatus<out F, out D> {
    class Loading(val visibleDuringProgress: Boolean) : FeeStatus<Nothing, Nothing>()

    class Loaded<F, D>(val feeModel: FeeModel<F, D>) : FeeStatus<F, D>()

    object NoFee : FeeStatus<Nothing, Nothing>()

    object Error : FeeStatus<Nothing, Nothing>()
}

fun <F, D1 : Any, D2 : Any> FeeStatus<F, D1>.mapDisplay(map: (D1) -> D2?): FeeStatus<F, D2> {
    return when (this) {
        FeeStatus.Error -> FeeStatus.Error

        is FeeStatus.Loaded -> {
            val newFeeDisplay = map(feeModel.display)

            if (newFeeDisplay == null) {
                FeeStatus.NoFee
            } else {
                FeeStatus.Loaded(FeeModel(feeModel.fee, newFeeDisplay))
            }
        }

        is FeeStatus.Loading -> this

        FeeStatus.NoFee -> FeeStatus.NoFee
    }
}

fun <F, D> FeeStatus<F, D>.mapProgress(map: (Boolean) -> Boolean): FeeStatus<F, D> {
    return when (this) {
        FeeStatus.Error -> FeeStatus.Error

        is FeeStatus.Loaded -> this

        is FeeStatus.Loading -> FeeStatus.Loading(map(visibleDuringProgress))

        FeeStatus.NoFee -> FeeStatus.NoFee
    }
}

inline fun <F, D> FeeStatus<F, D>.onLoaded(action: (FeeModel<F, D>) -> Unit) {
    if (this is FeeStatus.Loaded<F, D>) {
        action(feeModel)
    }
}
