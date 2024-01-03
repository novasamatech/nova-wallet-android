package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter.CallFilter
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter.CompoundCallFilter
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter.EverythingFilter
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.callFilter.WhiteListFilter
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

class ProxyCallFilterFactory {

    fun getCallFilterFor(proxyType: ProxyAccount.ProxyType): CallFilter {
        return when (proxyType) {
            ProxyAccount.ProxyType.Any,
            is ProxyAccount.ProxyType.Other -> EverythingFilter()

            ProxyAccount.ProxyType.NonTransfer -> CompoundCallFilter(
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

            ProxyAccount.ProxyType.Governance -> CompoundCallFilter(
                WhiteListFilter(Modules.TREASURY),
                WhiteListFilter(Modules.BOUNTIES),
                WhiteListFilter(Modules.UTILITY),
                WhiteListFilter(Modules.CHILD_BOUNTIES),
                WhiteListFilter(Modules.CONVICTION_VOTING),
                WhiteListFilter(Modules.REFERENDA),
                WhiteListFilter(Modules.WHITELIST)
            )

            ProxyAccount.ProxyType.Staking -> CompoundCallFilter(
                WhiteListFilter(Modules.STAKING),
                WhiteListFilter(Modules.SESSION),
                WhiteListFilter(Modules.UTILITY),
                WhiteListFilter(Modules.FAST_UNSTAKE),
                WhiteListFilter(Modules.VOTER_LIST),
                WhiteListFilter(Modules.NOMINATION_POOLS)
            )

            ProxyAccount.ProxyType.NominationPools -> CompoundCallFilter(
                WhiteListFilter(Modules.NOMINATION_POOLS),
                WhiteListFilter(Modules.UTILITY)
            )

            ProxyAccount.ProxyType.IdentityJudgement -> CompoundCallFilter(
                WhiteListFilter(Modules.IDENTITY, listOf("provide_judgement")),
                WhiteListFilter(Modules.UTILITY)
            )

            ProxyAccount.ProxyType.CancelProxy -> WhiteListFilter(Modules.PROXY, listOf("reject_announcement"))

            ProxyAccount.ProxyType.Auction -> CompoundCallFilter(
                WhiteListFilter(Modules.AUCTIONS),
                WhiteListFilter(Modules.CROWDLOAN),
                WhiteListFilter(Modules.REGISTRAR),
                WhiteListFilter(Modules.SLOTS)
            )
        }
    }
}

fun ProxyCallFilterFactory.getFirstMatchedTypeOrNull(call: GenericCall.Instance, proxyTypes: List<ProxyAccount.ProxyType>): ProxyAccount.ProxyType? {
    return proxyTypes.firstOrNull {
        val callFilter = this.getCallFilterFor(it)
        callFilter.canExecute(call)
    }
}
