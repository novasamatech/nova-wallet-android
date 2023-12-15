package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class ProxyAccount(
    val metaId: Long,
    val chainId: ChainId,
    val proxiedAccountId: ByteArray,
    val proxyType: ProxyType,
) {

    sealed class ProxyType(val name: String) {

        object Any : ProxyType("Any")

        object NonTransfer : ProxyType("NonTransfer")

        object Governance : ProxyType("Governance")

        object Staking : ProxyType("Staking")

        object IdentityJudgement : ProxyType("IdentityJudgement")

        object CancelProxy : ProxyType("CancelProxy")

        object Auction : ProxyType("Auction")

        object NominationPools : ProxyType("NominationPools")

        class Other(name: String) : ProxyType(name)
    }
}
