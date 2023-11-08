package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_api.domain.model.nativeMinimumBalance
import io.novafoundation.nova.feature_swap_api.domain.model.requireNativeAsset
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.TooSmallRemainingBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.toBuyAmountToKeepMainEDInFeeAsset
import io.novafoundation.nova.feature_swap_impl.domain.validation.totalDeductedAmountInFeeToken
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import jp.co.soramitsu.fearless_utils.hash.isPositive

class SwapSmallRemainingBalanceValidation(
    private val assetSourceRegistry: AssetSourceRegistry
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val feeChainAsset = value.feeAsset.token.configuration
        val chainAssetIn = value.detailedAssetIn.asset.token.configuration
        val chainIn = value.detailedAssetIn.chain
        val assetBalances = assetSourceRegistry.sourceFor(chainAssetIn).balance

        val assetInTotal = value.detailedAssetIn.asset.totalInPlanks
        val swapAmount = value.detailedAssetIn.amountInPlanks
        val assetInExistentialDeposit = assetBalances.existentialDeposit(chainIn, chainAssetIn)
        val totalDeductedAmount = value.totalDeductedAmountInFeeToken
        val remainingBalance = assetInTotal - swapAmount - totalDeductedAmount

        if (remainingBalance.isPositive() && remainingBalance < assetInExistentialDeposit) {
            val toBuyAmountToKeepEDInFeeAsset = value.toBuyAmountToKeepMainEDInFeeAsset
            return if (toBuyAmountToKeepEDInFeeAsset.isZero) {
                TooSmallRemainingBalance.NoNeedsToBuyMainAssetED(chainAssetIn, remainingBalance, assetInExistentialDeposit).validationError()
            } else {
                TooSmallRemainingBalance.NeedsToBuyMainAssetED(
                    feeChainAsset,
                    chainAssetIn,
                    value.swapFee.minimumBalanceBuyIn.requireNativeAsset(),
                    assetInExistentialDeposit,
                    toBuyAmountToKeepEDInCommissionAsset = value.swapFee.minimumBalanceBuyIn.nativeMinimumBalance,
                    toSellAmountToKeepEDUsingAssetIn = toBuyAmountToKeepEDInFeeAsset,
                    remainingBalance,
                    value.swapFee.networkFee
                ).validationError()
            }
        }

        return valid()
    }
}
