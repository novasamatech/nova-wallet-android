package io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.validations

import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransferValidationFailure
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransfersValidationSystemBuilder
import io.novafoundation.nova.feature_nft_impl.domain.validaiton.nftExists
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.enoughTotalToStayAboveED
import io.novafoundation.nova.feature_wallet_api.domain.validation.notPhishingAccount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validAddress
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

fun NftTransfersValidationSystemBuilder.notPhishingRecipient(
    factory: PhishingValidationFactory
) = notPhishingAccount(
    factory = factory,
    address = { it.transfer.recipient },
    chain = { it.transfer.destinationChain },
    warning = NftTransferValidationFailure::PhishingRecipient
)

fun NftTransfersValidationSystemBuilder.validAddress() = validAddress(
    address = { it.transfer.recipient },
    chain = { it.transfer.destinationChain },
    error = { NftTransferValidationFailure.InvalidRecipientAddress(it.transfer.destinationChain) }
)

fun NftTransfersValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED(
    assetSourceRegistry: AssetSourceRegistry
) = enoughTotalToStayAboveED(
    fee = { it.originFee },
    total = { it.originFeeAsset.total },
    existentialDeposit = { assetSourceRegistry.existentialDeposit(it.transfer.originChain, it.transfer.originChain.commissionAsset) },
    error = { NftTransferValidationFailure.NotEnoughFunds.ToStayAboveED(it.transfer.originChain.commissionAsset) }
)

fun NftTransfersValidationSystemBuilder.sufficientTransferableBalanceToPayOriginFee() = sufficientBalance(
    available = { it.originFeeAsset.transferable },
    fee = { it.originFee },
    error = { payload, availableToPayFees ->
        NftTransferValidationFailure.NotEnoughFunds.InCommissionAsset(
            chainAsset = payload.transfer.originChain.commissionAsset,
            fee = payload.originFee,
            availableToPayFees = availableToPayFees
        )
    }
)

fun NftTransfersValidationSystemBuilder.nftExists(nftRepository: NftRepository) = nftExists(
    nftRepository = nftRepository,
    substratePublicKey = { value -> value.transfer.sender.substratePublicKey!! },
    nftId = { value -> value.transfer.nftId },
    error = { NftTransferValidationFailure.NftAbsent }
)

private suspend fun AssetSourceRegistry.existentialDeposit(chain: Chain, asset: Chain.Asset): BigDecimal {
    val inPlanks = sourceFor(asset).balance.existentialDeposit(chain, asset)

    return asset.amountFromPlanks(inPlanks)
}
