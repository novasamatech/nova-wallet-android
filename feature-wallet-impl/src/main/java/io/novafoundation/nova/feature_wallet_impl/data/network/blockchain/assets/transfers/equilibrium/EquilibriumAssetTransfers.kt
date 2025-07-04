package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.equilibrium

import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.eqBalances
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.model.TransferParsedFromCall
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.BaseAssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.notDeadRecipientInUsedAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.positiveAmount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.recipientIsNotSystemAccount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientBalanceInUsedAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientTransferableBalanceToPayOriginFee
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.validAddress
import io.novafoundation.nova.feature_wallet_impl.domain.validaiton.recipientCanAcceptTransfer
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.ext.requireEquilibrium
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHexOrNull
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.call
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import kotlinx.coroutines.CoroutineScope

private const val TRANSFER_CALL = "transfer"

class EquilibriumAssetTransfers(
    chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
    extrinsicServiceFactory: ExtrinsicService.Factory,
    phishingValidationFactory: PhishingValidationFactory,
    private val remoteStorageSource: StorageDataSource,
    private val enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) : BaseAssetTransfers(chainRegistry, assetSourceRegistry, extrinsicServiceFactory, phishingValidationFactory, enoughTotalToStayAboveEDValidationFactory) {

    override fun getValidationSystem(coroutineScope: CoroutineScope) = ValidationSystem {
        validAddress()
        recipientIsNotSystemAccount()

        positiveAmount()
        sufficientBalanceInUsedAsset()
        sufficientTransferableBalanceToPayOriginFee()

        notDeadRecipientInUsedAsset(assetSourceRegistry)
        recipientCanAcceptTransfer(assetSourceRegistry)
    }

    override suspend fun parseTransfer(call: GenericCall.Instance, chain: Chain): TransferParsedFromCall? {
        return null
    }

    override fun ExtrinsicBuilder.transfer(transfer: AssetTransfer) {
        if (transfer.originChainAsset.type !is Chain.Asset.Type.Equilibrium) return

        val accountId = transfer.originChain.accountIdOrDefault(transfer.recipient)
        val amount = transfer.originChainAsset.planksFromAmount(transfer.amount)

        call(
            moduleName = Modules.EQ_BALANCES,
            callName = TRANSFER_CALL,
            arguments = mapOf(
                "asset" to transfer.originChainAsset.requireEquilibrium().id,
                "to" to accountId,
                "value" to amount
            )
        )
    }

    override suspend fun transferFunctions(chainAsset: Chain.Asset): List<Pair<String, String>> {
        return listOf(Modules.EQ_BALANCES to TRANSFER_CALL)
    }

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        if (chainAsset.type !is Chain.Asset.Type.Equilibrium) return false

        return queryIsTransferEnabledStorage(chainAsset) ?: super.areTransfersEnabled(chainAsset)
    }

    private suspend fun queryIsTransferEnabledStorage(chainAsset: Chain.Asset): Boolean? {
        return remoteStorageSource.query(
            chainAsset.chainId,
            keyBuilder = { it.getTransferEnabledStorage().storageKey() },
            binding = { scale, runtimeSnapshot ->
                if (scale == null) return@query null
                val returnType = runtimeSnapshot.getTransferEnabledStorage().returnType()
                bindBoolean(returnType.fromHexOrNull(runtimeSnapshot, scale))
            }
        )
    }

    private fun RuntimeSnapshot.getTransferEnabledStorage(): StorageEntry {
        return metadata.eqBalances().storage("IsTransfersEnabled")
    }
}
