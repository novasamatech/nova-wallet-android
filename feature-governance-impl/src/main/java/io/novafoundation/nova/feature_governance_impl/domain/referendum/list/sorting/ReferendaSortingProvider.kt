package io.novafoundation.nova.feature_governance_impl.domain.referendum.list.sorting

import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumGroup
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview

interface ReferendaSortingProvider {

    suspend fun getReferendumSorting(group: ReferendumGroup): Comparator<ReferendumPreview>

    suspend fun getGroupSorting(): Comparator<ReferendumGroup>

    suspend fun getReferendumSorting(): Comparator<ReferendumPreview>
}
