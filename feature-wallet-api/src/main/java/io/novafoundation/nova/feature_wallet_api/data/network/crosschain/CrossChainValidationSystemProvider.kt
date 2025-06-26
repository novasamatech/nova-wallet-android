package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem

interface CrossChainValidationSystemProvider {

    fun createValidationSystem(): AssetTransfersValidationSystem
}
