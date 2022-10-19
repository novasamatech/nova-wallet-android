package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.BACKGROUND_TRANSPARENT
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.model.VoterModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.launch

class ReferendumVotersViewModel(
    private val payload: ReferendumVotersPayload,
    private val router: GovernanceRouter,
    private val governanceSharedState: GovernanceSharedState,
    private val externalActions: ExternalActions.Presentation,
    private val addressIconGenerator: AddressIconGenerator,
) : BaseViewModel(), ExternalActions by externalActions {

    val title: String = payload.title

    val votersList = flowOf {
        val chain = getChain()
        val address = chain.addressOf(ByteArray(32))
        val icon = addressIconGenerator.createAddressIcon(ByteArray(32), 24, backgroundColorRes = BACKGROUND_TRANSPARENT)
        List(100) {
            VoterModel(
                AddressModel(address, icon),
                "354 votes",
                "354 to 0.1"
            )
        }
    }

    fun backClicked() {
        router.back()
    }

    fun voterClicked(voter: VoterModel) = launch {
        val chain = getChain()
        val type = ExternalActions.Type.Address(voter.addressModel.address)
        externalActions.showExternalActions(type, chain)
    }

    private suspend fun getChain(): Chain {
        return governanceSharedState.chain()
    }
}
