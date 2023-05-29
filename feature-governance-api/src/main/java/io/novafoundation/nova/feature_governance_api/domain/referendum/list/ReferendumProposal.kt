package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

sealed class ReferendumProposal {

    class Hash(val callHash: String) : ReferendumProposal()

    class Call(val call: GenericCall.Instance) : ReferendumProposal()
}
