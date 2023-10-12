package io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

typealias NftTransfersValidationSystem = ValidationSystem<NftTransferPayload, NftTransferValidationFailure>
typealias NftTransfersValidationSystemBuilder = ValidationSystemBuilder<NftTransferPayload, NftTransferValidationFailure>

sealed class NftTransferValidationFailure {

    sealed class NotEnoughFunds : NftTransferValidationFailure() {

        class InCommissionAsset(
            override val chainAsset: Chain.Asset,
            override val availableToPayFees: BigDecimal,
            override val fee: BigDecimal
        ) : NotEnoughFunds(), NotEnoughToPayFeesError

        class ToStayAboveED(val commissionAsset: Chain.Asset) : NotEnoughFunds()
    }

    class InvalidRecipientAddress(val chain: Chain) : NftTransferValidationFailure()

    class PhishingRecipient(val address: String) : NftTransferValidationFailure()

    object NftAbsent : NftTransferValidationFailure()
}

data class NftTransferPayload(
    val transfer: NftTransferModel,
    val originFee: BigDecimal,
    val originFeeAsset: Asset
)
