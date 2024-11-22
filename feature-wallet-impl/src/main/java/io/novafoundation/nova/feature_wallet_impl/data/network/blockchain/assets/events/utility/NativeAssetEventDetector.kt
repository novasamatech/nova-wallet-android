package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.utility

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.AssetEventDetector
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.model.DepositEvent
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

class NativeAssetEventDetector : AssetEventDetector {

    override fun detectDeposit(event: GenericEvent.Instance): DepositEvent? {
        return detectMinted(event)
    }

    private fun detectMinted(event: GenericEvent.Instance): DepositEvent? {
        if (!event.instanceOf(Modules.BALANCES, "Minted")) return null

        val (who, amount) = event.arguments

        return DepositEvent(
            destination = bindAccountId(who),
            amount = bindNumber(amount)
        )
    }
}