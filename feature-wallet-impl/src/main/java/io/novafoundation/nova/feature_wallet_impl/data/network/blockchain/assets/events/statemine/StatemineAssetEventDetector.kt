package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.statemine

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.AssetEventDetector
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.model.DepositEvent
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.StatemineAssetId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.extensions.requireHexPrefix
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.RuntimeType
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.definitions.types.toHexUntyped
import java.math.BigInteger

class StatemineAssetEventDetectorFactory(
    private val chainRegistry: ChainRegistry,
) {

    suspend fun create(chainAsset: Chain.Asset): AssetEventDetector {
        val assetType = chainAsset.requireStatemine()
        val runtime = chainRegistry.getRuntime(chainAsset.chainId)

        return StatemineAssetEventDetector(runtime, assetType)
    }
}

class StatemineAssetEventDetector(
    private val runtimeSnapshot: RuntimeSnapshot,
    private val assetType: Chain.Asset.Type.Statemine,
) : AssetEventDetector {

    private val targetAssetId = assetType.id.stringAssetId()

    override fun detectDeposit(event: GenericEvent.Instance): DepositEvent? {
        return detectTokensDeposited(event)
    }

    private fun detectTokensDeposited(event: GenericEvent.Instance): DepositEvent? {
        if (!event.instanceOf(assetType.palletNameOrDefault(), "Issued")) return null

        val (assetId, who, amount) = event.arguments

        val assetIdType = event.event.arguments.first()!!
        val assetIdAsString = decodedAssetItToString(assetId, assetIdType)
        if (assetIdAsString != targetAssetId) return null

        return DepositEvent(
            destination = bindAccountIdKey(who),
            amount = bindNumber(amount)
        )
    }

    private fun decodedAssetItToString(assetId: Any?, assetIdType: RuntimeType<*, *>): String {
        return if (assetId is BigInteger) {
            assetId.toString()
        } else {
            assetIdType.toHexUntyped(runtimeSnapshot, assetId).requireHexPrefix()
        }
    }

    private fun StatemineAssetId.stringAssetId(): String {
        return when (this) {
            is StatemineAssetId.Number -> value.toString()
            is StatemineAssetId.ScaleEncoded -> scaleHex
        }
    }
}
