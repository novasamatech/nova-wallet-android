package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.utility

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.TransferMode
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.model.TransferParsedFromCall
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.nativeTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.BaseAssetTransfers
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey

class NativeAssetTransfers(
    chainRegistry: ChainRegistry,
    assetSourceRegistry: AssetSourceRegistry,
    extrinsicServiceFactory: ExtrinsicService.Factory,
    phishingValidationFactory: PhishingValidationFactory,
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory,
    private val storageDataSource: StorageDataSource,
    private val accountRepository: AccountRepository,
) : BaseAssetTransfers(chainRegistry, assetSourceRegistry, extrinsicServiceFactory, phishingValidationFactory, enoughTotalToStayAboveEDValidationFactory) {

    companion object {

        private const val TRANSFER_ALL = "transfer_all"
        private const val TRANSFER = "transfer"
        private const val TRANSFER_KEEP_ALIVE = "transfer_keep_alive"
        private const val TRANSFER_ALLOW_DEATH = "transfer_allow_death"
    }

    private val parsableCalls = listOf(TRANSFER, TRANSFER_KEEP_ALIVE, TRANSFER_ALLOW_DEATH)

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

    override suspend fun parseTransfer(call: GenericCall.Instance, chain: Chain): TransferParsedFromCall? {
        val isOurs = parsableCalls.any { call.instanceOf(Modules.BALANCES, it) }
        if (!isOurs) return null

        val asset = chain.utilityAsset
        val amount = bindNumber(call.arguments["value"])
        val recipient = bindAccountIdentifier(call.arguments["dest"]).intoKey()

        return TransferParsedFromCall(asset.withAmount(amount), recipient)
    }

    override fun ExtrinsicBuilder.transfer(transfer: AssetTransfer) {
        nativeTransfer(
            accountId = transfer.originChain.accountIdOrDefault(transfer.recipient),
            amount = transfer.originChainAsset.planksFromAmount(transfer.amount),
            mode = transfer.transferMode
        )
    }

    override suspend fun transferFunctions(chainAsset: Chain.Asset) = listOf(
        Modules.BALANCES to TRANSFER,
        Modules.BALANCES to TRANSFER_ALLOW_DEATH,
        Modules.BALANCES to TRANSFER_ALL
    )

    private fun getAccountInfoStorageKey(metaAccount: MetaAccount, chain: Chain, runtime: RuntimeSnapshot): String {
        val accountId = metaAccount.requireAccountIdIn(chain)
        return runtime.metadata.module(Modules.SYSTEM).storage("Account").storageKey(runtime, accountId)
    }

    private val AssetTransfer.transferMode: TransferMode
        get() = if (transferringMaxAmount) {
            TransferMode.ALL
        } else {
            TransferMode.ALLOW_DEATH
        }
}
