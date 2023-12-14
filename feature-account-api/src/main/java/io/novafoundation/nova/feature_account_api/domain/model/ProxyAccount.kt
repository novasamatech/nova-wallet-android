package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class ProxyAccount(
    val metaId: Long,
    val chainId: ChainId,
    val proxiedAccountId: ByteArray,
    val proxyType: ProxyType,
) {

    sealed interface ProxyType {

        object Any : ProxyType

        object NonTransfer : ProxyType

        object Governance : ProxyType

        object Staking : ProxyType

        object IdentityJudgement : ProxyType

        object CancelProxy : ProxyType

        object Auction : ProxyType

        object NominationPools : ProxyType

        class Other(val name: String) : ProxyType
    }
}
