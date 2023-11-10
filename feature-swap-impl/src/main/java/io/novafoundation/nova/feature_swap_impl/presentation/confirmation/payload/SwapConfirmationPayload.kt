package io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload

import android.os.Parcelable
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.SwapFinishFlowDestination
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import java.math.BigDecimal
import kotlinx.android.parcel.Parcelize

@Parcelize
class SwapConfirmationPayload(
    val swapQuoteModel: SwapQuoteModel,
    val rate: BigDecimal,
    val slippage: Double,
    val feeAsset: AssetPayload,
    val swapFee: FeeDetails,
    val returnTo: SwapFinishFlowDestination
) : Parcelable {

    @Parcelize
    class SwapQuoteModel(
        val assetIn: AssetPayload,
        val assetOut: AssetPayload,
        val planksIn: Balance,
        val planksOut: Balance,
        val direction: Direction,
        val priceImpact: Double,
    ) : Parcelable

    @Parcelize
    class FeeDetails(
        val amount: Balance,
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

    @Parcelize
    enum class Direction : Parcelable {
        SPECIFIED_IN,
        SPECIFIED_OUT
    }
}
