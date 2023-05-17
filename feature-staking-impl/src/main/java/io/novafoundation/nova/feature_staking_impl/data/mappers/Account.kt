package io.novafoundation.nova.feature_staking_impl.data.mappers

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_staking_api.domain.model.StakingAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun mapAccountToStakingAccount(chain: Chain, metaAccount: MetaAccount): StakingAccount? = with(metaAccount) {
    val address = addressIn(chain)

    address?.let {
        StakingAccount(
            address = address,
            name = name,
        )
    }
}
