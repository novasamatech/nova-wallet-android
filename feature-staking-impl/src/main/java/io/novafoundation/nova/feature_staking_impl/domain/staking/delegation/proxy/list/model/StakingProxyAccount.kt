package io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.list.model

import jp.co.soramitsu.fearless_utils.runtime.AccountId

class StakingProxyAccount(
    val accountName: String,
    val proxyAccountId: AccountId
)
