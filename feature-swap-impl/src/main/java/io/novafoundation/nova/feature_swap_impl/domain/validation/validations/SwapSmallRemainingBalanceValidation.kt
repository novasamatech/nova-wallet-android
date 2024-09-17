package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.TooSmallRemainingBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.totalDeductedAmountInFeeToken
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novasama.substrate_sdk_android.hash.isPositive

class SwapSmallRemainingBalanceValidation(
    private val assetSourceRegistry: AssetSourceRegistry
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val chainAssetIn = value.detailedAssetIn.asset.token.configuration
        val chainIn = value.detailedAssetIn.chain
        val assetBalances = assetSourceRegistry.sourceFor(chainAssetIn).balance

        val balanceCountedTowardsEd = value.detailedAssetIn.asset.balanceCountedTowardsEDInPlanks
        val swapAmount = value.detailedAssetIn.amountInPlanks
        val assetInExistentialDeposit = assetBalances.existentialDeposit(chainIn, chainAssetIn)
        val totalDeductedAmount = value.totalDeductedAmountInFeeToken
        val remainingBalance = balanceCountedTowardsEd - swapAmount - totalDeductedAmount

        if (remainingBalance.isPositive() && remainingBalance < assetInExistentialDeposit) {
            return TooSmallRemainingBalance(chainAssetIn, remainingBalance, assetInExistentialDeposit).validationError()
        }

        return valid()
    }
}
