package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.common.utils.splitCamelCase
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.feature_account_impl.R

fun mapProxyTypeToString(resourceManager: ResourceManager, type: ProxyAccount.ProxyType): String {
    return when (type) {
        ProxyAccount.ProxyType.Any -> resourceManager.getString(R.string.account_proxy_type_any)
        ProxyAccount.ProxyType.NonTransfer -> resourceManager.getString(R.string.account_proxy_type_any)
        ProxyAccount.ProxyType.Governance -> resourceManager.getString(R.string.account_proxy_type_governance)
        ProxyAccount.ProxyType.Staking -> resourceManager.getString(R.string.account_proxy_type_staking)
        ProxyAccount.ProxyType.IdentityJudgement -> resourceManager.getString(R.string.account_proxy_type_identity_judgement)
        ProxyAccount.ProxyType.CancelProxy -> resourceManager.getString(R.string.account_proxy_type_cancel_proxy)
        ProxyAccount.ProxyType.Auction -> resourceManager.getString(R.string.account_proxy_type_auction)
        ProxyAccount.ProxyType.NominationPools -> resourceManager.getString(R.string.account_proxy_type_nomination_pools)
        is ProxyAccount.ProxyType.Other -> type.name.splitCamelCase().joinToString { it.capitalize() }
    }
}
