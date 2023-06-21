package io.novafoundation.nova.feature_governance_impl.domain.filters

import io.novafoundation.nova.common.utils.OptionsFilter
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview

class ReferendumTypeFilter(val selected: ReferendumType) : OptionsFilter<ReferendumPreview, ReferendumType> {

    override val options: List<ReferendumType>
        get() = ReferendumType.values().toList()

    override fun shouldInclude(model: ReferendumPreview): Boolean {
        val vote = model.referendumVote?.vote
        return when (selected) {
            ReferendumType.ALL -> true
            ReferendumType.VOTED -> vote != null
            ReferendumType.NOT_VOTED -> vote == null
        }
    }
}
