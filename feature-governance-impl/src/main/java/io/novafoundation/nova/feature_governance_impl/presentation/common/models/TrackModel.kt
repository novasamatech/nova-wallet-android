package io.novafoundation.nova.feature_governance_impl.presentation.common.models

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId

data class TrackModel(val trackId: TrackId, val name: String, val icon: Icon)
