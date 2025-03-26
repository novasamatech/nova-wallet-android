package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.orml

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.AssetEventDetector
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.model.DepositEvent
import io.novafoundation.nova.runtime.ext.requireOrml
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.extensions.requireHexPrefix
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.definitions.types.toHexUntyped

class OrmlAssetEventDetectorFactory(
    private val chainRegistry: ChainRegistry,
) {

    suspend fun create(chainAsset: Chain.Asset): AssetEventDetector {
        val ormlType = chainAsset.requireOrml()
        val runtime = chainRegistry.getRuntime(chainAsset.chainId)

        return OrmlAssetEventDetector(runtime, ormlType)
    }
}

private class OrmlAssetEventDetector(
    private val runtimeSnapshot: RuntimeSnapshot,
    private val ormlType: Chain.Asset.Type.Orml,
) : AssetEventDetector {

    private val targetCurrencyId = ormlType.currencyIdScale.requireHexPrefix()

    override fun detectDeposit(event: GenericEvent.Instance): DepositEvent? {
        return detectTokensDeposited(event)
    }

    private fun detectTokensDeposited(event: GenericEvent.Instance): DepositEvent? {
        if (!event.instanceOf(Modules.TOKENS, "Deposited")) return null

        val (currencyId, who, amount) = event.arguments

        val currencyIdType = runtimeSnapshot.typeRegistry[ormlType.currencyIdType]!!
        val currencyIdEncoded = currencyIdType.toHexUntyped(runtimeSnapshot, currencyId).requireHexPrefix()
        if (currencyIdEncoded != targetCurrencyId) return null

        return DepositEvent(
            destination = bindAccountIdKey(who),
            amount = bindNumber(amount)
        )
    }
}
