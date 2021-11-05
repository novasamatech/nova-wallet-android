package io.novafoundation.nova.feature_staking_impl.data.mappers

import io.novafoundation.nova.feature_account_api.data.mappers.stubNetwork
import io.novafoundation.nova.feature_account_api.domain.model.Account
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_staking_api.domain.model.StakingAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun mapAccountToStakingAccount(account: Account) = with(account) {
    StakingAccount(address, name, network)
}

fun mapAccountToStakingAccount(chain: Chain, metaAccount: MetaAccount) = with(metaAccount) {
    StakingAccount(
        address = addressIn(chain)!!, // TODO may be null in ethereum
        name = name,
        network = stubNetwork(chain.id),
    )
}
