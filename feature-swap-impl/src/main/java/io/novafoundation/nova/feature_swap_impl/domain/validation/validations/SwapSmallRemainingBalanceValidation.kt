package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.TooSmallRemainingBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.toBuyAmountToKeepEDInFeeAsset
import io.novafoundation.nova.feature_swap_impl.domain.validation.totalDeductedAmountInFeeToken
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.hash.isPositive

class SwapSmallRemainingBalanceValidation(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainRegistry: ChainRegistry
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val feeChainAsset = value.feeAsset.token.configuration
        val chainAssetIn = value.detailedAssetIn.asset.token.configuration
        val chainIn = chainRegistry.getChain(chainAssetIn.chainId)
        val assetBalances = assetSourceRegistry.sourceFor(chainAssetIn).balance

        val assetInTotal = value.detailedAssetIn.asset.totalInPlanks
        val toBuyAmountToKeepEDInFeeAsset = value.toBuyAmountToKeepEDInFeeAsset
        val swapAmount = chainAssetIn.planksFromAmount(value.detailedAssetIn.amount)
        val assetInExistentialDeposit = assetBalances.existentialDeposit(chainIn, chainAssetIn)
        val totalDeductedAmount = value.totalDeductedAmountInFeeToken
        val remainingBalance = assetInTotal - swapAmount - totalDeductedAmount

        if (remainingBalance.isPositive() && remainingBalance < assetInExistentialDeposit) {
            return if (toBuyAmountToKeepEDInFeeAsset.isZero) {
                TooSmallRemainingBalance.NoNeedsToBuyMainAssetED(chainAssetIn, remainingBalance, assetInExistentialDeposit).validationError()
            } else {
                TooSmallRemainingBalance.NeedsToBuyMainAssetED(
                    feeChainAsset,
                    chainAssetIn,
                    assetInExistentialDeposit,
                    toBuyAmountToKeepEDInCommissionAsset = toBuyAmountToKeepEDInFeeAsset,
                    toSellAmountToKeepEDUsingAssetIn = BigInteger.ZERO, // TODO how to convert toBuyAmountToKeepEDInFeeAsset to this value?
                    remainingBalance,
                    value.swapFee.networkFee
                ).validationError()
            }
        }

        return valid()
    }
}
