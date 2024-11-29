package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.AssetEventDetector
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.model.DepositEvent
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

class UnsupportedEventDetector : AssetEventDetector {

    override fun detectDeposit(event: GenericEvent.Instance): DepositEvent? {
        return null
    }
}
