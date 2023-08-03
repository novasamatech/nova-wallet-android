package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.alerts

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

sealed class NominationPoolAlert {

    object WaitingForNextEra : NominationPoolAlert()

    class RedeemTokens(val amount: Balance) : NominationPoolAlert()
}
