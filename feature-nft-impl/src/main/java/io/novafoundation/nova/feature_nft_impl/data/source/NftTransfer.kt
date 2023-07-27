package io.novafoundation.nova.feature_nft_impl.data.source

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransferModel
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransfersValidationSystem
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.validations.nftExists
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.validations.notPhishingRecipient
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.validations.sufficientCommissionBalanceToStayAboveED
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.validations.sufficientTransferableBalanceToPayOriginFee
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.validations.validAddress
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

abstract class NftTransfer(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val extrinsicService: ExtrinsicService,
    private val phishingValidationFactory: PhishingValidationFactory,
    private val nftRepository: NftRepository
) {

    abstract fun ExtrinsicBuilder.transfer(transfer: NftTransferModel)

    suspend fun performTransfer(transfer: NftTransferModel): Result<String> {
        return withContext(Dispatchers.IO) {
            val senderAccountId = transfer.sender.accountIdIn(transfer.originChain)!!

            extrinsicService.submitExtrinsicWithAnySuitableWallet(transfer.originChain, senderAccountId) {
                transfer(transfer)
            }
        }
    }

    suspend fun calculateFee(transfer: NftTransferModel): BigInteger {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(transfer.originChain) {
                transfer(transfer)
            }
        }
    }

    fun defaultValidationSystem(): NftTransfersValidationSystem = ValidationSystem {
        validAddress()

        notPhishingRecipient(phishingValidationFactory)

        nftExists(nftRepository)

        sufficientTransferableBalanceToPayOriginFee()

        sufficientCommissionBalanceToStayAboveED(assetSourceRegistry)
    }
}
