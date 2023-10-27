package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.validation.FeeChangeDetectedFailure
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

sealed class SwapValidationFailure {

    class FeeChangeDetected(override val payload: FeeChangeDetectedFailure.Payload) : SwapValidationFailure(), FeeChangeDetectedFailure

    object NonPositiveAmount : SwapValidationFailure()

    object InvalidSlippage : SwapValidationFailure()

    object NewRateExceededSlippage : SwapValidationFailure()

    object NotEnoughLiquidity : SwapValidationFailure()

    object NotEnoughFunds : SwapValidationFailure()

    class ToStayAboveED(val asset: Chain.Asset) : SwapValidationFailure()

    class TooSmallAmount(
        val assetIn: Chain.Asset,
        val existentialDeposit: Balance,
        val fullSwapRemainingBalance: Balance
    ) : SwapValidationFailure()

    class TooSmallAmountWithCustomFee(
        val feeAsset: Chain.Asset,
        val assetIn: Chain.Asset,
        val assetOut: Chain.Asset,
        val existentialDeposit: Balance,
        val fee: SwapFee,
        val fullSwapRemainingBalance: Balance
    ) : SwapValidationFailure()

    sealed class InsufficientBalance : SwapValidationFailure() {

        class NoNeedsToBuyMainAssetED(
            val assetIn: Chain.Asset,
            val maxSwapAmount: BigInteger,
            val fee: Fee
        ) : SwapValidationFailure()

        class NeedsToBuyMainAssetED(
            val feeAsset: Chain.Asset,
            val assetIn: Chain.Asset,
            val assetOut: Chain.Asset,
            val existentialDeposit: BigInteger,
            val maxSwapAmount: BigInteger,
            val fee: Fee
        ) : SwapValidationFailure()
    }

    sealed class TooSmallRemainingBalance : SwapValidationFailure() {

        class NoNeedsToBuyMainAssetED(
            val assetIn: Chain.Asset,
            val remainingBalance: BigInteger,
            val fee: Fee
        ) : SwapValidationFailure()

        class NeedsToBuyMainAssetED(
            val feeAsset: Chain.Asset,
            val assetIn: Chain.Asset,
            val assetOut: Chain.Asset,
            val existentialDeposit: BigInteger,
            val remainingBalance: BigInteger,
            val fee: Fee
        ) : SwapValidationFailure()
    }
}
