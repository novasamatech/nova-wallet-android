package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.receivingAmountInCommissionAsset
import io.novafoundation.nova.feature_wallet_api.domain.validation.PlanksProducer
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class DeadRecipientValidation(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val addingAmount: PlanksProducer<AssetTransferPayload>,
    private val assetToCheck: (AssetTransferPayload) -> Chain.Asset,
    private val skipIf: suspend (AssetTransferPayload) -> Boolean,
    private val failure: (AssetTransferPayload) -> AssetTransferValidationFailure.DeadRecipient,
) : AssetTransfersValidation {

    override suspend fun validate(value: AssetTransferPayload): ValidationStatus<AssetTransferValidationFailure> {
        if (skipIf(value)) {
            return valid()
        }

        val chain = value.transfer.destinationChain
        val chainAsset = assetToCheck(value)

        val balanceSource = assetSourceRegistry.sourceFor(chainAsset).balance

        val existentialDeposit = balanceSource.existentialDeposit(chainAsset)
        val recipientAccountId = value.transfer.destinationChain.accountIdOf(value.transfer.recipient)

        val recipientBalance = balanceSource.queryAccountBalance(chain, chainAsset, recipientAccountId).countedTowardsEd

        return validOrError(recipientBalance + addingAmount(value) >= existentialDeposit) {
            failure(value)
        }
    }
}

fun AssetTransfersValidationSystemBuilder.notDeadRecipientInCommissionAsset(
    assetSourceRegistry: AssetSourceRegistry
) = notDeadRecipient(
    assetSourceRegistry = assetSourceRegistry,
    assetToCheck = { it.transfer.destinationChain.commissionAsset },
    addingAmount = { it.receivingAmountInCommissionAsset },
    skipIf = { assetSourceRegistry.isAssetSelfSufficient(it.transfer.destinationChainAsset) },
    failure = { AssetTransferValidationFailure.DeadRecipient.InCommissionAsset(commissionAsset = it.transfer.destinationChain.commissionAsset) }
)

private suspend fun AssetSourceRegistry.isAssetSelfSufficient(asset: Chain.Asset) = sourceFor(asset).balance.isSelfSufficient(asset)

fun AssetTransfersValidationSystemBuilder.notDeadRecipientInUsedAsset(
    assetSourceRegistry: AssetSourceRegistry
) = notDeadRecipient(
    assetSourceRegistry = assetSourceRegistry,
    assetToCheck = { it.transfer.destinationChainAsset },
    addingAmount = { it.transfer.amountInPlanks },
    failure = { AssetTransferValidationFailure.DeadRecipient.InUsedAsset }
)

fun AssetTransfersValidationSystemBuilder.notDeadRecipient(
    assetSourceRegistry: AssetSourceRegistry,
    failure: (AssetTransferPayload) -> AssetTransferValidationFailure.DeadRecipient,
    assetToCheck: (AssetTransferPayload) -> Chain.Asset,
    addingAmount: PlanksProducer<AssetTransferPayload> = { BigInteger.ZERO },
    skipIf: suspend (AssetTransferPayload) -> Boolean = { false }
) = validate(
    DeadRecipientValidation(
        assetSourceRegistry = assetSourceRegistry,
        addingAmount = addingAmount,
        assetToCheck = assetToCheck,
        failure = failure,
        skipIf = skipIf
    )
)
