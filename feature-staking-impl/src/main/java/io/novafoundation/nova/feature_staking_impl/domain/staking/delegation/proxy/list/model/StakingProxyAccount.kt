package io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.list.model

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class StakingProxyAccount(
    val metaAccount: MetaAccount?,
    val proxyAccountId: AccountId
)
