package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.view

import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

sealed class WCNetworkListModel {

    class Label(val name: String, val needsAdditionalSeparator: Boolean) : WCNetworkListModel()

    class Chain(val chainUi: ChainUi) : WCNetworkListModel()
}
