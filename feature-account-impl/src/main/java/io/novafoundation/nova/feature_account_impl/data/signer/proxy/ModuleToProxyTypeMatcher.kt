package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module

class ModuleToProxyTypeMatcher(private val module: String) {

    fun matchToProxyTypes(proxyTypes: List<ProxyAccount.ProxyType>): ProxyAccount.ProxyType? {
        if (proxyTypes.isEmpty()) return null

        return proxyTypes.firstOrNull { getModulesSupportedByProxyType(it).isSupporting(module) }
    }

    private fun getModulesSupportedByProxyType(proxyType: ProxyAccount.ProxyType): SupportedModules {
        return when (proxyType) {
            ProxyAccount.ProxyType.Any,
            is ProxyAccount.ProxyType.Other -> SupportedModules.AnyModule

            ProxyAccount.ProxyType.NonTransfer -> SupportedModules.SpeificModules(Modules.STAKING, Modules.REFERENDA, Modules.NOMINATION_POOLS)
            ProxyAccount.ProxyType.Governance -> SupportedModules.SpeificModules(Modules.REFERENDA)
            ProxyAccount.ProxyType.Staking -> SupportedModules.SpeificModules(Modules.STAKING)
            ProxyAccount.ProxyType.IdentityJudgement -> SupportedModules.SpeificModules(Modules.IDENTITY)
            ProxyAccount.ProxyType.CancelProxy -> SupportedModules.SpeificModules(Modules.PROXY)
            ProxyAccount.ProxyType.Auction -> SupportedModules.SpeificModules(Modules.AUCTIONS)
            ProxyAccount.ProxyType.NominationPools -> SupportedModules.SpeificModules(Modules.NOMINATION_POOLS)
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
