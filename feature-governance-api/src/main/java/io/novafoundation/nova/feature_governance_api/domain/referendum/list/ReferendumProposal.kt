package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

sealed class ReferendumProposal {

    class Hash(val callHash: String) : ReferendumProposal()

    class Call(val call: GenericCall.Instance) : ReferendumProposal()
}
