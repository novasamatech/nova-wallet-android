package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.utility

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountInfo
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.nativeTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.BaseAssetTransfers
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class NativeAssetTransfers(
    chainRegistry: ChainRegistry,
    assetSourceRegistry: AssetSourceRegistry,
    extrinsicServiceFactory: ExtrinsicService.Factory,
    phishingValidationFactory: PhishingValidationFactory,
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory,
    private val storageDataSource: StorageDataSource,
    private val accountRepository: AccountRepository,
) : BaseAssetTransfers(chainRegistry, assetSourceRegistry, extrinsicServiceFactory, phishingValidationFactory, enoughTotalToStayAboveEDValidationFactory) {

    override suspend fun totalCanDropBelowMinimumBalance(chainAsset: Chain.Asset): Boolean {
        val chain = chainRegistry.getChain(chainAsset.chainId)
        val metaAccount = accountRepository.getSelectedMetaAccount()

        val accountInfo = storageDataSource.query(
            chainAsset.chainId,
            keyBuilder = { getAccountInfoStorageKey(metaAccount, chain, it) },
            binding = { it, runtime -> it?.let { bindAccountInfo(it, runtime) } }
        )

        return accountInfo != null && accountInfo.consumers.isZero
    }

    override fun totalCanDropBelowMinimumBalanceFlow(chainAsset: Chain.Asset): Flow<Boolean> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            val chain = chainRegistry.getChain(chainAsset.chainId)

            storageDataSource.observe(
                chainAsset.chainId,
                keyBuilder = { getAccountInfoStorageKey(metaAccount, chain, it) },
                binder = { it, runtime -> it?.let { bindAccountInfo(it, runtime) } }
            ).filterNotNull()
                .map { it.consumers.isZero }
        }
    }

    override fun ExtrinsicBuilder.transfer(transfer: AssetTransfer) {
        nativeTransfer(
            accountId = transfer.originChain.accountIdOrDefault(transfer.recipient),
            amount = transfer.originChainAsset.planksFromAmount(transfer.amount)
        )
    }

    override suspend fun transferFunctions(chainAsset: Chain.Asset) = listOf(
        Modules.BALANCES to "transfer",
        Modules.BALANCES to "transfer_allow_death",
    )

    private fun getAccountInfoStorageKey(metaAccount: MetaAccount, chain: Chain, runtime: RuntimeSnapshot): String {
        val accountId = metaAccount.requireAccountIdIn(chain)
        return runtime.metadata.module(Modules.SYSTEM).storage("Account").storageKey(runtime, accountId)
    }
}
