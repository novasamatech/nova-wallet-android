package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter.CallFilter
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter.AnyOfCallFilter
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter.EverythingFilter
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter.WhiteListFilter
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

class ProxyCallFilterFactory {

    fun getCallFilterFor(proxyType: ProxyType): CallFilter {
        return when (proxyType) {
            ProxyType.Any,
            is ProxyType.Other -> EverythingFilter()

            ProxyType.NonTransfer -> AnyOfCallFilter(
                WhiteListFilter(Modules.SYSTEM),
                WhiteListFilter(Modules.SCHEDULER),
                WhiteListFilter(Modules.BABE),
                WhiteListFilter(Modules.TIMESTAMP),
                WhiteListFilter(Modules.INDICES, listOf("claim", "free", "freeze")),
                WhiteListFilter(Modules.STAKING),
                WhiteListFilter(Modules.SESSION),
                WhiteListFilter(Modules.GRANDPA),
                WhiteListFilter(Modules.IM_ONLINE),
                WhiteListFilter(Modules.TREASURY),
                WhiteListFilter(Modules.BOUNTIES),
                WhiteListFilter(Modules.CHILD_BOUNTIES),
                WhiteListFilter(Modules.CONVICTION_VOTING),
                WhiteListFilter(Modules.REFERENDA),
                WhiteListFilter(Modules.WHITELIST),
                WhiteListFilter(Modules.CLAIMS),
                WhiteListFilter(Modules.VESTING, listOf("vest", "vest_other")),
                WhiteListFilter(Modules.UTILITY),
                WhiteListFilter(Modules.IDENTITY),
                WhiteListFilter(Modules.PROXY),
                WhiteListFilter(Modules.MULTISIG),
                WhiteListFilter(Modules.REGISTRAR, listOf("register", "deregister", "reserve")),
                WhiteListFilter(Modules.CROWDLOAN),
                WhiteListFilter(Modules.SLOTS),
                WhiteListFilter(Modules.AUCTIONS),
                WhiteListFilter(Modules.VOTER_LIST),
                WhiteListFilter(Modules.NOMINATION_POOLS),
                WhiteListFilter(Modules.FAST_UNSTAKE)
            )

            ProxyType.Governance -> AnyOfCallFilter(
                WhiteListFilter(Modules.TREASURY),
                WhiteListFilter(Modules.BOUNTIES),
                WhiteListFilter(Modules.UTILITY),
                WhiteListFilter(Modules.CHILD_BOUNTIES),
                WhiteListFilter(Modules.CONVICTION_VOTING),
                WhiteListFilter(Modules.REFERENDA),
                WhiteListFilter(Modules.WHITELIST)
            )

            ProxyType.Staking -> AnyOfCallFilter(
                WhiteListFilter(Modules.STAKING),
                WhiteListFilter(Modules.SESSION),
                WhiteListFilter(Modules.UTILITY),
                WhiteListFilter(Modules.FAST_UNSTAKE),
                WhiteListFilter(Modules.VOTER_LIST),
                WhiteListFilter(Modules.NOMINATION_POOLS)
            )

            ProxyType.NominationPools -> AnyOfCallFilter(
                WhiteListFilter(Modules.NOMINATION_POOLS),
                WhiteListFilter(Modules.UTILITY)
            )

            ProxyType.IdentityJudgement -> AnyOfCallFilter(
                WhiteListFilter(Modules.IDENTITY, listOf("provide_judgement")),
                WhiteListFilter(Modules.UTILITY)
            )

            ProxyType.CancelProxy -> WhiteListFilter(Modules.PROXY, listOf("reject_announcement"))

            ProxyType.Auction -> AnyOfCallFilter(
                WhiteListFilter(Modules.AUCTIONS),
                WhiteListFilter(Modules.CROWDLOAN),
                WhiteListFilter(Modules.REGISTRAR),
                WhiteListFilter(Modules.SLOTS)
            )
        }
    }
}

fun ProxyCallFilterFactory.getFirstMatchedTypeOrNull(call: GenericCall.Instance, proxyTypes: List<ProxyType>): ProxyType? {
    return proxyTypes.firstOrNull {
        val callFilter = this.getCallFilterFor(it)
        callFilter.canExecute(call)
    }
}
