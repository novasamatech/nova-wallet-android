package io.novafoundation.nova.feature_governance_api.data.network.offchain.model.vote

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import jp.co.soramitsu.fearless_utils.runtime.AccountId

sealed class UserVote {

    class Direct(val vote: AccountVote) : UserVote()

    class Delegated(val delegate: AccountId, val vote: AccountVote) : UserVote()
}
