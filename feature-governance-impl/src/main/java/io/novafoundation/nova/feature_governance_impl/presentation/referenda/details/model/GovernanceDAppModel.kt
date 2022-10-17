package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model

import io.novafoundation.nova.feature_governance_api.data.repository.ReferendumUrlConstructor

class GovernanceDAppModel(
    val name: String,
    val iconUrl: String?,
    val description: String,
    val urlConstructor: ReferendumUrlConstructor
)
