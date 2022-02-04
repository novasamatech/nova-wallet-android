package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidation
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.validation.PlanksProducer
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSourceProvider
import java.math.BigInteger

class DeadRecipientValidation(
    private val balanceSourceProvider: BalanceSourceProvider,
    private val addingAmount: PlanksProducer<AssetTransferPayload>,
    private val assetToCheck: (AssetTransferPayload) -> Asset,
    private val failure: (AssetTransferPayload) -> AssetTransferValidationFailure.DeadRecipient,
) : AssetTransfersValidation {

    override suspend fun validate(value: AssetTransferPayload): ValidationStatus<AssetTransferValidationFailure> {
        val chain = value.transfer.chain
        val chainAsset = assetToCheck(value).token.configuration

        val balanceSource = balanceSourceProvider.provideFor(chainAsset)

        val existentialDeposit = balanceSource.existentialDeposit(chain, chainAsset)
        val recipientBalance = balanceSource.queryTotalBalance(chain, chainAsset, value.transfer.recipient)

        return validOrError(recipientBalance + addingAmount(value) >= existentialDeposit) {
            failure(value)
        }
    }
}

fun ValidationSystemBuilder<AssetTransferPayload, AssetTransferValidationFailure>.notDeadRecipient(
    balanceSourceProvider: BalanceSourceProvider,
    failure: (AssetTransferPayload) -> AssetTransferValidationFailure.DeadRecipient,
    assetToCheck: (AssetTransferPayload) -> Asset,
    addingAmount: PlanksProducer<AssetTransferPayload> = { BigInteger.ZERO },
) = validate(
    DeadRecipientValidation(
        balanceSourceProvider = balanceSourceProvider,
        addingAmount = addingAmount,
        assetToCheck = assetToCheck,
        failure = failure
    )
)
