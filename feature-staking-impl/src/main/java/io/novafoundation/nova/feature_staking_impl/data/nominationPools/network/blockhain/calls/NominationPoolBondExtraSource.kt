package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

sealed class NominationPoolBondExtraSource {

    class FreeBalance(val amount: Balance) : NominationPoolBondExtraSource()

    object Rewards : NominationPoolBondExtraSource()
}
