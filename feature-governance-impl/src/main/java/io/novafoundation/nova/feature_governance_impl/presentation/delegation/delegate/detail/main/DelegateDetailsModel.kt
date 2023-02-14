package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateIcon
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateTypeModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.view.YourDelegationModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ShortenedTextModel

class DelegateDetailsModel(
    val addressModel: AddressModel,
    val metadata: Metadata,
    val stats: Stats?,
    val userDelegation: YourDelegationModel?
) {

    class Stats(
        val delegations: VotesModel,
        val delegatedVotes: String,
        val recentVotes: VotesModel,
        val allVotes: VotesModel,
    )

    class VotesModel(
        val extraInfoAvalable: Boolean,
        val votes: String,
        val customLabel: String?
    )

    class Metadata(
        val name: String?,
        val icon: DelegateIcon,
        val accountType: DelegateTypeModel?,
        val description: ShortenedTextModel?,
    )
}
