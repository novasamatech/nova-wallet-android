package io.novafoundation.nova.feature_wallet_impl.domain.send

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.RecipientSearchResult
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.AssetTransfersProvider
import io.novafoundation.nova.runtime.ext.isValidAddress
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.encrypt.qr.QrSharing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

class SendInteractor(
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val assetTransfersProvider: AssetTransfersProvider
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

    suspend fun validateSendAddress(chainId: ChainId, address: String): Boolean = withContext(Dispatchers.Default) {
        val chain = chainRegistry.getChain(chainId)

        chain.isValidAddress(address)
    }

    // TODO wallet phishing
    suspend fun isAddressFromPhishingList(address: String): Boolean {
        return /*walletRepository.isAccountIdFromPhishingList(address)*/ false
    }

    suspend fun getRecipientFromQrCodeContent(content: String): Result<String> {
        return withContext(Dispatchers.Default) {
            runCatching {
                QrSharing.decode(content).address
            }
        }
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

    fun validationSystemFor(asset: Chain.Asset) = assetTransfersProvider.provideFor(asset).validationSystem

    private fun getAssetTransfers(transfer: AssetTransfer) =
        assetTransfersProvider.provideFor(transfer.chainAsset)
}
