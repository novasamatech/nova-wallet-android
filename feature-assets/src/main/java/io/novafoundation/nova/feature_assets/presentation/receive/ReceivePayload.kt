package io.novafoundation.nova.feature_assets.presentation.receive

import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

sealed class ReceivePayload {

    class Asset(val assetPayload: AssetPayload): ReceivePayload ()

    class Chain(val chainId: ChainId): ReceivePayload()
}
