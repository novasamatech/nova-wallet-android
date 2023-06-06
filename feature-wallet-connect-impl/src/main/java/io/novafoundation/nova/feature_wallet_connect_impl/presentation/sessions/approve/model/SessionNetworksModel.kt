package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.model

import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainListOverview

class SessionNetworksModel(
    val label: String,
    val value: ChainListOverview
)
