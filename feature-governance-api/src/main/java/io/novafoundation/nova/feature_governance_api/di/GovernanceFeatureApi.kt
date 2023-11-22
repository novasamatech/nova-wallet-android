package io.novafoundation.nova.feature_governance_api.di

import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_governance_api.data.GovernanceStateUpdater
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceAdditionalState
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.DelegateDelegatorsInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.DelegateDetailsInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.ChooseTrackInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVotersInteractor
import io.novafoundation.nova.runtime.state.SelectableSingleAssetSharedState

interface GovernanceFeatureApi {

    val governanceSourceRegistry: GovernanceSourceRegistry

    val referendaListInteractor: ReferendaListInteractor

    val referendumDetailsInteractor: ReferendumDetailsInteractor

    val referendumVotersInteractor: ReferendumVotersInteractor

    val governanceUpdateSystem: UpdateSystem

    val delegateListInteractor: DelegateListInteractor

    val delegateDetailsInteractor: DelegateDetailsInteractor

    val newDelegationChooseTrackInteractor: ChooseTrackInteractor

    val delegateDelegatorsInteractor: DelegateDelegatorsInteractor

    val governanceStateUpdater: GovernanceStateUpdater
}
