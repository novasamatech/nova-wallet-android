package io.novafoundation.nova.feature_governance_impl.presentation.referenda.fullDetails

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize

@Parcelize
class ReferendumFullDetailsPayload(
    val proposer: ReferendumProposerPayload?,
    val approveThreshold: String?,
    val supportThreshold: String?,
    val hash: ByteArray?,
    val deposit: Balance?,
    val turnout: Balance?,
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
