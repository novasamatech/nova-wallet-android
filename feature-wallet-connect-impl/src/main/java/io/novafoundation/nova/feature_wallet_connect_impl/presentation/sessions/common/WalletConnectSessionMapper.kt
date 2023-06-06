package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_connect_impl.R
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.SessionDappMetadata
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.dAppTitle

interface WalletConnectSessionMapper {

    fun formatSessionDAppTitle(metadata: SessionDappMetadata?): String
}

class RealWalletConnectSessionMapper(
    private val resourceManager: ResourceManager
) : WalletConnectSessionMapper {

    override fun formatSessionDAppTitle(metadata: SessionDappMetadata?): String {
        return metadata?.dAppTitle ?: resourceManager.getString(R.string.wallet_connect_unknown_dapp)
    }
}
