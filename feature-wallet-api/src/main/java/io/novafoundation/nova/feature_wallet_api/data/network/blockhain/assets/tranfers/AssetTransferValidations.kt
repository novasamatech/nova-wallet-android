package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
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
            val commissionAsset: Chain.Asset,
            val fee: BigDecimal,
            val transferableBalance: BigDecimal,
        ) : NotEnoughFunds()

        class ToStayAboveED(val commissionAsset: Chain.Asset) : NotEnoughFunds()

        class ToPayCrossChainFee(
            val crossChainFeeAsset: Chain.Asset,
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
    val originFee: TransferFee,
    val crossChainFee: TransferFee?,
    val originUsedAsset: Asset
)

class TransferFee(
    val amount: BigDecimal,
    val asset: Asset,
)

val AssetTransferPayload.isReceivingCommissionAsset
    get() = transfer.destinationChainAsset == transfer.destinationChain.commissionAsset

val AssetTransferPayload.originFeeInUsedAsset: BigDecimal
    get() = originFeeIn(transfer.originChainAsset)


fun AssetTransferPayload.crossChainFeeIn(asset: Chain.Asset): BigDecimal {
    return crossChainFee.amountIn(asset)
}

fun AssetTransferPayload.originFeeIn(asset: Chain.Asset): BigDecimal {
    return originFee.amountIn(asset)
}

private fun TransferFee?.amountIn(otherAsset: Chain.Asset): BigDecimal = when {
    this == null -> BigDecimal.ZERO
    otherAsset == asset.token.configuration -> amount
    else -> BigDecimal.ZERO
}

fun AssetTransferPayload.sendingAmountIn(asset: Chain.Asset): BigDecimal {
    return if (asset == transfer.originChainAsset) {
        transfer.amount
    } else {
        BigDecimal.ZERO
    }
}

val AssetTransferPayload.receivingAmountInCommissionAsset: BigInteger
    get() = if (isReceivingCommissionAsset) {
        transfer.amountInPlanks
    } else {
        BigInteger.ZERO
    }

val AssetTransferPayload.sendingAmountInCommissionAsset: BigDecimal
    get() = sendingAmountIn(transfer.originChain.commissionAsset)

val AssetTransfer.amountInPlanks
    get() = originChainAsset.planksFromAmount(amount)
