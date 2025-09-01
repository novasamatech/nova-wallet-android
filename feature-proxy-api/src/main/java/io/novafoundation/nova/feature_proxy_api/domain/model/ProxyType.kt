package io.novafoundation.nova.feature_proxy_api.domain.model

sealed class ProxyType(open val name: String) {

    data object Any : ProxyType("Any")

    data object NonTransfer : ProxyType("NonTransfer")

    data object Governance : ProxyType("Governance")

    data object Staking : ProxyType("Staking")

    data object IdentityJudgement : ProxyType("IdentityJudgement")

    data object CancelProxy : ProxyType("CancelProxy")

    data object Auction : ProxyType("Auction")

    data object NominationPools : ProxyType("NominationPools")

    data class Other(override val name: String) : ProxyType(name)

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
