package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.NotEnoughFunds
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class EnoughNativeAssetBalanceToPayFeeConsideringEDValidation(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainRegistry: ChainRegistry
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val feeChainAsset = value.feeAsset.token.configuration

        if (feeChainAsset.isCommissionAsset) {
            val chain = chainRegistry.getChain(feeChainAsset.chainId)
            val existentialDeposit = assetSourceRegistry.existentialDepositInPlanks(chain, feeChainAsset)
            return validOrError(value.feeAsset.balanceCountedTowardsEDInPlanks - value.decimalFee.networkFee.amountByRequestedAccount >= existentialDeposit) {
                NotEnoughFunds.ToPayFeeAndStayAboveED(value.feeAsset.token.configuration)
            }
        }

        return valid()
    }
}

fun SwapValidationSystemBuilder.sufficientNativeBalanceToPayFeeConsideringED(
    assetSourceRegistry: AssetSourceRegistry,
    chainRegistry: ChainRegistry
) {
    validate(
        EnoughNativeAssetBalanceToPayFeeConsideringEDValidation(
            assetSourceRegistry = assetSourceRegistry,
            chainRegistry = chainRegistry
        )
    )
}
