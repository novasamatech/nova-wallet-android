package io.novafoundation.nova.feature_governance_api.di

import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.DelegateDetailsInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVotersInteractor

interface GovernanceFeatureApi {

    val governanceSourceRegistry: GovernanceSourceRegistry

    val referendaListInteractor: ReferendaListInteractor

    val referendumDetailsInteractor: ReferendumDetailsInteractor

    val referendumVotersInteractor: ReferendumVotersInteractor

    val governanceUpdateSystem: UpdateSystem

    val delegateListInteractor: DelegateListInteractor

    val delegateDetailsInteractor: DelegateDetailsInteractor
}
