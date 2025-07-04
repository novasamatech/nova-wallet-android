package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.evmErc20

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindByteArray
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.ethereumAddressToAccountId
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.AssetEventDetector
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.model.DepositEvent
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Queries
import io.novafoundation.nova.runtime.ext.requireErc20
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import javax.inject.Inject

@FeatureScope
class EvmErc20EventDetectorFactory @Inject constructor() {

    fun create(chainAsset: Chain.Asset): AssetEventDetector {
        return EvmErc20EventDetector(chainAsset.requireErc20())
    }
}

class EvmErc20EventDetector(
    private val erc20AssetType: Chain.Asset.Type.EvmErc20
) : AssetEventDetector {

    override fun detectDeposit(event: GenericEvent.Instance): DepositEvent? {
        return parseEvmLogEvent(event)?.toDepositEvent()
    }

    private fun parseEvmLogEvent(event: GenericEvent.Instance): Erc20Queries.Transfer? {
        if (!event.instanceOf(Modules.EVM, "Log")) return null

        val args = event.arguments.first().castToStruct()

        val address = bindAccountIdKey(args["address"])
        val contractAddress = erc20AssetType.contractAddress.ethereumAddressToAccountId().intoKey()

        if (contractAddress != address) return null

        val topics = bindList(args["topics"], ::bindHexString)

        val eventSignature = topics[0]
        if (eventSignature != Erc20Queries.transferEventSignature()) return null

        return Erc20Queries.parseTransferEvent(
            topic1 = topics[1],
            topic2 = topics[2],
            data = bindHexString(args["data"])
        )
    }

    private fun Erc20Queries.Transfer.toDepositEvent(): DepositEvent {
        return DepositEvent(
            destination = to.value.ethereumAddressToAccountId().intoKey(),
            amount = amount.value
        )
    }

    private fun bindHexString(decoded: Any?): String {
        return bindByteArray(decoded).toHexString(withPrefix = true)
    }
}
