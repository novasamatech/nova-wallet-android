package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.data.model.EvmFee
import io.novafoundation.nova.feature_account_api.data.model.InlineFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.math.BigInteger

sealed interface FeeParcelModel : Parcelable {

    val amount: BigDecimal
}

@Parcelize
class EvmFeeParcelModel(
    val gasLimit: BigInteger,
    val gasPrice: BigInteger,
    override val amount: BigDecimal
): FeeParcelModel

@Parcelize
class SimpleFeeParcelModel(
    val planks: BigInteger,
    override val amount: BigDecimal
): FeeParcelModel


fun mapFeeToParcel(decimalFee: DecimalFee): FeeParcelModel {
    return when(val fee = decimalFee.fee) {
        is EvmFee -> EvmFeeParcelModel(gasLimit = fee.gasLimit, gasPrice = fee.gasPrice, amount = decimalFee.decimalAmount)
        else -> SimpleFeeParcelModel(decimalFee.fee.amount, decimalFee.decimalAmount)
    }
}

fun mapFeeFromParcel(parcelFee: FeeParcelModel): DecimalFee {
    val fee = when(parcelFee) {
        is EvmFeeParcelModel -> EvmFee(gasLimit = parcelFee.gasLimit, gasPrice = parcelFee.gasPrice)
        is SimpleFeeParcelModel -> InlineFee(parcelFee.planks)
    }

    return DecimalFee(fee, parcelFee.amount)
}
