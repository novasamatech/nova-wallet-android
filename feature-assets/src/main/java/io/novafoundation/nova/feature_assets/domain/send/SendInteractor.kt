package io.novafoundation.nova.feature_assets.domain.send

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.domain.implementations.availableDestinationChains
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.RecipientSearchResult
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

class SendInteractor(
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val crossChainTransfersRepository: CrossChainTransfersRepository
) {

    // TODO wallet
    suspend fun getRecipients(query: String, chainId: ChainId): RecipientSearchResult {
//        val metaAccount = accountRepository.getSelectedMetaAccount()
//        val chain = chainRegistry.getChain(chainId)
//        val accountId = metaAccount.accountIdIn(chain)!!
//
//        val contacts = walletRepository.getContacts(accountId, chain, query)
//        val myAccounts = accountRepository.getMyAccounts(query, chain.id)
//
//        return withContext(Dispatchers.Default) {
//            val contactsWithoutMyAccounts = contacts - myAccounts.map { it.address }
//            val myAddressesWithoutCurrent = myAccounts - metaAccount
//
//            RecipientSearchResult(
//                myAddressesWithoutCurrent.toList().map { mapAccountToWalletAccount(chain, it) },
//                contactsWithoutMyAccounts.toList()
//            )
//        }

        return RecipientSearchResult(
            myAccounts = emptyList(),
            contacts = emptyList()
        )
    }

    // TODO wallet phishing
    suspend fun isAddressFromPhishingList(address: String): Boolean {
        return /*walletRepository.isAccountIdFromPhishingList(address)*/ false
    }

    suspend fun getTransferFee(transfer: AssetTransfer): BigInteger = withContext(Dispatchers.Default) {
        getAssetTransfers(transfer).calculateFee(transfer)
    }

    suspend fun performTransfer(
        transfer: AssetTransfer,
        fee: BigDecimal
    ): Result<String> = withContext(Dispatchers.Default) {
        getAssetTransfers(transfer).performTransfer(transfer)
            .onSuccess { hash ->
                walletRepository.insertPendingTransfer(hash, transfer, fee)
            }
    }

    suspend fun availableCrossChainDestinations(origin: Chain.Asset): List<Chain> {
        val configuration = crossChainTransfersRepository.getConfiguration()

        val chainsById = chainRegistry.chainsById.first()

        return configuration.availableDestinationChains(origin).mapNotNull(chainsById::get)
    }

    fun validationSystemFor(asset: Chain.Asset) = assetSourceRegistry.sourceFor(asset).transfers.validationSystem

    suspend fun areTransfersEnabled(asset: Chain.Asset) = assetSourceRegistry.sourceFor(asset).transfers.areTransfersEnabled(asset)

    private fun getAssetTransfers(transfer: AssetTransfer) =
        assetSourceRegistry.sourceFor(transfer.chainAsset).transfers
}
