package io.novafoundation.nova.feature_governance_api.domain.referendum.details

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface ReferendumDetailsInteractor {

    fun referendumDetailsFlow(
        referendumId: ReferendumId,
        selectedGovernanceOption: SupportedGovernanceOption,
        voterAccountId: AccountId?,
    ): Flow<ReferendumDetails?>

    suspend fun detailsFor(
        preImage: PreImage,
        chain: Chain,
    ): ReferendumCall?

    suspend fun previewFor(preImage: PreImage): PreimagePreview
}
