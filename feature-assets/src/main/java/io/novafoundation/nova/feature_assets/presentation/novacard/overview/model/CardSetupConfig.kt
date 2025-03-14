package io.novafoundation.nova.feature_assets.presentation.novacard.overview.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class CardSetupConfig(
    val refundAddress: String,
    val spendToken: Chain.Asset,
)
