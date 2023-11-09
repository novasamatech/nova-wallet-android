package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.isSelfSufficientAsset
import io.novafoundation.nova.runtime.ext.isCommissionAsset

class SufficientBalanceConsideringNonSufficientAssetsValidation(
    private val assetSourceRegistry: AssetSourceRegistry
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val assetIn = value.detailedAssetIn.asset
        val assetOut = value.detailedAssetOut.asset
        val amount = value.detailedAssetIn.amountInPlanks

        val isSelfSufficientAssetOut = assetSourceRegistry.isSelfSufficientAsset(assetOut.token.configuration)

        if (!isSelfSufficientAssetOut && assetIn.token.configuration.isCommissionAsset) {
            val existentialDeposit = assetSourceRegistry.existentialDepositInPlanks(value.detailedAssetIn.chain, assetOut.token.configuration)
            val fee = value.swapFee.networkFee.amount

            return validOrError(assetIn.totalInPlanks - existentialDeposit >= amount + fee) {
                SwapValidationFailure.InsufficientBalance.BalanceNotConsiderInsufficientReceiveAsset(
                    assetIn.token.configuration,
                    assetOut.token.configuration,
                    existentialDeposit
                )
            }
        }

        return valid()
    }
}

fun SwapValidationSystemBuilder.sufficientBalanceConsideringNonSufficientAssetsValidation(assetSourceRegistry: AssetSourceRegistry) = validate(
    SufficientBalanceConsideringNonSufficientAssetsValidation(assetSourceRegistry)
)
