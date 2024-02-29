package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novasama.substrate_sdk_android.runtime.AccountId

sealed class ReferendumVote(val vote: AccountVote) {

    class UserDirect(vote: AccountVote) : ReferendumVote(vote)

    class UserDelegated(
        override val who: AccountId,
        override val whoIdentity: Identity?,
        vote: AccountVote
    ) : ReferendumVote(vote), WithDifferentVoter

    class OtherAccount(
        override val who: AccountId,
        override val whoIdentity: Identity?,
        vote: AccountVote
    ) : ReferendumVote(vote), WithDifferentVoter
}

interface WithDifferentVoter {

    val who: AccountId

    val whoIdentity: Identity?
}
