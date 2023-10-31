package io.novafoundation.nova.feature_swap_impl.presentation.confirmation

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.android.parcel.Parcelize

@Parcelize
class SwapConfirmationPayload(
    val amountWithAssetIn: AmountWithAsset,
    val amountWithAssetOut: AmountWithAsset,
    val rate: BigDecimal,
    val priceDifference: Double,
    val slippage: Double,
    val networkFee: AmountWithAsset
) : Parcelable {

    @Parcelize
    class AmountWithAsset(
        val amount: BigInteger,
        val assetPayload: AssetPayload
    ) : Parcelable
}
