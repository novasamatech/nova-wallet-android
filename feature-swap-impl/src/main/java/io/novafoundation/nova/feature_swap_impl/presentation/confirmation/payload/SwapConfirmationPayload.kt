package io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload

import android.os.Parcelable
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapDirectionModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class SwapConfirmationPayload(
    val swapQuoteModel: SwapQuoteModel,
    val rate: BigDecimal,
    val slippage: Double,
    val feeAsset: AssetPayload,
    val swapFee: FeeDetails
) : Parcelable {

    @Parcelize
    class SwapQuoteModel(
        val assetIn: AssetPayload,
        val assetOut: AssetPayload,
        val planksIn: Balance,
        val planksOut: Balance,
        val direction: SwapDirectionModel,
        val priceImpact: Double,
        val path: List<SwapQuotePathModel>
    ) : Parcelable

    @Parcelize
    class SwapQuotePathModel(
        val from: AssetPayload,
        val to: AssetPayload,
        val sourceId: String,
        val sourceParams: Map<String, String>
    ) : Parcelable

    @Parcelize
    class FeeDetails(
        val networkFee: FeeParcelModel,
        val minimumBalanceBuyIn: MinimumBalanceBuyIn
    ) : Parcelable {

        sealed interface MinimumBalanceBuyIn : Parcelable {

            @Parcelize
            class NeedsToBuyMinimumBalance(
                val nativeAsset: AssetPayload,
                val nativeMinimumBalance: Balance,
                val commissionAsset: AssetPayload,
                val commissionAssetToSpendOnBuyIn: Balance
            ) : MinimumBalanceBuyIn

            @Parcelize
            object NoBuyInNeeded : MinimumBalanceBuyIn
        }
    }
}
