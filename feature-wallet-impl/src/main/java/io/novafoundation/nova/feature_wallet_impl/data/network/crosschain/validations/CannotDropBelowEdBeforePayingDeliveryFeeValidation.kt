package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.validations

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure.NotEnoughFunds.ToStayAboveEdBeforePayingDeliveryFees
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.isSendingCommissionAsset
import io.novafoundation.nova.feature_wallet_api.domain.model.deliveryFeePart
import io.novafoundation.nova.feature_wallet_api.domain.model.networkFeePart
import io.novasama.substrate_sdk_android.hash.isPositive

class CannotDropBelowEdBeforePayingDeliveryFeeValidation(
    private val assetSourceRegistry: AssetSourceRegistry
) : AssetTransfersValidation {

    override suspend fun validate(value: AssetTransferPayload): ValidationStatus<AssetTransferValidationFailure> {
        if (!value.isSendingCommissionAsset) return valid()

        val existentialDeposit = assetSourceRegistry.existentialDepositInPlanks(value.transfer.originChain, value.transfer.originChainAsset)

        val deliveryFeePart = value.originFee.deliveryFeePart()?.networkFee?.amount.orZero()
        val paysDeliveryFee = deliveryFeePart.isPositive()

        val networkFeePlanks = value.originFee.networkFeePart().networkFee.amount
        val sendingAmount = value.transfer.amountInPlanks + value.crossChainFee?.networkFee?.amount.orZero()
        val requiredAmountBeforePayingDeliveryFee = sendingAmount + networkFeePlanks + existentialDeposit

        val balanceCountedTowardsEd = value.originUsedAsset.balanceCountedTowardsEDInPlanks

        return when {
            !paysDeliveryFee -> valid()

            requiredAmountBeforePayingDeliveryFee <= balanceCountedTowardsEd -> valid()

            else -> {
                val availableBalance = (balanceCountedTowardsEd - networkFeePlanks - existentialDeposit).atLeastZero()

                validationError(
                    ToStayAboveEdBeforePayingDeliveryFees(
                        balanceCountedTowardsEd = balanceCountedTowardsEd,
                        existentialDeposit = existentialDeposit,
                        networkFee = networkFeePlanks,
                        maxPossibleTransferAmount = availableBalance,
                        chainAsset = value.transfer.originChainAsset
                    )
                )
            }
        }
    }
}

fun AssetTransfersValidationSystemBuilder.cannotDropBelowEdBeforePayingDeliveryFee(
    assetSourceRegistry: AssetSourceRegistry
) = validate(CannotDropBelowEdBeforePayingDeliveryFeeValidation(assetSourceRegistry))
