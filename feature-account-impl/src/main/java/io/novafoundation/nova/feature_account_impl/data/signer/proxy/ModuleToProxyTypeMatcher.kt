package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module

class ModuleToProxyTypeMatcher(private val module: String) {

    fun matchToProxyTypes(proxyTypes: List<ProxyType>): ProxyType? {
        if (proxyTypes.isEmpty()) return null

        return proxyTypes.firstOrNull { getModulesSupportedByProxyType(it).isSupporting(module) }
    }

    private fun getModulesSupportedByProxyType(proxyType: ProxyType): SupportedModules {
        return when (proxyType) {
            ProxyType.Any,
            is ProxyType.Other -> SupportedModules.AnyModule

            ProxyType.NonTransfer -> SupportedModules.SpeificModules(Modules.STAKING, Modules.REFERENDA, Modules.NOMINATION_POOLS)
            ProxyType.Governance -> SupportedModules.SpeificModules(Modules.REFERENDA)
            ProxyType.Staking -> SupportedModules.SpeificModules(Modules.STAKING)
            ProxyType.IdentityJudgement -> SupportedModules.SpeificModules(Modules.IDENTITY)
            ProxyType.CancelProxy -> SupportedModules.SpeificModules(Modules.PROXY)
            ProxyType.Auction -> SupportedModules.SpeificModules(Modules.AUCTIONS)
            ProxyType.NominationPools -> SupportedModules.SpeificModules(Modules.NOMINATION_POOLS)
        }
    }
}

private sealed interface SupportedModules {

    fun isSupporting(module: String): Boolean

    class SpeificModules(vararg modules: String) : SupportedModules {

        private val modulesSet = modules.toSet()

        override fun isSupporting(module: String): Boolean {
            return modulesSet.contains(module)
        }
    }

    object AnyModule : SupportedModules {
        override fun isSupporting(module: String) = true
    }
}

fun Module.toProxyTypeMatcher(): ModuleToProxyTypeMatcher {
    return ModuleToProxyTypeMatcher(name)
}
