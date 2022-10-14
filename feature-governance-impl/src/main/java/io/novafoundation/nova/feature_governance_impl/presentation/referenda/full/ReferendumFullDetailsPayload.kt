package io.novafoundation.nova.feature_governance_impl.presentation.referenda.full

import android.os.Parcelable
import io.novafoundation.nova.core_db.model.AssetAndChainId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize

@Parcelize
class ReferendumFullDetailsPayload(
    val proposer: ReferendumProposerPayload?,
    val voteThreshold: String?,
    val approveThreshold: String?,
    val supportThreshold: String?,
    val hash: ByteArray?,
    val deposit: Balance?,
    val turnout: Balance?,
    val electorate: Balance?,
    val referendumCall: ReferendumCallPayload?,
    val preImage: PreImagePreviewPayload?
) : Parcelable

sealed class ReferendumCallPayload : Parcelable {

    @Parcelize
    class TreasuryRequest(val amount: Balance, val beneficiary: AccountId) : ReferendumCallPayload()
}

sealed class PreImagePreviewPayload : Parcelable {

    @Parcelize
    object TooLong : PreImagePreviewPayload()

    @Parcelize
    class Preview(val preview: String) : PreImagePreviewPayload()
}

@Parcelize
class ReferendumProposerPayload(val accountId: AccountId, val offChainName: String?) : Parcelable
