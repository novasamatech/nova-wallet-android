package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.votedReferenda

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withLoadingShared
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.Voter
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.account
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.map

class VotedReferendaViewModel(
    private val interactor: ReferendaListInteractor,
    private val governanceSharedState: GovernanceSharedState,
    private val selectedTokenUseCase: TokenUseCase,
    private val governanceRouter: GovernanceRouter,
    private val referendumFormatter: ReferendumFormatter,
    val payload: VotedReferendaPayload,
) : BaseViewModel() {

    private val voter = Voter.account(payload.accountId)

    private val referendaListFlow = interactor.votedReferendaListFlow(voter, payload.onlyRecentVotes)
        .inBackground()
        .shareWhileSubscribed()

    val referendaUiFlow = referendaListFlow.map { referenda ->
        val token = selectedTokenUseCase.currentToken()
        val chain = governanceSharedState.chain()

        referenda.map { referendumFormatter.formatReferendumPreview(it, token, chain) }
    }
        .inBackground()
        .withLoadingShared()
        .shareWhileSubscribed()

    val votedReferendaCount = referendaListFlow.map {
        it.size.format()
    }
        .inBackground()
        .shareWhileSubscribed()

    fun openReferendum(referendum: ReferendumModel) {
        val payload = ReferendumDetailsPayload(referendum.id.value, allowVoting = false)
        governanceRouter.openReferendum(payload)
    }

    fun backClicked() {
        governanceRouter.back()
    }
}
