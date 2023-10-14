package io.novafoundation.nova.feature_nft_impl.domain.nft.send

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_api.data.repository.PendingSendNftTransactionRepository
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransferModel
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransfersValidationSystem
import io.novafoundation.nova.feature_nft_impl.data.source.BaseNftTransfer
import io.novafoundation.nova.feature_nft_impl.data.source.NftTransfersRegistry
import io.novafoundation.nova.feature_nft_impl.domain.nft.details.PricedNftDetails
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Price
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigInteger
import javax.inject.Inject

@FeatureScope
class NftSendInteractor @Inject constructor(
    private val walletRepository: WalletRepository,
    private val nftTransfersRegistry: NftTransfersRegistry,
    private val nftRepository: NftRepository,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val pendingSendNftTransactionRepository: PendingSendNftTransactionRepository
) {

    fun commissionAssetFlow(chainId: ChainId): Flow<Asset> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            val chain = chainRegistry.getChain(chainId)

            walletRepository.assetFlow(metaAccount.id, chain.commissionAsset)
        }
    }

    suspend fun getOriginFee(transfer: NftTransferModel): BigInteger {
        return withContext(Dispatchers.IO) {
            getNftTransfers(transfer).calculateFee(transfer)
        }
    }

    suspend fun performTransfer(transfer: NftTransferModel): Result<String> {
        return getNftTransfers(transfer).performTransfer(transfer)
            .onSuccess {
                pendingSendNftTransactionRepository.onNftSendTransactionSubmitted(transfer.nftId)
            }
    }

    fun defaultValidationSystem(): NftTransfersValidationSystem {
        return nftTransfersRegistry.defaultValidationSystem()
    }

    private fun getNftTransfers(transfer: NftTransferModel): BaseNftTransfer {
        return nftTransfersRegistry.get(transfer.nftType.key)
    }

    fun nftDetailsFlow(nftIdentifier: String): Flow<NftDetails> {
        return nftRepository.nftDetails(nftIdentifier)
    }
}
