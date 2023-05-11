package io.novafoundation.nova.feature_wallet_connect_impl.domain.model

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class WalletConnectSession(
    val connectedMetaAccount: MetaAccount,
    val dappMetadata: SessionDappMetadata?,
    val sessionTopic: String,
)

class SessionDappMetadata(
    val dAppUrl: String,
    val icon: String?,
    val name: String?
)

val SessionDappMetadata.dAppTitle: String
    get() = name ?: dAppUrl

class WalletConnectSessionDetails(
    val connectedMetaAccount: MetaAccount,
    val dappMetadata: SessionDappMetadata?,
    val chains: Set<Chain>,
    val sessionTopic: String,
    val status: SessionStatus,
) {

    enum class SessionStatus {

        ACTIVE, EXPIRED
    }
}
