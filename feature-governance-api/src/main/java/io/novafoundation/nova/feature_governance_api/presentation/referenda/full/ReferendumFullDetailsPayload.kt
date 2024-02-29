package io.novafoundation.nova.feature_governance_api.presentation.referenda.full

import android.os.Parcelable
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.PreimagePreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
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

fun ReferendumCallPayload(referendumCall: ReferendumCall?): ReferendumCallPayload? {
    return when (referendumCall) {
        is ReferendumCall.TreasuryRequest -> ReferendumCallPayload.TreasuryRequest(referendumCall.amount, referendumCall.beneficiary)
        null -> null
    }
}

fun PreImagePreviewPayload(preimagePreview: PreimagePreview): PreImagePreviewPayload {
    return when (preimagePreview) {
        is PreimagePreview.Display -> PreImagePreviewPayload.Preview(preimagePreview.value)
        PreimagePreview.TooLong -> PreImagePreviewPayload.TooLong
    }
}
