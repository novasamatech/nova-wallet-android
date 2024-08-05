package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumThreshold
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumVote
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumVotingModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_governance_impl.presentation.view.YourMultiVoteModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

interface ReferendumFormatter {

    fun formatVoting(voting: ReferendumVoting, threshold: ReferendumThreshold?, token: Token): ReferendumVotingModel?

    fun formatReferendumTrack(track: ReferendumTrack, asset: Chain.Asset): ReferendumTrackModel

    fun formatOnChainName(call: GenericCall.Instance): String

    fun formatUnknownReferendumTitle(referendumId: ReferendumId): String

    fun formatStatus(status: ReferendumStatus): ReferendumStatusModel

    fun formatTimeEstimation(status: ReferendumStatus): ReferendumTimeEstimation?

    fun formatId(referendumId: ReferendumId): String

    fun formatUserVote(referendumVote: ReferendumVote, chain: Chain, chainAsset: Chain.Asset): YourMultiVoteModel

    fun formatReferendumPreview(
        referendum: ReferendumPreview,
        token: Token,
        chain: Chain
    ): ReferendumModel
}
