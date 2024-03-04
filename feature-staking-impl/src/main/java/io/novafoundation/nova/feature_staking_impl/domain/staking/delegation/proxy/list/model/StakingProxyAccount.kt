package io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.list.model

import io.novasama.substrate_sdk_android.runtime.AccountId

class StakingProxyAccount(
    val accountName: String,
    val proxyAccountId: AccountId
)
