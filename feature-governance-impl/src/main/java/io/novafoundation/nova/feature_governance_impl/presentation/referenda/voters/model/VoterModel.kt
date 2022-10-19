package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.model

import io.novafoundation.nova.common.address.AddressModel

class VoterModel(
    val addressModel: AddressModel,
    val votesCount: String,
    val votesCountDetails: String
)
