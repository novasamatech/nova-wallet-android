package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.model.DepositEvent
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

interface AssetEventDetector {

    fun detectDeposit(event: GenericEvent.Instance): DepositEvent?
}

fun AssetEventDetector.tryDetectDeposit(event: GenericEvent.Instance): DepositEvent? {
    return runCatching { detectDeposit(event) }.getOrNull()
}
