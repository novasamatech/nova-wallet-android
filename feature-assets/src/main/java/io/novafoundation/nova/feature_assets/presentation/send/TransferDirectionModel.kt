package io.novafoundation.nova.feature_assets.presentation.send

import io.novafoundation.nova.feature_account_api.view.ChainChipModel

class TransferDirectionModel(
    val originChip: ChainChipModel,
    val originChainLabel: String,
    val destinationChip: ChainChipModel?
)
