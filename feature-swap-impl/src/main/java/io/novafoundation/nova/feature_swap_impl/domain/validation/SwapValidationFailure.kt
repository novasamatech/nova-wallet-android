package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.validation.FeeChangeDetectedFailure
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

sealed class SwapValidationFailure {

    class FeeChangeDetected(override val payload: FeeChangeDetectedFailure.Payload<SwapFee>) : SwapValidationFailure(), FeeChangeDetectedFailure<SwapFee>

    object NonPositiveAmount : SwapValidationFailure()

    object InvalidSlippage : SwapValidationFailure()

    class NewRateExceededSlippage(
        val assetIn: Chain.Asset,
        val assetOut: Chain.Asset,
        val selectedRate: BigDecimal,
        val newRate: BigDecimal
    ) : SwapValidationFailure()

    object NotEnoughLiquidity : SwapValidationFailure()

    sealed class NotEnoughFunds : SwapValidationFailure() {

        object InUsedAsset : NotEnoughFunds()

        class InCommissionAsset(
            override val chainAsset: Chain.Asset,
            override val maxUsable: BigDecimal,
            override val fee: BigDecimal
        ) : NotEnoughFunds(), NotEnoughToPayFeesError
    }

    class AmountOutIsTooLowToStayAboveED(
        val asset: Chain.Asset,
        val amountInPlanks: BigInteger,
        val existentialDeposit: BigInteger
    ) : SwapValidationFailure()

    sealed class InsufficientBalance : SwapValidationFailure() {

        class NoNeedsToBuyMainAssetED(
            val assetIn: Chain.Asset,
            val feeAsset: Chain.Asset,
            val maxSwapAmount: Balance,
            val fee: Fee
        ) : SwapValidationFailure()

        class NeedsToBuyMainAssetED(
            val feeAsset: Chain.Asset,
            val assetIn: Chain.Asset,
            val toBuyAmountToKeepEDInCommissionAsset: Balance,
            val toSellAmountToKeepEDUsingAssetIn: Balance,
            val maxSwapAmount: Balance,
            val fee: Fee
        ) : SwapValidationFailure()
    }

    sealed class TooSmallRemainingBalance : SwapValidationFailure() {

        class NoNeedsToBuyMainAssetED(
            val assetIn: Chain.Asset,
            val remainingBalance: Balance,
            val assetInExistentialDeposit: Balance
        ) : SwapValidationFailure()

        class NeedsToBuyMainAssetED(
            val feeAsset: Chain.Asset,
            val assetIn: Chain.Asset,
            val assetInExistentialDeposit: Balance,
            val toBuyAmountToKeepEDInCommissionAsset: Balance,
            val toSellAmountToKeepEDUsingAssetIn: Balance,
            val remainingBalance: Balance,
            val fee: Fee
        ) : SwapValidationFailure()
    }
}
