package io.novafoundation.nova.feature_assets.domain.send

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.assetFlow
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.isCrossChain
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.domain.implementations.availableDestinations
import io.novafoundation.nova.feature_wallet_api.domain.implementations.crossChainFeeAssetId
import io.novafoundation.nova.feature_wallet_api.domain.implementations.transferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.RecipientSearchResult
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

class SendInteractor(
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val crossChainWeigher: CrossChainWeigher,
    private val crossChainTransactor: CrossChainTransactor,
    private val crossChainTransfersRepository: CrossChainTransfersRepository,
    private val walletInteractor: WalletInteractor,
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

    suspend fun syncCrossChainConfig() = kotlin.runCatching {
        crossChainTransfersRepository.syncConfiguration()
    }

    suspend fun getOriginFee(transfer: AssetTransfer): BigInteger = withContext(Dispatchers.Default) {
        if (transfer.isCrossChain) {
            val config = crossChainTransfersRepository.getConfiguration().configurationFor(transfer)!!

            crossChainTransactor.estimateOriginFee(config, transfer)
        } else {
            getAssetTransfers(transfer).calculateFee(transfer)
        }
    }

    suspend fun getCrossChainFee(transfer: AssetTransfer): BigInteger? = if (transfer.isCrossChain) {
        withContext(Dispatchers.Default) {
            val config = crossChainTransfersRepository.getConfiguration().configurationFor(transfer)!!
            val crossChainFee = crossChainWeigher.estimateFee(config)

            crossChainFee.reserve.orZero() + crossChainFee.destination.orZero()
        }
    } else {
        null
    }

    fun crossChainFeeAssetFlow(
        originChainId: ChainId,
        originAsset: ChainAssetId,
        destinationChainId: ChainId
    ): Flow<Asset> {
        return flow {
            val fullAssetId = crossChainTransfersRepository.getConfiguration().crossChainFeeAssetId(
                originChainId = originChainId,
                originAssetId = originAsset,
                destinationChainId = destinationChainId
            )

            if (fullAssetId != null) {
                emitAll(walletInteractor.assetFlow(fullAssetId))
            }
        }
    }

    suspend fun performTransfer(
        transfer: AssetTransfer,
        originFee: BigDecimal,
        crossChainFee: BigDecimal?,
    ): Result<*> = withContext(Dispatchers.Default) {
        if (transfer.isCrossChain) {
            val crossChainTransfersConfiguration = crossChainTransfersRepository.getConfiguration()
            val crossChainFeeAssetId = crossChainTransfersConfiguration.crossChainFeeAsset(transfer)
            val crossChainFeePlanks = crossChainFeeAssetId.planksFromAmount(crossChainFee!!)

            val config = crossChainTransfersConfiguration.configurationFor(transfer)!!

            crossChainTransactor.performTransfer(config, transfer, crossChainFeePlanks)
        } else {
            getAssetTransfers(transfer).performTransfer(transfer)
                .onSuccess { hash ->
                    walletRepository.insertPendingTransfer(hash, transfer, originFee)
                }
        }
    }

    fun availableCrossChainDestinationsFlow(origin: Chain.Asset): Flow<List<ChainWithAsset>> {
        return crossChainTransfersRepository.configurationFlow().map { configuration ->
            val chainsById = chainRegistry.chainsById.first()

            configuration.availableDestinations(origin).mapNotNull { (chainId, assetId) ->
                val chain = chainsById[chainId] ?: return@mapNotNull null
                val asset = chain.assetsById[assetId] ?: return@mapNotNull null

                ChainWithAsset(chain, asset)
            }
        }
    }

    fun validationSystemFor(transfer: AssetTransfer) = if (transfer.isCrossChain) {
        crossChainTransactor.validationSystem
    } else {
        assetSourceRegistry.sourceFor(transfer.originChainAsset).transfers.validationSystem
    }

    suspend fun areTransfersEnabled(asset: Chain.Asset) = assetSourceRegistry.sourceFor(asset).transfers.areTransfersEnabled(asset)

    private fun getAssetTransfers(transfer: AssetTransfer) = assetSourceRegistry.sourceFor(transfer.originChainAsset).transfers

    private suspend fun CrossChainTransfersConfiguration.configurationFor(transfer: AssetTransfer) = transferConfiguration(
        originChain = transfer.originChain,
        originAsset = transfer.originChainAsset,
        destinationChain = transfer.destinationChain,
        destinationParaId = crossChainTransfersRepository.paraId(transfer.destinationChain.id)
    )

    private suspend fun CrossChainTransfersConfiguration.crossChainFeeAsset(transfer: AssetTransfer): Chain.Asset {
        val fullAssetId = crossChainFeeAssetId(
            originChainId = transfer.originChain.id,
            originAssetId = transfer.originChainAsset.id,
            destinationChainId = transfer.destinationChain.id
        )!!

        return chainRegistry.asset(fullAssetId)
    }
}
