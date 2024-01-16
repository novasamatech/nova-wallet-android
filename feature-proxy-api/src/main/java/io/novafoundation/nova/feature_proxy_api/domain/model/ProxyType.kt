package io.novafoundation.nova.feature_proxy_api.domain.model

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

    companion object
}

fun ProxyType.Companion.fromString(name: String): ProxyType {
    return when (name) {
        "Any" -> ProxyType.Any
        "NonTransfer" -> ProxyType.NonTransfer
        "Governance" -> ProxyType.Governance
        "Staking" -> ProxyType.Staking
        "IdentityJudgement" -> ProxyType.IdentityJudgement
        "CancelProxy" -> ProxyType.CancelProxy
        "Auction" -> ProxyType.Auction
        "NominationPools" -> ProxyType.NominationPools
        else -> ProxyType.Other(name)
    }
}
