package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class SwapValidationFailure {

    object NonPositiveAmount : SwapValidationFailure()

    class HighPriceImpact(val priceImpact: Fraction) : SwapValidationFailure()

    class InvalidSlippage(val minSlippage: Fraction, val maxSlippage: Fraction) : SwapValidationFailure()

    class NewRateExceededSlippage(
        val assetIn: Chain.Asset,
        val assetOut: Chain.Asset,
        val selectedRate: BigDecimal,
        val newRate: BigDecimal
    ) : SwapValidationFailure()

    object NotEnoughLiquidity : SwapValidationFailure()

    sealed class NotEnoughFunds : SwapValidationFailure() {

        class ToPayFeeAndStayAboveED(
            override val asset: Chain.Asset,
            override val errorModel: InsufficientBalanceToStayAboveEDError.ErrorModel
        ) : NotEnoughFunds(), InsufficientBalanceToStayAboveEDError

        object InUsedAsset : NotEnoughFunds()
    }

    class AmountOutIsTooLowToStayAboveED(
        val asset: Chain.Asset,
        val amountInPlanks: Balance,
        val existentialDeposit: Balance
    ) : SwapValidationFailure()

    class IntermediateAmountOutIsTooLowToStayAboveED(
        val asset: Chain.Asset,
        val existentialDeposit: Balance,
        val amount: Balance
    ) : SwapValidationFailure()

    class CannotReceiveAssetOut(
        val destination: ChainWithAsset,
        val requiredNativeAssetOnChainOut: ChainAssetWithAmount
    ) : SwapValidationFailure()

    sealed class InsufficientBalance : SwapValidationFailure() {

        class BalanceNotConsiderInsufficientReceiveAsset(
            val assetIn: Chain.Asset,
            val assetOut: Chain.Asset,
            val existentialDeposit: Balance
        ) : SwapValidationFailure()

        class BalanceNotConsiderConsumers(
            val assetIn: Chain.Asset,
            val assetInED: Balance,
            val feeAsset: Chain.Asset,
            val fee: Balance
        ) : SwapValidationFailure()

        class CannotPayFeeDueToAmount(
            val assetIn: Chain.Asset,
            val feeAmount: BigDecimal,
            val maxSwapAmount: BigDecimal
        ) : SwapValidationFailure()

        class CannotPayFee(
            val feeAsset: Chain.Asset,
            val balance: Balance,
            val fee: Balance
        ) : SwapValidationFailure()
    }

    class TooSmallRemainingBalance(
        val assetIn: Chain.Asset,
        val remainingBalance: Balance,
        val assetInExistentialDeposit: Balance
    ) : SwapValidationFailure()
}
