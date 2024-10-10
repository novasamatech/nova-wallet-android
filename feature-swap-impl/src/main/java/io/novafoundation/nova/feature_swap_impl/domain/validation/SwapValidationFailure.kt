package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.validation.FeeChangeDetectedFailure
import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class SwapValidationFailure {

    class FeeChangeDetected(override val payload: FeeChangeDetectedFailure.Payload<SwapFee>) : SwapValidationFailure(), FeeChangeDetectedFailure<SwapFee>

    object NonPositiveAmount : SwapValidationFailure()

    class InvalidSlippage(val minSlippage: Percent, val maxSlippage: Percent) : SwapValidationFailure()

    class NewRateExceededSlippage(
        val assetIn: Chain.Asset,
        val assetOut: Chain.Asset,
        val selectedRate: BigDecimal,
        val newRate: BigDecimal
    ) : SwapValidationFailure()

    object NotEnoughLiquidity : SwapValidationFailure()

    sealed class NotEnoughFunds : SwapValidationFailure() {

        class ToPayFeeAndStayAboveED(override val asset: Chain.Asset, override val errorModel: InsufficientBalanceToStayAboveEDError.ErrorModel) :
            NotEnoughFunds(), InsufficientBalanceToStayAboveEDError

        object InUsedAsset : NotEnoughFunds()

        object ToPayFee : NotEnoughFunds()
    }

    class AmountOutIsTooLowToStayAboveED(
        val asset: Chain.Asset,
        val amountInPlanks: Balance,
        val existentialDeposit: Balance
    ) : SwapValidationFailure()

    sealed class InsufficientBalance : SwapValidationFailure() {

        class BalanceNotConsiderInsufficientReceiveAsset(
            val assetIn: Chain.Asset,
            val assetOut: Chain.Asset,
            val existentialDeposit: Balance
        ) : SwapValidationFailure()

        class BalanceNotConsiderConsumers(
            val nativeAsset: Chain.Asset,
            val feeAsset: Chain.Asset,
            val existentialDeposit: Balance,
            val swapFee: SwapFee
        ) : SwapValidationFailure()

        class CannotPayFee(
            val assetIn: Chain.Asset,
            val feeAsset: Chain.Asset,
            val maxSwapAmount: Balance,
            val fee: Fee
        ) : SwapValidationFailure()
    }

    class TooSmallRemainingBalance(
        val assetIn: Chain.Asset,
        val remainingBalance: Balance,
        val assetInExistentialDeposit: Balance
    ): SwapValidationFailure()
}
