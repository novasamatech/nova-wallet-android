package io.novafoundation.nova.feature_assets.presentation.manageTokens.chain.model

import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

data class ChainTokenInstanceModel(
    val chainUi: ChainUi,
    val enabled: Boolean,
)
