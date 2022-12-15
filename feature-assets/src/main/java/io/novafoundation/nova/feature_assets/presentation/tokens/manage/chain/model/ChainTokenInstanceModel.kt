package io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.model

import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

data class ChainTokenInstanceModel(
    val chainUi: ChainUi,
    val enabled: Boolean,
    val switchable: Boolean,
)
