package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.equilibrium

import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.eqBalances
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.BaseAssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.positiveAmount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientBalanceInUsedAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientTransferableBalanceToPayOriginFee
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.validAddress
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.ext.requireEquilibrium
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey

class EquilibriumAssetTransfers(
    chainRegistry: ChainRegistry,
    assetSourceRegistry: AssetSourceRegistry,
    extrinsicService: ExtrinsicService,
    phishingValidationFactory: PhishingValidationFactory,
    private val remoteStorageSource: StorageDataSource,
) : BaseAssetTransfers(chainRegistry, assetSourceRegistry, extrinsicService, phishingValidationFactory) {

    override val validationSystem: AssetTransfersValidationSystem
        get() = ValidationSystem {
            validAddress()
            positiveAmount()
            sufficientBalanceInUsedAsset()
            sufficientTransferableBalanceToPayOriginFee()
        }

    override fun ExtrinsicBuilder.transfer(transfer: AssetTransfer) {
        if (transfer.originChainAsset.type !is Chain.Asset.Type.Equilibrium) return

        val accountId = transfer.originChain.accountIdOrDefault(transfer.recipient)
        val amount = transfer.originChainAsset.planksFromAmount(transfer.amount)

        call(
            moduleName = Modules.EQ_BALANCES,
            callName = "transfer",
            arguments = mapOf(
                "asset" to transfer.originChainAsset.requireEquilibrium().id,
                "to" to accountId,
                "value" to amount
            )
        )
    }

    override suspend fun transferFunctions(chainAsset: Chain.Asset): List<Pair<String, String>> = emptyList()

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        if (chainAsset.type !is Chain.Asset.Type.Equilibrium) return false

        return remoteStorageSource.query(
            chainAsset.chainId,
            keyBuilder = { it.getTransferEnabledStorage().storageKey() },
            binding = { scale, runtimeSnapshot ->
                if (scale == null) return@query false
                val returnType = runtimeSnapshot.getTransferEnabledStorage().returnType()
                bindBoolean(returnType.fromHexOrNull(runtimeSnapshot, scale))
            }
        )
    }

    private fun RuntimeSnapshot.getTransferEnabledStorage(): StorageEntry {
        return metadata.eqBalances().storage("IsTransfersEnabled")
    }
}
