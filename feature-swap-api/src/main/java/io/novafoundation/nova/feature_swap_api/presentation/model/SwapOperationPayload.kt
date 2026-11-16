package io.novafoundation.nova.feature_swap_api.presentation.model

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

/** An immutable, restorable snapshot of the operation the user reviewed. No services or mutable form state. */
@Parcelize
data class SwapOperationPayload(
    val metaAccountId: Long,
    val quote: SwapQuotePayload,
    val slippage: Double,
    val fee: SwapFeePayload,
    val returnToTab: Boolean,
) : Parcelable

@Parcelize
data class SwapQuotePayload(
    val assetIn: AssetPayload,
    val assetOut: AssetPayload,
    val amountIn: BigInteger,
    val amountOut: BigInteger,
    val priceImpact: Double,
    val direction: SwapDirectionParcel,
    val route: List<SwapRouteEdgePayload>,
    val roughFeeIn: BigInteger,
    val roughFeeOut: BigInteger,
    val executionMillis: List<Long>,
    val executionBufferMillis: Long,
) : Parcelable

@Parcelize
data class SwapRouteEdgePayload(
    val assetIn: AssetPayload,
    val assetOut: AssetPayload,
    val exchange: String,
    val pool: String?,
    val quotedAmount: BigInteger,
    val quote: BigInteger,
) : Parcelable

@Parcelize
data class SwapFeePayload(
    val segments: List<SwapFeeSegmentPayload>,
    val intermediateFees: SwapFeeAmountPayload,
    val maxAdditionalDeduction: BigInteger,
) : Parcelable

@Parcelize
data class SwapFeeSegmentPayload(
    val assetIn: AssetPayload,
    val assetOut: AssetPayload,
    val submissionFee: SwapFeeAmountPayload,
    val paidByAccount: List<SwapFeeAmountPayload>,
    val paidFromAmount: List<SwapFeeAmountPayload>,
    val serviceCommission: SwapFeeAmountPayload?,
    val displayComponents: List<SwapFeeComponentPayload>,
    val amountIn: BigInteger,
    val amountOut: BigInteger,
    val amountOutMin: BigInteger,
) : Parcelable

@Parcelize
data class SwapFeeAmountPayload(
    val asset: AssetPayload,
    val amount: BigInteger,
    val executingAccount: ByteArray? = null,
    val signingAccount: ByteArray? = null,
    val label: String? = null,
) : Parcelable

@Parcelize
data class SwapFeeComponentPayload(
    val crossChain: Boolean,
    val fees: List<SwapFeeAmountPayload>,
) : Parcelable
