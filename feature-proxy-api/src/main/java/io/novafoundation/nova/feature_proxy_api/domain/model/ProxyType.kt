package io.novafoundation.nova.feature_proxy_api.domain.model

sealed class ProxyType(open val name: String, val controllableFrom: List<ProxyType>) {

    data object Any : ProxyType("Any", emptyList())

    data object NonTransfer : ProxyType("NonTransfer", listOf(Any))

    data object Governance : ProxyType("Governance", listOf(Any, NonTransfer))

    data object Staking : ProxyType("Staking", listOf(Any, NonTransfer))

    data object IdentityJudgement : ProxyType("IdentityJudgement", listOf(Any, NonTransfer))

    data object CancelProxy : ProxyType("CancelProxy", listOf(Any, NonTransfer))

    data object Auction : ProxyType("Auction", listOf(Any, NonTransfer))

    data object NominationPools : ProxyType("NominationPools", listOf(Any, NonTransfer, Staking))

    data class Other(override val name: String) : ProxyType(name, listOf(Any))

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

fun ProxyType.isControllableFrom(proxyType: ProxyType): Boolean {
    return name == proxyType.name || controllableFrom.contains(proxyType)
}

fun ProxyType.Companion.min(first: ProxyType, second: ProxyType): ProxyType? {
    return first.takeIf { it.isControllableFrom(second) }
        ?: second.takeIf { it.isControllableFrom(first) }
}
