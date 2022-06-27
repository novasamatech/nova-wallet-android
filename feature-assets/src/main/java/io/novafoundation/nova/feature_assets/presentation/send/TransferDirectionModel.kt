package io.novafoundation.nova.feature_assets.presentation.send

import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

class TransferDirectionModel(
    val originChainUi: ChainUi,
    val originChainLabel: String,
    val destinationChainUi: ChainUi?
)
