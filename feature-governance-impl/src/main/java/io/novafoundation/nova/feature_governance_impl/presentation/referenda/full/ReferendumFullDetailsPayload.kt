package io.novafoundation.nova.feature_governance_impl.presentation.referenda.full

import android.os.Parcelable
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.PreimagePreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.fullId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.parcelize.Parcelize

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
    class TreasuryRequest(val amount: Balance, val beneficiary: AccountId, val asset: AssetPayload) : ReferendumCallPayload()
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
        is ReferendumCall.TreasuryRequest -> ReferendumCallPayload.TreasuryRequest(
            amount = referendumCall.amount,
            beneficiary = referendumCall.beneficiary,
            asset = referendumCall.chainAsset.fullId.toAssetPayload()
        )
        null -> null
    }
}

fun PreImagePreviewPayload(preimagePreview: PreimagePreview): PreImagePreviewPayload {
    return when (preimagePreview) {
        is PreimagePreview.Display -> PreImagePreviewPayload.Preview(preimagePreview.value)
        PreimagePreview.TooLong -> PreImagePreviewPayload.TooLong
    }
}
