package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDeposit
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks

class SufficientAmountOutToStayAboveEDValidation(
    private val assetSourceRegistry: AssetSourceRegistry
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val chainOut = value.detailedAssetOut.chain
        val assetOut = value.detailedAssetOut.asset
        val amountOut = value.detailedAssetOut.amountInPlanks
        val existentialDeposit = assetSourceRegistry.existentialDepositInPlanks(chainOut, assetOut.token.configuration)
        return validOrError(assetOut.freeInPlanks + amountOut >= existentialDeposit) {
            SwapValidationFailure.AmountOutIsTooLowToStayAboveED(
                assetOut.token.configuration,
                amountOut,
                existentialDeposit
            )
        }
    }
}

fun SwapValidationSystemBuilder.sufficientAmountOutToStayAboveEDValidation(assetSourceRegistry: AssetSourceRegistry) = validate(
    SufficientAmountOutToStayAboveEDValidation(assetSourceRegistry)
)
