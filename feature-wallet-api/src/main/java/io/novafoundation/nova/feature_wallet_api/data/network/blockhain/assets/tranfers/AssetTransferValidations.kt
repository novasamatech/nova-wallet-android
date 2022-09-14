package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

typealias AssetTransfersValidationSystem = ValidationSystem<AssetTransferPayload, AssetTransferValidationFailure>
typealias AssetTransfersValidation = Validation<AssetTransferPayload, AssetTransferValidationFailure>
typealias AssetTransfersValidationSystemBuilder = ValidationSystemBuilder<AssetTransferPayload, AssetTransferValidationFailure>

sealed class AssetTransferValidationFailure {

    sealed class WillRemoveAccount : AssetTransferValidationFailure() {
        object WillBurnDust : WillRemoveAccount()

        class WillTransferDust(val dust: BigDecimal) : WillRemoveAccount()
    }

    sealed class DeadRecipient : AssetTransferValidationFailure() {

        object InUsedAsset : DeadRecipient()

        class InCommissionAsset(val commissionAsset: Chain.Asset) : DeadRecipient()
    }

    sealed class NotEnoughFunds : AssetTransferValidationFailure() {
        object InUsedAsset : NotEnoughFunds()

        class InCommissionAsset(
            override val chainAsset: Chain.Asset,
            override val availableToPayFees: BigDecimal,
            override val fee: BigDecimal
        ) : NotEnoughFunds(), NotEnoughToPayFeesError

        class ToStayAboveED(val commissionAsset: Chain.Asset) : NotEnoughFunds()

        class ToPayCrossChainFee(
            val usedAsset: Chain.Asset,
            val fee: BigDecimal,
            val remainingBalanceAfterTransfer: BigDecimal,
        ) : NotEnoughFunds()
    }

    class InvalidRecipientAddress(val chain: Chain) : AssetTransferValidationFailure()

    class PhishingRecipient(val address: String) : AssetTransferValidationFailure()

    object NonPositiveAmount : AssetTransferValidationFailure()
}

data class AssetTransferPayload(
    val transfer: AssetTransfer,
    val originFee: BigDecimal,
    val crossChainFee: BigDecimal?,
    val originCommissionAsset: Asset,
    val originUsedAsset: Asset
)

val AssetTransferPayload.isSendingCommissionAsset
    get() = transfer.originChainAsset == transfer.originChain.commissionAsset

val AssetTransferPayload.isReceivingCommissionAsset
    get() = transfer.destinationChainAsset == transfer.destinationChain.commissionAsset

val AssetTransferPayload.originFeeInUsedAsset: BigDecimal
    get() = if (isSendingCommissionAsset) {
        originFee
    } else {
        BigDecimal.ZERO
    }

val AssetTransferPayload.receivingAmountInCommissionAsset: BigInteger
    get() = if (isReceivingCommissionAsset) {
        transfer.amountInPlanks
    } else {
        BigInteger.ZERO
    }

val AssetTransferPayload.sendingAmountInCommissionAsset: BigDecimal
    get() = if (isSendingCommissionAsset) {
        transfer.amount
    } else {
        0.toBigDecimal()
    }

val AssetTransfer.amountInPlanks
    get() = originChainAsset.planksFromAmount(amount)
