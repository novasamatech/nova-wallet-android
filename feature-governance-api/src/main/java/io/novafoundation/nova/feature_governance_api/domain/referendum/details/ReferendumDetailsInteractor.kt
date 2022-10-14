package io.novafoundation.nova.feature_governance_api.domain.referendum.details

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface ReferendumDetailsInteractor {

    fun referendumDetailsFlow(
        referendumId: ReferendumId,
        chain: Chain,
        voterAccountId: AccountId?,
    ): Flow<ReferendumDetails>

    suspend fun detailsFor(
        preImage: PreImage,
        chain: Chain,
    ): ReferendumCall?
}
