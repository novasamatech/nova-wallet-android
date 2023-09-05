package io.novafoundation.nova.feature_nft_impl.data.source

import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_impl.data.mappers.mapNftTypeLocalToTypeKey
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.math.BigInteger

abstract class BaseNftTransfer(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val extrinsicService: ExtrinsicService,
    private val phishingValidationFactory: PhishingValidationFactory,
    private val nftProvidersRegistry: NftProvidersRegistry,
    private val nftDao: NftDao,
    private val exceptionHandler: HttpExceptionHandler
) : NftTransfer {

    abstract fun ExtrinsicBuilder.transfer(transfer: NftTransferModel)

    suspend fun performTransfer(transfer: NftTransferModel): Result<String> {
        return withContext(Dispatchers.IO) {
            val senderAccountId = transfer.sender.requireAccountIdIn(transfer.originChain)

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

    suspend fun nftDetails(nftId: String): NftDetails {
        return runCatching {
            val nftTypeKey = mapNftTypeLocalToTypeKey(nftDao.getNftType(nftId))
            val nftProvider = nftProvidersRegistry.get(nftTypeKey)
            nftProvider.nftDetailsFlow(nftId)
        }.onFailure { throw exceptionHandler.transformException(it) }
            .getOrThrow()
            .first()
    }

    fun defaultValidationSystem(): NftTransfersValidationSystem = ValidationSystem {
        validAddress()

        notPhishingRecipient(phishingValidationFactory)

        nftExists(nftDetails = ::nftDetails)

        sufficientTransferableBalanceToPayOriginFee()

        sufficientCommissionBalanceToStayAboveED(assetSourceRegistry)
    }
}
